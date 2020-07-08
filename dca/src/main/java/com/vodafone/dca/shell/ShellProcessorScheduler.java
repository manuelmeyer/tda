package com.vodafone.dca.shell;

import java.io.File;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;
import com.vodafone.dca.domain.properties.MultiShellProcessorsProperties;

@EnableScheduling
@Component
public class ShellProcessorScheduler {
	
	private static final Logger LOG = LoggerFactory.getLogger(ShellProcessorScheduler.class);
	
	@Autowired
	private MultiShellProcessorsProperties multiShellProcessorProperties;
	
	private List<RunnableProcess> runnableProcesses = Lists.newArrayList();
	
	private String[] emptyEnv = new String[] {};
	private File tmpRunningDirectory = new File("/tmp");
	
	@PostConstruct
	public void buildRunnableProcesses() {
		if(!CollectionUtils.isEmpty(multiShellProcessorProperties.getShellProcessors())) {
			multiShellProcessorProperties.getShellProcessors().forEach(shellProcess -> 
					runnableProcesses.add(new RunnableProcess(shellProcess.getDelay(), shellProcess.getCommand())));
		}
		LOG.info("Processes to run {}", runnableProcesses);
	}
	
	@Scheduled(fixedDelay=1000, initialDelay=1000)
	public void runProcesses() {
		runnableProcesses.forEach(process -> checkAndRun(process));
	}

	private void checkAndRun(RunnableProcess process) {
		process.incCurrentDelay(1000);
		if(process.isTimeToRun()) {
			runProcess(process.command());
			process.resetDelay();
		}
	}

	private void runProcess(String command) {
		try {
			new SpawnProcess(command, emptyEnv, tmpRunningDirectory).call();
		} catch(Throwable t) {
			if(!(t instanceof InterruptedException)) {
				LOG.error("Error while running {}", command, t);
			}
		}
	}
}
