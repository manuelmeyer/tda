package com.dell.rti4t.xd.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

import com.dell.rti4t.xd.metrics.ExponentialMovingAverageRate;
import com.dell.rti4t.xd.utils.MoreObjects;

public class VFROInputOutputMetrics {
	
	static final private Logger LOG = LoggerFactory.getLogger(VFROInputOutputMetrics.class);
	
	private String metricName;
	private Class<?> classOwner;
	private String group;
	private String identity;
	
	volatile ExponentialMovingAverageRate input;
	volatile ExponentialMovingAverageRate output;
	
	public void incrementInput() {
		input.increment();
		if(LOG.isDebugEnabled()) {
			LOG.debug("inc input of {} count is now {}", this, input.getCount());
		}
	}

	public void incrementOutput() {
		output.increment();
		if(LOG.isDebugEnabled()) {
			LOG.debug("inc output of {} count is now {}", this, output.getCount());
		}
	}
	
	public void moreInput(int value) {
		input.incrementBy(value);
		if(LOG.isDebugEnabled()) {
			LOG.debug("Adding input {} to {} count is now {}", value, this, input.getCount());
		}
	}
	
	public void moreOutput(int value) {
		output.incrementBy(value);
		if(LOG.isDebugEnabled()) {
			LOG.debug("Adding output {} to {} count is now {}", value, this, output.getCount());
		}
	}

	public VFROInputOutputMetrics(String metricName, String group, Object owner, String identity) {
		this.group = group;
		this.metricName = metricName.toLowerCase();
		this.classOwner = owner.getClass();
		this.identity = identity;
		this.input = new ExponentialMovingAverageRate(1, 60, 10); 
		this.output = new ExponentialMovingAverageRate(1, 60, 10);		
	}
	
	public VFROInputOutputMetrics(String metricName, String group, Object owner) {
		this(metricName, group, owner, ClassUtils.getShortName(owner.getClass()));
	}

	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("classOwner", classOwner.getSimpleName())
				.add("group", group)
				.add("metricName", metricName)
				.toString();
	}

	public String group() {
		return group;
	}

	public String metricName() {
		return metricName;
	}

	public String identity() {
		return identity;
	}
}
