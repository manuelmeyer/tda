package com.dell.rti4t.xd.eventhandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.Lifecycle;
import org.springframework.messaging.MessageChannel;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.enrich.EventEnricher;
import com.dell.rti4t.xd.filter.EventFilter;
import com.dell.rti4t.xd.jmx.VFROInputOutputMetrics;
import com.dell.rti4t.xd.transformer.DataInputParser;
import com.dell.rti4t.xd.transformer.MapFieldReducer;

public abstract class AbstractDataTransporterEventHandler<T, R> implements DataTransporterEventHandler<R> {
	
	final protected Logger LOG = LoggerFactory.getLogger(getClass());
	
	final protected List<EventFilter> eventFilters;
	final protected List<EventEnricher> eventEnrichers;
	final protected int batchSize;
	final protected int batchTimeout; 
	final protected MapFieldReducer reducer;
	final protected DataInputParser<T, R> objectTransformer;
	final protected MessageChannel outputChannel;
	final private Lifecycle lifeCycle;
	
	final protected VFROInputOutputMetrics inputOutputMetrics;

	public AbstractDataTransporterEventHandler(String handlerName, 
							VFROInputOutputMetrics inputOutputMetrics, 
							Lifecycle lifeCycle, 
							MessageChannel outputChannel, int batchSize, int batchTimeout, 
							MapFieldReducer reducer, 
							DataInputParser<T, R> objectTransformer, 
							List<EventFilter> eventFilters, 
							List<EventEnricher> eventEnrichers) {
		this.batchSize = batchSize;
		this.batchTimeout = batchTimeout;
		this.reducer = reducer;
		this.eventFilters = eventFilters;
		this.eventEnrichers = eventEnrichers;
		this.outputChannel = outputChannel;
		this.objectTransformer = objectTransformer;
		this.lifeCycle = lifeCycle;
		this.inputOutputMetrics = inputOutputMetrics;
	}

	public AbstractDataTransporterEventHandler(String handlerName, 
							VFROInputOutputMetrics inputOutputMetrics, 
							Lifecycle lifeCycle, 
							MessageChannel outputChannel, 
							int batchSize, 
							int batchTimeout, 
							MapFieldReducer reducer, 
							DataInputParser<T, R> objectTransformer, 
							List<EventFilter> eventFilters) {
		this(handlerName, 
				inputOutputMetrics, 
				lifeCycle, 
				outputChannel, 
				batchSize, 
				batchTimeout, 
				reducer, 
				objectTransformer, 
				eventFilters, 
				new ArrayList<EventEnricher>());
	}

	protected boolean accept(DataTransporter dt) {
		for(EventFilter filter : eventFilters) {
			if(!filter.accept(dt)) {
				return false;
			}
		}
		return true;
	}
	
	protected DataTransporter enrich(DataTransporter dt) {
		for(EventEnricher enricher : eventEnrichers) {
			enricher.enrich(dt);
		}
		return dt;
	}

	@Override
	public boolean isRunning() {
		return lifeCycle.isRunning();
	}

	public void onEvent(R body, Map<String, Object> headers) {
		/*
		 * The synchronize is only between the timeout mechanism called in the factory and the accumulator
		 * Checking timeout is < the ns
		 */
		synchronized(this) { 
			try {
				accumulate(body, headers);
			} catch(Exception e) {
				LOG.error("Exception while processing event", e);
			}
		}
	}
	
	protected abstract void accumulate(DataTransporter dt);
	
	protected void chainAndAccumulate(DataTransporter dt) {
    	if(accept(dt)) {
    		enrich(dt);
    		accumulate(dt);
    	}
	}
	
	private void accumulate(R body, Map<String, Object> headers) {
		List<List<T>> elements = objectTransformer.parse(body);
		LOG.debug("Accumulating a list of {}", elements.size());
		int index = 0;
		inputOutputMetrics.moreInput(elements.size());
	    for(List<T> element : elements) {
	    	try {
	    		DataTransporter dt = objectTransformer.buildFromList(element);
	    		chainAndAccumulate(dt);
	        	index++;
	    	} catch(Exception e) {
	    		LOG.error("Exception while accumlating event {}, index {}", element, index, e);
	    	}
		}
	}
}
