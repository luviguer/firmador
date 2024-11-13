package es.arlabdevelopments.firmador.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FuncionesAuxiliares {
    
    @Autowired
    private Peticiones p;
    
    Logger logger = Logger.getLogger("Pruebas SpringBoot Funciones");

    
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


    
    public String generationLRN(String registrationNumber,String verifiableId,String credentialSubjectId,String lrnType) throws JsonProcessingException{

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
             respuesta= p.httpPetitionRegistrationNumber(jsonNumero,encodedString);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

      
        return respuesta;



    }




    public String generationJSONTerminos(String verifiableId,String credentialSubjectId) throws JsonProcessingException{

        String issuanceDate = Instant.now().toString();

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
            issuanceDate, credentialSubjectId, System.getenv("issuer_Terminos"), verifiableId
        );  

        return verifiableCredentialJson;

    }


    public String anadirVerifiablePresentattionTyC(String jsonTyc){

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
    



    public String generaionJSONParticipante(String verifiableId, String verifiableSubjectId, String legalName,
                                          String headquarterAddress, String legalAddress, String verifiableIdLRN) throws JsonProcessingException {

            String issuanceDate = Instant.now().toString();
            

            Map<String, Object> credentialSubject = new LinkedHashMap<>();
            credentialSubject.put("gx:legalName", legalName);
            credentialSubject.put("gx:headquarterAddress", Map.of("gx:countrySubdivisionCode", headquarterAddress));
            credentialSubject.put("gx:legalRegistrationNumber", Map.of("id", verifiableIdLRN));
            credentialSubject.put("gx:legalAddress", Map.of("gx:countrySubdivisionCode", legalAddress));
            credentialSubject.put("type", "gx:LegalParticipant");
            credentialSubject.put("gx-terms-and-conditions:gaiaxTermsAndConditions",System.getenv("gx-terms-and-conditions:gaiaxTermsAndConditions")); 
            credentialSubject.put("id", verifiableSubjectId);

            Map<String, Object> mainJson = new LinkedHashMap<>();
            mainJson.put("@context", new String[]{
                    "https://www.w3.org/2018/credentials/v1",
                    "https://w3id.org/security/suites/jws-2020/v1",
                    "https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#"
            });
            mainJson.put("type", new String[]{"VerifiableCredential"});
            mainJson.put("id", verifiableId);
            mainJson.put("issuer", System.getenv("issuer_Participant"));
            mainJson.put("issuanceDate", issuanceDate);
            mainJson.put("credentialSubject", credentialSubject);

            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(mainJson);
    }



    public String combineJson(String json1, String json2, String json3) throws JsonProcessingException {


            ObjectMapper objectMapper = new ObjectMapper();

            Map<String, Object> cred1 = objectMapper.readValue(json1, Map.class);
            Map<String, Object> cred2 = objectMapper.readValue(json2, Map.class);
            Map<String, Object> cred3 = objectMapper.readValue(json3, Map.class);

            Map<String, Object> presentation = new LinkedHashMap<>();
            presentation.put("@context", "https://www.w3.org/2018/credentials/v1");
            presentation.put("type", "VerifiablePresentation");
            presentation.put("verifiableCredential", Arrays.asList(cred1, cred2, cred3));

            return objectMapper.writeValueAsString(presentation);

    }



}
