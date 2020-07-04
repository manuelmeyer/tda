package com.vodafone.dca.flow;

import static com.vodafone.dca.common.DcaChannelNames.DCA_EVENT_INPUT_CHANNEL_FLOW_1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;

@Configuration
@Order(1)
public class InstanceFlow1 {
	
	private static final Logger LOG = LoggerFactory.getLogger(InstanceFlow1.class);
	
	@Bean(name = DCA_EVENT_INPUT_CHANNEL_FLOW_1)
	public MessageChannel dcaEventInputFLow1() {
		LOG.info("Creating channel {}", DCA_EVENT_INPUT_CHANNEL_FLOW_1);
		return MessageChannels.direct(DCA_EVENT_INPUT_CHANNEL_FLOW_1).get();
	}
		
	@Bean
	public IntegrationFlow mainInstanceFlow1() {
		return IntegrationFlows.from(DCA_EVENT_INPUT_CHANNEL_FLOW_1)
				.log("com.vodafone.dca.FLOW1-list")
				.split()
				.log("com.vodafone.dca.FLOW1")
				.nullChannel();
	}
}
