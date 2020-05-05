package com.dell.rti4t.xd.eventhandler;

import java.util.Map;

public interface DataTransporterEventHandler {
	void onEvent(Object body, Map<String, Object> headers);
	void flushOnTimeout();
	boolean isRunning();
}
