package com.dell.rti4t.xd.filter;

public class FieldBasedFilterAccepAll implements FieldBasedFilter {

	@Override
	public boolean accept(String field) {
		return true;
	}
	
	public String toString() {
		return "[accept all]";
	}
}
