package com.dell.rti4t.xd.integration;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.dell.rti4t.xd.kafkasource.KafkaDispatcher;

@ContextConfiguration(locations = { "classpath:test-kafka-dispatcher.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class TestKafkaDispatcher {
	
	private static Logger LOG = LoggerFactory.getLogger(TestKafkaDispatcher.class);

	@Autowired
	QueueChannel output;

	@Autowired
	KafkaDispatcher dispatcher;
	long totalMessages = 0;
	
	@Test
	public void canReceiveMessage() {
		long total = 0;
		long totalMsg = 0;
		for(int index = 0; index < 200000; index++) {
			Message<?> message = output.receive();
			int size = ((List)message.getPayload()).size();
			total += size;
			if(total++ % 100 == 0) {
				LOG.info("Received {} events", total);
			}
		}	
		dispatcher.stop();
	}
}
