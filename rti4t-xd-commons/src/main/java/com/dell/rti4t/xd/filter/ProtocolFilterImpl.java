package com.dell.rti4t.xd.filter;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.google.common.collect.Sets;

public class ProtocolFilterImpl implements EventFilter {
	
	static private final Logger LOG = LoggerFactory.getLogger(ProtocolFilterImpl.class);
	
	private Set<String> protocolSet;
	
	public void setProtocolSet(String[] protocolSet) {
		if(protocolSet != null && protocolSet.length > 0) {
			this.protocolSet = Sets.newHashSet(protocolSet);
			LOG.info("Setting protocol filter to [{}]", this.protocolSet);
		}
	}
	
	public boolean accept(DataTransporter dt) {
		if(protocolSet == null || protocolSet.isEmpty()) {
			return true;
		}
		return protocolSet.contains(dt.filter());
	}

	@Override
	public String description() {
		return "filter based on the DataTransporter.filter() field";
	}
}
