package es.arlabdevelopments.firmador.controller.ServiceOffering;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.Key;
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
import es.arlabdevelopments.firmador.controller.Peticiones;





@Controller
public class ControllerServicios {

    Logger logger = Logger.getLogger("Pruebas SpringBoot Servicios");



    @Autowired
    FuncionesAuxiliares_Servicios fauxService;

    @Autowired
    private Peticiones p;


    @GetMapping("/startServiceOffering")
        public String handlePrinicpal() {
            return "service/formularioService";
        }


    


    @PostMapping("/createService")
     public String handleService( 
        @RequestParam("requestType") String requestType,
        @RequestParam("accessType") String accessType,
        @RequestParam("formatType") String formatType,
        @RequestParam("url") String url,
        @RequestParam("hash") String hash,
        @RequestParam("registrationNumber") String registrationNumber,
        @RequestParam("legalAddress") String legalAddress,
        @RequestParam("headquartersAddress") String headquartersAddress,
        @RequestParam("verifiableId") String verifiableId,
        @RequestParam("credentialSubjectId") String credentialSubjectId,
        Model model) throws JsonProcessingException{



            String jsonServices_sinproof=fauxService.generationService(verifiableId,credentialSubjectId,requestType,accessType,formatType,url,hash);
            logger.info("json servicio sin proof" + jsonServices_sinproof);



            model.addAttribute("jsonServices_sinproof", jsonServices_sinproof);


            return "service/peticionDatosServicios";
     }



    @PostMapping("/uploadService")
    public String handleUpload(
        @RequestParam("archivo") MultipartFile file,
        @RequestParam("seleccion") String alias,
        @RequestParam("contrasena") String contrasena,
        @RequestParam("jsonServices_sinproof") String jsonServices_sinproof,
        Model model) throws IOException {

            
            if (file.isEmpty()) {
                logger.info("El archivo no se ha subido correctamente.");     
                
            }

            File f = fauxService.creaFichero(file);  
            model.addAttribute("aliases", Libreria.comprobarAlias(f));
            logger.info("Nombre del fichero: " + f.getName());
            logger.info("Valor del alias: " + alias);
            logger.info("Valor de la contraseña: " + contrasena);
            logger.info("Contenido del fichero: " + Base64.getEncoder().encodeToString(Files.readAllBytes(f.toPath())));

        
                //gestiono si la contrasñea es incorrecta
                Key clavePrivada = Libreria.clave(alias, contrasena, f);
                if (clavePrivada == null) {
                    logger.info("No se pudo obtener la clave privada. Verifica el alias y la contraseña.");

                    model.addAttribute("jsonServices_sinproof", jsonServices_sinproof);
                   
                
                    return "service/peticionDatosServicios"; 
                }
        


            String privateKey = "-----BEGIN PRIVATE KEY-----" +
                    Base64.getEncoder().encodeToString(clavePrivada.getEncoded()) +
                    "-----END PRIVATE KEY-----";                   
            logger.info("Valor de la clave privada: " + privateKey);
            logger.info("Valor del json sin proof del participante: " + jsonServices_sinproof);



            //añado el proof al json de servicios
            String jsonServices_connproof = p.httpPetition(privateKey, jsonServices_sinproof);

            String jsonServices_connproof_estructurado = fauxService.formatJson(jsonServices_connproof);




           
            model.addAttribute("jsonService", jsonServices_connproof_estructurado);

            return "service/muestraService"; 


    }


    
}