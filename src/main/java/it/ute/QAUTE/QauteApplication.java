package it.ute.QAUTE;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QauteApplication {

	public static void main(String[] args) {
		SpringApplication.run(QauteApplication.class, args);
	}

}
