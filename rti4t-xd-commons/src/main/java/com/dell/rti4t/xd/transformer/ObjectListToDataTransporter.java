package com.dell.rti4t.xd.transformer;

import java.util.List;

import com.dell.rti4t.xd.domain.DataTransporter;

public interface ObjectListToDataTransporter {
	DataTransporter buildFromObjectList(List<Object> objects);
}
