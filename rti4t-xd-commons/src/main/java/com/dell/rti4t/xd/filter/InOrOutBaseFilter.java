package com.dell.rti4t.xd.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

abstract public class InOrOutBaseFilter implements EventFilter, InitializingBean {
	
	final protected Logger LOG = LoggerFactory.getLogger(getClass());

	protected Boolean in = null;
	protected String filterField = "imsi";

	public void setInMode(boolean in) {
		this.in = in;
	}

	public void setFilterField(String filterField) {
		this.filterField = filterField;
	}
	
	protected void doAfterPropertiesSet() throws Exception {
		LOG.warn("No after properties set to be called");
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if(in == null || filterField == null) {
			throw new RuntimeException("inMode and filterField properties must be set");
		}
		doAfterPropertiesSet();
	}
}
