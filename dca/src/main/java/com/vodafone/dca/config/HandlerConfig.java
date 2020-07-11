package com.vodafone.dca.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.vodafone.dca.handler.DemographicsFileMoveHandler;
import com.vodafone.dca.handler.FileChunkDispatcher;

@Configuration
public class HandlerConfig {

	@Bean
	public DemographicsFileMoveHandler demographicsFileMoveHandler() {
		return new DemographicsFileMoveHandler();
	}
	
	@Bean
	public FileChunkDispatcher fileChunkDispatcher() {
		return new FileChunkDispatcher();
	}
}
