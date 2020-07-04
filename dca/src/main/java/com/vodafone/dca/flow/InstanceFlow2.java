package com.vodafone.dca.flow;

import static com.vodafone.dca.common.DcaChannelNames.DCA_EVENT_INPUT_CHANNEL_FLOW_2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;

@Configuration
@Order(1)
@ConditionalOnProperty(name="dca.instance2.enabled")
public class InstanceFlow2 {
	
	private static final Logger LOG = LoggerFactory.getLogger(InstanceFlow2.class);
	
	@Bean(name = DCA_EVENT_INPUT_CHANNEL_FLOW_2)
	public MessageChannel dcaEventInputFLow2() {
		LOG.info("Creating channel {}", DCA_EVENT_INPUT_CHANNEL_FLOW_2);
		return MessageChannels.direct(DCA_EVENT_INPUT_CHANNEL_FLOW_2).get();
	}
		
	@Bean
	public IntegrationFlow mainInstanceFlow2() {
		return IntegrationFlows.from(DCA_EVENT_INPUT_CHANNEL_FLOW_2)
				//.log("com.vodafone.dca.FLOW2-list")
				.split()
				//.log("com.vodafone.dca.FLOW2")
				.nullChannel();
	}
}
