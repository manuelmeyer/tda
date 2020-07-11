package com.vodafone.dca.config;

import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vodafone.dca.domain.properties.DemographicsInputProperties;
import com.vodafone.dca.domain.properties.DemographicsOutputProperties;
import com.vodafone.dca.transformer.MapFieldReducer;
import com.vodafone.dca.transformer.ParsedElementListToDataTransporter;

@Configuration
public class ServiceConfig {
	
	@Value("${dca.input.field-definition:}")
	private String captureInputFieldDefinition;
	
	@Autowired
	private DemographicsInputProperties demographicsInputProperties;
	
	@Autowired
	private DemographicsOutputProperties demographicsOutputProperties;

	@Bean
	public DelimitedLineTokenizer delimitedLineTokenizer() {
		DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
		delimitedLineTokenizer.setDelimiter(demographicsInputProperties.getFieldDelimiter());
		return delimitedLineTokenizer;
	}
	
	@Bean
	public ParsedElementListToDataTransporter captureOffsetListToDataTransporter() {
		ParsedElementListToDataTransporter offsetListToDataTransporter = new ParsedElementListToDataTransporter();
		offsetListToDataTransporter.setFieldNamesDefinitionFile(captureInputFieldDefinition);
		return offsetListToDataTransporter;
	}
	
	@Bean
	public ParsedElementListToDataTransporter demographicsOffsetListToDataTransporter() {
		ParsedElementListToDataTransporter offsetListToDataTransporter = new ParsedElementListToDataTransporter();
		offsetListToDataTransporter.setFieldNamesDefinitionFile(demographicsInputProperties.getFieldDefinition());
		return offsetListToDataTransporter;
	}
	
	@Bean
	public MapFieldReducer demographicsFieldReducer() {
		MapFieldReducer reducer = new MapFieldReducer();
		reducer.setFieldsOutDefinitionFile(demographicsOutputProperties.getFieldDefinition());
		reducer.setAnonymiseSet(demographicsOutputProperties.getAnonymize());
		return reducer;
	}
}
