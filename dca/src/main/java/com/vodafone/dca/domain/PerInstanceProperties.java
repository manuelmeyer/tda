package com.vodafone.dca.domain;

import java.util.UUID;

import com.google.common.base.MoreObjects;

public class PerInstanceProperties {
	
	private String salt = "";
	private boolean enabled;
	private String name = UUID.randomUUID().toString();
	private InstanceTemplate template;
	private OutputProperties output = new OutputProperties();
	private FilterProperties filter = new FilterProperties();
	
	public InstanceTemplate getTemplate() {
		return template;
	}

	public void setTemplate(InstanceTemplate template) {
		this.template = template;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getSalt() {
		return salt;
	}

	public void setSalt(String salt) {
		this.salt = salt;
	}

	public OutputProperties getOutput() {
		return output;
	}

	public void setOutput(OutputProperties output) {
		this.output = output;
	}

	public FilterProperties getFilter() {
		return filter;
	}

	public void setFilter(FilterProperties filter) {
		this.filter = filter;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("salt", salt)
				.add("name", name)
				.add("enabled", enabled)
				.add("filter", filter)
				.add("output", output)
				.toString();
	}
}
