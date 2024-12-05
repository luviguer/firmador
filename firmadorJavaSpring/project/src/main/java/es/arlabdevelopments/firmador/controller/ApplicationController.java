package es.arlabdevelopments.firmador.controller;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Key;
import java.util.Base64;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import es.arlabdevelopments.firmador.Libreria;

@Controller
public class ApplicationController {

    Logger logger = Logger.getLogger("Pruebas SpringBoot 4Pasos");

    @Autowired
    private Peticiones p;

    @Autowired
    private FuncionesAuxiliares_4Pasos faux;

    @GetMapping("/")
    public String handleInit() {
        return "menuPrincipal";
    }

    @GetMapping("/startParticipant")
    public String handlePrinicpal() {
        return "4pasos/incicioDePasos";
    }

    

    @GetMapping("/createParticipant")
    public String handleParticipant() {
        return "4pasos/formularioParticipante";
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

        //compruebo que se me ha generado el numero de registro si me han introducido uno valido lanzo un mensaje de error 
        if ("Numero no valido".equals(LRN)) {
            logger.warning("Número de registro legal inválido.");
            model.addAttribute("errorMessage", "El número de registro legal es inválido. Por favor, revise los datos ingresados.");
            model.addAttribute("legalName", legalName);
            model.addAttribute("registrationNumber", registrationNumber);
            model.addAttribute("verifiableIdLRN", verifiableIdLRN);
            model.addAttribute("verifiableSubjectIdLRN", verifiableSubjectIdLRN);
            model.addAttribute("headquarterAddress", headquarterAddress);
            model.addAttribute("legalAddress", legalAddress);
            model.addAttribute("verifiableId", verifiableId);
            model.addAttribute("verifiableSubjectId", verifiableSubjectId);
            model.addAttribute("lrnType", lrnType);
            return "formularioParticipante";
        }
    

        //estructuro bien el json y lo pongo bonito
        String LRN_beutiful=faux.formatJson(LRN);
        logger.info("Valor de la respuesta a la peticion de numero de registro: " + LRN_beutiful);
        model.addAttribute("lrn", LRN_beutiful);


        return "4pasos/terminosYcondiciones"; 
      
    }


    @GetMapping("/data")
    public String handlegetdata() {
        return "4pasos/terminosYcondiciones";
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

            logger.info("Valor del json sin proof del participante: " + json);


            model.addAttribute("tYc", jsonSinProof);
            model.addAttribute("verifiableId", verifiableIdTerminos);
            model.addAttribute("jsonResponse", json);
            model.addAttribute("lrn", lrnType);
            return "4pasos/peticionDatos";

     }

    @GetMapping("/termsAndConditions")
    public String handlegetterimos() {
        return "4pasos/peticionDatos";
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

        
                //gestiono si la contrasñea es incorrecta
                Key clavePrivada = Libreria.clave(alias, contrasena, f);
                if (clavePrivada == null) {
                    logger.info("No se pudo obtener la clave privada. Verifica el alias y la contraseña.");

                    model.addAttribute("jsonResponse", json);
                    model.addAttribute("lrn", credecialNumeroDeRegitro);
                    model.addAttribute("tYc", tYc);
                    model.addAttribute("verifiableIdTerminos", verifiableIdTerminos);
                    model.addAttribute("verifiableIdParticipant", verifiableIdParticipant);
                    model.addAttribute("errorMessage", "Contraseña incorrecta");
                    
                
                    return "4pasos/peticionDatos"; 
                }
        


            String privateKey = "-----BEGIN PRIVATE KEY-----" +
                    Base64.getEncoder().encodeToString(clavePrivada.getEncoded()) +
                    "-----END PRIVATE KEY-----";                   
            logger.info("Valor de la clave privada: " + privateKey);
            logger.info("Valor del json sin proof del participante: " + json);



            //añado el proof al json de participante
            String dev = p.httpPetition(privateKey, json);


            logger.info("Valor del json de terminos y condiciones sin proof: " + tYc);
            //añado el proof al json de terminos y condiciones
            String devTyC_proof=p.httpPetition(privateKey, tYc);
            logger.info("Valor del json de terminos y condiciones con proof: " + devTyC_proof);


            //añado el contexto al json de terminos y condiciones
            String  devTyC_proof_completo=faux.anadirVerifiablePresentattionTyC(devTyC_proof);


            //llamada a la API para consguir la credencial de terminos y condiciones
            String credencialTerminosYCondiciones=p.httpPetitionTerminos(devTyC_proof_completo,verifiableIdTerminos);
            logger.info("Valor de la credencial de terminos y condicion: " + credencialTerminosYCondiciones);


                //compruebo que la llamada para terminos y condiciones ha sido con exito 
                if (credencialTerminosYCondiciones.contains("Error")) {

                            logger.warning("el error es: "+credencialTerminosYCondiciones);
                            model.addAttribute("errorMessage", credencialTerminosYCondiciones);
                            model.addAttribute("jsonResponse", json);
                            model.addAttribute("lrn", credecialNumeroDeRegitro);
                            model.addAttribute("tYc", tYc);
                            model.addAttribute("verifiableIdTerminos", verifiableIdTerminos);
                            model.addAttribute("verifiableIdParticipant", verifiableIdParticipant);
                            return "4pasos/peticionDatos";
                        }


            //genero ya el json para el participante uniendolo con las otras dos credenciales
            String jsonParticipante=faux.combineJson( credecialNumeroDeRegitro, devTyC_proof, dev);
            logger.info("Valor del json combine: " + jsonParticipante);


            //llamada a la API pra conseguir la credencial de participante
            String credencialParticipante=faux.formatJson(dev);

                //compruebo que la llamada para participante ha sido con exito 
                if (credencialParticipante.contains("Error")) {

                    logger.warning("el error es: "+credencialParticipante);
                    model.addAttribute("errorMessage", credencialTerminosYCondiciones);
                    model.addAttribute("jsonResponse", json);
                    model.addAttribute("lrn", credecialNumeroDeRegitro);
                    model.addAttribute("tYc", tYc);
                    model.addAttribute("verifiableIdTerminos", verifiableIdTerminos);
                    model.addAttribute("verifiableIdParticipant", verifiableIdParticipant);
                    return "4pasos/peticionDatos";
                }

        



           model.addAttribute("credencialTerminosYCondiciones", faux.formatJson(devTyC_proof));
           model.addAttribute("credencialParticipante",faux.formatJson(dev) );
           model.addAttribute("credecialNumeroDeRegitro", credecialNumeroDeRegitro);

            return "4pasos/muestraJws"; 


    }



    



}