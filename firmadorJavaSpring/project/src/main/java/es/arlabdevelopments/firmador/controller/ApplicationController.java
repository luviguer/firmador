package es.arlabdevelopments.firmador.controller;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import java.util.LinkedHashMap;
import java.util.Arrays;

import es.arlabdevelopments.firmador.Libreria;


@Controller
public class ApplicationController {

    Logger logger = Logger.getLogger("Pruebas SpringBoot");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/")
    public String handleInit() {
        return "index";
    }

    @GetMapping("/startParticipant")
    public String handlePrinicpal() {
        return "principal";
    }

    

    @GetMapping("/createParticipant")
    public String handleParticipant() {
        return "formularioParticipante";
    }


    @PostMapping("/data")
    public String handleForm(
            @RequestParam("legal-name") String legalName,
            @RequestParam("registration-number") String registrationNumber,
            @RequestParam("verifiable-id-LRN") String verifiableIdLRN,
            @RequestParam("credential-subject-id-LRN") String verifiableSubjectIdLRN,
            @RequestParam("headquarter-address") String headquarterAddress,
            @RequestParam("legal-address") String legalAddress,
            @RequestParam("verifiable-id") String verifiableId,
            @RequestParam("credential-subject-id") String verifiableSubjectId,
            @RequestParam("lrn-type") String lrnType,
            Model model) throws IOException {
        
                

        //GENERO EL JSON PARA EL PARTICIPANTE PERO SIN PROOF        

        String jsonResponseString = generaionJSONParticipante( verifiableId,  verifiableSubjectId,  legalName, headquarterAddress,  legalAddress, verifiableIdLRN);


        logger.info("Valor de el json para el participante sin proof: " + jsonResponseString);
        model.addAttribute("jsonResponse", jsonResponseString);
        model.addAttribute("verifiableIdParticipant", verifiableId);





        //GENERO LRN

        logger.info("valor de el verifiableIDLRN" + verifiableIdLRN);

        String LRN=generationLRN(registrationNumber, verifiableIdLRN, verifiableSubjectIdLRN, lrnType);
        logger.info("Valor de la respuesta a la peticion de numero de registro: " + LRN);
        model.addAttribute("lrn", LRN);


        return "terminosYcondiciones"; 
      
    }


    
    @PostMapping("/termsAndConditions")
     public String handleTerms( 
        @RequestParam("lrn") String lrnType,
        @RequestParam("json") String json,
        @RequestParam("verifiableId") String verifiableId,
        @RequestParam("credentialSubjectId") String credentialSubjectId,
        @RequestParam("verifiableIdParticipant") String verifiableIdParticipant,
        Model model) throws JsonProcessingException{

            
            
            String jsonSinProof=generationJSONTerminos(verifiableId,credentialSubjectId);
            logger.info("json sin proof para la peticion de terminos y condiciones: " + jsonSinProof);


            model.addAttribute("tYc", jsonSinProof);
            model.addAttribute("verifiableId", verifiableId);
            model.addAttribute("jsonResponse", json);
            model.addAttribute("lrn", lrnType);
            return "peticionDatos";

     }


