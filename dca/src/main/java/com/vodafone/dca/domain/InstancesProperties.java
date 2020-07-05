package com.vodafone.dca.domain;

import com.google.common.base.MoreObjects;

public class InstancesProperties {
	
	private PerInstanceProperties instance1;
	private PerInstanceProperties instance2;
	
	public PerInstanceProperties getInstance1() {
		return instance1;
	}

	public void setInstance1(PerInstanceProperties instance1) {
		this.instance1 = instance1;
	}
	
	public PerInstanceProperties getInstance2() {
		return instance2;
	}

	public void setInstance2(PerInstanceProperties instance2) {
		this.instance2 = instance2;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("instance1", instance1)
				.add("instance2", instance2)
				.toString();
	}
}
