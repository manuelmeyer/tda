package com.vodafone.dca.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vodafone.dca.domain.properties.FilterBlackWhiteListProperties;
import com.vodafone.dca.filter.InOrOutListBasedFilter;

@Configuration
public class FilterConfig {
	
	@Autowired
	private FilterBlackWhiteListProperties filterBlackWhiteListProperties;
	
	@Bean
	public InOrOutListBasedFilter blackListFilter() {
		return InOrOutListBasedFilter.newBuilder()
				.withFileScanFrequency(filterBlackWhiteListProperties.getFileScanFrequency())
				.withFilterField(filterBlackWhiteListProperties.getFilterField())
				.withListFilePath(filterBlackWhiteListProperties.getBlackListFile())
				.withInMode(false)
				.build();
	}
	
	@Bean
	public InOrOutListBasedFilter whiteListFilter() {
		return InOrOutListBasedFilter.newBuilder()
				.withFileScanFrequency(filterBlackWhiteListProperties.getFileScanFrequency())
				.withFilterField(filterBlackWhiteListProperties.getFilterField())
				.withListFilePath(filterBlackWhiteListProperties.getWhiteListFile())
				.withInMode(true)
				.build();
	}
}
