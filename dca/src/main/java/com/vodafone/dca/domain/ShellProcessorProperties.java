package com.vodafone.dca.domain;

import com.google.common.base.MoreObjects;

public class ShellProcessorProperties {
	
	private int delay;
	private String command;
	
	public int getDelay() {
		return delay;
	}
	
	public void setDelay(int delay) {
		this.delay = delay;
	}
	
	public String getCommand() {
		return command;
	}
	
	public void setCommand(String command) {
		this.command = command;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("delay", delay)
				.add("command", command)
				.toString();
	}	
}
