package com.vodafone.dca.config;

import static com.vodafone.dca.common.DcaChannelNames.DCA_EVENT_INPUT_CHANNEL_FLOW_PREFIX;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Maps;
import com.vodafone.dca.domain.DataTransporter;
import com.vodafone.dca.filter.InOrOutListBasedFilter;
import com.vodafone.dca.infra.BaseFlowErrorHandlers;
import com.vodafone.dca.infra.RingBufferMessageDispatcher;
import com.vodafone.dca.source.AmqpInboundChannel;
import com.vodafone.dca.transformer.CsvBytesToOffsetParser;
import com.vodafone.dca.transformer.CsvBytesToOffsetParser.Offset;
import com.vodafone.dca.transformer.ParsedElementListToDataTransporter;

@Configuration
@Order(100)
public class SourceConfig {
	
	private static final Logger LOG = LoggerFactory.getLogger(SourceConfig.class);
	
	@Value("${dca.source.ring-buffer.size:128}")
	private int ringBufferSize;
	
	@Value("${dca.source.ring-buffer.parallel-consumers:8}")
	private int ringBufferParallelConsumers;
	
	@Autowired
	private GenericApplicationContext applicationContext;
	
	@Autowired
	private InOrOutListBasedFilter blackListFilter;
	
	@Autowired
	private InOrOutListBasedFilter whiteListFilter;
	
	@Autowired
	private ParsedElementListToDataTransporter captureOffsetListToDataTransporter;
	
	private Map<String, RingBufferMessageDispatcher<List<DataTransporter>, List<DataTransporter>>> dispatchers = Maps.newHashMap();
	
	@PostConstruct
	public void getInputChannels() {
		List<String> inputChannels = Arrays.stream(applicationContext.getBeanDefinitionNames())
			.filter(name -> name.startsWith(DCA_EVENT_INPUT_CHANNEL_FLOW_PREFIX))
			.collect(Collectors.toList());
		
		Assert.notEmpty(inputChannels, "There are no input channels of prefix " + DCA_EVENT_INPUT_CHANNEL_FLOW_PREFIX);
		
		LOG.info("input channels are {}", inputChannels);
		
		inputChannels.forEach(channelName -> {
			RingBufferMessageDispatcher<List<DataTransporter>, List<DataTransporter>> ringBuffer = createRingBufferDispatcher(channelName, (DirectChannel)applicationContext.getBean(channelName));
			dispatchers.put(channelName, ringBuffer);
			applicationContext.registerBean("rb-for-" + channelName, RingBufferMessageDispatcher.class, () -> ringBuffer);
		});
	}

	protected RingBufferMessageDispatcher<List<DataTransporter>, List<DataTransporter>> createRingBufferDispatcher(String channelName, 
					DirectChannel channelBean) {
		return new RingBufferMessageDispatcher<List<DataTransporter>, List<DataTransporter>>()
				.withThreadNamePrefix(channelName)
				.withBufferSizeAndParallelConsumers(ringBufferSize, ringBufferParallelConsumers)
				.withErrorHandlers(baseFlowErrorHandlers())
				.withMessageHandler(dataTransporterList -> sendToChannel(channelBean, dataTransporterList))
				.initialise();
	}

	protected boolean sendToChannel(DirectChannel channelBean, List<DataTransporter> dataTransporterList) {
		return channelBean.send(MessageBuilder.withPayload(dataTransporterList).build());
	}
	
	@Bean
	public BaseFlowErrorHandlers<List<DataTransporter>, List<DataTransporter>> baseFlowErrorHandlers() {
		return new BaseFlowErrorHandlers<List<DataTransporter>, List<DataTransporter>>();
	}
	
	@Bean
	public BiConsumer<byte[], Map<String, Object>> rabbitConsumer() {
		return (body, header) -> dispatchToRingBuffers(body);
	}
	
	private void dispatchToRingBuffers(byte[] body) {
		List<List<Offset>> offsetList = CsvBytesToOffsetParser.parse(body);
		List<DataTransporter> dataTransporters = offsetList
				.stream()
				.map(offsets -> captureOffsetListToDataTransporter.buildFromOffsetList(offsets))
				.filter(dataTransporter -> blackListFilter.accept(dataTransporter))
				.filter(dataTransporter -> whiteListFilter.accept(dataTransporter))
				.collect(Collectors.toList());
		if (!CollectionUtils.isEmpty(dataTransporters)) {
			dispatchers.forEach((k, v) -> v.receive(dataTransporters));
		}
	}

	@Bean
	public AmqpInboundChannel amqpInboundChannel() {
		return new AmqpInboundChannel();
	}
}
