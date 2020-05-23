package com.dell.rti4t.xd.filter;

import static com.dell.rti4t.xd.testutil.EventTestBuilder.buildEvent;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dell.rti4t.xd.common.ImsiHistory;
import com.dell.rti4t.xd.common.ReductionMapHandler;
import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.filter.DataReductionImpl.ReductionMode;
import com.dell.rti4t.xd.testutil.EventTestBuilder;

public class TestDataReduction {
	
	static private final Logger LOG = LoggerFactory.getLogger(TestDataReduction.class);
	
	@Test
	public void checkFlowFromEventFile() throws Exception {
		InputStream is = this.getClass().getClassLoader().getResourceAsStream("events-in.csv");

		DataReductionImpl dataReduction = new DataReductionImpl();
		dataReduction.afterPropertiesSet();
		
		DataTransporter dt = new DataTransporter();
		Map<String, Object> fields = new HashMap<String, Object>();
		dt.setFields(fields);
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		
		String line;
		int sequence = -1;
		while((line = reader.readLine()) != null) {
			dt.setFields(buildMap(fields, line));
			int newSequence = Integer.valueOf(dt.getFieldValue("sequence"));
			assertTrue(newSequence > sequence);
			sequence = newSequence;
			boolean rc = dataReduction.accept(dt);
			//assertTrue(format("Failed on %d - %s", sequence, line), rc);
		}
	}
	
	private Map<String, Object> buildMap(Map<String, Object> fieldsMap, String line) {
		String[] fields = line.split(",");
		fieldsMap.put("sequence", fields[0]);
		fieldsMap.put("imsi", fields[1]);
		fieldsMap.put("timeUTC", fields[2]);
		fieldsMap.put("lac", fields[3]);
		fieldsMap.put("cellTower", fields[4]);
		return fieldsMap;
	}

	@Test
	public void canFilterWhenMissingFields() throws Exception {
		DataReductionImpl dataReduction = new DataReductionImpl();
		dataReduction.afterPropertiesSet();
		
		DataTransporter dt = new DataTransporter();
		Map<String, Object> fields = new HashMap<String, Object>();
		
		dt.setFields(fields);
		
		assertFalse(dataReduction.accept(dt));
		fields.put("imsi", "imsi");

		assertFalse(dataReduction.accept(dt));
		fields.put("lac", "10");
		fields.put("cellTower", "10");
		assertFalse(dataReduction.accept(dt));

		fields.put("timeUTC", "10");
		assertTrue(dataReduction.accept(dt));
	}
	
	@Test
	public void canAcceptWhenLacOrCellChange() throws Exception {
		DataReductionImpl dataReduction = new DataReductionImpl();
		dataReduction.afterPropertiesSet();
		
		DataTransporter dt = new DataTransporter();
		Map<String, Object> fields = new HashMap<String, Object>();
		
		dt.setFields(fields);
		fields.put("imsi", "imsi");
		fields.put("lac", "10");
		fields.put("cellTower", "10");
		fields.put("timeUTC", "10");
		assertTrue(dataReduction.accept(dt));

		fields.put("lac", "11");
		assertTrue(dataReduction.accept(dt));
	}
	
	@Test
	public void canSeeCellThrottling() throws Exception {
		DataReductionImpl dataReduction = new DataReductionImpl();
		dataReduction.afterPropertiesSet();
		
		DataTransporter dt = new DataTransporter();
		Map<String, Object> fields = new HashMap<String, Object>();
		dt.setFields(fields);
		
		boolean rc;
		
		fields.put("imsi", "imsi");
		fields.put("lac", "302");
		fields.put("cellTower", "61554");
		fields.put("timeUTC", "148110 19 49 000".replaceAll(" ", ""));
		
		assertTrue(dataReduction.accept(dt));

		fields.put("lac", "307");
		fields.put("cellTower", "42385");
		fields.put("timeUTC", "148110 19 55 000".replaceAll(" ", ""));

		assertTrue(dataReduction.accept(dt));

		fields.put("lac", "307");
		fields.put("cellTower", "63620");
		fields.put("timeUTC", "148110 19 40 000".replaceAll(" ", ""));
		
		rc = dataReduction.accept(dt);
		LOG.info("rc0 {} for {}", rc, fields);
		
		fields.put("lac", "307");
		fields.put("cellTower", "42385");
		fields.put("timeUTC", "148110 19 55 000".replaceAll(" ", ""));
		
		rc = dataReduction.accept(dt);
		LOG.info("rc1 {} for {}", rc, fields);
		
		fields.put("lac", "302");
		fields.put("cellTower", "61554");
		fields.put("timeUTC", "148110 19 61 000".replaceAll(" ", ""));
		
		rc = dataReduction.accept(dt);
		LOG.info("rc2 {} for {}", rc, fields);
		
		fields.put("lac", "307");
		fields.put("cellTower", "63620");
		fields.put("timeUTC", "148110 19 37 000".replaceAll(" ", ""));
		
		rc = dataReduction.accept(dt);
		LOG.info("rc3 {} for {}", rc, fields);		
	}
	
