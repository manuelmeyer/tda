package com.vodafone.dca.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vodafone.dca.domain.MultiInstancesProperties;
import com.vodafone.dca.domain.MultiShellProcessorsProperties;
import com.vodafone.dca.domain.PerInstanceProperties;

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
	@ConfigurationProperties(prefix="dca")
	public MultiShellProcessorsProperties multiShellProcessorProperties() {
		LOG.info("Creating MultiShellProcessorProperties");
		return new MultiShellProcessorsProperties();
	}
	
	@Bean
	public PerInstanceProperties instance1Properties() {
		return multiInstancesProperties().getInstance1();
	}
	
	@Bean
	public PerInstanceProperties instance2Properties() {
		return multiInstancesProperties().getInstance2();
	}

}