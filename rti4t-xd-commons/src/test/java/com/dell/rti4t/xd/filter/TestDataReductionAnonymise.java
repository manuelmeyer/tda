package com.dell.rti4t.xd.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.transformer.MapFieldReducer;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/anonymise.xml"})
public class TestDataReductionAnonymise {
	
	@Autowired
	MapFieldReducer mapFieldReducer;
	
	@Test
	public void canFollowAnonymiseRules() {
		DataTransporter dt = new DataTransporter();
		Map<String, Object> fields = new HashMap<String, Object>();
		dt.setFields(fields);
		fields.put("a", "hello");
		fields.put("b", "world");
		fields.put("c", "untouch");
		
		String transformed;
		String[] parts;
		
		transformed = mapFieldReducer.transform(dt);
		parts = transformed.split(",");
		assertEquals(3, parts.length);
		assertEquals("hello", parts[0]);
		assertNotEquals("world", parts[1]);
		assertEquals("untouch", parts[2]);

		fields.put("a", "he"); // length < 3
		transformed = mapFieldReducer.transform(dt);
		parts = transformed.split(",");
		assertEquals(3, parts.length);
		assertNotEquals("he", parts[0]);
		assertNotEquals("world", parts[1]);
		assertEquals("untouch", parts[2]);
	}
}
