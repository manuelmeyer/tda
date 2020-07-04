package com.vodafone.dca.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vodafone.dca.filter.InOrOutListBasedFilter;

@Configuration
public class FilterConfig {
	
	@Value("${dca.filter.common.bw.white-list-file:}")
	private String whiteListFilePath;

	@Value("${dca.filter.common.bw.black-list-file:}")
	private String blackListFilePath;
	
	@Value("${dca.filter.common.bw.file-scan-frequency:60}")
	private int fileScanFrequency;

	@Value("${dca.filter.common.bw.filter-field:imsi}")
	private String filterField;
		
	@Bean
	public InOrOutListBasedFilter blackListFilter() {
		return InOrOutListBasedFilter.newBuilder()
				.withFileScanFrequency(fileScanFrequency)
				.withFilterField(filterField)
				.withListFilePath(blackListFilePath)
				.withInMode(false)
				.build();
	}
	
	@Bean
	public InOrOutListBasedFilter whiteListFilter() {
		return InOrOutListBasedFilter.newBuilder()
				.withFileScanFrequency(fileScanFrequency)
				.withFilterField(filterField)
				.withListFilePath(whiteListFilePath)
				.withInMode(true)
				.build();
	}
}
