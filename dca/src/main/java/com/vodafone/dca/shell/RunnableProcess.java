package com.vodafone.dca.shell;

import com.google.common.base.MoreObjects;

public class RunnableProcess {
	
	private int delay;
	private String command;
	private int currentDelay;
	
	public RunnableProcess(int delay, String command) {
		this.delay = delay;
		this.command = command;
		this.currentDelay = 0;
	}
	
	public int delay() {
		return delay;
	}
	
	public String command() {
		return command;
	}
	
	public int currentDelay() {
		return currentDelay;
	}
	
	public void resetDelay() {
		currentDelay = 0;
	}
	
	public void incCurrentDelay(int delay) {
		this.currentDelay += delay;
	}
	
	public boolean isTimeToRun() {
		return currentDelay >= delay;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("delay", delay)
				.add("command", command)
				.add("currentDelay", currentDelay)
				.toString();
	}
}
