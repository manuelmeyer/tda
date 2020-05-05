package com.dell.rti4t.processor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.FutureTask;

import org.hsqldb.lib.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.Lifecycle;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import com.dell.rti4t.xd.external.SpawnProcess;

public class ShellProcessor extends AbstractReplyProducingMessageHandler implements Lifecycle {
	
	private static final Logger LOG = LoggerFactory.getLogger(ShellProcessor.class);
	
	private boolean isRunning = false;
	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	private String command;
	private String[] environmentParams = null;
	private String environmentProperties = null;
	private File workingDirectory = null;
	
	private boolean hasRunningCommand = false;
	public boolean hasRunningCommand() {
		return this.hasRunningCommand;
	}
	
	private boolean output = true; // propagate output as a return message or not
	public void setPropagateOutput(boolean output) {
		this.output = output;
	}
	
	public void start() {
		LOG.info("Starting");
		isRunning = true;
	}

	public void stop() {
		LOG.info("Stopping");
		isRunning = false;
	}

	public boolean isRunning() {
		LOG.info("isRunning returns {}", isRunning);
		return isRunning;
	}
	
	public void setEnvironmentParams(String[] environmentParams) {
		if(environmentParams == null || environmentParams.length == 0) {
			return;
		}
		this.environmentParams = environmentParams;
	}

	public void setEnvironmentProperties(String environmentProperties) {
		if(!StringUtil.isEmpty(environmentProperties)) {
			this.environmentProperties = environmentProperties;
		}
	}

	public void setCommand(String command) {
		this.command = command;
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		if(!isRunning) {
			return null;
		}
		
		int status = -1;
		StringWriter output = buildOutput();
		StringWriter error = new StringWriter();
		Object payload = requestMessage.getPayload();
		BufferedReader reader = buildReader(payload);
		
		FutureTask<Integer> task = null;
		
		try {
			hasRunningCommand = true;
			try {
				SpawnProcess process = new SpawnProcess(command, 
												getEnvironmentParams(), 
												workingDirectory, 
												output, error, reader);
				task = new FutureTask<Integer>(process);
				taskExecutor.execute(task);
				status = task.get();
				LOG.debug("{} terminated with {}", command, status);
			} catch(InterruptedException ie) {
				LOG.error("InterruptedException while spanning process [{}] - cancelling current tasks", command);
				task.cancel(true);
				return null;
			} catch(Exception e) {
				LOG.error("Exception while spanning process [{}]", command);
				throw new RuntimeException(e);
			} 
		} finally {
			hasRunningCommand = false;
		}
		
		if(status == 0) {
			if(output == null) {
				return null;
			}
			String fwdMessage = output.getBuffer().toString();
			if(fwdMessage == null || fwdMessage.length() == 0) {
				return null;
			}
			return MessageBuilder.withPayload(output.getBuffer().toString()).build();
		}
		LOG.error("Command {} came back in error {} - {}", command, status, error.getBuffer().toString());
		return null;
	}

	private String[] getEnvironmentParams() {
		if(environmentProperties == null) {
			return environmentParams;
		}
		try {
			Properties props = new Properties();
			props.load(new FileInputStream(environmentProperties));
			String[] environmentParams = new String[props.size()];
			int index = 0;
			for(Entry<Object, Object> prop : props.entrySet()) {
				environmentParams[index++] = String.format("%s=%s", prop.getKey(), prop.getValue());
			}
			return environmentParams;
		} catch(Exception e) {
			LOG.error("Cannot get properties from {}", environmentProperties, e);
			throw new RuntimeException(e);
		}
	}

	private BufferedReader buildReader(Object payload) {
		if(payload != null) {
			String strPayload = payload.toString();
			LOG.debug("ShellProcess payload is {}", strPayload);
			return new BufferedReader(new StringReader(strPayload));
		}
		return null;
	}

	private StringWriter buildOutput() {
		return output ? new StringWriter() : null;
	}
}
