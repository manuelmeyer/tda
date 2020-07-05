package com.vodafone.dca.flow;

import static com.vodafone.dca.common.DcaChannelNames.DCA_EVENT_INPUT_CHANNEL_FLOW_1;
import static com.vodafone.dca.common.DcaChannelNames.DCA_EVENT_OUTPUT_CHANNEL_FLOW_1;
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
import com.vodafone.dca.filter.LacCellReductionFilter;
import com.vodafone.dca.handler.AccumulatorEventHandler;
import com.vodafone.dca.reduction.ReductionMapHandler;
import com.vodafone.dca.service.PepperManager;
import com.vodafone.dca.transformer.MapFieldReducer;

@Configuration
@Order(1)
@ConditionalOnProperty(name="dca.instance1.enabled")
public class InstanceFlow1 {
	
	private static final Logger LOG = LoggerFactory.getLogger(InstanceFlow1.class);
	
	@Value("${dca.instance1.filter.lac-cell.file-scan-frequency:60}")
	private int fileScanFrequency;
	
	@Value("${dca.instance1.filter.lac-cell.lac-field:lac}")
	private String lacField;
	
	@Value("${dca.instance1.filter.lac-cell.cell-tower-field:cellTower}")
	private String cellField;
	
	@Value("${dca.instance1.filter.lac-cell.follow-exit:true}")
	private boolean followExit;
	
	@Value("${dca.instance1.filter.lac-cell.lac-cell-file}")
	private String lacCellFilePath;
	
	@Value("${dca.instance1.filter.reduction.mode:IMSIS_CHANGE_CELL_ONLY}")
	private ReductionMode reductionMode;
	
	@Value("${dca.instance1.salt:}")
	private String pepper;
	
	@Value("${dca.instance1.output.anonymise-fields:}")
	private String anonymiseFields[];

	@Value("${dca.instance1.output.field-definition}")
	private String fieldsOutDefinitionFile;
	
	@Value("${dca.instance1.output.batch-size:200}")
	private int batchSize;
	
	@Value("${dca.instance1.output.batch-timeout:500}")
	private int batchTimeout;
	
	@Value("${dca.instance1.output.file-directory}")
	private String outputDirectory;

	@Value("${dca.instance1.output.file-size-threshold}")
	private int fileSizeThreshold;
	
	@Value("${dca.instance1.output.file-prefix:}")
	private String filePrefix;

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
	public GenericSelector<DataTransporter> instance1LacCellFilter() {
		return LacCellFilter.newBuilder()
				.withFileScanFrequency(fileScanFrequency)
				.withLacCellFields(lacField, cellField)
				.withFollowExit(followExit)
				.withLacCellFilePath(lacCellFilePath)
				.withReductionMapHandler(instance1ReductionMapHandler())
				.build();
	}
	
	@Bean
	public GenericSelector<DataTransporter> instance1DataReductionFilter() {
		DataReductionFilter dataReductionFilter = new DataReductionFilter();
		dataReductionFilter.setReductionMapHandler(instance1ReductionMapHandler());
		dataReductionFilter.setReductionMode(reductionMode);
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
		mapFieldReducer.setAnonymiseSet(anonymiseFields);
		return mapFieldReducer;
	}
	
	@Bean
	public AccumulatorEventHandler instance1Accumulator() {
		return new AccumulatorEventHandler("instance1", instance1OutputMessageChannel(), batchSize, batchTimeout);
	}
	
	@Bean
	public FileNameGenerator instance1FileNameGenerator() {
		GenericFileNameGenerator fileNameGenerator = new GenericFileNameGenerator();
		fileNameGenerator.setDirectory(outputDirectory);
		fileNameGenerator.setFilePrefix(filePrefix);
		fileNameGenerator.setFileSizeThreshold(fileSizeThreshold);
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
				.handle(Files.outboundAdapter(new File(outputDirectory))
						.fileExistsMode(APPEND)
						.fileNameGenerator(instance1FileNameGenerator()))
				.nullChannel();
	}
}
