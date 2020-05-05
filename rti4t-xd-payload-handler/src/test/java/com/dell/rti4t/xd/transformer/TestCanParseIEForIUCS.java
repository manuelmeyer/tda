package com.dell.rti4t.xd.transformer;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.enrich.EventEnricher;
import com.dell.rti4t.xd.transformer.IEParserImpl;

public class TestCanParseIEForIUCS {
	
	@Test
	public void canParseIE() {
		EventEnricher parser = new IEParserImpl();
		Map<String, Object> fields = new HashMap<String, Object>();
		fields.put("protocolDetailMap.callType", "2093");

		fields.put("protocolDetailMap.informationElements", "303=012F 03 0A 0081 4000 00 00000018 00 55BA06BB 0000CF08;312=01380310FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0000000000000000".replaceAll(" ", ""));
		DataTransporter dt = new DataTransporter(fields, null);
		dt = parser.enrich(dt) ;
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.transactionType"), "81");
		
		fields.put("protocolDetailMap.informationElements", "303=012F 03 0A 0046 0000 00 000001D2 00 55C1E258 00062638;312=01380310FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0000000000000000".replaceAll(" ", ""));
		dt = parser.enrich(dt) ;
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.transactionType"), "46");
		
		fields.put("protocolDetailMap.informationElements", "303=012F 03 0A 001B 4000 00 000000A000 55EF52FE000B69E0".replaceAll(" ", ""));
		dt = parser.enrich(dt);
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.transactionType"), "1B");
	}
	
	@Test
	public void canParseIEStatus() {
		EventEnricher parser = new IEParserImpl();
		Map<String, Object> fields = new HashMap<String, Object>();
		fields.put("protocolDetailMap.callType", "2093");

		fields.put("protocolDetailMap.informationElements", "303=012F 03 0A 0081 4000 0000000018 01 AB 55BA06BB 0000CF08;312=01380310FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0000000000000000".replaceAll(" ", ""));

		DataTransporter dt = new DataTransporter(fields, null);
		dt = parser.enrich(dt) ;
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.ranapCause"), "AB");
		
		fields.put("protocolDetailMap.informationElements", "303=012F 03 0A 0081 4000 00 00000018 1F 01 02 03 04 05  55BA06BB 0000CF08;312=01380310FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0000000000000000".replaceAll(" ", ""));
		dt = parser.enrich(dt);

		/*
		protocolDetailMap.ranapCause,
		protocolDetailMap.ccCause,
		protocolDetailMap.mmCause,
		protocolDetailMap.transStatsSmsCpCause,
		protocolDetailMap.transStatsSmsRpCause,
		 */
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.ranapCause"), "01");
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.ccCause"), "02");
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.mmCause"), "03");
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.transStatsSmsCpCause"), "04");
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.transStatsSmsRpCause"), "05");
	}
	
	@Test
	public void canHandleNonRightCallTypeIE() throws Exception {
		String ie = "302=012E 03 08 0032 F451 00 365F3E 55EC4EA0 000B69E0".replaceAll(" ", "");
		EventEnricher parser = new IEParserImpl();
		
		Map<String, Object> fields = new HashMap<String, Object>();
		fields.put("protocolDetailMap.informationElements", ie);
		fields.put("protocolDetailMap.callType", "1234");

		DataTransporter dt = new DataTransporter(fields, null);
		dt = parser.enrich(dt);
	}
	
	@Test
	public void testCanHandle302IEs() {
		String ie = "302=012E 03 0A 00400000 00 0000097F 01 AB 55BA06B9 000A1608".replaceAll(" ", "");
		EventEnricher parser = new IEParserImpl();
		
		Map<String, Object> fields = new HashMap<String, Object>();
		fields.put("protocolDetailMap.informationElements", ie);
		fields.put("protocolDetailMap.callType", "2091");

		DataTransporter dt = new DataTransporter(fields, null);
		dt = parser.enrich(dt);
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.ranapCause"), "AB");
	}
	
	@Test
	public void canHandleMultipleEIs() {
		String ie =   "303=012F 03 0A 0080 0000 00 0000000C 00 55BA06A6 0002B750;" // oldest
					+ "303=012F 03 0A 0046 0000 00 00000583 08 33 55BA06A6 0002B750;" 
					+ "303=012F 03 0A 001B 4000 00 0000026A 01 12 55BA06A6 0002E630;" 
					+ "303=012F 03 0A 0043 4000 00 00000184 06 02 04 55BA06A6 000CAA30;"
					+ "303=012F 03 0A 0045 4000 00 0000015D 08 EF 55BA06A7 00037E88;" // most recent for CP Cause 
					+ "303=012F 03 0B 0018 4000 00 00000077 01 53 55BA06A7 0008FCC8;" // most recent for Ranap Cause
					+ "303=012F 03 0A 0081 4000 00 00000013 00 55BA06A7 000B1FA8;" // newest
					+ "312=01380310FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF0000000000000000";
		
		ie = ie.replaceAll(" ", "");
		
		EventEnricher parser = new IEParserImpl();
		
		Map<String, Object> fields = new HashMap<String, Object>();
		fields.put("protocolDetailMap.informationElements", ie);
		fields.put("protocolDetailMap.callType", "2093");

		DataTransporter dt = new DataTransporter(fields, null);
		
		dt = parser.enrich(dt);
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.transactionType"), "81");
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.ranapCause"), "53");
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.transStatsSmsCpCause"), "EF");
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.mmCause"), "04");
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.ccCause"), "02");
		
		Assert.assertNull(dt.getFieldValue("protocolDetailMap.transStatsSmsRpCause"));
		Assert.assertNull(dt.getFieldValue("protocolDetailMap.transStatsSmsRpCause"));
	}
	
	@Test
	public void canHandle301IEs() {
		String ie = "422=01A603100000045E00001B2EFFFFFFFFFFFFFFFF0000000000000000;425=01A9030055BA06B8000B46B8;426=01AA030055BA06B8000F3688;323=014303010055BA06B8000B46B8;" 
				+ "301=012D 03 0B 0069 4000 00 0000 0118 02 1F 55BA06B8 000B46B8;"
				+ "310=013603100000045E00001B2EFFFFFFFFFFFFFFFF55BA06B8000F3688;315=013B030201A055BA06B200009858;315=013B03070660040200058155BA06B20002DE60";
		ie = ie.replaceAll(" ", "");
		EventEnricher parser = new IEParserImpl();
		
		Map<String, Object> fields = new HashMap<String, Object>();
		fields.put("protocolDetailMap.informationElements", ie);
		fields.put("protocolDetailMap.callType", "2081");

		DataTransporter dt = new DataTransporter(fields, null);
		dt = parser.enrich(dt);
		Assert.assertEquals(dt.getFieldValue("protocolDetailMap.ccCause"), "1F");

	}
}
