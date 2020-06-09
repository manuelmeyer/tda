package com.dell.rti4t.xd.utils;

public class MoreObjects {
	StringBuffer sb = new StringBuffer();
	String separator = "";
	
	public MoreObjects(Object object) {
		sb.append("<").append(object.getClass().getSimpleName());
	}
	
	public MoreObjects add(String name, Object value) {
		sb.append(separator).append(name).append("=").append(value);
		separator = ",";
		return this;
	}
	
	public String toString() {
		return sb.append(">").toString();
	}

	public static MoreObjects toStringHelper(Object object) {
		return new MoreObjects(object);
	}
}
