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


import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;

@Controller
public class ApplicationController {
    Logger logger = Logger.getLogger("Firmador SpringBoot");

    private final ObjectMapper objectMapper = new ObjectMapper();


    @GetMapping("/")
    public String handleInit() {
        return "index";
    }

    //envio al formulario de participante
    @GetMapping("/createParticipant")
    public String handleUploadPre(){

        return "formularioParticipante";
    }


    //recogida de datos de el participante
    @PostMapping("/createParticipant")
    public String handleForm(
            @RequestParam("legal-name") String legalName,
            @RequestParam("registration-number") String registrationNumber,
            @RequestParam("headquarter-address") String headquarterAddress,
            @RequestParam("legal-address") String legalAddress,
            @RequestParam("parent-organization") String parentOrganization,
            @RequestParam("sub-organization") String subOrganization,
            Model model) throws IOException {
        
 Map<String, Object> jsonResponse = new HashMap<>();

        // Definir "@context"
        jsonResponse.put("@context", new String[]{
                "https://www.w3.org/2018/credentials/v1",
                "https://w3id.org/security/suites/jws-2020/v1",
                "https://registry.lab.gaia-x.eu/development/api/trusted-shape-registry/v1/shapes/jsonld/trustframework#"
        });

        // Definir "type"
        jsonResponse.put("type", new String[]{"VerifiableCredential"});

        // Definir "id"
        jsonResponse.put("id", "https://arlabdevelopments.com/.well-known/ArsysParticipant.json");

        // Definir "issuer" - Coloca aquí el valor correcto, como el ID del emisor
        jsonResponse.put("issuer", "did:web:arlabdevelopments.com");

        // Definir "issuanceDate"
        jsonResponse.put("issuanceDate", Instant.now().toString());

        // Crear el credentialSubject
        Map<String, Object> credentialSubject = new HashMap<>();
        credentialSubject.put("type", "gx:LegalParticipant");
        credentialSubject.put("gx:legalName", legalName);

        // Crear gx:legalRegistrationNumber
        Map<String, Object> legalRegistrationNumber = new HashMap<>();
        legalRegistrationNumber.put("id", "https://arlabdevelopments.com/.well-known/legalRegistrationNumberVC.json");
        credentialSubject.put("gx:legalRegistrationNumber", legalRegistrationNumber);

        // Crear gx:headquarterAddress
        Map<String, String> headquarterAddressInfo = new HashMap<>();
        headquarterAddressInfo.put("gx:countrySubdivisionCode", headquarterAddress);
        credentialSubject.put("gx:headquarterAddress", headquarterAddressInfo);

        // Crear gx:legalAddress
        Map<String, String> legalAddressInfo = new HashMap<>();
        legalAddressInfo.put("gx:countrySubdivisionCode", legalAddress);
        credentialSubject.put("gx:legalAddress", legalAddressInfo);

        // Añadir ID al credentialSubject
        credentialSubject.put("id", "https://arlabdevelopments.com/.well-known/ArsysParticipant.json");

        // Añadir credentialSubject al jsonResponse
        jsonResponse.put("credentialSubject", credentialSubject);
        
        // Convertir el JSON a String utilizando ObjectMapper
        String jsonResponseString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonResponse);
        

        logger.info("Valor de el json: " + jsonResponseString);

        model.addAttribute("jsonResponse", jsonResponseString);
        return "introduceCertificado"; 
      
    }



    @PostMapping("/upload")
    public String handleUpload(
            @RequestParam("archivoCertificado") MultipartFile file,
            @RequestParam("aliasSeleccionado") String alias,
            @RequestParam("contrasenaCertificado") String contrasena,
            @RequestParam("jsonResponse") String jsonResponseString,
            Model model) throws IOException {

        
        //Libreria.comprobarAlias(f);

        if (file.isEmpty()) {
        logger.info("El archivo no fue enviado correctamente.");
        }    

        File f=creaFichero(file);

        ArrayList<String> aliasLibreria=Libreria.comprobarAlias(f);
        model.addAttribute("aliases", Libreria.comprobarAlias(f));
        logger.info("Valor del aliasLibreria: " + aliasLibreria.toString());
        
        logger.info("Valor del archivoCertificado: " + f.getName());
        logger.info("Valor del aliasSeleccionado: " + alias);
        logger.info("Valor del contrasenaCertificado: " + contrasena);
        logger.info("Valor del jsonResponse: " + jsonResponseString);   


        logger.info("Nombre del fichero: " + f.getName());
        try {
            logger.info("Contenido del fichero: " + Base64.getEncoder().encodeToString(Files.readAllBytes(f.toPath())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        logger.info("Ha llegado aqui: " );
        
        String privateKey = "-----BEGIN PRIVATE KEY-----" +
                Base64.getEncoder().encodeToString(Libreria.clave(alias, contrasena, f).getEncoded()) + "-----END PRIVATE KEY-----";

        logger.info("Valor de la clave privada: " + privateKey);
        logger.info("Valor del json: " + jsonResponseString);

        String dev = httpPetition(privateKey, jsonResponseString);
        model.addAttribute("data", dev);

        return "muestra_jws";



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