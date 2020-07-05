package com.vodafone.dca.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vodafone.dca.domain.InstancesProperties;
import com.vodafone.dca.domain.PerInstanceProperties;

@Configuration
@EnableConfigurationProperties
public class InstancePropertiesConfig {
	
	private static final Logger LOG = LoggerFactory.getLogger(InstancePropertiesConfig.class);
	
	@Bean
	@ConfigurationProperties(prefix="dca.instances")
	public InstancesProperties instancesProperties() {
		LOG.info("creating instance1Properties");
		return new InstancesProperties();
	}
	
	@Bean
	public PerInstanceProperties instance1Properties() {
		return instancesProperties().getInstance1();
	}
}
