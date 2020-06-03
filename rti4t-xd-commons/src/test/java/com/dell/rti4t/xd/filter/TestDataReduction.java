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
		dataReduction.setReductionMode(ReductionMode.IMSIS_CHANGE_CELL_ONLY);
		dataReduction.afterPropertiesSet();
		
		DataTransporter dt;
		
		dt = buildEvent("imsi", 10, 10, 1000);		
		assertTrue(dataReduction.accept(dt));

		dt = buildEvent("imsi", 11, 10, 2000);		
		assertTrue(dataReduction.accept(dt));
	}
	
	@Test
	public void canSeeCellThrottling() throws Exception {
		DataReductionImpl dataReduction = new DataReductionImpl();
		dataReduction.setReductionMode(ReductionMode.IMSIS_CHANGE_CELL_ONLY_STRONG_DEDUP);
		dataReduction.afterPropertiesSet();
		
		DataTransporter dt;
		
		dt = buildEvent(1949, 302, 61554);
		assertTrue(dataReduction.accept(dt));
		
		dt = buildEvent(1955, 307, 42385);
		assertTrue(dataReduction.accept(dt));
		
		dt = buildEvent(1940, 500, 63620); // change of cell but in the past.
		assertFalse(dataReduction.accept(dt));

		dt = buildEvent(1960, 302, 61554);
		assertFalse(dataReduction.accept(dt));

		dt = buildEvent(1965, 307, 42385);
		assertFalse(dataReduction.accept(dt));
		
		dt = buildEvent(1970, 400, 42385);
		assertTrue(dataReduction.accept(dt));

		dt = buildEvent(1975, 307, 42385);
		assertFalse(dataReduction.accept(dt));
		
		dt = buildEvent(1990, 302, 61554);
		assertTrue(dataReduction.accept(dt));
	}
	
	@Test
	public void canAcceptBasedOnTime() throws Exception {
		DataReductionImpl dataReduction = new DataReductionImpl();
		dataReduction.setReductionMode(ReductionMode.IMSIS_CHANGE_CELL);
		dataReduction.afterPropertiesSet();
		
		DataTransporter dt = new DataTransporter();
		
		dt = buildEvent(1000, 10, 10); // first seen
		assertTrue(dataReduction.accept(dt));

		dt = buildEvent(1030, 10, 10); // dedup
		assertFalse(dataReduction.accept(dt));
		
		dt = buildEvent(1050, 10, 10);
		assertFalse(dataReduction.accept(dt));

		dt = buildEvent(1061, 10, 10); // 60s no event, we let it go
		assertTrue(dataReduction.accept(dt));

		dt = buildEvent(999, 10, 10); // blocked in the past
		assertFalse(dataReduction.accept(dt));

		dt = buildEvent(1100, 10, 10); // dedup
		assertFalse(dataReduction.accept(dt));

		dt = buildEvent(1122, 10, 10); // 60s since last 60s, we let it go
		assertTrue(dataReduction.accept(dt));
	}
	
	@Test
	public void canFilterCellChangeNoTimeWithModeCellOnly() throws Exception {
		DataReductionImpl dataReduction = new DataReductionImpl();
		dataReduction.setReductionMode(ReductionMode.IMSIS_CHANGE_CELL_ONLY);
		dataReduction.afterPropertiesSet();
		
		DataTransporter dt = buildEvent(100, 10, 10);
		assertTrue(dataReduction.accept(dt));

		dt = buildEvent(130, 10, 10);
		assertFalse(dataReduction.accept(dt));
		
		dt = buildEvent(200, 10, 10);
		assertFalse(dataReduction.accept(dt));
		
		// more than 8h after it was last seen on this cell tower event is visible
		int timeUTC =  200 + (8 * 3600) + 1; 
		dt = buildEvent(timeUTC, 10, 10);
		assertTrue(dataReduction.accept(dt));
		
		timeUTC += 2; // then quiet again
		dt = buildEvent(timeUTC, 10, 10);
		assertFalse(dataReduction.accept(dt));
		
		timeUTC += (8 * 3600); // then visible after 8h
		dt = buildEvent(timeUTC, 10, 10);
		assertTrue(dataReduction.accept(dt));
	}
	
	@Test
	public void canFilterCellChangeOnlyWithModeCellOnly() throws Exception {
		DataReductionImpl dataReduction = new DataReductionImpl();
		dataReduction.setReductionMode(ReductionMode.IMSIS_CHANGE_CELL_ONLY);
		dataReduction.afterPropertiesSet();
		
		DataTransporter dt = new DataTransporter();
		
		dt = buildEvent("imsi", 1000, 10, 10);
		assertTrue(dataReduction.accept(dt));
		ImsiHistory imsiHistory = ReductionMapHandler.getImsiHistory("imsi");

		dt = buildEvent("imsi", 1030, 10, 10);
		assertFalse(dataReduction.accept(dt));
		imsiHistory.isGeoFence(true);
		
		dt = buildEvent("imsi", 1040, 11, 11);
		imsiHistory.isGeoFence(true);
		assertTrue(dataReduction.accept(dt));
		
		LOG.info("Imsi history {}", imsiHistory);
		assertEquals(imsiHistory.lac(), 11);
		assertEquals(imsiHistory.cellTower(), 11);
		assertEquals(imsiHistory.previousLac(), 10);
		assertEquals(imsiHistory.previousCellTower(), 10);
	}
}