    @PostMapping("/upload")
    public String handleUpload(
        @RequestParam("archivo") MultipartFile file,
        @RequestParam("seleccion") String alias,
        @RequestParam("contrasena") String contrasena,
        @RequestParam("json") String json,
        @RequestParam("verifiableId") String verifiableId,
        @RequestParam("verifiableIdParticipant") String verifiableIdParticipant,
        @RequestParam("lrn") String lrn,
        @RequestParam("tYc") String tYc,
       
        Model model) throws IOException {

            if (file.isEmpty()) {
                logger.info("El archivo no se ha subido correctamente.");     
                
            }

            File f = creaFichero(file);  

            model.addAttribute("aliases", Libreria.comprobarAlias(f));

            logger.info("Nombre del fichero: " + f.getName());
            logger.info("Valor del alias: " + alias);
            logger.info("Valor de la contraseña: " + contrasena);
            logger.info("Contenido del fichero: " + Base64.getEncoder().encodeToString(Files.readAllBytes(f.toPath())));

            String privateKey = "-----BEGIN PRIVATE KEY-----" +
                    Base64.getEncoder().encodeToString(Libreria.clave(alias, contrasena, f).getEncoded()) + "-----END PRIVATE KEY-----";

            logger.info("Valor de la clave privada: " + privateKey);
            logger.info("Valor del json: " + json);


            String dev = httpPetition(privateKey, json);
            String devTyC_proof=httpPetition(privateKey, tYc);

             logger.info("Valor del json fcon proof: " + devTyC_proof);
            String  devTyC_proof_completo=anadirVerifiablePresentattionTyC(devTyC_proof);
            String devTyC=httpPetitionTerminos(devTyC_proof_completo,verifiableId);

            logger.info("Valor del json final terminos y condicion: " + devTyC);

            String jsonParticipante=combineJson( lrn, devTyC_proof, dev);

            logger.info("Valor del json combine: " + jsonParticipante);

            String jsonParticipanteFinal=httpPetitionTerminos(jsonParticipante,verifiableIdParticipant);

        
            model.addAttribute("tYc", devTyC);
            model.addAttribute("data", jsonParticipanteFinal);
            model.addAttribute("lrn", lrn);

            return "muestraJws"; 



    }



    

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private File creaFichero(String file) {
        File f = null;
        try {
            f = File.createTempFile("cert", "p12");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(Base64.getDecoder().decode(file));
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return f;
    }

    private File creaFichero(MultipartFile file) {
        File f = null;
        try {
            f = File.createTempFile("cert", "p12");
            FileOutputStream fos = new FileOutputStream(f);
            fos.write(file.getBytes());
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return f;
    }


    
    private String generationLRN(String registrationNumber,String verifiableId,String credentialSubjectId,String lrnType) throws JsonProcessingException{

    String jsonNumero="";

        if(lrnType.equals("EORI")){
             jsonNumero = String.format(
                "{\n" +
                "  \"type\": \"gx:legalRegistrationNumber\",\n" +
                "  \"id\": \"%s\",\n" +
                "  \"gx:EORI\": \"%s\"\n" +
                "}",
                verifiableId, registrationNumber 
                );
        }
        if(lrnType.equals("vatID")){
              jsonNumero = String.format(
                "{\n" +
                "  \"type\": \"gx:legalRegistrationNumber\",\n" +
                "  \"id\": \"%s\",\n" +
                "  \"gx:vatID\": \"%s\"\n" +
                "}",
                verifiableId, registrationNumber 
                );
        }
        if(lrnType.equals("leiCode")){
              jsonNumero = String.format(
                "{\n" +
                "  \"type\": \"gx:legalRegistrationNumber\",\n" +
                "  \"id\": \"%s\",\n" +
                "  \"gx:leiCode\": \"%s\"\n" +
                "}",
                verifiableId, registrationNumber 
                );
        }

        logger.info("Valor del jsonLRN: " + jsonNumero);
        


        String respuesta="no se ha generado";

        try {
             String encodedString = URLEncoder.encode(verifiableId, StandardCharsets.UTF_8.toString());
             respuesta= httpPetitionRegistrationNumber(jsonNumero,encodedString);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

      
        return respuesta;



    }




    private String generationJSONTerminos(String verifiableId,String credentialSubjectId) throws JsonProcessingException{

        String issuanceDate = Instant.now().toString();
        String issuer = "did:web:arlabdevelopments.com";

        String verifiableCredentialJson = String.format(
            "{\n" +
            "  \"@context\": [\n" +
            "    \"https://www.w3.org/2018/credentials/v1\",\n" +
            "    \"https://w3id.org/security/suites/jws-2020/v1\",\n" +
            "    \"https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#\"\n" +
            "  ],\n" +
            "  \"type\": \"VerifiableCredential\",\n" +
            "  \"issuanceDate\": \"%s\",\n" +
            "  \"credentialSubject\": {\n" +
            "    \"@context\": \"https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#\",\n" +
            "    \"type\": \"gx:GaiaXTermsAndConditions\",\n" +
            "    \"gx:termsAndConditions\": \"The PARTICIPANT signing the Self-Description agrees as follows:\\n- to update its descriptions about any changes, be it technical, organizational, or legal - especially but not limited to contractual in regards to the indicated attributes present in the descriptions.\\n\\nThe keypair used to sign Verifiable Credentials will be revoked where Gaia-X Association becomes aware of any inaccurate statements in regards to the claims which result in a non-compliance with the Trust Framework and policy rules defined in the Policy Rules and Labelling Document (PRLD).\",\n" +
            "    \"id\": \"%s\"\n" +
            "  },\n" +
            "  \"issuer\": \"%s\",\n" +
            "  \"id\": \"%s\"\n" +
            "}",
            issuanceDate, credentialSubjectId, issuer, verifiableId
        );

         

        

    return verifiableCredentialJson;



    }


    private String anadirVerifiablePresentattionTyC(String jsonTyc){

        String finalJson = String.format(
                    "{\n" +
                    "  \"@context\": \"https://www.w3.org/2018/credentials/v1\",\n" +
                    "  \"type\": \"VerifiablePresentation\",\n" +
                    "  \"verifiableCredential\": [\n" +
                    "    %s\n" +
                    "  ]\n" +
                    "}",
                    jsonTyc
                );

        return finalJson;

    }
    



    private String generaionJSONParticipante(String verifiableId, String verifiableSubjectId, String legalName,
                                          String headquarterAddress, String legalAddress, String verifiableIdLRN) throws JsonProcessingException {

            String issuanceDate = Instant.now().toString();
            String issuer = "did:web:gx-notary.arsys.es:v1";

            // Define the credentialSubject map
            Map<String, Object> credentialSubject = new LinkedHashMap<>();
            credentialSubject.put("gx:legalName", legalName);
            credentialSubject.put("gx:headquarterAddress", Map.of("gx:countrySubdivisionCode", headquarterAddress));
            credentialSubject.put("gx:legalRegistrationNumber", Map.of("id", verifiableIdLRN));
            credentialSubject.put("gx:legalAddress", Map.of("gx:countrySubdivisionCode", legalAddress));
            credentialSubject.put("type", "gx:LegalParticipant");
            credentialSubject.put("gx-terms-and-conditions:gaiaxTermsAndConditions", 
            "70c1d713215f95191a11d38fe2341faed27d19e083917bc8732ca4fea4976700"); // Cambiar valor según se necesite
            credentialSubject.put("id", verifiableSubjectId);

            // Define the main JSON structure in the correct order
            Map<String, Object> mainJson = new LinkedHashMap<>();
            mainJson.put("@context", new String[]{
                    "https://www.w3.org/2018/credentials/v1",
                    "https://w3id.org/security/suites/jws-2020/v1",
                    "https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#"
            });
            mainJson.put("type", new String[]{"VerifiableCredential"});
            mainJson.put("id", verifiableId);
            mainJson.put("issuer", issuer);
            mainJson.put("issuanceDate", issuanceDate);
            mainJson.put("credentialSubject", credentialSubject);

            // Convert to JSON string
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(mainJson);
    }



    private String combineJson(String json1, String json2, String json3) throws JsonProcessingException {
            // Crear un ObjectMapper para convertir objetos a JSON
            ObjectMapper objectMapper = new ObjectMapper();

            // Convertir los JSON de entrada en mapas
            Map<String, Object> cred1 = objectMapper.readValue(json1, Map.class);
            Map<String, Object> cred2 = objectMapper.readValue(json2, Map.class);
            Map<String, Object> cred3 = objectMapper.readValue(json3, Map.class);

            // Crear la estructura final
            Map<String, Object> presentation = new LinkedHashMap<>();
            presentation.put("@context", "https://www.w3.org/2018/credentials/v1");
            presentation.put("type", "VerifiablePresentation");
            presentation.put("verifiableCredential", Arrays.asList(cred1, cred2, cred3));

            // Convertir la estructura final a JSON
            return objectMapper.writeValueAsString(presentation);
        }





    
    private String httpPetition(String pem, String json){
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");    
        @SuppressWarnings("deprecation")
        RequestBody body = RequestBody.create(mediaType, "pem= "+ URLEncoder.encode(pem)+" &json=" + URLEncoder.encode(json));
        logger.info("URL API: " + System.getenv("API_PROTOCOL")+"://"+System.getenv("API_HOST")+":"+System.getenv("API_PORT")+"/"+System.getenv("API_URI"));
        Request request = new Request.Builder()
                .url(System.getenv("API_PROTOCOL")+"://"+System.getenv("API_HOST")+":"+System.getenv("API_PORT")+"/"+System.getenv("API_URI"))
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            logger.severe("ha habido un error en al ejecución de la llamada al API");
            throw new RuntimeException(e);
        }
        try {
            return response.body().string();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    //Peticion para el numero de registro
    private String httpPetitionRegistrationNumber(String jsonResponseString, String url) {
       
        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");    
        @SuppressWarnings("deprecation")
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonResponseString
            );



        Request request = new Request.Builder()
                .url("https://gx-notary.arsys.es/v1/registrationNumberVC?"+url)
                .post(body)
                .addHeader("Content-Type", "application/json")  
                .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            logger.severe("ha habido un error en al ejecución de la llamada al API");
            throw new RuntimeException(e);
        }
       
        try {
                    return response.body().string();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }           

          
    }


    //Peticion para terminos y condiciones
    private String httpPetitionTerminos(String jsonResponseString,String verifiableId) throws UnsupportedEncodingException {


        String encodedString = URLEncoder.encode(verifiableId, StandardCharsets.UTF_8.toString());

        OkHttpClient client = new OkHttpClient();
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");    
        @SuppressWarnings("deprecation")
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                jsonResponseString
            );



        Request request = new Request.Builder()
                .url("https://compliance.arlabdevelopments.com/v1-staging/api/credential-offers?vcid="+encodedString)
                .post(body)
                .addHeader("Content-Type", "application/json")  
                .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
        } catch (IOException e) {
            logger.severe("ha habido un error en al ejecución de la llamada al API");
            throw new RuntimeException(e);
        }
       
        try {
                    return response.body().string();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }           

          
    }



    
    


    





}