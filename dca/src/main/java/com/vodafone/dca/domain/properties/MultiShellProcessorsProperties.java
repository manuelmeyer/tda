package com.vodafone.dca.domain.properties;

import java.io.File;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class MultiShellProcessorsProperties {
	
	private static final Logger LOG = LoggerFactory.getLogger(MultiShellProcessorsProperties.class);
	
	private List<ShellProcessorProperties> shellProcessors = Lists.newArrayList();

	public List<ShellProcessorProperties> getShellProcessors() {
		return shellProcessors;
	}

	public void setShellProcessors(List<ShellProcessorProperties> shellProcessors) {
		LOG.info("shell to run are {}", shellProcessors);
		this.shellProcessors = checkCommandPaths(shellProcessors);
	}
	
	private List<ShellProcessorProperties> checkCommandPaths(List<ShellProcessorProperties> shellProcessors) {
		shellProcessors.forEach(process -> checkExistsCommand(process));
		return shellProcessors;
	}

	private void checkExistsCommand(ShellProcessorProperties process) {
		String command = process.getCommand();
		FileSystemResource fileSystemResource = new FileSystemResource(command);
		if(!checkFilePath(process, fileSystemResource)) {
			checkFilePath(process, new ClassPathResource(command));
		}
	}

	private boolean checkFilePath(ShellProcessorProperties process, Resource resource) {
		try {
			if(resource.exists()) {
				File fileCommand = resource.getFile();
				if (fileCommand.canExecute()) {
					String absolutePath = fileCommand.getAbsolutePath();
					String command = process.getCommand();
					if(!absolutePath.equals(command)) {
						LOG.info("Setting path of command '{}' to '{}'", command, absolutePath);
						process.setCommand(fileCommand.getAbsolutePath());
					}
					return true;
				}
			}
		} catch(Exception e) {
			LOG.error("Cannot check {}", process, e);
		}
		LOG.error("Process {} contains a non existing/executable command", process);
		return false;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("shellProcessors", shellProcessors)
				.toString();
	}
}
