package com.vodafone.dca.config;

import static com.vodafone.dca.common.DcaChannelNames.DCA_EVENT_INPUT_CHANNEL_FLOW_PREFIX;
import static com.vodafone.dca.common.DcaChannelNames.DCA_EVENT_OUTPUT_CHANNEL_FLOW_PREFIX;
import static com.vodafone.dca.common.DcaChannelNames.NULL_CHANNEL;
import static org.springframework.integration.file.support.FileExistsMode.APPEND;

import java.io.File;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.core.convert.converter.Converter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.GenericSelector;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.dsl.Files;
import org.springframework.util.StringUtils;

import com.vodafone.dca.domain.DataTransporter;
import com.vodafone.dca.domain.properties.LacCellFilterProperties;
import com.vodafone.dca.domain.properties.MultiInstancesProperties;
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
@Order(0)
public class DynamicInstancesFlowConfig {
	
	private static final Logger LOG = LoggerFactory.getLogger(DynamicInstancesFlowConfig.class);
	
	@Autowired
	private MultiInstancesProperties multiInstancesProperties;
	
	@Autowired
	private GenericApplicationContext applicationContext;
	
	@Autowired
	private IntegrationFlowContext integrationFlowContext;
	
	class InstanceContext {
		DirectChannel inputChannel;
		DirectChannel outputChannel;
		
		PerInstanceProperties instance;
		
		ReductionMapHandler reductionMapHandler;
		GenericSelector<DataTransporter> dataReductionFilter;
		GenericSelector<DataTransporter> lacCellFilter;
		GenericFileNameGenerator fileNameGenerator;
		Converter<DataTransporter, String> mapFieldReducer;
		AccumulatorEventHandler accumulator;
		PepperManager pepperManager;
		LacCellReductionFilter lacCellReductionFilter;
		
		InstanceContext(PerInstanceProperties instance) {
			this.instance = instance;
		}
	}
	
	@PostConstruct
	public void buildDynamicInstances() {
		LOG.info("Creating {} dynamic instances...", multiInstancesProperties.getInstances().size());
		multiInstancesProperties.getInstances().forEach(instance -> buildDynamicInstance(new InstanceContext(instance)));
	}
	
	protected void buildDynamicInstance(InstanceContext instanceContext) {		
		if(instanceContext.instance.isEnabled()) {
			createAndRegisterInputChannel(instanceContext);
			createAndRegisterOutputChannel(instanceContext);
			
			createReductionMapHandler(instanceContext);
			createAndRegisterPepperManager(instanceContext);

			createAndRegisterLacCellFilter(instanceContext);
			createAndRegisterDataReductionFilter(instanceContext);
			createAndRegisterMapFieldReducer(instanceContext);
			createAndRegisterAccumulator(instanceContext);
			createAndRegisterInputFlow(instanceContext);
			
			createAndRegisterFileGenerator(instanceContext);
			createAndRegisterPepperManager(instanceContext);
			createAndRegisterOutputFlow(instanceContext);
		}
	}
	
	protected void createReductionMapHandler(InstanceContext instanceContext) {
		instanceContext.reductionMapHandler = register(instanceContext, new ReductionMapHandler(), ReductionMapHandler.class);
	}

	protected void createAndRegisterPepperManager(InstanceContext instanceContext) {
		PepperManager pepperManager = new PepperManager();
		String pepper = instanceContext.instance.getSalt();
		if (StringUtils.isEmpty(pepper)) {
			pepperManager.setPepper(pepper);
		}
		instanceContext.pepperManager = register(instanceContext, pepperManager, PepperManager.class);
	}
	
	@SuppressWarnings("unchecked")
	protected <T> T register(InstanceContext instanceContext, T bean, Class<T> clazz) {
		String beanName = beanName(instanceContext, bean);
		applicationContext.registerBean(beanName, clazz, () -> bean);
		return (T)applicationContext.getBean(beanName);
	}
	
	protected <T> String beanName(InstanceContext instanceContext, T bean) {
		return instanceContext.instance.getName() + "-" + bean.getClass().getSimpleName() + "-" + UUID.randomUUID().toString();
	}

	protected void createAndRegisterMapFieldReducer(InstanceContext instanceContext) {
		OutputProperties output = instanceContext.instance.getOutput();
		MapFieldReducer mapFieldReducer = new MapFieldReducer();
		mapFieldReducer.setPepperManager(instanceContext.pepperManager);
		mapFieldReducer.setFieldsOutDefinitionFile(output.getFieldDefinition());
		mapFieldReducer.setAnonymiseSet(output.getAnonymiseFields());
		instanceContext.mapFieldReducer = register(instanceContext, mapFieldReducer, MapFieldReducer.class);
	}

	protected void createAndRegisterFileGenerator(InstanceContext instanceContext) {
		OutputProperties output = instanceContext.instance.getOutput();
		GenericFileNameGenerator fileNameGenerator = new GenericFileNameGenerator();
		fileNameGenerator.setDirectory(output.getFileDirectory());
		fileNameGenerator.setFilePrefix(output.getFilePrefix());
		fileNameGenerator.setFileSizeThreshold(output.getFileSizeThreshold());
		instanceContext.fileNameGenerator = register(instanceContext, fileNameGenerator, GenericFileNameGenerator.class);
	}

	protected void createAndRegisterDataReductionFilter(InstanceContext instanceContext) {
		DataReductionFilter dataReductionFilter = new DataReductionFilter();
		dataReductionFilter.setReductionMapHandler(instanceContext.reductionMapHandler);
		dataReductionFilter.setReductionMode(instanceContext.instance.getFilter().getReduction().getMode());
		instanceContext.dataReductionFilter = register(instanceContext, dataReductionFilter, DataReductionFilter.class);
	}

