package com.vodafone.dca.it;

import static com.vodafone.dca.it.TestUtils.assertGeneratedIsExpected;
import static com.vodafone.dca.it.TestUtils.cleanGeneratedFiles;
import static com.vodafone.dca.it.TestUtils.createAMQPChannel;
import static com.vodafone.dca.it.TestUtils.fileExists;
import static com.vodafone.dca.it.TestUtils.generatedFiles;
import static com.vodafone.dca.it.TestUtils.loadFileData;
import static com.vodafone.dca.it.TestUtils.moveDemoSampleFileToTmp;
import static com.vodafone.dca.it.TestUtils.sendMessages;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import com.rabbitmq.client.Channel;
import com.vodafone.dca.common.FileUtils;
import com.vodafone.dca.domain.properties.FilterBlackWhiteListProperties;
import com.vodafone.dca.domain.properties.MultiDemographicsProperties;
import com.vodafone.dca.domain.properties.MultiInstancesProperties;
import com.vodafone.dca.domain.properties.MultiShellProcessorsProperties;
import com.vodafone.dca.source.AmqpInboundChannel;

@SpringBootTest
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
		
	"dca.demographics-input-field-definition=demographics/demographics-in.def",
	
	"dca.demographics[0].enabled=true",
	"dca.demographics[0].name=DM1",
	"dca.demographics[0].input.field-definition=demographics/demographics-in.def",
	"dca.demographics[0].input.file-directory=${java.io.tmpdir}",
	"dca.demographics[0].input.file-pattern=*0.dat",
	"dca.demographics[0].input.file-poll-rate=400",	
	"dca.demographics[0].output.field-definition=demographics/demographics-in.def",
	"dca.demographics[0].output.end-script=demographics/endscript.sh",
	
	"dca.demographics[1].enabled=true",
	"dca.demographics[1].name=DM2",
	"dca.demographics[1].input.field-definition=demographics/demographics-in.def",
	"dca.demographics[1].input.file-directory=${java.io.tmpdir}",
	"dca.demographics[1].input.file-pattern=*1.dat",
	"dca.demographics[1].input.file-poll-rate=400",	
	"dca.demographics[1].output.field-definition=demographics/demographics-in.def",
	"dca.demographics[1].output.end-script=demographics/endscript.sh",
	"dca.demographics[1].output.anonymise-fields=imsi",
		
	"dca.input.field-definition:test-refdata/input.def",
	
	"dca.filter-bw-list.white-list-file=test-refdata/white-list.csv",
	"dca.filter-bw-list.black-list-file=test-refdata/black-list.csv",
	"dca.filter-bw-list.file-scan-frequency=120",
	"dca.filter-bw-list.filter-field=imsi",
	
	"dca.instances[0].enabled=true",
	"dca.instances[0].name=instance1",
	"dca.instances[0].template=NETWORK_RAIL",
	"dca.instances[0].filter.lac-cell.lac-cell-file=test-refdata/instance1/lac-cells.csv",
	"dca.instances[0].filter.lac-cell.follow-exit=true",
	"dca.instances[0].filter.reduction.mode=IMSIS_CHANGE_CELL_ONLY",
	"dca.instances[0].output.field-definition=test-refdata/instance1/output.def",
	"dca.instances[0].output.file-directory=${java.io.tmpdir}",
	"dca.instances[0].output.file-size-threshold=100000",
	
	"dca.instances[1].enabled=true",
	"dca.instances[1].name=instance2",
	"dca.instances[1].template=HULL",
	"dca.instances[1].filter.lac-cell.lac-cell-file=test-refdata/instance2/lac-cells.csv",
	"dca.instances[1].filter.reduction.mode=IMSIS_CHANGE_CELL",
	"dca.instances[1].output.anonymise-fields=imsi",
	"dca.instances[1].output.field-definition=test-refdata/instance2/output.def",
	"dca.instances[1].output.file-directory=${java.io.tmpdir}",
	"dca.instances[1].output.file-size-threshold=100000",
	
	"dca.instances[2].enabled=true",
	"dca.instances[2].name=instance3",
	"dca.instances[2].template=ROAMERS",
//	"dca.instances[2].filter.lac-cell.lac-cell-file=test-refdata/instance2/lac-cells.csv",
	"dca.instances[2].filter.reduction.mode=IMSIS_CHANGE_CELL",
//	"dca.instances[2].output.anonymise-fields=imsi",
	"dca.instances[2].output.field-definition=test-refdata/instance2/output.def",
	"dca.instances[2].output.file-directory=${java.io.tmpdir}",
	"dca.instances[2].output.file-size-threshold=100000",
	
	"dca.source.rabbit.properties-file=test-refdata/rabbit.properties",
	
	"dca.shell-processors[0].delay=5000",
	"dca.shell-processors[0].command=echo hello, world; sleep 10; date",
	
	"dca.shell-processors[1].delay=10000",
	"dca.shell-processors[1].command=/opt/SP/pivotal/output/capture/instance_30/bin/rt-file-process",
	
	"spring.batch.job=false"
})
@EnableConfigurationProperties
public class DcaApplicationTests {
	
