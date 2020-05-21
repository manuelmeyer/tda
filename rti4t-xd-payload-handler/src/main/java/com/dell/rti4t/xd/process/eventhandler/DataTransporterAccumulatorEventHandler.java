package com.dell.rti4t.xd.process.eventhandler;

import java.util.List;
import java.util.Map;

import org.springframework.context.Lifecycle;
import org.springframework.messaging.MessageChannel;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.enrich.EventEnricher;
import com.dell.rti4t.xd.filter.EventFilter;
import com.dell.rti4t.xd.jmx.VFROInputOutputMetrics;
import com.dell.rti4t.xd.transformer.MapFieldReducer;
import com.dell.rti4t.xd.transformer.ObjectListToDataTransporter;

public class DataTransporterAccumulatorEventHandler extends AccumulatorEventHandler {

	public DataTransporterAccumulatorEventHandler(String handlerName, VFROInputOutputMetrics inputOutputMetrics, Lifecycle lifeCycle, MessageChannel outputChannel, int batchSize, int batchTimeout, MapFieldReducer reducer, ObjectListToDataTransporter transformer, List<EventFilter> eventFilters, List<EventEnricher> eventEnrichers) {
		super(handlerName, inputOutputMetrics, lifeCycle, outputChannel, batchSize, batchTimeout, reducer, transformer, eventFilters, eventEnrichers);
	}
	
	@SuppressWarnings("unchecked")
	@Override 
	public void onEvent(Object body, Map<String, Object> headers) {
		List<Map<String, Object>> maps = (List<Map<String, Object>>) body;
		for(Map<String, Object> map : maps) {
			DataTransporter dataTransporter = new DataTransporter(map);
			chainAndAccumulate(dataTransporter);
		}
	}
}
