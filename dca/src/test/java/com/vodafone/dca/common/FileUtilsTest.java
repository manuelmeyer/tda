package com.vodafone.dca.common;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileUtilsTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(FileUtilsTest.class);
	
	@Test
	public void canFindResourceFile() throws Exception {
		File existingFile = FileUtils.fileFromPath("test-refdata/instance1/lac-cells.csv");
		assertTrue(existingFile.exists());
		LOG.info("Found file {}", existingFile.getCanonicalPath());
	}
}
