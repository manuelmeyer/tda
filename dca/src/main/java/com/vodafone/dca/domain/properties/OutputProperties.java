package com.vodafone.dca.domain.properties;

import com.google.common.base.MoreObjects;

public class OutputProperties {
	
	private String[] anonymiseFields = {};
	private String fieldDefinition;
	private String fileDirectory;
	private Integer fileSizeThreshold;
	private String filePrefix = "";
	private int batchSize = 200;
	private int batchTimeout = 500;
	
	public String getFilePrefix() {
		return filePrefix;
	}

	public void setFilePrefix(String filePrefix) {
		this.filePrefix = filePrefix;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public int getBatchTimeout() {
		return batchTimeout;
	}

	public void setBatchTimeout(int batchTimeout) {
		this.batchTimeout = batchTimeout;
	}

	public String[] getAnonymiseFields() {
		return anonymiseFields;
	}
	
	public void setAnonymiseFields(String[] anonymiseFields) {
		this.anonymiseFields = anonymiseFields;
	}
	
	public String getFieldDefinition() {
		return fieldDefinition;
	}
	
	public void setFieldDefinition(String fieldDefinition) {
		this.fieldDefinition = fieldDefinition;
	}
	
	public String getFileDirectory() {
		return fileDirectory;
	}
	
	public void setFileDirectory(String fieldDirectory) {
		this.fileDirectory = fieldDirectory;
	}
	
	public Integer getFileSizeThreshold() {
		return fileSizeThreshold;
	}
	
	public void setFileSizeThreshold(Integer fileSizeThreshold) {
		this.fileSizeThreshold = fileSizeThreshold;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("anonymiseFields", anonymiseFields)
				.add("filePrefix", filePrefix)
				.add("fieldDefinition", fieldDefinition)
				.add("fileDirectory", fileDirectory)
				.add("fileSizeThreshold", fileSizeThreshold)
				.add("batchSize", batchSize)
				.add("batchTimeout", batchTimeout)
				.toString();
	}
}
