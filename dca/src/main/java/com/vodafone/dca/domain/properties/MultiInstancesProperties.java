package com.vodafone.dca.domain.properties;

import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class MultiInstancesProperties {
	
	private List<PerInstanceProperties> instances = Lists.newArrayList();

	public List<PerInstanceProperties> getInstances() {
		return instances;
	}

	public void setInstances(List<PerInstanceProperties> instances) {
		this.instances = instances;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("instances", instances)
				.toString();
	}
}
