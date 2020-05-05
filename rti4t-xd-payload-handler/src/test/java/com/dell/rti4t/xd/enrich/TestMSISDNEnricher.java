package com.dell.rti4t.xd.enrich;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.enrich.MSISDNEnricherImpl;

public class TestMSISDNEnricher {
	
	@Test
	public void canEnrichDT() throws Exception {
		MSISDNEnricherImpl enricher = new MSISDNEnricherImpl();
		enricher.setDataSourceFilePath("imsi-msisdn.txt");
		enricher.afterPropertiesSet();

		Map<String, Object> fields = new HashMap<String, Object>();
		DataTransporter dt = new DataTransporter(fields, "unused");
		
		dt = enricher.enrich(dt);
		assertNull(dt.getFieldValue("msisdn"));
		
		fields.put("imsi",  "1111111111113");
		dt = enricher.enrich(dt);
		assertEquals("333333333333", dt.getFieldValue("msisdn"));
	}
}
