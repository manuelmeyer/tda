package com.dell.rti4t.xd.kafkasource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.monitor.ExponentialMovingAverageRate;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.StringUtils;

import com.dell.rti4t.xd.utils.FileUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.WorkHandler;
import com.lmax.disruptor.dsl.Disruptor;

//@ManagedResource
public class KafkaDispatcher extends AbstractEndpoint {
	
	private static final Logger LOG = LoggerFactory.getLogger(KafkaDispatcher.class);
	
	final static String ILLEGAL_ARG_PARALLEL_READERS = "Parallel topic readers value must be between 1 and 128 - found %s";
	final static String ILLEGAL_ARG_PARALLEL_SOURCES = "Parallel sources value must be between 1 and 128 - found %s";
	final static String ILLEGAL_BUFFER_SIZE = "Buffer size must me a power of 2 - found %s";
	final static String ILLEGAL_PROPERTIES_FILE = "Properties <%s> file does not exist";
	
	private boolean isStarted = false;
	
	private int bufferSize = 8192;
	private int perTopicConsumers = 4;
	private int parallelSources = 4;
	protected int outputBatchSize = 2048;
	private ExecutorService kafkaConsumerExecutor; 
	private RingBuffer<FanoutEvent> ringBuffer;
	private Disruptor<FanoutEvent> disruptor;
	private MessageChannel outputChannel;	
	private long pollValue = 0;
	
	public void setOutputBatchSize(int outputBatchSize) {
		this.outputBatchSize = outputBatchSize;
	}
	
	private Map<String, Integer> topicAndThreads = new HashMap<String, Integer>();
	protected String streamName; // unused so far but that is the XD stream name
	
	private Properties props = new Properties();
    private String consumerPropertiesFile;
    
    @ManagedMetric(description = "slots left in the ring buffer",
            metricType = MetricType.COUNTER, category = "ringbuffer")
    public long getBufferRemainingCapacity() {
    	return ringBuffer.remainingCapacity();
    }
	
	private ExecutorService ringBufferExecutor = Executors.newCachedThreadPool(
			new ThreadFactoryBuilder().setNameFormat("ring-buffer-readers-%d").build()
	);
	
