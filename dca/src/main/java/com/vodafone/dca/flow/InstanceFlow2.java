package com.vodafone.dca.flow;

import static com.vodafone.dca.common.DcaChannelNames.DCA_EVENT_INPUT_CHANNEL_FLOW_2;
import static com.vodafone.dca.common.DcaChannelNames.DCA_EVENT_OUTPUT_CHANNEL_FLOW_2;
import static com.vodafone.dca.common.DcaChannelNames.NULL_CHANNEL;
import static org.springframework.integration.file.support.FileExistsMode.APPEND;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import com.vodafone.dca.file.GenericFileNameGenerator;
import com.vodafone.dca.filter.DataReductionFilter;
import com.vodafone.dca.filter.DataReductionFilter.ReductionMode;
import com.vodafone.dca.filter.LacCellFilter;
import com.vodafone.dca.handler.AccumulatorEventHandler;
import com.vodafone.dca.reduction.ReductionMapHandler;
import com.vodafone.dca.service.PepperManager;
import com.vodafone.dca.transformer.MapFieldReducer;

@Configuration
@Order(1)
@ConditionalOnProperty(name="dca.instances.instance2.enabled")
public class InstanceFlow2 {
	
	private static final Logger LOG = LoggerFactory.getLogger(InstanceFlow2.class);
	
	@Bean(name = DCA_EVENT_INPUT_CHANNEL_FLOW_2)
	public MessageChannel dcaEventInputFLow2() {
		LOG.info("Creating channel {}", DCA_EVENT_INPUT_CHANNEL_FLOW_2);
		return MessageChannels.direct(DCA_EVENT_INPUT_CHANNEL_FLOW_2).get();
	}
	
	@Value("${dca.instances.instance2.filter.lac-cell.file-scan-frequency:60}")
	private int fileScanFrequency;
	
	@Value("${dca.instances.instance2.filter.lac-cell.lac-field:lac}")
	private String lacField;
	
	@Value("${dca.instances.instance2.filter.lac-cell.cell-tower-field:cellTower}")
	private String cellField;
	
	@Value("${dca.instances.instance2.filter.lac-cell.follow-exit:true}")
	private boolean followExit;
	
	@Value("${dca.instances.instance2.filter.lac-cell.lac-cell-file}")
	private String lacCellFilePath;
	
	@Value("${dca.instances.instance2.filter.reduction.mode:IMSIS_CHANGE_CELL}")
	private ReductionMode reductionMode;
	
	@Value("${dca.instances.instance2.salt:}")
	private String pepper;

	@Value("${dca.instances.instance2.output.field-definition}")
	private String fieldsOutDefinitionFile;
	
	@Value("${dca.instances.instance2.output.batch-size:200}")
	private int batchSize;
	
	@Value("${dca.instances.instance2.output.batch-timeout:500}")
	private int batchTimeout;
	
	@Value("${dca.instances.instance2.output.file-directory}")
	private String outputDirectory;

	@Value("${dca.instances.instance2.output.file-size-threshold}")
	private int fileSizeThreshold;
	
	@Value("${dca.instances.instance2.output.file-prefix:}")
	private String filePrefix;

	@Value("${dca.instances.instance2.output.anonymise-fields:}")
	private String anonymiseFields[];

	@Bean(name = DCA_EVENT_INPUT_CHANNEL_FLOW_2)
	public MessageChannel dcaEventInputFLow1() {
		LOG.info("Creating channel {}", DCA_EVENT_INPUT_CHANNEL_FLOW_2);
		return MessageChannels.direct(DCA_EVENT_INPUT_CHANNEL_FLOW_2).get();
	}
	
	@Bean(name = DCA_EVENT_OUTPUT_CHANNEL_FLOW_2)
	public MessageChannel instance2OutputMessageChannel() {
		LOG.info("Creating output channel {}", DCA_EVENT_OUTPUT_CHANNEL_FLOW_2);
		return MessageChannels.direct(DCA_EVENT_OUTPUT_CHANNEL_FLOW_2).get();
	}
	
	@Bean
	public ReductionMapHandler instance2ReductionMapHandler() {
		return new ReductionMapHandler();
	}

	@Bean
	public GenericSelector<DataTransporter> instance2LacCellFilter() {
		return LacCellFilter.newBuilder()
				.withFileScanFrequency(fileScanFrequency)
				.withLacCellFields(lacField, cellField)
				.withFollowExit(followExit)
				.withLacCellFilePath(lacCellFilePath)
				.withReductionMapHandler(instance2ReductionMapHandler())
				.build();
	}
	
	@Bean
	public GenericSelector<DataTransporter> instance2DataReductionFilter() {
		DataReductionFilter dataReductionFilter = new DataReductionFilter();
		dataReductionFilter.setReductionMapHandler(instance2ReductionMapHandler());
		dataReductionFilter.setReductionMode(reductionMode);
		return dataReductionFilter;
	}
	
	@Bean
	public PepperManager instance2PepperManager() {
		PepperManager pepperManager = new PepperManager();
		if (StringUtils.isEmpty(pepper)) {
			pepperManager.setPepper(pepper);
		}
		return pepperManager;
	}
	
	@Bean
	public Converter<DataTransporter, String> instance2MapFieldReducer() {
		MapFieldReducer mapFieldReducer = new MapFieldReducer();
		mapFieldReducer.setPepperManager(instance2PepperManager());
		mapFieldReducer.setFieldsOutDefinitionFile(fieldsOutDefinitionFile);
		mapFieldReducer.setAnonymiseSet(anonymiseFields);
		return mapFieldReducer;
	}
	
	@Bean
	public AccumulatorEventHandler instance2Accumulator() {
		return new AccumulatorEventHandler("instance2", instance2OutputMessageChannel(), batchSize, batchTimeout);
	}
	
	@Bean
	public FileNameGenerator instance2FileNameGenerator() {
		GenericFileNameGenerator fileNameGenerator = new GenericFileNameGenerator();
		fileNameGenerator.setDirectory(outputDirectory);
		fileNameGenerator.setFilePrefix(filePrefix);
		fileNameGenerator.setFileSizeThreshold(fileSizeThreshold);
		return fileNameGenerator;
	}
	
	@Bean
	public IntegrationFlow mainInstanceFlow2() {
		return IntegrationFlows.from(DCA_EVENT_INPUT_CHANNEL_FLOW_2)
				.split()
				.<DataTransporter>handle((dataTransporter, h) -> dataTransporter.resetShadowMap())
				.filter(instance2LacCellFilter(), e -> e.discardChannel(NULL_CHANNEL))
				.filter(instance2DataReductionFilter(), e -> e.discardChannel(NULL_CHANNEL))
				.transform(instance2MapFieldReducer())
				.<String>handle((p, h) -> instance2Accumulator().accumulate(p))
				.nullChannel();
	}
	
	@Bean
	public IntegrationFlow outputInstance2Flow() {
		return IntegrationFlows.from(DCA_EVENT_OUTPUT_CHANNEL_FLOW_2)
				.handle(Files
							.outboundAdapter(new File(outputDirectory))
							.fileExistsMode(APPEND)
							.fileNameGenerator(instance2FileNameGenerator()))
				.nullChannel();
	}

}
