package com.dell.rti4t.xd.amqp;

import static java.lang.Thread.currentThread;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslatorTwoArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;

public class StringConsumerDelegateImpl implements ConsumerDelegate {
	
	static private final Logger LOG = LoggerFactory.getLogger(StringConsumerDelegateImpl.class);

	private MessageChannel channel;
	
	private int bufferSize = 128;
	
	private Executor executor = Executors.newCachedThreadPool();

	private Map<String, DataTransporterProducerWithTranslator> producerMap = new ConcurrentHashMap<String, DataTransporterProducerWithTranslator>();
	private List<DataTransporterEventHandler> handlers = new ArrayList<DataTransporterEventHandler>();
	
	private int parallelConsumers = 2;

	private boolean isRunning;
	
	public void setParallelConsumers(int parallelConsumers) {
		this.parallelConsumers = parallelConsumers;
	}
	
	public class MessageFlowFactory implements EventFactory<MessageFlow> {
		public MessageFlow newInstance() {
			return new MessageFlow();
		}
	}

	public class DataTransporterEventHandler implements WorkHandler<MessageFlow> {
		class Context {
			final String filter;
			final StringBuilder builder;
			int accumulated = 0;
			long watchDog = 0;
			public Context(String filter) {
				this.filter = filter;
				this.builder = new StringBuilder(32 * 1024);
			}
		}
		
		final String filter;
		Map<String, Context> contextMap = new HashMap<String, Context>();

		public DataTransporterEventHandler(String filter) {
			this.filter = filter;
		}
		
		public void onEvent(MessageFlow event) throws Exception {
			try { 
				if(isRunning) {
					String message = new String(event.body);
					Message<String> msg = MessageBuilder
							.withPayload(message)
							.copyHeaders(event.headers)
							.build();
					if(LOG.isTraceEnabled()) {
						LOG.trace("Sending {}", msg.toString());
					}
					channel.send(msg);
				}
			} catch(Exception e) {
				LOG.error("Exception while processing event", e);
			} finally {
				event.body = null;
				event.headers = null;
			}
		}
	}

	public class DataTransporterProducerWithTranslator {
		private final RingBuffer<MessageFlow> ringBuffer;

		public DataTransporterProducerWithTranslator(RingBuffer<MessageFlow> ringBuffer) {
			this.ringBuffer = ringBuffer;
		}

		private final EventTranslatorTwoArg<MessageFlow, byte[], Map<String, Object>> translator = new EventTranslatorTwoArg<MessageFlow, byte[], Map<String, Object>>() {
			public void translateTo(MessageFlow event, long sequence, byte[] body, Map<String, Object> headers) {
				event.body = body;
				event.headers = headers;
			}
		};

		public void onData(byte[] b, Map<String, Object> map) {
			if(isRunning) {
				if(LOG.isTraceEnabled()) {
					LOG.trace("Getting a msg of size {}", b.length);
				}
				ringBuffer.publishEvent(translator, b, map);
			}
		}
	}

	public StringConsumerDelegateImpl() {
		LOG.info("New consumer delegate created");
	}

	class MessageFlow {
		Map<String, Object> headers;
		byte[] body;
	}
	
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}

	public void setMessageChannel(MessageChannel channel) {
		this.channel = channel;
	}

	private DataTransporterProducerWithTranslator getProducer(String filter) {
		DataTransporterProducerWithTranslator producer = producerMap.get(filter);
		if (producer == null) {
			synchronized (handlers) {
				if (producerMap.get(filter) == null) {
					LOG.info("Creating disruptor for {}", filter);
					producer = createNewProducer(filter);
					producerMap.put(filter, producer);
				}
			}
		}
		return producer;
	}

	private DataTransporterProducerWithTranslator createNewProducer(String filter) {
		Disruptor<MessageFlow> disruptor = new Disruptor<>(new MessageFlowFactory(), bufferSize, executor);
		DataTransporterEventHandler[] eventHandlers = new DataTransporterEventHandler[parallelConsumers];
		LOG.info("Creating {} consumer(s) for {}", parallelConsumers, filter);
		for(int index = 0; index < parallelConsumers ; index++) {
			DataTransporterEventHandler eventHandler = new DataTransporterEventHandler(filter);
			eventHandlers[index] = eventHandler;
			this.handlers.add(eventHandler);
		}
		disruptor.handleEventsWithWorkerPool(eventHandlers);
		RingBuffer<MessageFlow> ringBuffer = disruptor.getRingBuffer();

		DataTransporterProducerWithTranslator producer = new DataTransporterProducerWithTranslator(ringBuffer);
		disruptor.start();
		return producer;
	}

	@Override
	public void initialise() {
		LOG.info("initialising consumer");
		isRunning = true;
	}

	@Override
	public void consume(byte[] b, Map<String, Object> map) {
		getProducer(currentThread().getName()).onData(b, map);
	}

	@Override
	public void shutdown() {
		isRunning = false;
	}
}
