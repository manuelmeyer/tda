package com.dell.rti4t.xd.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.support.MetricType;
import org.springframework.util.ClassUtils;

public class VFROJMXExposedMetrics {
	
	static private final Logger LOG = LoggerFactory.getLogger(VFROJMXExposedMetrics.class);
	
	VFROInputOutputMetrics vfROMetrics;
	
	public VFROJMXExposedMetrics(VFROInputOutputMetrics metrics) {
		this.vfROMetrics = metrics;
		LOG.debug("VFROJMXExposedMetrics({})", metrics);
	}
	
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Output Rate per Second")
	public double getOutputMeanSendRate() {
		return vfROMetrics.output.getMean();
	}
	
	@ManagedMetric(metricType = MetricType.GAUGE, displayName = "Input Rate per Second")
	public double getInputMeanSendRate() {
		return vfROMetrics.input.getMean();
	}
	
	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "Component Received count")
	public long getInputReceivedCount() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("getInputReceivedCount({}) {}", vfROMetrics, vfROMetrics.input.getCount());
		}
		return vfROMetrics.input.getCount();
	}

	@ManagedMetric(metricType = MetricType.COUNTER, displayName = "Component Send Count")
	public long getOutputSentCount() {
		if(LOG.isDebugEnabled()) {
			LOG.debug("getOutputSentCount({}) {}", vfROMetrics, vfROMetrics.output.getCount());
		}
		return vfROMetrics.output.getCount();
	}

	public String type() {
		return vfROMetrics.group();
	}

	public String name() {
		return vfROMetrics.metricName();
	}

	public String identity() {
		return vfROMetrics.identity();
	}
}
