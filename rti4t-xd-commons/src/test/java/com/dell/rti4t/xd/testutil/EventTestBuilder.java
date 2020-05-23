package com.dell.rti4t.xd.testutil;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import com.dell.rti4t.xd.domain.DataTransporter;

public class EventTestBuilder {
	
	private static final Random random = new Random();
	
	public static DataTransporter buildEvent(String imsi, int timeUTC, int lac, int cell) {
		Map<String, Object> fields = new HashMap<String, Object>();
		DataTransporter dt = new DataTransporter(fields, "unused");
		fields.put("imsi", imsi);
		fields.put("lac", String.valueOf(lac));
		fields.put("cellTower", String.valueOf(cell));
		fields.put("timeUTC", buildUTC(timeUTC));
		return dt;
	}
	
	public static DataTransporter generateEvent() {
		return buildEvent(generateString(), generateAbsInt(), generateAbsInt(), generateAbsInt());
	}
	
	public static int generateIntInRange(int low, int high) {
		return low + genarateIntBoundedBy(high - low);
	}
	
	public static int genarateIntBoundedBy(int bound) {
		return random.nextInt(bound);
	}
	
	private static int generateAbsInt() {
		return Math.abs(random.nextInt());
	}

	public static DataTransporter buildEvent(int timeUTC, int lac, int cell) {
		return buildEvent("123456789012345", timeUTC, lac, cell);
	}

	public static String buildUTC(int sec) {
		return String.valueOf(sec) + "000";
	}
	
	public static String generateString() {
		return UUID.randomUUID().toString();
	}
}
