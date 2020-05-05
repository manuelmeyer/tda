package com.dell.rti4t.xd.process;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.messaging.Message;

import com.dell.rti4t.xd.eventhandler.DataTransporterEventHandler;
import com.dell.rti4t.xd.eventhandler.DataTransporterEventHandlerFactory;

public class LORSConsumer extends AbstractReplyProducingMessageHandler {

	static private final Logger LOG = LoggerFactory.getLogger(LORSConsumer.class.getPackage().getName() + ".LORS");
	
	private DataTransporterEventHandlerFactory eventHandlerFactory;
	public void setEventHandlerFactory(DataTransporterEventHandlerFactory eventHandlerFactory) {
		this.eventHandlerFactory = eventHandlerFactory;
	}
	
	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		String producerName = Thread.currentThread().getName();
		DataTransporterEventHandler producer = eventHandlerFactory.getEventHandler(producerName, getOutputChannel());
		if(producer.isRunning()) {
			producer.onEvent(requestMessage.getPayload(), (Map<String, Object>)requestMessage.getHeaders());
		}
		return null;
	}
}
