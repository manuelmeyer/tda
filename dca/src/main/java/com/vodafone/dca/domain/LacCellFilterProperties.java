package com.vodafone.dca.domain;

import com.google.common.base.MoreObjects;

public class LacCellFilterProperties {
	
	private int fileScanFrequency = 60;
	private String lacField = "lac";
	private String cellTowerField = "cellTower";
	private boolean followExit = false;
	private String lacCellFile;
	
	public int getFileScanFrequency() {
		return fileScanFrequency;
	}
	
	public void setFileScanFrequency(int fileScanFrequency) {
		this.fileScanFrequency = fileScanFrequency;
	}
	
	public String getLacField() {
		return lacField;
	}
	
	public void setLacField(String lacField) {
		this.lacField = lacField;
	}
	
	public String getCellTowerField() {
		return cellTowerField;
	}
	
	public void setCellTowerField(String cellTowerField) {
		this.cellTowerField = cellTowerField;
	}
	
	public boolean isFollowExit() {
		return followExit;
	}
	
	public void setFollowExit(boolean followExit) {
		this.followExit = followExit;
	}
	
	public String getLacCellFile() {
		return lacCellFile;
	}
	public void setLacCellFile(String lacCellFile) {
		this.lacCellFile = lacCellFile;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("fileScanFrequency", fileScanFrequency)
				.add("lacField", lacField)
				.add("cellTowerField", cellTowerField)
				.add("followExit", followExit)
				.add("lacCellFile", lacCellFile)
				.toString();
	}	
}
