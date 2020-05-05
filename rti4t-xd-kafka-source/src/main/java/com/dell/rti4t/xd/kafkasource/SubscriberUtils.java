package com.dell.rti4t.xd.kafkasource;


public class SubscriberUtils {
	
	static TypeHelper<Long> longCaster = new TypeHelper<Long>(Long.class);
	static TypeHelper<Integer> intCaster = new TypeHelper<Integer>(Integer.class);
	static TypeHelper<Double> doubleCaster = new TypeHelper<Double>(Double.class);

	public static String getCellKey(final ProtocolEventMapper sis) {
		return CellTower.keyFromLacAndId((int) sis.getProtocolDetails().get("lac"), (int) sis.getProtocolDetails().get("cellId"));
	}

	public static boolean isImsiAvilable(final ProtocolEventMapper sis) {
		final String imsi = sis.getPrimaryId();
		return imsi != null && !imsi.trim().isEmpty();
	}

	public static boolean isRealMsisdnAvilable(final ProtocolEventMapper sis) {
		final String realmsisdn = (String) sis.getProtocolDetails().get("realMsisdn");
		return realmsisdn != null && !realmsisdn.trim().isEmpty();
	}

	public static boolean isLACAvailable(final ProtocolEventMapper sis) {
		return (int)intCaster.transform(sis.getProtocolDetails().get("lac")) > 0;
	}

	public static boolean isCellKeyAvailable(final ProtocolEventMapper sis) {
		final int cellId = (int)intCaster.transform(sis.getProtocolDetails().get("cellId"));
		return isLACAvailable(sis) && cellId > 0;
	}

	public static boolean isCachedLocationAvilable(final ProtocolEventMapper sis) {
		final double latitude = (double) doubleCaster.transform(sis.getProtocolDetails().get("latitude"));
		final double longitude = (double) doubleCaster.transform(sis.getProtocolDetails().get("longitude"));
		return latitude != 90.0 && longitude != 180.0;
	}

	public static boolean isCachedDeviceAvilable(final ProtocolEventMapper sis) {
		return sis.getProtocolDetails().get("device") != null;
	}

	public static boolean isStartTimeAvailable(final ProtocolEventMapper sis) {
		return sis.getEventStartTime() > 0L;
	}

	public static boolean isCurrentTimeAvailable(final ProtocolEventMapper sis) {
		return sis.getEventEndTime() > 0L;
	}

	public static boolean isExtTagsAvailable(final ProtocolEventMapper sis) {
		final int[] extTags = (int[]) sis.getProtocolDetails().get("extTags");
		return extTags != null && extTags.length > 0;
	}

//	@Deprecated
//	public static Subscriber createSubscriberFromSubscriberInfoStruct(final ProtocolEvent sis) {
//		final Subscriber subscriber = new Subscriber(sis.getProtocolName());
//		subscriber.setImsi(sis.getPrimaryId());
//		subscriber.setRealImsi((String) sis.getProtocolDetails().get("realImsi"));
//		subscriber.setMsisdn(sis.getServiceId());
//		subscriber.setRealMsisdn((String) sis.getProtocolDetails().get("realMsisdn"));
//		subscriber.setImei(sis.getSourceId());
//		subscriber.setRealImei((String) sis.getProtocolDetails().get("realImei"));
//		subscriber.setMccmnc((String) sis.getProtocolDetails().get("mccmnc"));
//		subscriber.setLac((int) sis.getProtocolDetails().get("lac"));
//		subscriber.setCellTower((int) sis.getProtocolDetails().get("cellId"));
//		subscriber.setTransactionTarget(sis.getServiceTarget());
//		subscriber.setFirstLac((int) sis.getProtocolDetails().get("firstLac"));
//		subscriber.setFirstCellTower((int) sis.getProtocolDetails().get("firstCellId"));
//		if (isCachedLocationAvilable(sis)) {
//			subscriber.setLatitude((double) sis.getProtocolDetails().get("latitude"));
//			subscriber.setLongitude((double) sis.getProtocolDetails().get("longitude"));
//			subscriber.setRadii90((double) sis.getProtocolDetails().get("radii90"));
//		}
//		if (isCachedDeviceAvilable(sis)) {
//			subscriber.setDevice((String) sis.getProtocolDetails().get("device"));
//		}
//		if (isStartTimeAvailable(sis)) {
//			subscriber.setStartTimeInMilliseconds(sis.getEventStartTime() * 1000L);
//		}
//		if (isCurrentTimeAvailable(sis)) {
//			subscriber.setTimeInMilliseconds(sis.getEventEndTime() * 1000L);
//		}
//		subscriber.setProtocolAttributeMap(sis.getProtocolAttributeMap());
//		subscriber.setEventType(sis.getProtocolType());
//		subscriber.setEventStatus((long) sis.getProtocolDetails().get("status"));
//		subscriber.setProtocolDetailMap(sis.getProtocolDetails());
//		subscriber.setTagBitMask(sis.getTag());
//		if (isExtTagsAvailable(sis)) {
//			subscriber.setExtTags((int[]) sis.getProtocolDetails().get("extTags"));
//		}
//		subscriber.setIngestTime(sis.getIngestTime());
//		subscriber.setProcessedTime(System.currentTimeMillis());
//		return subscriber;
//	}

//	public static Subscriber updateGeoFenceTag(final ProtocolEvent sis, final Subscriber subscriber) {
//		subscriber.setTagBitMask(TagOperations.setSubscriberGeoFenceTag(sis.getTag()));
//		return subscriber;
//	}
//
//	public static ProtocolEvent scrubEncodedLength(final ProtocolEvent sis, final Anonymizer anonymizer) {
//		sis.setPrimaryId(anonymizer.extractLengthEncodedString(sis.getPrimaryId()));
//		final String realImsi = (String) sis.getProtocolDetails().get("realImsi");
//		sis.getProtocolDetails().put("realImsi", anonymizer.extractLengthEncodedString(realImsi));
//		sis.setServiceId(anonymizer.extractLengthEncodedString(sis.getServiceId()));
//		final String realMsisdn = (String) sis.getProtocolDetails().get("realMsisdn");
//		sis.getProtocolDetails().put("realMsisdn", anonymizer.extractLengthEncodedString(realMsisdn));
//		sis.setServiceTarget(anonymizer.extractLengthEncodedString(sis.getServiceTarget()));
//		return sis;
//	}
}
