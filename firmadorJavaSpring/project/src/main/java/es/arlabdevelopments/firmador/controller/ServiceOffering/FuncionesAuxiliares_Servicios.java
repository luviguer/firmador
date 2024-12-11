package es.arlabdevelopments.firmador.controller.ServiceOffering;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;


@Service
public class FuncionesAuxiliares_Servicios {


    public  String generationService(
        String verifiableId,
        String credentialSubjectId,
        String requestType,
        String accessType,
        String formatType,
        String termsUrl,
        String termsHash) throws JsonProcessingException {

            // Fecha de emisión actual en formato ISO-8601
            String issuanceDate = Instant.now().toString();
            String issuer="did:web:arlabdevelopments.com";

            // Generar el JSON con los datos de entrada
            String verifiableCredentialJson = String.format(
                "{\n" +
                "  \"@context\": [\n" +
                "    \"https://www.w3.org/2018/credentials/v1\",\n" +
                "    \"https://w3id.org/security/suites/jws-2020/v1\",\n" +
                "    \"https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#\"\n" +
                "  ],\n" +
                "  \"type\": \"VerifiableCredential\",\n" +
                "  \"id\": \"%s\",\n" +
                "  \"issuer\": \"%s\",\n" +
                "  \"issuanceDate\": \"%s\",\n" +
                "  \"credentialSubject\": {\n" +
                "    \"type\": \"gx:ServiceOffering\",\n" +
                "    \"gx:providedBy\": {\n" +
                "      \"id\": \"%s\"\n" +
                "    },\n" +
                "    \"gx:policy\": \"\",\n" +
                "    \"gx:termsAndConditions\": {\n" +
                "      \"gx:URL\": \"%s\",\n" +
                "      \"gx:hash\": \"%s\"\n" +
                "    },\n" +
                "    \"gx:dataAccountExport\": {\n" +
                "      \"gx:requestType\": \"%s\",\n" +
                "      \"gx:accessType\": \"%s\",\n" +
                "      \"gx:formatType\": \"%s\"\n" +
                "    },\n" +
                "    \"id\": \"%s\"\n" +
                "  }\n" +
                "}",
                verifiableId,                                    // ID principal
                issuer,                                          // Issuer (desde variable de entorno)
                issuanceDate,                                    // Fecha de emisión
                credentialSubjectId,                             // ID del credentialSubject
                termsUrl,                                        // URL de términos y condiciones
                termsHash,                                       // Hash de términos y condiciones
                requestType,                                     // Tipo de solicitud (requestType)
                accessType,                                      // Tipo de acceso (accessType)
                formatType,                                      // Tipo de formato (formatType)
                credentialSubjectId                              // ID dentro de credentialSubject
            );

            return verifiableCredentialJson;
}


    public File creaFichero(String file) {
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

        public File creaFichero(MultipartFile file) {
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



        //funcion para poner el json bien estructurado y bonito
        public String formatJson(String inputJson) {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT); 

            try {
                JsonNode jsonNode = objectMapper.readTree(inputJson);

                return objectMapper.writeValueAsString(jsonNode);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Invalid JSON: " + e.getMessage(), e);
            }
        }




        public String combineJson(String json1, String json2, String json3,String json4) throws JsonProcessingException {


            ObjectMapper objectMapper = new ObjectMapper();

            Map<String, Object> cred1 = objectMapper.readValue(json1, Map.class);
            Map<String, Object> cred2 = objectMapper.readValue(json2, Map.class);
            Map<String, Object> cred3 = objectMapper.readValue(json3, Map.class);
            Map<String, Object> cred4 = objectMapper.readValue(json4, Map.class);


            Map<String, Object> presentation = new LinkedHashMap<>();
            presentation.put("@context", "https://www.w3.org/2018/credentials/v1");
            presentation.put("type", "VerifiablePresentation");
            presentation.put("verifiableCredential", Arrays.asList(cred1, cred2, cred3, cred4));

            return objectMapper.writeValueAsString(presentation);

    }

    public static boolean esNumeroDeRegistro(String jsonString) {
        // Buscamos si el campo "credentialSubject" contiene el tipo "gx:legalRegistrationNumber"
        String searchString = "\"type\"\\s*:\\s*\"gx:legalRegistrationNumber\"";
        
        // Usamos el método 'matches' con la expresión regular para verificar si el tipo es el correcto
        return jsonString.matches("(?s).*" + searchString + ".*");
    }

    public static boolean esParticipante(String jsonString) {
        // Buscamos si el campo "credentialSubject" contiene "gx:headquarterAddress"
        String searchString = "\"gx:headquarterAddress\"\\s*:\\s*\\{";
    
        // Usamos el método 'matches' para verificar si el JSON contiene "gx:headquarterAddress"
        return jsonString.matches("(?s).*" + searchString + ".*");
    }


    public static boolean esTerminosYCondiciones(String jsonString) {
        // Buscamos si el campo "credentialSubject" contiene el tipo "gx:GaiaXTermsAndConditions"
        String searchString = "\"type\"\\s*:\\s*\"gx:GaiaXTermsAndConditions\"";
        
        // Usamos el método 'matches' con la expresión regular para verificar si el tipo es el correcto
        return jsonString.matches("(?s).*" + searchString + ".*");
    }

        
    }
