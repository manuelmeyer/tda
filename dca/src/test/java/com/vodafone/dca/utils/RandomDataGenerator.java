package com.vodafone.dca.utils;

import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.google.common.collect.Maps;
import com.vodafone.dca.domain.DataTransporter;

public class RandomDataGenerator {
	
	static Random random = new Random();
	
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
	
	public static DataTransporter generateEvent() {
		DataTransporter event = new DataTransporter();
		Map<String, Object> map = Maps.newHashMap();
		String timeUTC = new Date().getTime() / 1000 + "000";
		map.put("timeUTC", timeUTC);
		event.setFields(map);
		return event;
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
