package es.arlabdevelopments.firmador.controller;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import es.arlabdevelopments.firmador.Libreria;

@Controller
public class ApplicationController {

    Logger logger = Logger.getLogger("Pruebas SpringBoot");

    @Autowired
    private Peticiones p;

    @Autowired
    private FuncionesAuxiliares faux;

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
        
                

        //genero el json para el participante pero aun le falta el proof para enviarlo a la API       

        String jsonResponseString = faux.generaionJSONParticipante( verifiableId,  verifiableSubjectId,  legalName, headquarterAddress,  legalAddress, verifiableIdLRN);
        logger.info("Valor de el json para el participante sin proof: " + jsonResponseString);
        model.addAttribute("jsonResponse", jsonResponseString);
        model.addAttribute("verifiableIdParticipant", verifiableId);


        //genero el numero de registro legal 
        logger.info("valor de el verifiableIDLRN" + verifiableIdLRN);
        String LRN=faux.generationLRN(registrationNumber, verifiableIdLRN, verifiableSubjectIdLRN, lrnType);
        logger.info("Valor de la respuesta a la peticion de numero de registro: " + LRN);
        model.addAttribute("lrn", LRN);


        return "terminosYcondiciones"; 
      
    }


    
    @PostMapping("/termsAndConditions")
     public String handleTerms( 
        @RequestParam("lrn") String lrnType,
        @RequestParam("json") String json,
        @RequestParam("verifiableIdTerminos") String verifiableIdTerminos,
        @RequestParam("credentialSubjectIdTerminos") String credentialSubjectIdTerminos,
        @RequestParam("verifiableIdParticipant") String verifiableIdParticipant,
        Model model) throws JsonProcessingException{

            
            //genero el json para terminos y condiciones pero sin proof
            String jsonSinProof=faux.generationJSONTerminos(verifiableIdTerminos,credentialSubjectIdTerminos);
            logger.info("json sin proof para la peticion de terminos y condiciones: " + jsonSinProof);


            model.addAttribute("tYc", jsonSinProof);
            model.addAttribute("verifiableId", verifiableIdTerminos);
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
        @RequestParam("verifiableIdTerminos") String verifiableIdTerminos,
        @RequestParam("verifiableIdParticipant") String verifiableIdParticipant,
        @RequestParam("lrn") String credecialNumeroDeRegitro,
        @RequestParam("tYc") String tYc,
       
        Model model) throws IOException {

            
            if (file.isEmpty()) {
                logger.info("El archivo no se ha subido correctamente.");     
                
            }

            File f = faux.creaFichero(file);  
            model.addAttribute("aliases", Libreria.comprobarAlias(f));
            logger.info("Nombre del fichero: " + f.getName());
            logger.info("Valor del alias: " + alias);
            logger.info("Valor de la contraseña: " + contrasena);
            logger.info("Contenido del fichero: " + Base64.getEncoder().encodeToString(Files.readAllBytes(f.toPath())));

            String privateKey = "-----BEGIN PRIVATE KEY-----" +
                    Base64.getEncoder().encodeToString(Libreria.clave(alias, contrasena, f).getEncoded()) + "-----END PRIVATE KEY-----";

            logger.info("Valor de la clave privada: " + privateKey);
            logger.info("Valor del json: " + json);



            //añado el proof al json de participante
            String dev = p.httpPetition(privateKey, json);

            //añado el proof al json de terminos y condiciones
            String devTyC_proof=p.httpPetition(privateKey, tYc);
            logger.info("Valor del json de terminos y condiciones con proof: " + devTyC_proof);

            //añado el contexto al json de terminos y condiciones
            String  devTyC_proof_completo=faux.anadirVerifiablePresentattionTyC(devTyC_proof);

            //llamada a la API para consguir la credencial de terminos y condiciones
            String credencialTerminosYCondiciones=p.httpPetitionTerminos(devTyC_proof_completo,verifiableIdTerminos);
            logger.info("Valor de la credencial de terminos y condicion: " + credencialTerminosYCondiciones);

            //genero ya el json para el participante uniendolo con las otras dos credenciales
            String jsonParticipante=faux.combineJson( credecialNumeroDeRegitro, devTyC_proof, dev);
            logger.info("Valor del json combine: " + jsonParticipante);

            //llamada a la API pra conseguir la credencial de participante
            String credencialParticipante=p.httpPetitionTerminos(jsonParticipante,verifiableIdParticipant);

        
            model.addAttribute("credencialTerminosYCondiciones", credencialTerminosYCondiciones);
            model.addAttribute("credencialParticipante", credencialParticipante);
            model.addAttribute("credecialNumeroDeRegitro", credecialNumeroDeRegitro);

            return "muestraJws"; 



    }



    
    




}