package com.dell.rti4t.xd.process.eventhandler;

import java.util.List;

import org.springframework.context.Lifecycle;
import org.springframework.messaging.MessageChannel;

import com.dell.rti4t.xd.enrich.EventEnricher;
import com.dell.rti4t.xd.eventhandler.DataTransporterEventHandler;
import com.dell.rti4t.xd.filter.EventFilter;
import com.dell.rti4t.xd.transformer.MapFieldReducer;
import com.dell.rti4t.xd.transformer.ObjectListToDataTransporter;

public class AccumulatorEventHandlerFactory extends AbstractEventHandlerFactory {
	
	@Override
	protected DataTransporterEventHandler createNewEventHandler(String handlerName, 
									Lifecycle lifeCycle, 
									MessageChannel outputChannel, 
									int batchSize, 
									int batchTimeout, 
									MapFieldReducer reducer, 
									ObjectListToDataTransporter transformer, 
									List<EventFilter> eventFilters, List<EventEnricher> enrichers) {
		LOG.debug("Creating new AccumulatorEventHandler for {}", handlerName);
		AccumulatorEventHandler accEventHandler = new AccumulatorEventHandler(handlerName, this, outputChannel, batchSize, batchTimeout, reducer, transformer, eventFilters, enrichers);
		return accEventHandler;
	}
}
