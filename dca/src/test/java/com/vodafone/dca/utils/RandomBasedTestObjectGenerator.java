package com.vodafone.dca.utils;

import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.vodafone.dca.domain.DataTransporter;

public class RandomBasedTestObjectGenerator {
	
	static String DEFAULT_IMSI = "123456789012345";
	static Random random = new Random();
	
	public static DataTransporter buildEvent(String imsi, int timeUTC, int lac, int cell) {
		return eventBuilder()
				.withImsi(imsi)
				.withTimeUTC(timeUTC)
				.withLacCell(lac, cell)
				.build();
	}
	
	public static DataTransporter buildEvent(int timeUTC, int lac, int cell) {
		return buildEvent(DEFAULT_IMSI, timeUTC, lac, cell);
	}
	
	public static DataTransporter buildEvent(String imsi) {
		return buildEvent(imsi, 0, 0, 0);
	}
	
	public static DataTransporter generateRandomEvent() {
		return buildEvent(generateString(), generateAbsInt(), generateAbsInt(), generateAbsInt());
	}
	
	public static int generateIntInRange(int low, int high) {
		return low + genarateIntBoundedBy(high - low);
	}
	
	public static int genarateIntBoundedBy(int bound) {
		return random.nextInt(bound);
	}
	
	public static int generateInt() {
		return random.nextInt();
	}
	
	public static int generateAbsInt() {
		return Math.abs(generateInt());
	}

	public static String buildUTC(int sec) {
		return String.valueOf(sec) + "000";
	}
	
	public static String generateString() {
		return UUID.randomUUID().toString();
	}
	
	public static DataTransporter generateImsiEvent(String imsi) {
		return eventBuilder()
				.withImsi(imsi)
				.build();
	}
	
	public static DataTransporter generateNowEvent() {
		return eventBuilder()
				.withNowTimeUTC()
				.build();
	}
	
	public static EventBuilder eventBuilder() {
		return new EventBuilder();
	}

	public static class EventBuilder {
		
		Map<String, Object> fieldMap = Maps.newHashMap();
		
		public EventBuilder withField(String name, Object value) {
			fieldMap.put(name, value);
			return this;
		}
		
		public EventBuilder withImsi(String imsi) {
			return withField("imsi", imsi);
		}
		
		public EventBuilder withTimeUTC(int timeUTC) {
			return withField("timeUTC", buildUTC(timeUTC));
		}

		public EventBuilder withNowTimeUTC() {
			return withField("timeUTC", (new Date().getTime() / 1000 + "000").toString());
		}
		
		public EventBuilder withLacCell(long lac, long cell) {
			return withField("lac", Long.valueOf(lac)).withField("cellTower", Long.valueOf(cell));
		}
		
		public DataTransporter build() {
			return new DataTransporter(fieldMap, "unused");
		}
	}
	
	public static Map<String, Object> generateRandomMap() {
		Map<String, Object> rMap = Maps.newHashMap();
		int totalCreated = generateIntInRange(4, 7);
		int modulo = generateIntInRange(1, 3);
		for (int index = 0; index < totalCreated; index++) {
			String key = generateString();
			rMap.put(key, index % modulo == 0 ? generateInt() : generateString());
		}
		return rMap;
	}
}
