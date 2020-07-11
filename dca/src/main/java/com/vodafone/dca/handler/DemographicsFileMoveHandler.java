package com.vodafone.dca.handler;

import static com.vodafone.dca.common.FileUtils.changeSuffix;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.vodafone.dca.domain.properties.DemographicsInputProperties;

public class DemographicsFileMoveHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(DemographicsFileMoveHandler.class);
	
	@Autowired
	private DemographicsInputProperties demographicsProperties;
	
	public File changeFileName(File source, String newSuffix) {
		File target = null;
		try {
			target = changeSuffix(source, newSuffix);
			LOG.info("moving {} to {}", source, target);
			Files.move(Paths.get(source.getAbsolutePath()), Paths.get(target.getAbsolutePath()), REPLACE_EXISTING);
			return target;
		} catch(Exception e) {
			LOG.error("Cannot move {} to {}", source, target, e);
			return null;
		}
	}
}
