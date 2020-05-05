package com.dell.rti4t.xd.filter;

import com.dell.rti4t.xd.domain.DataTransporter;

public interface EventFilter {
	boolean accept(DataTransporter dt);
	String description();
}
