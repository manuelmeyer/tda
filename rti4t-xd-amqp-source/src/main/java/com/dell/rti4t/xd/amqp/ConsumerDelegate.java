package com.dell.rti4t.xd.amqp;

import java.util.Map;


public interface ConsumerDelegate {
	void initialise();
	void consume(byte[] b, Map<String, Object> map);
	void shutdown();
}