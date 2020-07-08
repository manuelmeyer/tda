package com.vodafone.dca.flow;

import static com.vodafone.dca.common.DcaChannelNames.DCA_EVENT_INPUT_CHANNEL_FLOW_1;
import static com.vodafone.dca.common.DcaChannelNames.DCA_EVENT_OUTPUT_CHANNEL_FLOW_1;
import static com.vodafone.dca.common.DcaChannelNames.NULL_CHANNEL;
import static org.springframework.integration.file.support.FileExistsMode.APPEND;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.dsl.Files;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StringUtils;

import com.vodafone.dca.domain.DataTransporter;
import com.vodafone.dca.domain.properties.LacCellFilterProperties;
import com.vodafone.dca.domain.properties.OutputProperties;
import com.vodafone.dca.domain.properties.PerInstanceProperties;
import com.vodafone.dca.file.GenericFileNameGenerator;
import com.vodafone.dca.filter.DataReductionFilter;
import com.vodafone.dca.filter.LacCellFilter;
import com.vodafone.dca.filter.LacCellReductionFilter;
import com.vodafone.dca.handler.AccumulatorEventHandler;
import com.vodafone.dca.reduction.ReductionMapHandler;
import com.vodafone.dca.service.PepperManager;
import com.vodafone.dca.transformer.MapFieldReducer;

@Configuration
@Order(1)
@ConditionalOnProperty(name="dca.instances.instance1.enabled")
public class InstanceFlow1 {
	
	private static final Logger LOG = LoggerFactory.getLogger(InstanceFlow1.class);
	
	@Autowired
	private PerInstanceProperties instance1Properties;
	
	@Bean(name = DCA_EVENT_INPUT_CHANNEL_FLOW_1)
	public MessageChannel dcaEventInputFlow1() {
		LOG.info("Creating channel {}", DCA_EVENT_INPUT_CHANNEL_FLOW_1);
		return MessageChannels.direct(DCA_EVENT_INPUT_CHANNEL_FLOW_1).get();
	}
	
	@Bean(name = DCA_EVENT_OUTPUT_CHANNEL_FLOW_1)
	public MessageChannel instance1OutputMessageChannel() {
		LOG.info("Creating output channel {}", DCA_EVENT_OUTPUT_CHANNEL_FLOW_1);
		return MessageChannels.direct(DCA_EVENT_OUTPUT_CHANNEL_FLOW_1).get();
	}
	
	@Bean
	public ReductionMapHandler instance1ReductionMapHandler() {
		return new ReductionMapHandler();
	}

	@Bean
	public GenericSelector<DataTransporter> instance1LacCellFilter() {
		LacCellFilterProperties lacCellFilter = instance1Properties.getFilter().getLacCell();
		return LacCellFilter.newBuilder()
				.withFileScanFrequency(lacCellFilter.getFileScanFrequency())
				.withLacCellFields(lacCellFilter.getLacField(), lacCellFilter.getCellTowerField())
				.withFollowExit(lacCellFilter.isFollowExit())
				.withLacCellFilePath(lacCellFilter.getLacCellFile())
				.withReductionMapHandler(instance1ReductionMapHandler())
				.build();
	}
	
	@Bean
	public GenericSelector<DataTransporter> instance1DataReductionFilter() {
		DataReductionFilter dataReductionFilter = new DataReductionFilter();
		dataReductionFilter.setReductionMapHandler(instance1ReductionMapHandler());
		dataReductionFilter.setReductionMode(instance1Properties.getFilter().getReduction().getMode());
		return dataReductionFilter;
	}
	
	@Bean
	public GenericSelector<DataTransporter> instance1LacCellReductionFilter() {
		LacCellReductionFilter lacCellReductionFilter = new LacCellReductionFilter();
		lacCellReductionFilter.setDataReductionFilter(instance1DataReductionFilter());
		lacCellReductionFilter.setLacCellFilter(instance1LacCellFilter());
		return lacCellReductionFilter;
	}
	
	@Bean
	public PepperManager instance1PepperManager() {
		PepperManager pepperManager = new PepperManager();
		String pepper = instance1Properties.getSalt();
		if (StringUtils.isEmpty(pepper)) {
			pepperManager.setPepper(pepper);
		}
		return pepperManager;
	}
	
	@Bean
	public Converter<DataTransporter, String> instance1MapFieldReducer() {
		OutputProperties output = instance1Properties.getOutput();
		MapFieldReducer mapFieldReducer = new MapFieldReducer();
		mapFieldReducer.setPepperManager(instance1PepperManager());
		mapFieldReducer.setFieldsOutDefinitionFile(output.getFieldDefinition());
		mapFieldReducer.setAnonymiseSet(output.getAnonymiseFields());
		return mapFieldReducer;
	}
	
	@Bean
	public AccumulatorEventHandler instance1Accumulator() {
		OutputProperties output = instance1Properties.getOutput();
		return new AccumulatorEventHandler("instance1", instance1OutputMessageChannel(), 
				output.getBatchSize(), 
				output.getBatchTimeout());
	}
	
	@Bean
	public FileNameGenerator instance1FileNameGenerator() {
		OutputProperties output = instance1Properties.getOutput();
		GenericFileNameGenerator fileNameGenerator = new GenericFileNameGenerator();
		fileNameGenerator.setDirectory(output.getFileDirectory());
		fileNameGenerator.setFilePrefix(output.getFilePrefix());
		fileNameGenerator.setFileSizeThreshold(output.getFileSizeThreshold());
		return fileNameGenerator;
	}
	
	@Bean
	public IntegrationFlow mainInstanceFlow1() {
		return IntegrationFlows.from(DCA_EVENT_INPUT_CHANNEL_FLOW_1)
				.split()
				.<DataTransporter>handle((dataTransporter, h) -> dataTransporter.resetShadowMap())
				.filter(instance1LacCellReductionFilter(), e -> e.discardChannel(NULL_CHANNEL))
				.transform(instance1MapFieldReducer())
				.<String>handle((p, h) -> instance1Accumulator().accumulate(p))
				.nullChannel();
	}
	
	@Bean
	public IntegrationFlow outputInstance1Flow() {
		return IntegrationFlows.from(DCA_EVENT_OUTPUT_CHANNEL_FLOW_1)
				.handle(Files.outboundAdapter(new File(instance1Properties.getOutput().getFileDirectory()))
						.fileExistsMode(APPEND)
						.fileNameGenerator(instance1FileNameGenerator()))
				.nullChannel();
	}
}
