package com.dell.rti4t.xd.kafkasource;

import java.util.Map;

public class ProtocolEventMapper {
	
	private TypeHelper<Long> longCaster = new TypeHelper<Long>(Long.class);
	private TypeHelper<Integer> intCaster = new TypeHelper<Integer>(Integer.class);
	
	Map<String, Object> map;
	void map(Map<String, Object> map) {
		this.map = map;
	}
	@SuppressWarnings("unchecked")
	Map<String, Object> getProtocolDetails() {
		return (Map<String, Object>) map.get("protocolDetails");
	}
	
	public String getPrimaryId() {
		return (String)map.get("primaryId");
	}
	public long getEventStartTime() {
		return longCaster.transform(map.get("eventStartTime"));
	}
	public long getEventEndTime() {
		return longCaster.transform(map.get("eventEndTime"));
	}
	public void setImsi(String imsi) {
		map.put("imsi", imsi);
	}
	public void setRealImsi(String realImsi) {
		map.put("realImsi", realImsi);
	}
	public void setCallingDigits(String callingDigits) {
		map.put("callingDigits", callingDigits);
	}
	public Object getServiceId() {
		return map.get("serviceId");
	}
	public void setMsisdn(Object msisdn) {
		map.put("msisdn", msisdn);
	}
	
	public void setRealMsisdn(String realMsisdn) {
		map.put("realMsisdn", realMsisdn);
	}
	public Object getSourceId() {
		return map.get("sourceId");
	}
	public void setImei(Object imei) {
		map.put("imei", imei);
	}
	public void setRealImei(String realImei) {
		map.put("realImei", realImei);
	}
	public void setMccmnc(String mccncc) {
		map.put("mccncc", mccncc);
	}
	public void setLac(int lac) {
		map.put("lac", lac);
	}
	public void setCellTower(int cellTower) {
		map.put("cellTower", cellTower);
	}
	public void setCellKey(String cellKey) {
		map.put("cellKey", cellKey);
	}
	public String getServiceTarget() {
		return (String)map.get("serviceTarget");
	}
	public void setTransactionTarget(String transactionTarget) {
		map.put("transactionTarget", transactionTarget);
	}
	public void setFirstLac(int firstLac) {
		map.put("firstLac", firstLac);
	}
	public void setFirstCellTower(int firstCellTower) {
		map.put("firstCellTower", firstCellTower);
	}
	public void setLatitude(double latitude) {
		map.put("latitude", latitude);
	}
	public void setLongitude(double longitude) {
		map.put("longitude", longitude);
	}
	public void setRadii90(double radii90) {
		map.put("radii90", radii90);
	}
	public void setDevice(String device) {
		map.put("device", device);
	}
	public void setStartTimeInMilliseconds(long startTimeInMilliseconds) {
		map.put("startTimeUTC", startTimeInMilliseconds);
	}
	public void setEventType(int eventType) {
		map.put("eventType", eventType);
	}
	public void setTimeUTC(long timeUTC) {
		map.put("timeUTC", timeUTC);
	}
	public int getProtocolType() {
		return intCaster.transform(map.get("protocolType"));
	}
	public void setEventStatus(long eventStatus) {
		map.put("eventStatus", eventStatus);
	}
	public Object getTag() {
		return map.get("tag");
	}
	public void setTagBitMask(Object tagBitMask) {
		map.put("tagBitMask", tagBitMask);
	}
	public void setExtTags(int[] extTags) {
		map.put("extTags", extTags);
	}
	public Object getIngestTime() {
		return map.get("ingestTime");
	}
	public void setIngestTime(Object ingestTime) {
		map.put("ingestTime", ingestTime);
	}
	public void setProcessedTime(long processedTime) {
		map.put("processedTime", processedTime);
	}
}
