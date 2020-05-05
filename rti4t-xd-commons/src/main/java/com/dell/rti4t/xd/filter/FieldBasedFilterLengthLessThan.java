package com.dell.rti4t.xd.filter;

public class FieldBasedFilterLengthLessThan implements FieldBasedFilter {
	
	private final int maxLength;
	
	public FieldBasedFilterLengthLessThan(int maxLength) {
		this.maxLength = maxLength;
	}

	@Override
	public boolean accept(String field) {
		return field.length() < maxLength;
	}
	
	public String toString() {
		return "[accept if length < " + maxLength + "]";
	}
}
