package es.arlabdevelopments.firmador.controller;
import es.arlabdevelopments.firmador.Libreria;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.Base64;
import java.util.logging.Logger;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.ArrayList;


@Controller
public class ApplicationController {

    Logger logger = Logger.getLogger("Pruebas SpringBoot");
    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping("/")
    public String handleInit() {
        return "index";
    }

    

    @GetMapping("/createParticipant")
    public String handleParticipant() {
        return "formularioParticipante";
    }




    @PostMapping("/data")
    public String handleForm(
            @RequestParam("legal-name") String legalName,
            @RequestParam("registration-number") String registrationNumber,
            @RequestParam("headquarter-address") String headquarterAddress,
            @RequestParam("legal-address") String legalAddress,
            @RequestParam("parent-organization") String parentOrganization,
            @RequestParam("sub-organization") String subOrganization,
            Model model) throws IOException {
        

        Map<String, Object> jsonResponse = new HashMap<>();



        jsonResponse.put("@context", new String[]{
                "https://www.w3.org/2018/credentials/v1",
                "https://w3id.org/security/suites/jws-2020/v1",
                "https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#"
        });
        jsonResponse.put("type", new String[]{"VerifiableCredential"});
        jsonResponse.put("id", "https://arlabdevelopments.com/.well-known/ArsysParticipant.json");
        jsonResponse.put("issuer", "did:web:arlabdevelopments.com");
        jsonResponse.put("issuanceDate", Instant.now().toString());
        Map<String, Object> credentialSubject = new HashMap<>();
        credentialSubject.put("type", "gx:LegalParticipant");
        credentialSubject.put("gx:legalName", legalName);
        Map<String, Object> legalRegistrationNumber = new HashMap<>();
        legalRegistrationNumber.put("id", "https://arlabdevelopments.com/.well-known/legalRegistrationNumberVC.json");
        credentialSubject.put("gx:legalRegistrationNumber", legalRegistrationNumber);
        Map<String, String> headquarterAddressInfo = new HashMap<>();
        headquarterAddressInfo.put("gx:countrySubdivisionCode", headquarterAddress);
        credentialSubject.put("gx:headquarterAddress", headquarterAddressInfo);
        Map<String, String> legalAddressInfo = new HashMap<>();
        legalAddressInfo.put("gx:countrySubdivisionCode", legalAddress);
        credentialSubject.put("gx:legalAddress", legalAddressInfo);
        credentialSubject.put("id", "https://arlabdevelopments.com/.well-known/ArsysParticipant.json");
        jsonResponse.put("credentialSubject", credentialSubject);
        
        String jsonResponseString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponse);       
        logger.info("Valor de el json: " + jsonResponseString);

        model.addAttribute("jsonResponse", jsonResponseString);

        return "peticionDatos"; 
      
    }





    @PostMapping("/upload")
    public String handleUpload(
        @RequestParam("archivo") MultipartFile file,
        @RequestParam("seleccion") String alias,
        @RequestParam("contrasena") String contrasena,
        @RequestParam("json") String json,
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
            model.addAttribute("data", dev);

            return "muestraJws"; 



    }





    

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
    
}