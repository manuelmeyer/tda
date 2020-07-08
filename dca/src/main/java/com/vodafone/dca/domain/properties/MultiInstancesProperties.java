package com.vodafone.dca.domain.properties;

import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class MultiInstancesProperties {
	
	private List<PerInstanceProperties> instances = Lists.newArrayList();
	private FilterBlackWhiteListProperties filterBlackWhiteList = new FilterBlackWhiteListProperties();
	
	public FilterBlackWhiteListProperties getFilterBlackWhiteList() {
		return filterBlackWhiteList;
	}

	public void setFilterBlackWhiteList(FilterBlackWhiteListProperties filterBlackWhiteList) {
		this.filterBlackWhiteList = filterBlackWhiteList;
	}

	public List<PerInstanceProperties> getInstances() {
		return instances;
	}

	public void setInstances(List<PerInstanceProperties> instances) {
		this.instances = instances;
	}

	public PerInstanceProperties getInstance1() {
		return instances.get(0);
	}

	public PerInstanceProperties getInstance2() {
		return instances.get(1);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("instances", instances)
				.toString();
	}
}
