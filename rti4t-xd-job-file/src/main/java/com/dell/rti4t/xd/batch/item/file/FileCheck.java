package com.dell.rti4t.xd.batch.item.file;

import static org.springframework.batch.core.ExitStatus.COMPLETED;
import static org.springframework.batch.core.ExitStatus.FAILED;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;

import reactor.util.StringUtils;

public class FileCheck {
	
	private static final Logger LOG = LoggerFactory.getLogger(FileCheck.class);
	
	private String path;
	public void setPath(String path) {
		this.path = path;
	}
	
	public ExitStatus fileExists() {
		if(StringUtils.isEmpty(path)) {
			LOG.error("Empty path - returning false.");
			return FAILED;
		}
		File check = new File(path);
		boolean exists = check.exists() && check.canRead() && check.isFile();
		LOG.debug("isFileExists({}) returns {}", path, exists);
		return (exists ? COMPLETED : FAILED);
	}
}
