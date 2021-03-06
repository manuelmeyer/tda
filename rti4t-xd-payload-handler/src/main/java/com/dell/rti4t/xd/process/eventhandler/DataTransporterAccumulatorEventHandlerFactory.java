package com.dell.rti4t.xd.process.eventhandler;

import java.util.List;

import org.springframework.context.Lifecycle;
import org.springframework.messaging.MessageChannel;

import com.dell.rti4t.xd.enrich.EventEnricher;
import com.dell.rti4t.xd.eventhandler.AbstractEventHandlerFactory;
import com.dell.rti4t.xd.eventhandler.DataTransporterEventHandler;
import com.dell.rti4t.xd.filter.EventFilter;
import com.dell.rti4t.xd.jmx.VFROInputOutputMetrics;
import com.dell.rti4t.xd.transformer.DataInputParser;
import com.dell.rti4t.xd.transformer.MapFieldReducer;

public class DataTransporterAccumulatorEventHandlerFactory extends AbstractEventHandlerFactory {
	
	@Override
	protected DataTransporterEventHandler createNewEventHandler(String handlerName, 
									VFROInputOutputMetrics inputOutputMetrics,
									Lifecycle lifeCycle, 
									MessageChannel outputChannel, 
									int batchSize, 
									int batchTimeout, 
									MapFieldReducer reducer, 
									DataInputParser transformer, 
									List<EventFilter> eventFilters, 
									List<EventEnricher> enrichers) {
		LOG.debug("Creating new AccumulatorEventHandler for {}", handlerName);
		DataTransporterAccumulatorEventHandler accEventHandler = new DataTransporterAccumulatorEventHandler(handlerName, 
				inputOutputMetrics, 
				this, 
				outputChannel, 
				batchSize, 
				batchTimeout, 
				reducer, 
				transformer, 
				eventFilters, 
				enrichers);
		return accEventHandler;
	}
}