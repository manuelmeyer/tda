package com.vodafone.dca.config;

import static com.vodafone.dca.filter.InOrOutPatternBasedFilter.DiscriminationMode.HAS_MINIMUM_LENGTH;
import static com.vodafone.dca.filter.InOrOutPatternBasedFilter.DiscriminationMode.STARTS_WITH;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.core.GenericSelector;

import com.google.common.collect.Lists;
import com.vodafone.dca.domain.DataTransporter;
import com.vodafone.dca.domain.properties.FilterBlackWhiteListProperties;
import com.vodafone.dca.filter.InOrOutListBasedFilter;
import com.vodafone.dca.filter.InOrOutPatternBasedFilter;

@Configuration
public class FilterConfig {
	
	@Autowired
	private FilterBlackWhiteListProperties filterBlackWhiteListProperties;
	
	@Bean
	public GenericSelector<DataTransporter> blackListFilter() {
		return InOrOutListBasedFilter.newBuilder()
				.withFileScanFrequency(filterBlackWhiteListProperties.getFileScanFrequency())
				.withFilterField(filterBlackWhiteListProperties.getFilterField())
				.withListFilePath(filterBlackWhiteListProperties.getBlackListFile())
				.withInMode(false)
				.build();
	}
	
	@Bean
	public GenericSelector<DataTransporter> whiteListFilter() {
		return InOrOutListBasedFilter.newBuilder()
				.withFileScanFrequency(filterBlackWhiteListProperties.getFileScanFrequency())
				.withFilterField(filterBlackWhiteListProperties.getFilterField())
				.withListFilePath(filterBlackWhiteListProperties.getWhiteListFile())
				.withInMode(true)
				.build();
	}
	
	@Bean
	public GenericSelector<DataTransporter> roamer2342xFilter() {
		List<String> patterns = Lists.newArrayList("23425", "23427", "23415", "00000");
		InOrOutPatternBasedFilter filter = new InOrOutPatternBasedFilter();
		filter.setInMode(false);
		filter.setDiscriminationMode(STARTS_WITH);
		filter.setPatterns(patterns);
		return filter;
	}
	
	@Bean
	public GenericSelector<DataTransporter> hasMinimumLength() {
		InOrOutPatternBasedFilter filter = new InOrOutPatternBasedFilter();
		filter.setInMode(true);
		filter.setDiscriminationMode(HAS_MINIMUM_LENGTH);
		return filter;
	}
}
