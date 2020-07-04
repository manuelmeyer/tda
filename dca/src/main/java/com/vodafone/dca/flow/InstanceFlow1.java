package com.vodafone.dca.flow;

import static com.vodafone.dca.common.DcaChannelNames.DCA_EVENT_INPUT_CHANNEL_FLOW_1;
import static com.vodafone.dca.common.DcaChannelNames.DCA_EVENT_OUTPUT_CHANNEL_FLOW_1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.StringUtils;

import com.vodafone.dca.domain.DataTransporter;
import com.vodafone.dca.filter.DataReductionFilter;
import com.vodafone.dca.filter.LacCellFilter;
import com.vodafone.dca.handler.AccumulatorEventHandler;
import com.vodafone.dca.reduction.ReductionMapHandler;
import com.vodafone.dca.service.PepperManager;
import com.vodafone.dca.transformer.MapFieldReducer;

@Configuration
@Order(1)
public class InstanceFlow1 {
	
	private static final Logger LOG = LoggerFactory.getLogger(InstanceFlow1.class);
	
	@Value("${dca.instance1.filter.lac-cell.file-scan-frequency:60}")
	private int fileScanFrequency;
	
	@Value("${dca.instance1.filter.lac-cell.lac-field:lac}")
	private String lacField;
	
	@Value("${dca.instance1.filter.lac-cell.cell-tower-field:cellTower}")
	private String cellField;
	
	@Value("${dca.instance1.filter.lac-cell.follow-exit:false}")
	private boolean followExit;
	
	@Value("${dca.instance1.filter.lac-cell.lac-cell-file}")
	private String lacCellFilePath;
	
	@Value("${dca.instance1.salt:}")
	private String pepper;

	@Value("${dca.instance1.output.field-definition}")
	private String fieldsOutDefinitionFile;
	
	@Bean(name = DCA_EVENT_INPUT_CHANNEL_FLOW_1)
	public MessageChannel dcaEventInputFLow1() {
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
	public LacCellFilter instance1LacCellFilter() {
		return LacCellFilter.newBuilder()
				.withFileScanFrequency(fileScanFrequency)
				.withLacCellFields(lacField, cellField)
				.withFollowExit(followExit)
				.withLacCellFilePath(lacCellFilePath)
				.withReductionMapHandler(instance1ReductionMapHandler())
				.build();
	}
	
	@Bean
	public DataReductionFilter instance1DataReductionFilter() {
		DataReductionFilter dataReductionFilter = new DataReductionFilter();
		dataReductionFilter.setReductionMapHandler(instance1ReductionMapHandler());
		return dataReductionFilter;
	}
	
	@Bean
	public PepperManager instance1PepperManager() {
		PepperManager pepperManager = new PepperManager();
		if (StringUtils.isEmpty(pepper)) {
			pepperManager.setPepper(pepper);
		}
		return pepperManager;
	}
	
	@Bean
	public Converter<DataTransporter, String> instance1MapFieldReducer() {
		MapFieldReducer mapFieldReducer = new MapFieldReducer();
		mapFieldReducer.setPepperManager(instance1PepperManager());
		mapFieldReducer.setFieldsOutDefinitionFile(fieldsOutDefinitionFile);
		return mapFieldReducer;
	}
	
	@Bean
	public AccumulatorEventHandler instance1Accumulator() {
		return new AccumulatorEventHandler("instance1", instance1OutputMessageChannel(), 100, 1000);
	}
	
	@Bean
	public IntegrationFlow mainInstanceFlow1() {
		return IntegrationFlows.from(DCA_EVENT_INPUT_CHANNEL_FLOW_1)
				.split()
				.filter(instance1LacCellFilter(), e -> e.discardChannel("nullChannel"))
				.filter(instance1DataReductionFilter(), e -> e.discardChannel("nullChannel"))
				.transform(instance1MapFieldReducer())
				.<String>handle((p, h) -> instance1Accumulator().accumulate(p))
				.nullChannel();
	}
	
	@Bean
	public IntegrationFlow outputInstance1Flow() {
		return IntegrationFlows.from(DCA_EVENT_OUTPUT_CHANNEL_FLOW_1)
				.log("com.vodafone.dca.output")
				.nullChannel();
	}
}
