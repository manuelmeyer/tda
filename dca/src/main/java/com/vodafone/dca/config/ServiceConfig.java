package com.vodafone.dca.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vodafone.dca.transformer.ParsedElementListToDataTransporter;

@Configuration
public class ServiceConfig {
	
	@Value("${dca.input.field-definition:}")
	private String captureInputFieldDefinition;
	
	@Bean
	public ParsedElementListToDataTransporter captureOffsetListToDataTransporter() {
		ParsedElementListToDataTransporter offsetListToDataTransporter = new ParsedElementListToDataTransporter();
		offsetListToDataTransporter.setFieldNamesDefinitionFile(captureInputFieldDefinition);
		return offsetListToDataTransporter;
	}
}
