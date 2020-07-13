package com.vodafone.dca.domain;

import java.io.Serializable;
import java.util.Map;

import com.google.common.collect.Maps;

@SuppressWarnings("serial")
public class DataTransporter implements Serializable {
	
	public static class FieldMapFilterValue {
		String filter;
		long threadId;
		Map<String, Object> maskMap;
		
		public FieldMapFilterValue() {
			maskMap = Maps.newHashMap();
			threadId = Thread.currentThread().getId();
		}
		
		void setFilter(String filter) {
			this.filter = filter;
		}
		
		String getFilter() {
			return filter;
		}
		
		void put(String key, Object value) {
			maskMap.put(key, value);
		}
		
		Object get(String key) {
			return maskMap.get(key);
		}

		public Map<String, Object> map() {
			return maskMap;
		}

		public void clear() {
			maskMap.clear();
		}
	}
	
	static MaskMap maskMap = new MaskMap();
	
	public static class MaskMap extends ThreadLocal<FieldMapFilterValue> {
		@Override
		protected FieldMapFilterValue initialValue() {
			return new FieldMapFilterValue();
		}
	}
	
	public DataTransporter resetShadowMap() {
		maskMap.get().clear();
		return this;
	}
	
	private String filterValue;
	private Map<String, Object> fields;
	
	public DataTransporter(Map<String, Object> fields, String filterValue) {
		setFieldsAndFilter(fields, filterValue);
	}
	
	public DataTransporter(Map<String, Object> fields) {
		setFieldsAndFilter(fields, null);
	}
	
	public DataTransporter() {
	}
	
	protected void setFieldsAndFilter(Map<String, Object> fields, String filterValue)  {
		this.filterValue = filterValue;
		this.fields = fields;
		resetShadowMap();
	}

	public void setFields(Map<String, Object> fields) {
		setFieldsAndFilter(fields, this.filterValue);
	}
	
	public String getFieldValue(String valueName) {
		if(fields == null) { 
			return null;
		}
		Object field = maskMap.get().get(valueName);
		if(field == null) {
			field = fields.get(valueName);
		}
		return field == null ? null : String.valueOf(field);
	}
	
	public void putFieldValue(String key, String value) {
		maskMap.get().put(key, value);
	}
	
	public void setFilterValue(String filterValue) {
		this.filterValue = filterValue;
	}
	
	protected Map<String, Object> fffields() {
		return fields;
	}
	
	public String filter() {
		return filterValue;
	}
	
	public String toString() {
		return String.format("<filterValue='%s', fields='%s'>", filterValue, mergeFields());
	}

	public Map<String, Object> mergeFields() {
		Map<String, Object> merged = Maps.newHashMap(fields);
		merged.putAll(maskMap.get().map());
		return merged;
	}
}
