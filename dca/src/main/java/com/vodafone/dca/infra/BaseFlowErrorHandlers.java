package com.vodafone.dca.infra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BaseFlowErrorHandlers<R, T> {
	
	private static final Logger LOG = LoggerFactory.getLogger(BaseFlowErrorHandlers.class);
	
	public void onFlowError(T input, Throwable t) {
		LOG.error("error in flow - input is {}", input, t);
	}
	
	public void onConverterFlowError(R input, Throwable t) {
		LOG.error("converter error in flow  - input is {}", input, t);
	}
}
