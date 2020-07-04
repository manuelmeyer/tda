package com.vodafone.dca.filter;

import java.util.List;

import org.springframework.integration.core.GenericSelector;

import com.google.common.collect.Lists;
import com.vodafone.dca.domain.DataTransporter;

public class MetaFilter implements GenericSelector<DataTransporter> {
	private List<GenericSelector<DataTransporter>> selectors = Lists.newArrayList();
	
	@SafeVarargs
	public MetaFilter(GenericSelector<DataTransporter> ...selectors) {
		for(GenericSelector<DataTransporter> selector : selectors) {
			this.selectors.add(selector);
		}
	}

	@Override
	public boolean accept(DataTransporter source) {
		for(GenericSelector<DataTransporter> selector : selectors) {
			if(!selector.accept(source)) {
				return false;
			}
		}
		return true;
	}
}
