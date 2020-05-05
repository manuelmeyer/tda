package com.dell.rti4t.xd.synctap;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.monitor.ExponentialMovingAverageRate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

//@ManagedResource
public class SyncTapDispatcher extends AbstractEndpoint {
	
	private static final Logger LOG = LoggerFactory.getLogger(SyncTapDispatcher.class);
	
	private boolean isStarted = false;
	
	protected String streamName; 
	protected String sourceChannel;
	protected MessageChannel outputChannel;
	protected MessageChannel errorChannel;
	
	private Thread interceptThread;

	public void setStreamName(String streamName) {
		this.streamName = streamName;
	}
	
	public void setSourceChannel(String sourceChannel) {
		this.sourceChannel = sourceChannel;
	}
	
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}
	
	ExponentialMovingAverageRate errors = new ExponentialMovingAverageRate(1, 60, 10);
	
	class Interceptor extends ChannelInterceptorAdapter {
		@Override
		public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
			try {
				outputChannel.send(message);
			} catch(Exception e) {
				if(!e.getClass().equals(MessageDeliveryException.class)) {
					errors.increment();
					errorChannel.send(MessageBuilder.withPayload(e).build());
				}
			}
		}
	}
	
	protected DirectChannel interceptDirectChannel(String name) {
		ApplicationContext context = getApplicationContext().getParent();
		Map<String, DirectChannel> beans = context.getBeansOfType(DirectChannel.class);
		for(Entry<String, DirectChannel> entry : beans.entrySet()) {
			String channelName = entry.getKey();
			if(sourceChannel.equals(channelName)) {
				return entry.getValue();
			}
		}
		return null;
	}
	
	private void boundInterceptor(String sourceChannel) {
		DirectChannel channel = interceptDirectChannel(sourceChannel);
		if(channel != null) {
			for(ChannelInterceptor interceptor : channel.getChannelInterceptors()) {
				if(interceptor.getClass() == Interceptor.class) {
					return;
				}
			}
			LOG.info("Bouding interceptor to {}", sourceChannel);
			channel.addInterceptor(new Interceptor());
		}
	}

	
	private void unboundInterceptor(String sourceChannel) {
		DirectChannel channel = interceptDirectChannel(sourceChannel);
		if(channel != null) {
			for(ChannelInterceptor interceptor : channel.getChannelInterceptors()) {
				if(interceptor.getClass() == Interceptor.class) {
					LOG.info("removing interceptor to {}", sourceChannel);
					channel.removeInterceptor(interceptor);
					return;
				}
			}
		}
	}

	class InterceptChannel implements Runnable {

		@Override
		public void run() {
			for(;;) {
				if(isStarted) {
					boundInterceptor(sourceChannel);
					LOG.info("Send in error {}", errors.getStatistics().toString());
				}
				try {
					Thread.sleep(10000);
				} catch(InterruptedException e) {
					return;
				}
			}
		}
	}
  
	@Override
	protected void onInit() throws Exception {
		if(StringUtils.isEmpty(sourceChannel)) {
			throw new IllegalArgumentException("Source channel cannot be empty");
		}
		sourceChannel = sourceChannel + ".0";
		LOG.info("Will sync tap {}", sourceChannel);
		errorChannel = getApplicationContext().getBean("errorChannel", MessageChannel.class);
	}
	
	@Override
	protected void doStart() {
		LOG.info("Starting sync tap");
		isStarted = true;
		interceptThread = new Thread(new InterceptChannel());
		interceptThread.start();
	}
	
	@Override
	protected void doStop() {
		if(isStarted) {
			LOG.info("Stoping sync tab");
			isStarted = false;
			interceptThread.interrupt();
			unboundInterceptor(sourceChannel);
		}
	}
}
