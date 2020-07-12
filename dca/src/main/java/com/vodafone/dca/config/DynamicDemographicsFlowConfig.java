package com.vodafone.dca.config;

import static com.vodafone.dca.common.BeanUtils.beanName;
import static com.vodafone.dca.common.BeanUtils.register;
import static com.vodafone.dca.handler.DemographicsFileMoveHandler.changeFileName;

import java.io.File;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.dsl.context.IntegrationFlowContext;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.dsl.FileInboundChannelAdapterSpec;
import org.springframework.integration.file.dsl.Files;
import org.springframework.util.StringUtils;

import com.vodafone.dca.common.BeanUtils;
import com.vodafone.dca.domain.properties.DemographicsInputProperties;
import com.vodafone.dca.domain.properties.DemographicsOutputProperties;
import com.vodafone.dca.domain.properties.DemographicsProperties;
import com.vodafone.dca.domain.properties.MultiDemographicsProperties;
import com.vodafone.dca.handler.DemographicFileOutputWriter;
import com.vodafone.dca.service.PepperManager;
import com.vodafone.dca.transformer.MapFieldReducer;
import com.vodafone.dca.transformer.ParsedElementListToDataTransporter;


@Configuration
@Order(0)
public class DynamicDemographicsFlowConfig {
	
	protected static final Logger LOG = LoggerFactory.getLogger(DynamicDemographicsFlowConfig.class);
	
	@Autowired
	protected MultiDemographicsProperties multiDemographicsProperties;
	
	@Autowired
	protected GenericApplicationContext applicationContext;
	
	@Autowired
	protected IntegrationFlowContext integrationFlowContext;
	
	class DemographicsContext {
		String name;
		FileReadingMessageSource fileSource;
		
		MapFieldReducer reducer;
		DelimitedLineTokenizer lineTokenizer;
		ParsedElementListToDataTransporter parser;
		DemographicFileOutputWriter outputWriter;
		
		DemographicsProperties demographics;
		DemographicsInputProperties input;
		DemographicsOutputProperties output;
		PepperManager pepperManager;
				
		DemographicsContext(DemographicsProperties demographics) {
			this.demographics = demographics;
			this.name = demographics.getName();
			this.input = demographics.getInput();
			this.output = demographics.getOutput();
		}
	}

	@PostConstruct
	public void buildDynamicInstances() {
		LOG.info("Creating {} dynamic demographics...", multiDemographicsProperties.getDemographics().size());
		multiDemographicsProperties.getDemographics().forEach(demographic -> buildDynamicInstance(new DemographicsContext(demographic)));
	}

	protected void buildDynamicInstance(DemographicsContext context) {
		if(context.demographics.isEnabled()) {
			createAndRegisterFileSource(context);
			createAndRegisterPepperManager(context);
			
			createLineTokenizer(context);
			createAndRegisterMapReducer(context);
			createAndRegisterParser(context);
			createAndRegisterOutputFileWriter(context);
			
			createAndRegisterDemographicsFlow(context);
		}
	}
	
	protected void createAndRegisterPepperManager(DemographicsContext context) {
		PepperManager pepperManager = new PepperManager();
		String pepper = context.output.getSalt();
		if (StringUtils.isEmpty(pepper)) {
			pepperManager.setPepper(pepper);
		}
		context.pepperManager = BeanUtils.register(applicationContext, context.name, pepperManager, PepperManager.class);
	}
	
	protected void createAndRegisterDemographicsFlow(DemographicsContext context) {
		IntegrationFlow demographicsFlow = demographicsFlow(context);
		integrationFlowContext.registration(demographicsFlow)
								.autoStartup(true)
								.register();
	}

	protected void createAndRegisterParser(DemographicsContext context) {
		ParsedElementListToDataTransporter parser = new ParsedElementListToDataTransporter();
		parser.setFieldNamesDefinitionFile(context.input.getFieldDefinition());
		context.parser = register(applicationContext, context.name, parser, ParsedElementListToDataTransporter.class);
	}

	protected void createAndRegisterMapReducer(DemographicsContext context) {
		MapFieldReducer reducer = new MapFieldReducer();
		reducer.setFieldsOutDefinitionFile(context.output.getFieldDefinition());
		reducer.setPepperManager(context.pepperManager);
		reducer.setAnonymiseSet(context.output.getAnonymiseFields());
		context.reducer = register(applicationContext, context.name, reducer, MapFieldReducer.class);
	}

	protected void createLineTokenizer(DemographicsContext context) {
		context.lineTokenizer = new DelimitedLineTokenizer();
		context.lineTokenizer.setDelimiter(context.input.getFieldDelimiter());
	}
	
	protected void createAndRegisterOutputFileWriter(DemographicsContext context) {
		DemographicFileOutputWriter outputWriter = DemographicFileOutputWriter.newBuilder()
			.withLineTokeniserAndParser(context.lineTokenizer, context.parser)
			.withReducer(context.reducer)
			.withOutputProperties(context.output)
			.build();
		context.outputWriter = register(applicationContext, context.name, outputWriter, DemographicFileOutputWriter.class);
	}
	
	protected void createAndRegisterFileSource(DemographicsContext context) {
		FileInboundChannelAdapterSpec fileSource = demographicsFileSource(context);
		context.fileSource = (FileReadingMessageSource)(Object)register(applicationContext, context.name, fileSource, FileInboundChannelAdapterSpec.class);
	}

	protected FileInboundChannelAdapterSpec demographicsFileSource(DemographicsContext context) {
		return Files.inboundAdapter(new File(context.input.getFileDirectory()))
				.preventDuplicates(false)
				.patternFilter(context.input.getFilePattern());
	}
	
	protected IntegrationFlow demographicsFlow(DemographicsContext context) {
		return IntegrationFlows.from(context.fileSource, 
					c -> c.poller(Pollers.fixedRate(context.input.getFilePollRate())
										.maxMessagesPerPoll(100))
				)
				.log("com.vodafone.dca.demographics.flow.1-" + context.name)
				.<File>handle((file, h) -> changeFileName(file, context.input.getFileSuffixNew()))
				.log("com.vodafone.dca.demographics.flow.2-" + context.name)
				.<File>handle((file, h) -> context.outputWriter.generateOutputFile(file))
				.nullChannel();
	}
}