	public void setPollValue(long pollValue) {
		this.pollValue = pollValue;
	}
	public void setStreamName(String streamName) {
		this.streamName = streamName;
	}
	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
	}
	public void setKafkaPerTopicConsumers(int perTopicConsumers) {
		this.perTopicConsumers = perTopicConsumers;
	}
	public void setParallelSources(int parallelSources) {
		this.parallelSources = parallelSources;
	}
	public void setConsumerPropertiesFile(String consumerPropertiesFile) {
		this.consumerPropertiesFile = consumerPropertiesFile;
	}
	
	public void setTopics(String[] topics) {
		if(topics != null) {
			for(String topic : topics) {
				addTopic(topic);
			}
		}
	}
	
	private void addTopic(String topic) {
		String[] values = topic.split(":");
		if(values.length == 1) {
			topicAndThreads.put(values[0], 1);
		} else {
			topicAndThreads.put(values[0], Integer.valueOf(values[1]));
		}
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}
	
	public class FanoutEvent {
		public Object event;
	}
	
	public class FanoutEventFactory implements EventFactory<FanoutEvent> {
		public FanoutEvent newInstance() {
			return new FanoutEvent();
		}
	}
	
	List<KafkaConsumer<String, byte[]>> consumers = new ArrayList<KafkaConsumer<String, byte[]>>();
	
	private CountDownLatch startupLatch = new CountDownLatch(1);
    Map<String, ExponentialMovingAverageRate> globalRates = new TreeMap<String, ExponentialMovingAverageRate>();
	
    public class LogStats implements Runnable {
		@Override
		public void run() {
			while(isStarted) {
				try {
					// dumpApplicationContext();
					StringBuffer sb = new StringBuffer("[").append(streamName).append("] <");
					String separator = "";
					double total = 0.0;
					for(Entry<String, ExponentialMovingAverageRate> rate : globalRates.entrySet()) {
						double mean = rate.getValue().getMean();
						sb.append(separator).append(rate.getKey()).append("=").append(String.format("%1$,.2f", mean));
						total += mean;
						separator = ",";
					}
					sb.append("> total=").append(String.format("%1$,.2f", total));
					sb.append(" remainingCapacity=").append(String.format("%1$,d", getBufferRemainingCapacity()));
					LOG.info(sb.toString());
				} catch(Exception e) {
					LOG.error("error gathering stats", e);
				}
				try {
					Thread.sleep(5000);
				} catch(Exception e) {
					return;
				}
			}
		}
		
		class Interceptor extends ChannelInterceptorAdapter {
			@Override
			public Message<?> preSend(Message<?> message, MessageChannel channel) {
				return message;
			}
		}

		private void dumpApplicationContext() {
			ApplicationContext context = getApplicationContext().getParent();
			LOG.info("Parent applicationName {}", context.getApplicationName());
			Map<String, DirectChannel> beans = context.getBeansOfType(DirectChannel.class);
			//getBeanFactory()
			for(Entry<String, DirectChannel> entry : beans.entrySet()) {
				if(entry != null && entry.getValue() != null) {
					LOG.info("{}={}", entry.getKey(), entry.getValue().getClass().getCanonicalName());
					DirectChannel channel = entry.getValue();
					LOG.info("Full channel name is {}", channel.getFullChannelName());
					boolean intercepted = false;
					for(ChannelInterceptor interceptor : channel.getChannelInterceptors()) {
						if(interceptor.getClass() == Interceptor.class) {
							intercepted = true;
							break;
						}
					}
					if(!intercepted) {
						channel.addInterceptor(new Interceptor());
					}
				}
			}
		}
    }
    
    public class InternalKafkaConsumer implements Runnable {
    	List<String> topics = new ArrayList<String>();
    	ExponentialMovingAverageRate topicRate;
    	
		public InternalKafkaConsumer(String topic, ExponentialMovingAverageRate rate) {
			topics.add(topic);
			topicRate = rate;
		}

		@Override
		public void run() {
			waitStartSignal();
			try(KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props)) {
				registerConsumer(consumer);
			    consumer.subscribe(this.topics);
				while(isStarted) {
					ConsumerRecords<String, byte[]> records = consumer.poll(pollValue);
					int recordCout = records.count();
					if(recordCout > 0) {
						List<byte[]> events = new ArrayList<byte[]>(recordCout);
						Iterator<ConsumerRecord<String, byte[]>> iterator = records.iterator();
						while(iterator.hasNext()) {
							topicRate.increment();
							events.add(iterator.next().value());
						}
						ringBuffer.publishEvent(translator, events);
					}
				}
			} catch(Exception e) {
				if(isStarted) {
					LOG.error("Consumer thread ended with exception", e);
				}
			}
    	}

		private void waitStartSignal() {
			try {
				startupLatch.await();
			} catch(InterruptedException e) {
				LOG.error("startup interrupted - exiting kafka reader thread");
				throw new RuntimeException(e);
			}
		}

		private void accumulateEvent(byte[] value) {
		}
    }
    
    class LocalMapper extends ThreadLocal<ObjectMapper> {
    	@Override
    	protected ObjectMapper initialValue() {
    		return new ObjectMapper();
    	}
    }
    
    LocalMapper localMapper = new LocalMapper();
    
	public class FanoutEventHandler implements WorkHandler<FanoutEvent> {
		private ProtocolEventMapper eventMapper;
		
		public FanoutEventHandler() {
			eventMapper = new ProtocolEventMapper();
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public void onEvent(FanoutEvent eventEvent) throws Exception {
			if(isStarted) {
				try {
					List<byte[]> events = (List<byte[]>)eventEvent.event;
					List<Map<String, Object>> currentBatch = new ArrayList<Map<String, Object>>(events.size());
					for(byte[] event : events) {
						currentBatch.add(shiftValues(localMapper.get().readValue(event, HashMap.class)));
					}
					Message<List<Map<String, Object>>> msg = MessageBuilder.withPayload(currentBatch).build();
					outputChannel.send(msg);
					eventEvent.event = null; // discard the event from the ring buffer
				} catch(Exception e) {
					LOG.error("Error while processing event", e);
				}
			}
		}

		private Map<String, Object> shiftValues(Map<String, Object> newValue) {
			eventMapper.map(newValue);
			ProtocolEventToSubscriber.turnToSubscriber(eventMapper);
			flattenMaps(newValue, eventMapper.getProtocolDetails());
			return newValue;
		}

		private void flattenMaps(Map<String, Object> newValue, Map<String, Object> protocolDetails) {
			for(Entry<String, Object> entry : protocolDetails.entrySet()) {
				newValue.put("protocolDetailMap." + entry.getKey(), entry.getValue());
			}
		}
	}
	
	private final EventTranslatorOneArg<FanoutEvent, Object> translator = new EventTranslatorOneArg<FanoutEvent, Object>() {
		public void translateTo(FanoutEvent eventEvent, long sequence, Object event) {
			eventEvent.event = event;
		}
	};
	
	@Override
	protected void onInit() throws Exception {
		readKafkaProperties();
		createDisruptor();
		createKafkaConsummers();
	}
	
	protected synchronized void registerConsumer(KafkaConsumer<String, byte[]> consumer) {
		consumers.add(consumer);
	}

	private void createKafkaConsummers() throws Exception {
		if(pollValue == 0) {
			pollValue = Long.MAX_VALUE;
		}
	    kafkaConsumerExecutor = Executors.newCachedThreadPool(
	    		new ThreadFactoryBuilder().setNameFormat("KafkaConsumers-%d").build()
	    );

	    for(Entry<String, Integer> entry : topicAndThreads.entrySet()) {
	    	String topic = entry.getKey();
	    	int nThread = entry.getValue();
			if(nThread < 1 || nThread > 128) {
				throwException(ILLEGAL_ARG_PARALLEL_READERS, nThread);
			}
	    	LOG.info("Creating {} readers for topic {}", nThread, topic);
	    	ExponentialMovingAverageRate rate = new ExponentialMovingAverageRate(1, 60, 10);
    	    globalRates.put(topic, rate);
	    	kafkaConsumerExecutor.execute(new InternalKafkaConsumer(topic, rate));
	    }
	}
	
	private void readKafkaProperties() throws Exception {
		if(StringUtils.isEmpty(consumerPropertiesFile)) {
			throwException(ILLEGAL_PROPERTIES_FILE, "[null]");
		}
		props = FileUtils.readProperties(consumerPropertiesFile);
		LOG.info("Kafka properties are {}", props);
	}

	private void createDisruptor() throws Exception {
		if((bufferSize & (bufferSize - 1)) != 0) {
			throwException(ILLEGAL_BUFFER_SIZE, bufferSize);
		}
		if(parallelSources < 1 | parallelSources > 128) {
			throwException(ILLEGAL_ARG_PARALLEL_SOURCES, perTopicConsumers);
		}
		disruptor = new Disruptor<>(new FanoutEventFactory(), bufferSize, ringBufferExecutor);
		FanoutEventHandler[] eventHandlers = new FanoutEventHandler[parallelSources];
		LOG.info("Creating {} ring buffer consumer(s)", parallelSources);
		for(int index = 0; index < parallelSources ; index++) {
			FanoutEventHandler eventHandler = new FanoutEventHandler();
			eventHandlers[index] = eventHandler;
		}
		disruptor.handleEventsWithWorkerPool(eventHandlers);
		ringBuffer = disruptor.getRingBuffer();
		disruptor.start();
	}
	
	private void throwException(String msg, Object value) {
		String errorMsg = String.format(msg, String.valueOf(value));
		LOG.error(errorMsg);
		throw new IllegalArgumentException(errorMsg);
	}

	@Override
	protected void doStart() {
		LOG.info("Starting kafka source");
		isStarted = true;
		startupLatch.countDown();
    	kafkaConsumerExecutor.execute(new LogStats());
	}
	
	@Override
	protected void doStop() {
		if(isStarted) {
			LOG.info("Stoping kafka source");
			isStarted = false;
			stopServices();
		}
	}

	private void stopServices() {
		LOG.info("Shutting down services");
		if(kafkaConsumerExecutor != null) {
			for(KafkaConsumer<String, byte[]> consumer : consumers) {
				consumer.wakeup();
			}
			try {
				kafkaConsumerExecutor.awaitTermination(3, TimeUnit.SECONDS);
				kafkaConsumerExecutor.shutdownNow();
			} catch(Exception e) {
				LOG.error("Error while stopping kafka services");
			}
		}
		
		try {
			disruptor.shutdown(3, TimeUnit.SECONDS);
		} catch(Exception e) {
			LOG.error("Error while stopping disruptor");
		}
	}
}
