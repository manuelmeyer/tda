package com.dell.rti4t.xd.eventhandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.Lifecycle;
import org.springframework.messaging.MessageChannel;

import com.dell.rti4t.xd.enrich.EventEnricher;
import com.dell.rti4t.xd.filter.EventFilter;
import com.dell.rti4t.xd.jmx.VFROInputOutputMetrics;
import com.dell.rti4t.xd.transformer.DataInputParser;
import com.dell.rti4t.xd.transformer.MapFieldReducer;

abstract public class AbstractEventHandlerFactory implements DataTransporterEventHandlerFactory {
	
	final protected Logger LOG = LoggerFactory.getLogger(getClass());
	
	protected boolean isRunning = false;
	protected Map<String, DataTransporterEventHandler> producerMap = new ConcurrentHashMap<String, DataTransporterEventHandler>();
	protected List<EventFilter> eventFilters;
	protected List<EventEnricher> eventEnrichers;

	protected int batchSize = 2048;
	protected int batchTimeout = 5000;
	protected DataInputParser transformer;
	protected MapFieldReducer reducer;
	protected String streamName;
	private VFROInputOutputMetrics inputOutputMetrics;
	private String configuredHandlerFactoryId;
	private String handlerFactoryId;
	
    @PostConstruct
    public void createMetrics() {
    	//inputOutputMetrics = new VFROInputOutputMetrics("inputoutput", streamName, this, "eventhandler");
    	inputOutputMetrics = new VFROInputOutputMetrics(streamName, "eventhandler", this, "inputoutput");
    }

    public void setConfiguredHandlerFactoryId(String configuredHandlerFactoryId) {
    	this.configuredHandlerFactoryId = configuredHandlerFactoryId;
	}

    public void setHandlerFactoryId(String handlerFactoryId) {
    	this.handlerFactoryId = handlerFactoryId;
	}

    public boolean registerBean() {
    	return handlerFactoryId != null && handlerFactoryId.equals(configuredHandlerFactoryId);
    }
	
	public void setStreamName(String streamName) {
		LOG.info("Stream name is {}", streamName);
		this.streamName = streamName;
	}

	public void setDataTransporterHandler(DataInputParser transformer) {
		this.transformer = transformer;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public void setBatchTimeout(int timeout) {
		this.batchTimeout = timeout;
	}

	public void setEventFilters(List<EventFilter> eventFilters) {
		this.eventFilters = eventFilters;
	}
	
	public void setEventEnrichers(List<EventEnricher> eventEnrichers) {
		this.eventEnrichers = eventEnrichers;
	}

	
	class WatchDog implements Runnable {
		@Override
		public void run() {
			for (;;) {
				try {
					synchronized(producerMap) {
						if(isRunning) {
							onWatchDog();
						}
					}
					Thread.sleep(batchTimeout);
				} catch (InterruptedException e) {
					LOG.info("WatchDog interrupted - exiting");
					break;
				} catch(Exception e) {
					LOG.error("Error while flushing producers", e);
				}
			}
		}
	}
	
	public AbstractEventHandlerFactory() {
		new Thread(new WatchDog()).start();
	}

	protected void onWatchDog() {
		for(DataTransporterEventHandler producer : producerMap.values()) {
			synchronized(producer) {
				producer.flushOnTimeout();
			}
		}
	}

	@Override
	public void start() {
		LOG.info("Starting component - isRunning is {}", isRunning);
		isRunning = true;
	}

	@Override
	public void stop() {
		LOG.info("Stopping component - isRunning is {}", isRunning);
		isRunning = false;
	}

	@Override
	public boolean isRunning() {
		return isRunning;
	}

	public void setMapFieldReducer(MapFieldReducer reducer) {
		this.reducer = reducer;
	}
	
	abstract protected DataTransporterEventHandler createNewEventHandler(String handlerName, 
			VFROInputOutputMetrics inputOutputMetrics, 
			Lifecycle lifeCycle, 
			MessageChannel outputChannel, 
			int batchSize, 
			int batchTimeout, 
			MapFieldReducer reducer, 
			DataInputParser transformer, 
			List<EventFilter> eventFilters, 
			List<EventEnricher> enrichers);

	@Override
	public DataTransporterEventHandler getEventHandler(String handlerName, MessageChannel outputChannel) {
		DataTransporterEventHandler eventHandler = producerMap.get(handlerName);
		if(eventHandler == null) {
			synchronized(producerMap) {
				eventHandler = producerMap.get(handlerName);
				if(eventHandler == null) {
					eventHandler = createNewEventHandler(handlerName,
							inputOutputMetrics,
							this, 
							outputChannel, 
							batchSize, 
							batchTimeout, 
							reducer, 
							transformer, 
							eventFilters, 
							eventEnrichers);
					producerMap.put(handlerName, eventHandler);
				}
			}
		}
		return eventHandler;
	}

	public VFROInputOutputMetrics getVFROMetrics() {
		return inputOutputMetrics;
	}
}
