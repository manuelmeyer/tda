package com.dell.rti4t.xd.domain;

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("serial")
public class DataTransporter implements Serializable {
	
	private String filterValue;
	private Map<String, Object> fields;
	
	public DataTransporter(Map<String, Object> fields, String filterValue) {
		this.filterValue = filterValue;
		this.fields = fields;
	}
	
	public DataTransporter(Map<String, Object> fields) {
		this(fields, null);
	}
	
	public DataTransporter() {
	}

	public void setFields(Map<String, Object> fields) {
		this.fields = fields;
	}
	
	public String getFieldValue(String valueName) {
		if(fields == null) { 
			return null;
		}
		Object field = fields.get(valueName);
		if(field == null) {
			return null;
		}
		return String.valueOf(field);
	}
	
	public void putFieldValue(String key, String value) {
		fields.put(key, value);
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
		return String.format("<filterValue='%s', fields='%s'>", filterValue, fields);
	}
}
