package com.dell.rti4t.xd.filter;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dell.rti4t.xd.domain.DataTransporter;

public class OrEventFilter implements EventFilter {
	
	private static final Logger LOG = LoggerFactory.getLogger(OrEventFilter.class);
	
	private List<EventFilter> filters = new ArrayList<EventFilter>();
	public void setFilters(List<EventFilter> filters) {
		this.filters = filters;
	}
	@Override
	public boolean accept(DataTransporter dt) {
		for(EventFilter filter : filters) {
			if(filter.accept(dt)) {
				if(LOG.isDebugEnabled()) {
					LOG.debug("accepted by {}", filter.description());
				}
				return true;
			}
		}
		return false;
	}
	
	@Override
	public String description() {
		return "combine filters in an 'or' operation";
	}
}
