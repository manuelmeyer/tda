package com.dell.rti4t.xd.integration;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;


@ContextConfiguration(locations = { "classpath:test-module-amqp.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class TestModuleAggAmqp {
	
	private static Logger LOG = LoggerFactory.getLogger(TestModuleAggAmqp.class);

	@Autowired
	QueueChannel output;
	
	int totalSent = 0;
	int totalReceived = 0;
	
	public static void main(String[] args) throws Exception {
		new TestModuleAggAmqp().run();
		System.exit(0);
	}
	
	private void run() throws Exception {
		LOG.info("Loading test data");
		//final String payload = loadTestData("networkrail.simple"); // sample.all.protocols");
		final String payload = loadTestData("networkrail.simple");
		final Channel channel = createAMQPChannel();
		
		final int totalMsg = 100000;
		final int delay = 16;
		
		sendMessages(payload, totalMsg, channel, delay);
	}
	
	private void sendMessages(String payload, int total, Channel channel, int delay) {
		try {
			byte[] bytes = payload.getBytes();
			for(;;) {
				channel.basicPublish("DECODER.STREAM", "TEST.LORS.TEST", null, bytes);
				Thread.sleep(delay);
				totalSent++;
				if(totalSent % 1000 == 0) {
					LOG.info("Total sent is {}", totalSent);
				}
				if(total > 0 && totalSent == total) {
					break;
				}
			}
			LOG.info("Finish writting");
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testAggregateWithFlatFormat() throws Exception {
		LOG.info("Loading test data");
		//final String payload = loadTestData("sample.all.protocols");
		//final Channel channel = createAMQPChannel();
		
		//final int totalMsg = 2;
		//Thread.sleep(10000); // time for everything to start
		
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				sendMessages(payload, totalMsg, channel, 10);
//			}
//		}).start();
		
		long t0 = System.currentTimeMillis();
		for(int index = 0;; index++) {
			Message<String> msg = (Message<String>) output.receive(Long.MAX_VALUE);
			if(msg == null) {
				break;
			}
			String body = msg.getPayload();
			int max = Math.min(20, body.length());
			
//			if(index % 10 == 0) {
				LOG.info("Received message {}\n\twith headers {}\n\tand body {}", 
							index, 
							msg.getHeaders(),
							body.substring(0, max) + "...");
//			}
			totalReceived++;
		}
		long t1 = System.currentTimeMillis();
//		LOG.info("Total sent {}, total received {}", totalSent, totalReceived);
//		LOG.info("Total message red {} in {} ms, {} msg/s", totalMsg, (t1 - t0), (totalMsg * 1000)/(t1 - t0));
//		assertTrue(totalSent > 0 && totalSent == totalReceived);
	}

	static private String loadTestData(String file) throws Exception {
		ClassPathResource resource = new ClassPathResource(file);
		byte[] encoded = Files.readAllBytes(Paths.get(resource.getURI()));
		return new String(encoded);
	}
	
	static private Channel createAMQPChannel() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        
       factory.setUsername("decoder");
       factory.setPassword("decoder");
       factory.setVirtualHost("/");
       factory.setHost("localhost");
       factory.setPort(5672);
       Connection connection = factory.newConnection();

       Channel channel = connection.createChannel();
       return channel;

	}
}
