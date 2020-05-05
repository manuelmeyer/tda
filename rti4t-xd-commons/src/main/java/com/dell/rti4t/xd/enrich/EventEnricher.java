package com.dell.rti4t.xd.enrich;

import com.dell.rti4t.xd.domain.DataTransporter;

public interface EventEnricher {
	DataTransporter enrich(DataTransporter dt);
}
