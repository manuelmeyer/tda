package com.vodafone.dca;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableMBeanExport;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.jmx.config.EnableIntegrationMBeanExport;

@EnableIntegration
@EnableIntegrationMBeanExport
@EnableMBeanExport
@SpringBootApplication
public class DcaApplication {

	public static void main(String[] args) {
		SpringApplication.run(DcaApplication.class, args);
	}
}
