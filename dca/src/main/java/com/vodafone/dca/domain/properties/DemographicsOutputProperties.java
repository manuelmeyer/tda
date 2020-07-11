package com.vodafone.dca.domain.properties;

import com.google.common.base.MoreObjects;

public class DemographicsOutputProperties {
	
	private String fileDirectory;
	private String[] anonymize = new String[]{};
	private String endScript;
	private String fieldDefinition;
	private String fileSuffixProcessing = ".processing";
	private int batchSize = 10_000;
	
	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public String getFileSuffixProcessing() {
		return fileSuffixProcessing;
	}

	public void setFileSuffixProcessing(String fileSuffixProcessing) {
		this.fileSuffixProcessing = fileSuffixProcessing;
	}

	public String getFieldDefinition() {
		return fieldDefinition;
	}

	public void setFieldDefinition(String fieldDefinition) {
		this.fieldDefinition = fieldDefinition;
	}

	public String getEndScript() {
		return endScript;
	}

	public void setEndScript(String endScript) {
		this.endScript = endScript;
	}

	public String getFileDirectory() {
		return fileDirectory;
	}
	
	public void setFileDirectory(String fileDirectory) {
		this.fileDirectory = fileDirectory;
	}
	
	public String[] getAnonymize() {
		return anonymize;
	}
	
	public void setAnonymize(String[] anonymize) {
		this.anonymize = anonymize;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("fileDirectory", fileDirectory)
				.add("anonymize", anonymize)
				.add("endScript", endScript)
				.add("fieldDefinition", fieldDefinition)
				.add("batchSize", batchSize)
				.toString();
	}
}
