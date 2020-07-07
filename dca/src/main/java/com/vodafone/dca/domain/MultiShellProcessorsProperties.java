package com.vodafone.dca.domain;

import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class MultiShellProcessorsProperties {
	private List<ShellProcessorProperties> shellProcessors = Lists.newArrayList();

	public List<ShellProcessorProperties> getShellProcessors() {
		return shellProcessors;
	}

	public void setShellProcessors(List<ShellProcessorProperties> shellProcessors) {
		this.shellProcessors = shellProcessors;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("shellProcessors", shellProcessors)
				.toString();
	}
}
