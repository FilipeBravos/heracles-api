package br.com.heracles.heracles_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/** @EnableScheduling liga o job diario da regua de cobranca (ver InadimplenciaScheduler). */
@SpringBootApplication
@EnableScheduling
public class HeraclesApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(HeraclesApiApplication.class, args);
	}

}
