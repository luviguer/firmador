
package es.arlabdevelopments.firmador.controller;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import org.springframework.stereotype.Service;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

@Service
public class Peticiones {

    


        Logger logger = Logger.getLogger("Pruebas SpringBoot Peticiones");
        
        //peticion para generar el proof 
        public String httpPetition(String pem, String json){
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
        public String httpPetitionRegistrationNumber(String jsonResponseString, String url) {
        
            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");    
            @SuppressWarnings("deprecation")
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    jsonResponseString
                );



            Request request = new Request.Builder()
                    .url(System.getenv("API_NumRegistration")+url)
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


        //Peticion para terminos y condiciones y uso la misma para el participante
        public String httpPetitionTerminos(String jsonResponseString,String verifiableId) throws UnsupportedEncodingException {


            String encodedString = URLEncoder.encode(verifiableId, StandardCharsets.UTF_8.toString());

            OkHttpClient client = new OkHttpClient();
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");    
            @SuppressWarnings("deprecation")
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/json; charset=utf-8"),
                    jsonResponseString
                );



            Request request = new Request.Builder()
                    .url(System.getenv("API_TerminosYParticipante")+encodedString)
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