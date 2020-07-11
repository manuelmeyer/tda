package com.vodafone.dca.domain.properties;

import com.google.common.base.MoreObjects;

public class DemographicsInputProperties {
	
	private String fieldDefinition;
	private String fileDirectory;
	private String filePattern = "*.dat";
	private long filePollRate = 5000;
	private boolean preventDuplicate = false;
	
	private String fileSuffixNew = ".new";
	
	private String fieldDelimiter = "|";
	
	public String getFieldDefinition() {
		return fieldDefinition;
	}

	public void setFieldDefinition(String fieldDefinition) {
		this.fieldDefinition = fieldDefinition;
	}

	public String getFieldDelimiter() {
		return fieldDelimiter;
	}

	public void setFieldDelimiter(String fieldDelimiter) {
		this.fieldDelimiter = fieldDelimiter;
	}

	public String getFileSuffixNew() {
		return fileSuffixNew;
	}

	public void setFileSuffixNew(String fileSuffixNew) {
		this.fileSuffixNew = fileSuffixNew;
	}

	public String getFileDirectory() {
		return fileDirectory;
	}
	
	public void setFileDirectory(String fileDirectory) {
		this.fileDirectory = fileDirectory;
	}
	
	public String getFilePattern() {
		return filePattern;
	}
	
	public void setFilePattern(String pattern) {
		this.filePattern = pattern;
	}
	
	public long getFilePollRate() {
		return filePollRate;
	}
	
	public void setFilePollRate(long pollRate) {
		this.filePollRate = pollRate;
	}

	public boolean isPreventDuplicate() {
		return preventDuplicate;
	}

	public void setPreventDuplicate(boolean preventDuplicate) {
		this.preventDuplicate = preventDuplicate;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("fileDirectory", fileDirectory)
				.add("filePattern", filePattern)
				.add("filePollRate", filePollRate)
				.add("preventDuplicate", preventDuplicate)
				.add("fileSuffixNew", fileSuffixNew)
				.toString();
	}
}
