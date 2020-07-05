package com.vodafone.dca.domain;

import com.google.common.base.MoreObjects;

public class FilterProperties {
	private ReductionFilterProperties reduction;
	private LacCellFilterProperties lacCell;
	
	public ReductionFilterProperties getReduction() {
		return reduction;
	}
	public void setReduction(ReductionFilterProperties reduction) {
		this.reduction = reduction;
	}
	public LacCellFilterProperties getLacCell() {
		return lacCell;
	}
	public void setLacCell(LacCellFilterProperties lacCell) {
		this.lacCell = lacCell;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("reduction", reduction)
				.add("lacCell", lacCell)
				.toString();
	}
}
