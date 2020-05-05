package com.dell.rti4t.xd.enrich;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.enrich.SubFieldEnricher;

public class TestSubFieldEnricher {
	
	@Test
	public void canExtractCountryCode() {
		SubFieldEnricher enricher = new SubFieldEnricher();
		DataTransporter dt = new DataTransporter();
		dt.setFields(new HashMap<String, Object>());
		
		dt.putFieldValue("imsi", "12345657");
		enricher.enrich(dt);
		String countryCode = dt.getFieldValue("countryCode");
		Assert.assertNotNull(countryCode);
		Assert.assertEquals("12345", countryCode);		
		
		dt.setFields(new HashMap<String, Object>());
		dt.putFieldValue("imsi", "123");
		enricher.enrich(dt);
		countryCode = dt.getFieldValue("countryCode");
		Assert.assertNull(countryCode);
	}
}
