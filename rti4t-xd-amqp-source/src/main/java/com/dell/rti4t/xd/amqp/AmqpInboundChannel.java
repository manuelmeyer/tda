package com.dell.rti4t.xd.amqp;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.util.Assert;

import com.dell.rti4t.xd.utils.FileUtils;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class AmqpInboundChannel extends AbstractEndpoint {

	private static Logger LOG = LoggerFactory.getLogger(AmqpInboundChannel.class);

	private ConsumerDelegate consumerDelegate;

	private int readerCount;
	private String propertiesPath;
	private String routingKey;
	private String exchange;
	private String userName;
	private String password;
	private String vHost;
	private String queueName;
	private Integer ttl;
	private Integer port;
	private String host;
	private int qos = 1;

	private Channel channel;
	
	public void setPrefetchCount(int prefetchCount) {
		this.qos = prefetchCount;
	}

	public void setReaderCount(int readerCount) {
		this.readerCount = readerCount;
	}
	
	public void setConsumerDelegate(ConsumerDelegate consumerDelegate) {
		this.consumerDelegate = consumerDelegate;
	}
	
	public class AmqpConsumer extends DefaultConsumer {
		
		public AmqpConsumer(Channel channel) {
			super(channel);
		}
		
		@Override
		public void handleDelivery(String consumerTag, Envelope envelope, BasicProperties properties, byte[] body) throws IOException {
			consumerDelegate.consume(body, properties.getHeaders());
            channel.basicAck(envelope.getDeliveryTag(), false);
		}
	}
	
	@Override
	protected void onInit() throws Exception {
		loadProperties();
		createChannelAndQ();
	}
	
	private void loadProperties() throws Exception {
		Assert.notNull(propertiesPath, "propertiesPath property cannot be null");
		LOG.info("Reading properties from {}", propertiesPath);
		
		InputStream input = new FileInputStream(FileUtils.fileFromPath(propertiesPath)); // inputStreamFromPath(propertiesPath);
		Properties properties = new Properties();
		properties.load(input);
		
		routingKey = properties.getProperty("rabbit.routingkey");
		exchange = properties.getProperty("rabbit.exchange");
		userName = properties.getProperty("rabbit.username");
		password = properties.getProperty("rabbit.password");
		queueName = properties.getProperty("rabbit.q.name");

		vHost = properties.getProperty("rabbit.vHost", "/");
		host = properties.getProperty("rabbit.host", "localhost");
		ttl = Integer.valueOf(properties.getProperty("rabbit.q.ttl", "60000"));
		port = Integer.valueOf(properties.getProperty("rabbit.port", "50672"));
		qos = Integer.valueOf(properties.getProperty("rabbit.qos", "1"));
	}

	public void setRabbitPropertiesFile(String path) {
		this.propertiesPath = path;
	}
	
	private void startListeners() throws Exception {
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Thread.sleep(5000);
					LOG.info("Binding {} to {}[{}]", queueName, exchange, routingKey);
				    channel.queueBind(queueName, exchange, routingKey);
			
				    LOG.info("Starting {} basicConsumers", readerCount);
					for(int index = 0; index < readerCount; index++) {
						AmqpConsumer consumer = new AmqpConsumer(channel);
				        channel.basicConsume(queueName, false, consumer);
					}
				} catch(Exception e) {
					LOG.error("Cannot start listeners", e);
				}
			}
			
		}).start();
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void createChannelAndQ() throws Exception {
		ConnectionFactory factory = new ConnectionFactory();
        
        factory.setUsername(userName);
        factory.setPassword(password);
        factory.setVirtualHost(vHost);
        factory.setHost(host);
        factory.setPort(port);
        Connection connection = factory.newConnection();

        channel = connection.createChannel();
        channel.basicQos(qos);
        LOG.info("Creating a channel with a QoS of {}", qos);
        
        channel.queueDeclare(queueName, false, false, true, (Map)Collections.singletonMap("x-message-ttl", ttl));
 	}

	@Override
	protected void doStart() {
		LOG.info("Requested to start");
		try {
			consumerDelegate.initialise();
			startListeners();
		} catch(Exception e) {
			LOG.error("Cannot bind and start listeners");
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void doStop() {
		LOG.info("Requested to stop");
		try {
			consumerDelegate.shutdown();
			channel.close();
		} catch(Exception e) {
			LOG.error("Error while closing channel", e);
		}
	}
}
