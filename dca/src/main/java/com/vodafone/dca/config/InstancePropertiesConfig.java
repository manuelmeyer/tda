package com.vodafone.dca.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vodafone.dca.domain.properties.DemographicsInputProperties;
import com.vodafone.dca.domain.properties.DemographicsOutputProperties;
import com.vodafone.dca.domain.properties.FilterBlackWhiteListProperties;
import com.vodafone.dca.domain.properties.MultiDemographicsProperties;
import com.vodafone.dca.domain.properties.MultiInstancesProperties;
import com.vodafone.dca.domain.properties.MultiShellProcessorsProperties;

@Configuration
@EnableConfigurationProperties
public class InstancePropertiesConfig {
	
	private static final Logger LOG = LoggerFactory.getLogger(InstancePropertiesConfig.class);
	
	@Bean
	@ConfigurationProperties(prefix="dca")
	public MultiInstancesProperties multiInstancesProperties() {
		LOG.info("Creating InstancesProperties");
		return new MultiInstancesProperties();
	}

	@Bean
	@ConfigurationProperties(prefix="dca.filter-bw-list")
	public FilterBlackWhiteListProperties filterBlackWhiteListProperties() {
		LOG.info("Creating FilterBlackWhiteListProperties");
		return new FilterBlackWhiteListProperties();
	}

	@Bean
	@ConfigurationProperties(prefix="dca")
	public MultiShellProcessorsProperties multiShellProcessorProperties() {
		LOG.info("Creating MultiShellProcessorProperties");
		return new MultiShellProcessorsProperties();
	}
	
	@Bean
	@ConfigurationProperties(prefix = "dca")
	public MultiDemographicsProperties multiDemographicsProperties() {
		return new MultiDemographicsProperties();
	}
	
	@Bean
	@ConfigurationProperties(prefix="dca.demographics[0].input")
	public DemographicsInputProperties demographicsInputProperties() {
		return new DemographicsInputProperties();
	}
	
	@Bean
	@ConfigurationProperties(prefix="dca.demographics[0].output")
	public DemographicsOutputProperties demographicsOutputProperties() {
		return new DemographicsOutputProperties();
	}
}
