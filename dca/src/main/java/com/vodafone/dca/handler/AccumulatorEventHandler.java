package com.vodafone.dca.handler;

import static java.lang.System.currentTimeMillis;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

public class AccumulatorEventHandler implements SmartLifecycle {
	
	private static final Logger LOG = LoggerFactory.getLogger(AccumulatorEventHandler.class);
	
	private boolean isRunning = false;
	
	class Context {
		StringBuilder builder = new StringBuilder(32 * 1024);
		int accumulated = 0;
		long watchDog = 0;
	}
	
	private Context context;
	
	private String handlerName;
	private long batchTimeout;
	private int batchSize;
	private MessageChannel outputChannel;

	public AccumulatorEventHandler(String handlerName, MessageChannel outputChannel, int batchSize, int batchTimeout) {
		this.handlerName = handlerName;
		this.outputChannel = outputChannel;
		this.batchSize = batchSize;
		this.batchTimeout = batchTimeout;
		this.context = new Context();
	}

	@PostConstruct
	public void startWatchDog() {
		new Thread(() -> waitAndFlush()).start();
	}

	private void waitAndFlush() {
		for(;;) {
			try {
				Thread.sleep(1000);
				if(isRunning()) {
					flushOnTimeout();
				}
			} catch(InterruptedException ie) {
				return;
			}
		}
	}

	protected synchronized void flushOnTimeout() {
		long now = System.currentTimeMillis();
		if(context.accumulated > 0 && (context.watchDog + batchTimeout - now) < 0) {
			LOG.debug("Timeout expired for {}", Thread.currentThread().getName());
			fflush(context);
		}
	}
	
	public Object accumulate(String dt) {
		context.builder.append(dt).append("\n");
		context.watchDog = currentTimeMillis();
		context.accumulated++;
		if (context.accumulated == batchSize) {
			fflush(context);
		}
		return null;
	}

	private void fflush(Context context) {
		try {
			if(context.accumulated > 0) {
				Message<String> msg = MessageBuilder
						.withPayload(context.builder.toString())
						.setHeader("data-type", handlerName)
						.build();
				context.builder.setLength(0);
				context.accumulated = 0;
				if(isRunning()) {
					outputChannel.send(msg);
				} else {
					LOG.warn("Discarding {} events in {} ms - component is not running.", context.accumulated);
				}
			}
		} catch(Exception e) {
			LOG.error("Cannot flush message", e);
		}
	}

	@Override
	public void start() {
		isRunning = true;
	}

	@Override
	public void stop() {
		isRunning = false;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	@Override
	public int getPhase() {
		return 0;
	}
}
