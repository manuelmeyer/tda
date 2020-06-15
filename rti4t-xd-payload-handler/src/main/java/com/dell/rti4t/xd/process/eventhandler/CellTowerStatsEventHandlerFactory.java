package com.dell.rti4t.xd.process.eventhandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.context.Lifecycle;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import com.dell.rti4t.xd.enrich.EventEnricher;
import com.dell.rti4t.xd.eventhandler.AbstractEventHandlerFactory;
import com.dell.rti4t.xd.eventhandler.DataTransporterEventHandler;
import com.dell.rti4t.xd.filter.EventFilter;
import com.dell.rti4t.xd.jmx.VFROInputOutputMetrics;
import com.dell.rti4t.xd.transformer.DataInputParser;
import com.dell.rti4t.xd.transformer.MapFieldReducer;
import com.google.common.util.concurrent.AtomicLongMap;

public class CellTowerStatsEventHandlerFactory extends AbstractEventHandlerFactory {
	
	private StringBuffer outMessage = new StringBuffer(32 * 1024);
	
	private MessageChannel outputChannel;
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}
	
	private AtomicLongMap<String> cellIn = AtomicLongMap.create();
	private AtomicLongMap<String> cellOut = AtomicLongMap.create();
	
	@Override
	protected DataTransporterEventHandler createNewEventHandler(String handlerName, VFROInputOutputMetrics inputOutputMetrics, Lifecycle lifeCycle, MessageChannel outputChannel, int batchSize, int batchTimeout, MapFieldReducer reducer, DataInputParser transformer, List<EventFilter> eventFilters, List<EventEnricher> eventEnrichers) {
		LOG.debug("Creating new CellTowerStatsEventHandler for {}", handlerName);
		CellTowerStatsEventHandler eventHandler = new CellTowerStatsEventHandler(handlerName, inputOutputMetrics, lifeCycle, outputChannel, batchSize, batchTimeout, reducer, transformer, eventFilters);
		eventHandler.setCellMaps(cellIn, cellOut);
		return eventHandler;
	}
	
	@Override
	protected void onWatchDog() {
		String output = buildOutputMessage();
		if(output == null) {
			return;
		}
		LOG.debug("outputting {} bytes", output.length());
		Message<String> msg = MessageBuilder.withPayload(output).setHeader("data-type", "cells").build();
		outputChannel.send(msg);
	}

	private String buildOutputMessage() {
		outMessage.setLength(0);
		Map<String, Long> valuesIn = new HashMap<String, Long>(cellIn.asMap());
		Map<String, Long> valuesOut = new HashMap<String, Long>(cellOut.asMap());
		
		// no new data in... nothing we can do
		if(valuesIn.size() == 0 && valuesOut.size() == 0) {
			return null;
		}
		
		cellIn.clear();
		cellOut.clear();
		
		// get the in, and from there the related out count
		for(Entry<String, Long> entry : valuesIn.entrySet()) {
			String key = entry.getKey();
			Long in = entry.getValue();
			Long out = valuesOut.get(key);
			if(out != null) {
				valuesOut.remove(key);
			} else {
				out = 0L;
			}
			outMessage.append(key).append(",").append(in).append(",").append(out).append("\n");
		}
		
		// get the out only, left alone
		for(Entry<String, Long> entry : valuesOut.entrySet()) {
			String key = entry.getKey();
			Long out = entry.getValue();
			outMessage.append(key).append(",").append(0).append(",").append(out).append("\n");
		}
		return outMessage.toString();
	}
}
