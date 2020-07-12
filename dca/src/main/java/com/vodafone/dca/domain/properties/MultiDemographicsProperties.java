package com.vodafone.dca.domain.properties;

import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;

public class MultiDemographicsProperties {
	
	private List<DemographicsProperties> demographics = Lists.newArrayList();
	private String fieldDefinition;
	
	public List<DemographicsProperties> getDemographics() {
		return demographics;
	}
	public void setDemographics(List<DemographicsProperties> demographics) {
		this.demographics = demographics;
	}
	public String getDemographicsInputFieldDefinition() {
		return fieldDefinition;
	}
	public void setDemographicsInputFieldDefinition(String fieldDefinition) {
		this.fieldDefinition = fieldDefinition;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("fieldDefinition", fieldDefinition)
				.add("demographics", demographics)
				.toString();
	}
}
