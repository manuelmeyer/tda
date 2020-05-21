package com.dell.rti4t.xd.eventhandler;

import static com.dell.rti4t.xd.csv.CSVToObjectParser.parse;

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
import com.dell.rti4t.xd.transformer.MapFieldReducer;
import com.dell.rti4t.xd.transformer.ObjectListToDataTransporter;

public abstract class AbstractDataTransporterEventHandler implements DataTransporterEventHandler {
	
	final protected Logger LOG = LoggerFactory.getLogger(getClass());
	
	final protected List<EventFilter> eventFilters;
	final protected List<EventEnricher> eventEnrichers;
	final protected int batchSize;
	final protected int batchTimeout; 
	final protected MapFieldReducer reducer;
	final protected ObjectListToDataTransporter transformer;
	final protected MessageChannel outputChannel;
	final private Lifecycle lifeCycle;
	
	final protected VFROInputOutputMetrics inputOutputMetrics;

	public AbstractDataTransporterEventHandler(String handlerName, VFROInputOutputMetrics inputOutputMetrics, Lifecycle lifeCycle, MessageChannel outputChannel, int batchSize, int batchTimeout, MapFieldReducer reducer, ObjectListToDataTransporter transformer, List<EventFilter> eventFilters, List<EventEnricher> eventEnrichers) {
		this.batchSize = batchSize;
		this.batchTimeout = batchTimeout;
		this.reducer = reducer;
		this.transformer = transformer;
		this.eventFilters = eventFilters;
		this.eventEnrichers = eventEnrichers;
		this.outputChannel = outputChannel;
		this.lifeCycle = lifeCycle;
		this.inputOutputMetrics = inputOutputMetrics;
	}

	public AbstractDataTransporterEventHandler(String handlerName, VFROInputOutputMetrics inputOutputMetrics, Lifecycle lifeCycle, MessageChannel outputChannel, int batchSize, int batchTimeout, MapFieldReducer reducer, ObjectListToDataTransporter transformer, List<EventFilter> eventFilters) {
		this(handlerName, inputOutputMetrics, lifeCycle, outputChannel, batchSize, batchTimeout, reducer, transformer, eventFilters, new ArrayList<EventEnricher>());
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

	public void onEvent(Object body, Map<String, Object> headers) {
		/*
		 * The synchronize is only between the timeout mechanism called in the factory and the accumulator
		 * Checking timeout is < the ns
		 */
		synchronized(this) { 
			try {
				accumulate((String)body, headers);
			} catch(Exception e) {
				LOG.error("Exception while processing event", e);
			}
		}
	}
	
	protected void accumulate(DataTransporter dt) {
	}
	
	protected void chainAndAccumulate(DataTransporter dt) {
		inputOutputMetrics.incrementInput();
    	if(accept(dt)) {
    		inputOutputMetrics.incrementOutput();
    		enrich(dt);
    		accumulate(dt);
    	}
	}

	public void accumulate(String body, Map<String, Object> headers) {
		LOG.trace("Message read {}", body);
		List<List<Object>> elements = parse(body);
		LOG.debug("Accumulating a list of {}", elements.size());
		int index = 0;
	    for(List<Object> element : elements) {
	    	try {
	    		DataTransporter dt = transformer.buildFromObjectList(element);
	    		chainAndAccumulate(dt);
	        	index++;
	    	} catch(Exception e) {
	    		LOG.error("Exception while accumlating event {}, index {}", element, index, e);
	    	}
		}
	}
}