	protected void createAndRegisterLacCellFilter(InstanceContext instanceContext) {
		LacCellFilterProperties lacCellFilter = instanceContext.instance.getFilter().getLacCell();
		instanceContext.lacCellFilter = register(instanceContext, 
				LacCellFilter.newBuilder()
					.withFileScanFrequency(lacCellFilter.getFileScanFrequency())
					.withLacCellFields(lacCellFilter.getLacField(), lacCellFilter.getCellTowerField())
					.withFollowExit(lacCellFilter.isFollowExit())
					.withLacCellFilePath(lacCellFilter.getLacCellFile())
					.withReductionMapHandler(instanceContext.reductionMapHandler)
					.build(), 
				LacCellFilter.class);
	}
	
	protected void createAndRegisterAccumulator(InstanceContext instanceContext) {
		OutputProperties output = instanceContext.instance.getOutput();
		instanceContext.accumulator = register(instanceContext, 
				new AccumulatorEventHandler(instanceContext.instance.getName(), 
					instanceContext.outputChannel, 
					output.getBatchSize(), 
					output.getBatchTimeout()),
				AccumulatorEventHandler.class);
	}

	protected void createAndRegisterInputFlow(InstanceContext instanceContext) {
		IntegrationFlow inputFlow = createInputFlow(instanceContext);
		integrationFlowContext.registration(inputFlow)
								.autoStartup(true)
								.register();
	}
	
	protected void createAndRegisterOutputFlow(InstanceContext instanceContext) {
		IntegrationFlow outputFlow = createOutputFlow(instanceContext);
		integrationFlowContext.registration(outputFlow)
								.autoStartup(true)
								.register();
	}


	protected IntegrationFlow createOutputFlow(InstanceContext instanceContext) {
		return IntegrationFlows.from(instanceContext.outputChannel)
				.handle(Files.outboundAdapter(new File(instanceContext.instance.getOutput().getFileDirectory()))
						.fileExistsMode(APPEND)
						.fileNameGenerator(instanceContext.fileNameGenerator))
				.nullChannel();
	}
	
	protected IntegrationFlow createInputFlow(InstanceContext instanceContext) {
		switch(instanceContext.instance.getTemplate()) {
			case NETWORK_RAIL:
				return createNetworkRailInputFlow(instanceContext);
			case HULL:
				return createHullInputFlow(instanceContext);
			case ROAMERS:
				throw new RuntimeException("ROAMERS flow is not implemented yet.");
			default:
				throw new RuntimeException("Invalid template type. " + instanceContext.instance.getTemplate());
		}
	}
	
	protected IntegrationFlow createHullInputFlow(InstanceContext instanceContext) {
		LOG.info("listening to {}", instanceContext.inputChannel);
		return IntegrationFlows.from(instanceContext.inputChannel)
				.split()
				.<DataTransporter>handle((dataTransporter, h) -> dataTransporter.resetShadowMap())
				.filter(instanceContext.lacCellFilter, e -> e.discardChannel(NULL_CHANNEL))
				.filter(instanceContext.dataReductionFilter, e -> e.discardChannel(NULL_CHANNEL))
				.transform(instanceContext.mapFieldReducer)
				.<String>handle((p, h) -> instanceContext.accumulator.accumulate(p))
				.nullChannel();
	}


	protected IntegrationFlow createNetworkRailInputFlow(InstanceContext instanceContext) {
		LOG.info("listening to {}", instanceContext.inputChannel);
		
		createAndRegisterLacCellReductionFilter(instanceContext);
		
		return IntegrationFlows.from(instanceContext.inputChannel)
				.split()
				.<DataTransporter>handle((dataTransporter, h) -> dataTransporter.resetShadowMap())
				.filter(instanceContext.lacCellReductionFilter, e -> e.discardChannel(NULL_CHANNEL))
				.transform(instanceContext.mapFieldReducer)
				.<String>handle((p, h) -> instanceContext.accumulator.accumulate(p))
				.nullChannel();
	}

	protected void createAndRegisterLacCellReductionFilter(InstanceContext instanceContext) {
		LacCellReductionFilter lacCellReductionFilter = new LacCellReductionFilter();
		lacCellReductionFilter.setDataReductionFilter(instanceContext.dataReductionFilter);
		lacCellReductionFilter.setLacCellFilter(instanceContext.lacCellFilter);
		instanceContext.lacCellReductionFilter = register(instanceContext, lacCellReductionFilter, LacCellReductionFilter.class);
	}

	protected void createAndRegisterInputChannel(InstanceContext instanceContext) {
		String beanName = channelInputName(instanceContext.instance);
		LOG.info("creating input channel of name {}", beanName);
		DirectChannel channel = MessageChannels.direct(beanName).get();
		applicationContext.registerBean(beanName, DirectChannel.class, () -> channel);
		instanceContext.inputChannel = (DirectChannel)applicationContext.getBean(beanName);
	}

	protected void createAndRegisterOutputChannel(InstanceContext instanceContext) {
		String beanName = channelOutputName(instanceContext.instance);
		LOG.info("creating output channel of name {}", beanName);
		DirectChannel channel = MessageChannels.direct(beanName).get();
		applicationContext.registerBean(beanName, DirectChannel.class, () -> channel);
		instanceContext.outputChannel = (DirectChannel)applicationContext.getBean(beanName);
	}
	
	private String channelInputName(PerInstanceProperties instance) {
		return DCA_EVENT_INPUT_CHANNEL_FLOW_PREFIX + "-" + instance.getName();
	}
	
	private String channelOutputName(PerInstanceProperties instance) {
		return DCA_EVENT_OUTPUT_CHANNEL_FLOW_PREFIX + "-" + instance.getName();
	}
}
