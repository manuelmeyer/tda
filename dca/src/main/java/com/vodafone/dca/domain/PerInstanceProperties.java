package com.vodafone.dca.domain;

import com.google.common.base.MoreObjects;

public class PerInstanceProperties {
	
	private String salt = "";
	private boolean enabled;
	private OutputProperties output;
	private FilterProperties filter;

	public boolean getEnabled() {
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
				.add("enabled", enabled)
				.add("filter", filter)
				.add("output", output)
				.toString();
	}
}
