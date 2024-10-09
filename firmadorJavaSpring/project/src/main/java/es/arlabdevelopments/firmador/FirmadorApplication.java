package es.arlabdevelopments.firmador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FirmadorApplication {

	public static void main(String[] args) {
		try {
			SpringApplication.run(FirmadorApplication.class, args);
		} catch (Exception e) {
			e.printStackTrace(); 
		}
	}
}




