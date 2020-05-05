package com.dell.rti4t.xd.process.eventhandler;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.Lifecycle;
import org.springframework.messaging.MessageChannel;

import com.dell.rti4t.xd.enrich.EventEnricher;
import com.dell.rti4t.xd.eventhandler.DataTransporterEventHandler;
import com.dell.rti4t.xd.eventhandler.DataTransporterEventHandlerFactory;
import com.dell.rti4t.xd.filter.EventFilter;
import com.dell.rti4t.xd.transformer.MapFieldReducer;
import com.dell.rti4t.xd.transformer.ObjectListToDataTransporter;

abstract public class AbstractEventHandlerFactory implements DataTransporterEventHandlerFactory {

	final protected Logger LOG = LoggerFactory.getLogger(getClass());
	
	protected boolean isRunning = false;
	protected Map<String, DataTransporterEventHandler> producerMap = new ConcurrentHashMap<String, DataTransporterEventHandler>();
	protected List<EventFilter> eventFilters;
	protected List<EventEnricher> eventEnrichers;

	protected int batchSize = 2048;
	protected int batchTimeout = 5000;
	protected ObjectListToDataTransporter transformer;
	protected MapFieldReducer reducer;

	public void setDataTransporterHandler(ObjectListToDataTransporter transformer) {
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
		//LOG.info("Querying lifecyle status - isRunning is {}", isRunning);
		return isRunning;
	}

	public void setMapFieldReducer(MapFieldReducer reducer) {
		this.reducer = reducer;
	}
	
	abstract protected DataTransporterEventHandler createNewEventHandler(String handlerName, Lifecycle lifeCycle, MessageChannel outputChannel, int batchSize, int batchTimeout, MapFieldReducer reducer, ObjectListToDataTransporter transformer, 
						List<EventFilter> eventFilters, List<EventEnricher> enrichers);

	@Override
	public DataTransporterEventHandler getEventHandler(String handlerName, MessageChannel outputChannel) {
		DataTransporterEventHandler eventHandler = producerMap.get(handlerName);
		if(eventHandler == null) {
			synchronized(producerMap) {
				eventHandler = producerMap.get(handlerName);
				if(eventHandler == null) {
					eventHandler = createNewEventHandler(handlerName, this, outputChannel, batchSize, batchTimeout, reducer, transformer, eventFilters, eventEnrichers);
					producerMap.put(handlerName, eventHandler);
				}
			}
		}
		return eventHandler;
	}
}
