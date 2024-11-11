import jsonld from 'jsonld/lib/jsonld.js';
import { CompactSign, importPKCS8 } from 'jose'
import crypto from 'crypto'
import express from 'express';
import bodyParser from 'body-parser';
import fs from 'fs';
import https from 'https';

import vcCtx from "./public/credentials_v1_context.json" with { type: "json" };
import jwsCtx from "./public/jws2020_v1_context.json" with { type: "json" };
import trustframeworkCtx from "./public/trustframework_context.json" with { type: "json" };

let privateKey  = process.env.PRIVATEKEY;
let certificate = process.env.CERTIFICATE;
console.log("CERTIFICATE:", certificate);
console.log("PRIVATE KEY:", privateKey);

var credentials = {key: privateKey, cert: certificate};
// Aquí monto el API REST para crear el JWS según la petición que reciba
const app = express();
app.use(express.json());
app.use(bodyParser.urlencoded({ extended: false }));
var httpsServer = https.createServer(credentials, app);

const PORT_HTTPS = 3000;

httpsServer.listen(PORT_HTTPS,() => {
  console.log("Server HTTPS Listening on PORT:", PORT_HTTPS);
});

app.post("/jws", async (request, response) => {
  let verifiableCredential = JSON.parse(request.body.json);
  let privateKey = request.body.pem.trim();
  console.log(await createJWS(privateKey, verifiableCredential));
  response.send(await createSignedJSON(privateKey, verifiableCredential));
});

//Funcion de normalización del payload
async function normalize(payload) {
  return await jsonld.canonize(payload, {
    algorithm: 'URDNA2015',
    format: 'application/n-quads',
    documentLoader: staticDocumentLoader
  })
}
//Función intermedia para el hasheo del payload
function hash(payload) {
  return computePayloadHash(payload)
}
// Funcion de hasheo del payload
async function computePayloadHash(payload) {
  const encoder = new TextEncoder()
  const data = encoder.encode(payload)
  const digestBuffer = await crypto.subtle.digest('SHA-256', data)
  const digestArray = new Uint8Array(digestBuffer)
  return Array.from(digestArray)
    .map(b => b.toString(16).padStart(2, '0'))
    .join('')
}
// Constantes de contexto
const CACHED_CONTEXTS = {
  "https://www.w3.org/2018/credentials/v1": vcCtx,
  "https://w3id.org/security/suites/jws-2020/v1": jwsCtx,
  "https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#": trustframeworkCtx
}
//Carga estatica de documentos
const staticDocumentLoader = async url => {
  if (url in CACHED_CONTEXTS) {
    return {
      contextUrl: undefined,
      document: CACHED_CONTEXTS[url],
      documentUrl: url
    }
  }
  const document = await (await fetch(url)).json()
  return {
    contextUrl: undefined,
    document,
    documentUrl: url
  }
}

// Función asincrona para la creación del JWS al con los parametros pasados
async function createJWS(pem, json) {
  const rsaPrivateKey = await importPKCS8(pem, 'PS256')

  const credentialNormalized = await normalize(json)
  const credentialHashed = await hash(credentialNormalized)
  const credentialEncoded = new TextEncoder().encode(credentialHashed)

  return await new CompactSign(credentialEncoded).setProtectedHeader({
    alg: 'PS256',
    b64: false, crit: ['b64']
  }).sign(rsaPrivateKey);
}
async function createSignedJSON(pem, json) {

  console.log(json)
  const signedVerifiableCredential =
  {
    ...json,
    proof: {
      type: 'JsonWebSignature2020',
      created: new Date().toISOString(),
      proofPurpose: 'assertionMethod',
      verificationMethod: "did:web:arlabdevelopments.com#JWK2020-RSA",
      jws: await createJWS(pem, json)
    }
  };
  return await signedVerifiableCredential;
}
