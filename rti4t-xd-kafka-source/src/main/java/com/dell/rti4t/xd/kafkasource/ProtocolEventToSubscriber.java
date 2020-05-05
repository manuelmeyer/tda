package com.dell.rti4t.xd.kafkasource;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class ProtocolEventToSubscriber {

	static private TypeHelper<Long> longCaster = new TypeHelper<Long>(Long.class);
	static private TypeHelper<Integer> intCaster = new TypeHelper<Integer>(Integer.class);
	static private TypeHelper<Double> doubleCaster = new TypeHelper<Double>(Double.class);
	
	static public ProtocolEventMapper turnToSubscriber(ProtocolEventMapper event) {
        Map<String, Object> protocolDetails = event.getProtocolDetails();
        if(protocolDetails == null) {
        	return event;
        }
		ProtocolEventDefaults.populateDetailMapWithMissingDefaultValues((protocolDetails));
		event.setImsi(event.getPrimaryId());
		event.setRealImsi((String)event.getProtocolDetails().get("realImsi"));
		event.setCallingDigits((String)event.getProtocolDetails().get("callingDigits"));
		event.setMsisdn(event.getServiceId());
		event.setRealMsisdn((String)event.getProtocolDetails().get("realMsisdn"));
		event.setImei(event.getSourceId());
		event.setRealImei((String)event.getProtocolDetails().get("realImei"));
		event.setMccmnc((String)event.getProtocolDetails().get("mccmnc"));
		event.setLac((int)intCaster.transform(event.getProtocolDetails().get("lac")));
		event.setCellTower((int)intCaster.transform(event.getProtocolDetails().get("cellId")));
		event.setCellKey(SubscriberUtils.getCellKey(event));
		event.setTransactionTarget(event.getServiceTarget());
		event.setFirstLac((int)intCaster.transform(event.getProtocolDetails().get("firstLac")));
		event.setFirstCellTower((int)intCaster.transform(event.getProtocolDetails().get("firstCellId")));
        if(SubscriberUtils.isCachedLocationAvilable(event)) {
        	event.setLatitude((double)doubleCaster.transform(event.getProtocolDetails().get("latitude")));
        	event.setLongitude((double)doubleCaster.transform(event.getProtocolDetails().get("longitude")));
        	event.setRadii90((double)doubleCaster.transform(event.getProtocolDetails().get("radii90")));
        }
        if (SubscriberUtils.isCachedDeviceAvilable(event)) {
        	event.setDevice((String)event.getProtocolDetails().get("device"));
        }
        if (SubscriberUtils.isStartTimeAvailable(event)) {
        	event.setStartTimeInMilliseconds(event.getEventStartTime() * 1000L);
        }
        if (SubscriberUtils.isCurrentTimeAvailable(event)) {
        	event.setTimeUTC(event.getEventEndTime() * 1000L);
        }
        event.setEventType((int)event.getProtocolType());
        event.setEventStatus((long)longCaster.transform(event.getProtocolDetails().get("status")));
        event.setTagBitMask(event.getTag());
        if (SubscriberUtils.isExtTagsAvailable(event)) {
        	event.setExtTags((int[])event.getProtocolDetails().get("extTags"));
        }
        event.setIngestTime(event.getIngestTime());
        event.setProcessedTime(System.currentTimeMillis());
        return event;
	}
}
