package com.dell.rti4t.xd.batch.item.file;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.filter.EventFilter;

public class DataTransporterItemProcessor implements ItemProcessor<DataTransporter, DataTransporter>{
	
	private static final Logger LOG = LoggerFactory.getLogger(DataTransporterItemProcessor.class);
	
	private List<EventFilter> eventFilters = new ArrayList<EventFilter>(0);
	public void setEventFilters(List<EventFilter> eventFilters) {
		LOG.info("Setting {} as filters", eventFilters);
		this.eventFilters = eventFilters;
	}
	
	@Override
	public DataTransporter process(DataTransporter item) throws Exception {
		for(EventFilter filter : eventFilters) {
			if(filter.accept(item) == false) {
				return null;
			}
		}
		return item;
	}
}
