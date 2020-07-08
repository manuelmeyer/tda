package com.vodafone.dca.domain.properties;

import com.google.common.base.MoreObjects;

public class FilterBlackWhiteListProperties {
	
	private String whiteListFile = "";
	private String blackListFile = "";
	private int fileScanFrequency = 60;
	private String filterField = "imsi";
	
	public String getWhiteListFile() {
		return whiteListFile;
	}
	
	public void setWhiteListFile(String whiteListFile) {
		this.whiteListFile = whiteListFile;
	}
	
	public String getBlackListFile() {
		return blackListFile;
	}
	
	public void setBlackListFile(String blackListFile) {
		this.blackListFile = blackListFile;
	}
	
	public int getFileScanFrequency() {
		return fileScanFrequency;
	}
	
	public void setFileScanFrequency(int fileScanFrequency) {
		this.fileScanFrequency = fileScanFrequency;
	}
	
	public String getFilterField() {
		return filterField;
	}
	
	public void setFilterField(String filterField) {
		this.filterField = filterField;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("whiteListFile", whiteListFile)
				.add("blackListFile", blackListFile)
				.add("fileScanFrequency", fileScanFrequency)
				.add("filterField", filterField)
				.toString();
	}
}
