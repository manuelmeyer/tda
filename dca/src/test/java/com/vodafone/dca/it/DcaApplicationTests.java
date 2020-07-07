package com.vodafone.dca.it;

import static com.vodafone.dca.it.TestUtils.assertGeneratedIsExpected;
import static com.vodafone.dca.it.TestUtils.cleanGeneratedFiles;
import static com.vodafone.dca.it.TestUtils.createAMQPChannel;
import static com.vodafone.dca.it.TestUtils.generatedFiles;
import static com.vodafone.dca.it.TestUtils.loadFileData;
import static com.vodafone.dca.it.TestUtils.sendMessages;
import static org.awaitility.Awaitility.await;
import static org.junit.Assert.assertNotNull;

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
import com.vodafone.dca.domain.MultiInstancesProperties;
import com.vodafone.dca.domain.MultiShellProcessorsProperties;
import com.vodafone.dca.domain.PerInstanceProperties;
import com.vodafone.dca.source.AmqpInboundChannel;

@SpringBootTest
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
	"dca.input.field-definition:test-refdata/input.def",
	
	"dca.instances[0].enabled=true",
	"dca.instances[0].name=instance1",
	"dca.instances[0].template=NETWORK_RAIL",
	"dca.instances[0].filter.lac-cell.lac-cell-file=test-refdata/instance1/lac-cells.csv",
	"dca.instances[0].filter.lac-cell.follow-exit=true",
	"dca.instances[0].filter.reduction.mode=IMSIS_CHANGE_CELL_ONLY",
	"dca.instances[0].output.field-definition=test-refdata/instance1/output.def",
	"dca.instances[0].output.file-directory=${java.io.tmpdir}",
	"dca.instances[0].output.file-size-threshold=10000",
	
	"dca.instances[1].enabled=true",
	"dca.instances[1].name=instance2",
	"dca.instances[1].template=HULL",
	"dca.instances[1].filter.lac-cell.lac-cell-file=test-refdata/instance2/lac-cells.csv",
	"dca.instances[1].filter.reduction.mode=IMSIS_CHANGE_CELL",
	"dca.instances[1].output.anonymise-fields=imsi",
	"dca.instances[1].output.field-definition=test-refdata/instance2/output.def",
	"dca.instances[1].output.file-directory=${java.io.tmpdir}",
	"dca.instances[1].output.file-size-threshold=10000",
	
	"dca.source.rabbit.properties-file=test-refdata/rabbit.properties",
	
	"dca.shell-processors[0].delay=5000",
	"dca.shell-processors[0].command=echo hello, world; sleep 10; date",
	
	"dca.shell-processors[1].delay=10000",
	"dca.shell-processors[1].command=/opt/SP/pivotal/output/capture/instance_30/bin/rt-file-process"
})
@EnableConfigurationProperties
public class DcaApplicationTests {
	
	private static final Logger LOG = LoggerFactory.getLogger(DcaApplicationTests.class);
	
	@Autowired
	AmqpInboundChannel amqpInboundChannel;
	
	@Autowired
	MultiInstancesProperties instancesProperties;
	
	@Autowired
	PerInstanceProperties instance1Properties;
	
	@Autowired
	PerInstanceProperties instance2Properties;
	
	@Autowired
	MultiShellProcessorsProperties multiShellProcessorProperties;

	@Value("${java.io.tmpdir}")
	String tmpDir;
	
	Channel amqpChannel;
	
	@Before
	public void cleanTempFilesAndWaitReadyToListen() throws Exception {
		await().until(() -> amqpInboundChannel.isReadyToListen());
		amqpChannel = createAMQPChannel();
		cleanGeneratedFiles(tmpDir, "instance1", "instance2");
	}
	
	@Test
	public void canGetPropertiesUsingConfigurationProperties() throws Exception {
		assertNotNull(instancesProperties);
		LOG.info("instancesProperties is {}", instancesProperties);
		
		assertNotNull(instance1Properties);
		LOG.info("instance1Properties is {}", instance1Properties);
		
		assertNotNull(instance2Properties);
		LOG.info("instance2Properties is {}", instance2Properties);
		
		assertNotNull(multiShellProcessorProperties);
		LOG.info("multiShellProcessorProperties is {}", multiShellProcessorProperties);
		
		Thread.sleep(40000);
	}

	@Test
	public void canGenerateInstance1Files() throws Exception {
		sendMessages(loadFileData("rabbit-messages/networkrail.simple"), 1, amqpChannel, 100);
		await()
			.until(() -> generatedFiles(tmpDir, "instance1").count() == 1);
		assertGeneratedIsExpected(tmpDir, "instance1", "generated-expected/instance1/networkrail.simple.expected");
		
		await()
			.until(() -> generatedFiles(tmpDir, "instance2").count() == 1);
		assertGeneratedIsExpected(tmpDir, "instance2", "generated-expected/instance2/networkrail.simple.expected");
	}
	
	@Test
	@Ignore // just to see how it reacts under fire.
	public void hammerIt() throws Exception {
		sendMessages(loadFileData("rabbit-messages/sample.all.protocols"), 3000, amqpChannel, 10);
	}
}
