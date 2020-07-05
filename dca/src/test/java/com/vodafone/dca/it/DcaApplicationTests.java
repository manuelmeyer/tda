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
import com.vodafone.dca.domain.InstancesProperties;
import com.vodafone.dca.source.AmqpInboundChannel;

@SpringBootTest
@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
	"dca.input.field-definition:test-refdata/input.def",
	
	"dca.instances.instance1.enabled=true",
	"dca.instances.instance1.filter.lac-cell.lac-cell-file=test-refdata/instance1/lac-cells.csv",
	"dca.instances.instance1.filter.reduction.mode=IMSIS_CHANGE_CELL_ONLY",
	"dca.instances.instance1.output.field-definition=test-refdata/instance1/output.def",
	"dca.instances.instance1.output.file-directory=${java.io.tmpdir}",
	"dca.instances.instance1.output.file-size-threshold=10000",
	
	"dca.instances.instance2.enabled=true",
	"dca.instances.instance2.filter.lac-cell.lac-cell-file=test-refdata/instance2/lac-cells.csv",
	"dca.instances.instance1.filter.reduction.mode=IMSIS_CHANGE_CELL",
	"dca.instances.instance2.output.anonymise-fields=imsi",
	"dca.instances.instance2.output.field-definition=test-refdata/instance2/output.def",
	"dca.instances.instance2.output.file-directory=${java.io.tmpdir}",
	"dca.instances.instance2.output.file-size-threshold=10000",
	
	"dca.source.rabbit.properties-file=test-refdata/rabbit.properties"
})
@EnableConfigurationProperties
public class DcaApplicationTests {
	
	private static final Logger LOG = LoggerFactory.getLogger(DcaApplicationTests.class);
	
	@Autowired
	AmqpInboundChannel amqpInboundChannel;
	
	@Autowired
	InstancesProperties instancesProperties;

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
	public void canGetPropertiesUsingConfigurationProperties() {
		assertNotNull(instancesProperties);
		LOG.info("instancesProperties is {}", instancesProperties);
	}

	@Test
	public void canGenerateInstance1Files() throws Exception {
		sendMessages(loadFileData("rabbit-messages/networkrail.simple"), 1, amqpChannel, 100);
		await()
			.until(() -> generatedFiles(tmpDir, "instance1").count() == 1 && generatedFiles(tmpDir, "instance2").count() == 1);
		assertGeneratedIsExpected(tmpDir, "instance1", "generated-expected/instance1/networkrail.simple.expected");
		assertGeneratedIsExpected(tmpDir, "instance2", "generated-expected/instance2/networkrail.simple.expected");
	}
}
