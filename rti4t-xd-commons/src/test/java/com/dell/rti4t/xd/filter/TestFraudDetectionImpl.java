package com.dell.rti4t.xd.filter;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.google.common.collect.Maps;

public class TestFraudDetectionImpl {
	
	@SuppressWarnings("unused")
	private static final Logger LOG = LoggerFactory.getLogger(TestFraudDetectionImpl.class);
	
	@Test
	public void canPassThroughWhenDetectFraudIsFalse() throws Exception {
		FraudDetectionImpl underTest = new FraudDetectionImpl();
		underTest.setFraudDetection(false);
		underTest.afterPropertiesSet();
		
		DataTransporter input = createDataTransporter();
		
		assertTrue(underTest.accept(input));
		
		input.putFieldValue("imsi", "234150000000000");
		input.putFieldValue("msisdn", "060102030405");
		assertTrue(underTest.accept(input));

		// new msisdn
		input.putFieldValue("msisdn", "070102030405");
		assertTrue(underTest.accept(input));
		assertTrue(underTest.accept(input));
	}
	
	@Test
	public void canDisableWhenFieldsNotPresent() throws Exception {
		FraudDetectionImpl underTest = buildFraudFilter();
		DataTransporter input = createDataTransporter();

		assertFalse(underTest.accept(input));

		input.putFieldValue("imsi", "234150000000000");
		assertFalse(underTest.accept(input));

		input.putFieldValue("imsi", null);
		input.putFieldValue("msisdn", "060606060606");
		assertFalse(underTest.accept(input));
	}
	
	@Test
	public void canDetectFraudAndAddFraudValuesForMsisdn() throws Exception {		
		FraudDetectionImpl underTest = buildFraudFilter();
		DataTransporter input = createDataTransporter();

		input.putFieldValue("imsi", "234150000000000");
		input.putFieldValue("msisdn", "060606060606");
		assertFalse(underTest.accept(input));

		input.putFieldValue("msisdn", "070707070707");
		assertTrue(underTest.accept(input));
		
		assertNotNull(input.getFieldValue(FraudDetectionImpl.SWAPPED_MSISDN_KEY));
		assertEquals("060606060606", input.getFieldValue(FraudDetectionImpl.SWAPPED_MSISDN_KEY));
		
		assertNotNull(input.getFieldValue(FraudDetectionImpl.SWAPPED_IMEI_KEY));
		assertEquals("", input.getFieldValue(FraudDetectionImpl.SWAPPED_IMEI_KEY));
		
		// same values are not repeated.
		assertFalse(underTest.accept(input));
		
		input.putFieldValue("msisdn", null); // null value does not change
		assertFalse(underTest.accept(input));

		input.putFieldValue("msisdn", "070707070707"); // same value after a null does not change
		assertFalse(underTest.accept(input));

	}
	
	@Test
	public void canDetectFraudAndAddFraudValuesForImei() throws Exception {		
		FraudDetectionImpl underTest = buildFraudFilter();
		DataTransporter input = createDataTransporter();

		input.putFieldValue("imsi", "234150000000000");
		input.putFieldValue("imei", "060606060606");
		assertFalse(underTest.accept(input));

		input.putFieldValue("imei", "070707070707");
		assertTrue(underTest.accept(input));
		
		assertNotNull(input.getFieldValue(FraudDetectionImpl.SWAPPED_IMEI_KEY));
		assertEquals("060606060606", input.getFieldValue(FraudDetectionImpl.SWAPPED_IMEI_KEY));

		assertNotNull(input.getFieldValue(FraudDetectionImpl.SWAPPED_MSISDN_KEY));
		assertEquals("", input.getFieldValue(FraudDetectionImpl.SWAPPED_MSISDN_KEY));

		// same values are not repeated.
		assertFalse(underTest.accept(input));

		input.putFieldValue("imei", null); // null value does not change
		assertFalse(underTest.accept(input));

		input.putFieldValue("imei", "070707070707"); // same value after a null does not change
		assertFalse(underTest.accept(input));
	}

	private DataTransporter createDataTransporter() {
		DataTransporter input = new DataTransporter();
		input.setFields(Maps.<String, Object>newHashMap());
		return input;
	}

	private FraudDetectionImpl buildFraudFilter() throws Exception {
		FraudDetectionImpl underTest = new FraudDetectionImpl();
		underTest.setFraudDetection(true);
		underTest.afterPropertiesSet();
		return underTest;
	}
}