	@Test
	public void canAcceptBasedOnTime() throws Exception {
		DataReductionImpl dataReduction = new DataReductionImpl();
		dataReduction.afterPropertiesSet();
		
		DataTransporter dt = new DataTransporter();
		Map<String, Object> fields = new HashMap<String, Object>();
		
		dt.setFields(fields);
		fields.put("imsi", "imsi");
		fields.put("lac", "10");
		fields.put("cellTower", "10");
		fields.put("timeUTC", "10000 00 00 000".replaceAll(" ", ""));
		assertTrue(dataReduction.accept(dt));

		fields.put("timeUTC", "10000 00 30 000".replaceAll(" ", ""));
		assertFalse(dataReduction.accept(dt));
		
		fields.put("timeUTC", "10000 00 50 000".replaceAll(" ", ""));
		assertFalse(dataReduction.accept(dt));

		fields.put("timeUTC", "10000 00 61 000".replaceAll(" ", ""));
		assertTrue(dataReduction.accept(dt));

		fields.put("timeUTC", "10000 00 91 000".replaceAll(" ", ""));
		assertFalse(dataReduction.accept(dt));

		fields.put("timeUTC", "10000 0 191 000".replaceAll(" ", ""));
		assertTrue(dataReduction.accept(dt));
	}
	
	@Test
	public void canFilterCellChangeNoTimeWithModeCellOnly() throws Exception {
		DataReductionImpl dataReduction = new DataReductionImpl();
		dataReduction.setReductionMode(ReductionMode.IMSIS_CHANGE_CELL_ONLY);
		dataReduction.afterPropertiesSet();
		
		DataTransporter dt = new DataTransporter();
		Map<String, Object> fields = new HashMap<String, Object>();
		
		dt.setFields(fields);
		fields.put("imsi", "imsi");
		fields.put("lac", "10");
		fields.put("cellTower", "10");
		fields.put("timeUTC", "10000 00 00 000".replaceAll(" ", ""));
		assertTrue(dataReduction.accept(dt));

		fields.put("timeUTC", "10000 00 30 000".replaceAll(" ", ""));
		assertFalse(dataReduction.accept(dt));
		
		fields.put("timeUTC", "10000 00 61 000".replaceAll(" ", ""));
		assertFalse(dataReduction.accept(dt));
		
		// more than 8h after it was last seen on this cell tower event is visible
		long timeUTC = 100000000000L + (8 * 3600 * 1000) + 1000; 
		fields.put("timeUTC", String.valueOf(timeUTC));
		assertTrue(dataReduction.accept(dt));
		
		timeUTC += 2000; // then quiet again
		fields.put("timeUTC", String.valueOf(timeUTC));
		assertFalse(dataReduction.accept(dt));
		
		timeUTC += (8 * 3600 * 1000); // then visible after 8h
		fields.put("timeUTC", String.valueOf(timeUTC));
		assertTrue(dataReduction.accept(dt));
	}
	
	@Test
	public void canFilterCellChangeOnlyWithModeCellOnly() throws Exception {
		DataReductionImpl dataReduction = new DataReductionImpl();
		dataReduction.setReductionMode(ReductionMode.IMSIS_CHANGE_CELL_ONLY);
		dataReduction.afterPropertiesSet();
		
		DataTransporter dt = new DataTransporter();
		
		dt = buildEvent("imsi", 10, 10, 1000);
		assertTrue(dataReduction.accept(dt));
		ImsiHistory imsiHistory = ReductionMapHandler.getImsiHistory("imsi");

		dt = buildEvent("imsi", 10, 10, 1030);
		assertFalse(dataReduction.accept(dt));
		imsiHistory.isGeoFence(true);
		
		dt = buildEvent("imsi", 11, 11, 1030);
		imsiHistory.isGeoFence(true);
		assertTrue(dataReduction.accept(dt));
		
		LOG.info("Imsi history {}", imsiHistory);
		assertEquals(imsiHistory.lac, 11);
		assertEquals(imsiHistory.cellTower, 11);
		assertEquals(imsiHistory.previousLac, 10);
		assertEquals(imsiHistory.previousCellTower, 10);
	}
}
