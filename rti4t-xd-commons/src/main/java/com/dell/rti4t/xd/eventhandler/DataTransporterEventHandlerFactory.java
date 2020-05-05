package com.dell.rti4t.xd.eventhandler;

import org.springframework.context.Lifecycle;
import org.springframework.messaging.MessageChannel;

public interface DataTransporterEventHandlerFactory extends Lifecycle {
	DataTransporterEventHandler getEventHandler(String handlerName, MessageChannel messageChannel);
}
