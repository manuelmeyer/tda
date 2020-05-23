package com.dell.rti4t.xd.testutil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.dell.rti4t.xd.domain.DataTransporter;

public abstract class DataTransporterAssert {
	
	public static void assertDtEquals(DataTransporter dt, Integer lac, Integer cell, Integer timeUTC) {
		assertDtEquals(dt, lac, cell, timeUTC, null, null, null);
	}
	
	public static void assertDtEquals(DataTransporter dt, Integer lac, Integer cell, Integer timeUTC, Integer formerLac, Integer formerCell, Integer formerTimeUTC) {
		assertEquals(String.valueOf(lac), dt.getFieldValue("lac"));
		assertEquals(String.valueOf(cell), dt.getFieldValue("cellTower"));
		assertEquals(String.valueOf(timeUTC) + "000", dt.getFieldValue("timeUTC"));
		asssertNullOrEquals(formerLac, dt.getFieldValue("previousLac"));
		asssertNullOrEquals(formerCell, dt.getFieldValue("previousCellTower"));
		asssertNullOrEquals(nullOrAdd(formerTimeUTC, "000"), dt.getFieldValue("previousTimeUTC"));
	}

	public static String nullOrAdd(Integer value, String suffix) {
		return value == null ? null : String.valueOf(value) + suffix;
	}

	public static void asssertNullOrEquals(Object value, String fieldValue) {
		if(value != null) {
			assertEquals(String.valueOf(value), fieldValue);
		} else {
			assertNull(fieldValue);
		}
	}

}
