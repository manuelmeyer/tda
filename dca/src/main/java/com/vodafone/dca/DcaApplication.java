package com.vodafone.dca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.integration.config.EnableIntegration;

@EnableIntegration
@SpringBootApplication
public class DcaApplication {

	public static void main(String[] args) {
		SpringApplication.run(DcaApplication.class, args);
	}
}