	private static final Logger LOG = LoggerFactory.getLogger(DcaApplicationTests.class);
	
	@Autowired
	AmqpInboundChannel amqpInboundChannel;
	
	@Autowired
	MultiInstancesProperties instancesProperties;
	
	@Autowired
	FilterBlackWhiteListProperties filterBlackWhiteListProperties;
	
	@Autowired
	MultiShellProcessorsProperties multiShellProcessorProperties;
	
	@Autowired
	MultiDemographicsProperties multiDemographicsProperties;
	
	@Value("${java.io.tmpdir}")
	String tmpDir;
	
	Channel amqpChannel;
	
	@Before
	public void cleanTempFilesAndWaitReadyToListen() throws Exception {
		await().until(() -> amqpInboundChannel.isReadyToListen());
		amqpChannel = createAMQPChannel();
		cleanGeneratedFiles(tmpDir, "instance1", "instance2", "instance3", ".dat", ".processing", ".csv", ".new");
		File shellScript = FileUtils.fileFromPath("demographics/endscript.sh");
		shellScript.setExecutable(true);
	}
	
	@Test
	public void canGetPropertiesUsingConfigurationProperties() throws Exception {
		assertNotNull(instancesProperties);
		LOG.info("instancesProperties is {}", instancesProperties);
		
		assertNotNull(multiShellProcessorProperties);
		LOG.info("multiShellProcessorProperties is {}", multiShellProcessorProperties);
		
		assertNotNull(filterBlackWhiteListProperties);
		LOG.info("filterBlackWhiteListProperties is {}", filterBlackWhiteListProperties);
		
		assertNotNull(multiDemographicsProperties);
		LOG.info("multiDemographicsProperties is {}", multiDemographicsProperties);
	}

	@Test
	public void canDynaInstancesGenerateFilesWithCapturedData() throws Exception {
		sendRabbitMessageAndCheckGeneratedIsExpectedFor("networkrail.simple");
		sendRabbitMessageAndCheckGeneratedIsExpectedFor("sample.all.protocols");
	}
	
	@Test
	@Ignore // just to see how it reacts under fire.
	public void hammerIt() throws Exception {
		sendMessages(loadFileData("rabbit-messages/sample.all.protocols"), 3000, amqpChannel, 10);
	}
	
	@Test
	public void canBuildDemographics() throws Exception {
		String targetFile0 = moveDemoSampleFileToTmp(tmpDir, "demographics/simple.dat", "simple0.dat");
		String targetFile1 = moveDemoSampleFileToTmp(tmpDir, "demographics/simple.dat", "simple1.dat");
		
		LOG.info("Target file are {} and {}", targetFile0, targetFile1);
		
		await().until(() -> generatedFiles(tmpDir, "0.csv").count() == 1);
		await().until(() -> generatedFiles(tmpDir, "1.csv").count() == 1);
		
		assertGeneratedIsExpected(tmpDir, "0.csv", "demographics/demo0/simple.csv.expected");
		assertGeneratedIsExpected(tmpDir, "1.csv", "demographics/demo1/simple.csv.expected");
	}
	
	protected void sendRabbitMessageAndCheckGeneratedIsExpectedFor(String fileSample) throws Exception {
		LOG.info("sendAndCheckFor({})", fileSample);
		sendMessages(loadFileData("rabbit-messages/" + fileSample), 1, amqpChannel, 100);
		
		LOG.info(" - instance1");
		await().until(() -> generatedFiles(tmpDir, "instance1").count() == 1);
		Thread.sleep(2000); // wait for messages to be flushed once we start writing.
		assertGeneratedIsExpected(tmpDir, "instance1", "generated-expected/instance1/" + fileSample + ".expected");
		
		LOG.info(" - instance2");
		await().until(() -> generatedFiles(tmpDir, "instance2").count() == 1);
		assertGeneratedIsExpected(tmpDir, "instance2", "generated-expected/instance2/" + fileSample + ".expected");
		
		if(fileExists("generated-expected/instance3/" + fileSample + ".expected")) {
			LOG.info(" - instance3");
			await().until(() -> generatedFiles(tmpDir, "instance3").count() == 1);
			assertGeneratedIsExpected(tmpDir, "instance3", "generated-expected/instance3/" + fileSample + ".expected");
		} else {
			LOG.info(" - instance3 - no files expected");
			assertTrue(generatedFiles(tmpDir, "instance3").count() == 0);
		}
		
		cleanGeneratedFiles(tmpDir, "instance1", "instance2", "instance3", ".dat");
	}
}
