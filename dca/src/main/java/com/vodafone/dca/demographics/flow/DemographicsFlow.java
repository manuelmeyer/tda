package com.vodafone.dca.demographics.flow;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.dsl.FileInboundChannelAdapterSpec;
import org.springframework.integration.file.dsl.Files;
import org.springframework.messaging.MessageChannel;

import com.vodafone.dca.domain.properties.DemographicsInputProperties;
import com.vodafone.dca.handler.DemographicsFileMoveHandler;
import com.vodafone.dca.handler.DemographicFileOutputWriter;

@Configuration
@ConditionalOnProperty("demographics.enabled")
public class DemographicsFlow {
	
	private static final Logger LOG = LoggerFactory.getLogger(DemographicsFlow.class);
	
	@Autowired
	private DemographicsInputProperties demographicsProperties;
	
	@Autowired
	private DemographicsFileMoveHandler demographicsFileMoveHandler;
	
	@Autowired
	private DemographicFileOutputWriter fileChunkDispatcher;
	
	@Bean(name = "demographicsProcessingFlowChannel")
	public MessageChannel demographicsProcessingFlowChannel() {
		return MessageChannels.direct("demographicsProcessingFlowChannel").get();
	}
	
	@Bean
	public FileInboundChannelAdapterSpec demographicsFileSource() {
		return Files.inboundAdapter(new File(demographicsProperties.getFileDirectory()))
				.preventDuplicates(false)
				.patternFilter(demographicsProperties.getFilePattern());
	}
	
	@Bean
	public IntegrationFlow demographicsInputFlow() {
		return IntegrationFlows.from(demographicsFileSource(), 
					c -> c.poller(Pollers.fixedRate(demographicsProperties.getFilePollRate())
										.maxMessagesPerPoll(100))
				)
				.log("com.vodafone.dca.demographics.flow.1")
				.<File>handle((file, h) -> 
						demographicsFileMoveHandler.changeFileName(file, demographicsProperties.getFileSuffixNew()))
				.log("com.vodafone.dca.demographics.flow.2")
				.channel(demographicsProcessingFlowChannel())
				.get();
	}
	
	@Bean
	public IntegrationFlow demographicsProcessingFlow() {
		return IntegrationFlows.from(demographicsProcessingFlowChannel())
				.log("com.vodafone.dca.demographics.flow.3")
				.<File>handle((file, h) -> fileChunkDispatcher.generateOutputFile(file))
				.nullChannel();
	}
}
