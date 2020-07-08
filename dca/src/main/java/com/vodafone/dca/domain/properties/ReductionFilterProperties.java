package com.vodafone.dca.domain.properties;

import com.google.common.base.MoreObjects;
import com.vodafone.dca.filter.DataReductionFilter.ReductionMode;

public class ReductionFilterProperties {
	private ReductionMode mode = ReductionMode.NONE;

	public ReductionMode getMode() {
		return mode;
	}

	public void setMode(ReductionMode mode) {
		this.mode = mode;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("mode", mode)
				.toString();
	}
}
