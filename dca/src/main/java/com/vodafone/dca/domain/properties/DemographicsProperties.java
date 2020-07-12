package com.vodafone.dca.domain.properties;

import java.util.UUID;

import com.google.common.base.MoreObjects;

public class DemographicsProperties {
	
	private DemographicsInputProperties input;
	private DemographicsOutputProperties output;
	private boolean enabled;
	private String name = UUID.randomUUID().toString();
	
	public DemographicsInputProperties getInput() {
		return input;
	}
	
	public void setInput(DemographicsInputProperties input) {
		this.input = input;
	}
	
	public DemographicsOutputProperties getOutput() {
		return output;
	}
	
	public void setOutput(DemographicsOutputProperties output) {
		this.output = output;
	}
	
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("input", input)
				.add("output", output)
				.add("enabled", enabled)
				.add("name", name)
				.toString();
	}
}
