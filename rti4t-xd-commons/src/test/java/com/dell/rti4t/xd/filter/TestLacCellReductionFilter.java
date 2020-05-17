package com.dell.rti4t.xd.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.filter.DataReductionImpl.ReductionMode;

public class TestLacCellReductionFilter {
	
	static private final Logger LOG = LoggerFactory.getLogger(TestLacCellReductionFilter.class);

	@Test
	public void canFilterAndFollowExit() throws Exception {
		DataReductionImpl dataReduction = new DataReductionImpl();
		dataReduction.setReductionMode(ReductionMode.IMSIS_CHANGE_CELL_ONLY);
		dataReduction.afterPropertiesSet();

		LacCellFilterImpl lacCellFilter = new LacCellFilterImpl();
		lacCellFilter.setLacCellFilePath("lac-and-cells.txt");
		lacCellFilter.setFollowExit(true);
		lacCellFilter.afterPropertiesSet();
		
		LacCellReductionFilterImpl underTest = new LacCellReductionFilterImpl();
		underTest.setDataReductionFilter(dataReduction);
		underTest.setLacCellFilter(lacCellFilter);
		
		DataTransporter dt;
		
		dt = buildEvent(10, 1, 60);	
		assertTrue(underTest.accept(dt));
		LOG.info("dt1 {}", dt);
		assertEquals("10", dt.getFieldValue("lac"));
		assertEquals("1", dt.getFieldValue("cellTower"));
		assertEquals("60000", dt.getFieldValue("timeUTC"));
		assertNull(dt.getFieldValue("previousLac"));
		assertNull(dt.getFieldValue("previousCellTower"));
		assertNull(dt.getFieldValue("previousTimeUTC"));

		dt = buildEvent(12, 5, 62);		
		assertTrue(underTest.accept(dt));		
		assertEquals("12", dt.getFieldValue("lac"));
		assertEquals("5", dt.getFieldValue("cellTower"));
		assertEquals("62000", dt.getFieldValue("timeUTC"));		
		assertEquals("10", dt.getFieldValue("previousLac"));
		assertEquals("1", dt.getFieldValue("previousCellTower"));
		assertEquals("60000", dt.getFieldValue("previousTimeUTC"));

		LOG.info("dt2 {}", dt);

		dt = buildEvent(12, 5, 63);
		assertFalse(underTest.accept(dt));

		dt = buildEvent(12, 5, 64);
		assertFalse(underTest.accept(dt));

		dt = buildEvent(12, 5, 65);
		assertFalse(underTest.accept(dt));
		
		dt = buildEvent(12, 5, 66);
		assertFalse(underTest.accept(dt));
		
		dt = buildEvent(10, 1, 67);		
		assertTrue(underTest.accept(dt));
		LOG.info("dt-second {}", dt);
		assertEquals("10", dt.getFieldValue("lac"));
		assertEquals("1", dt.getFieldValue("cellTower"));
		assertEquals("67000", dt.getFieldValue("timeUTC"));
		assertEquals("12", dt.getFieldValue("previousLac"));
		assertEquals("5", dt.getFieldValue("previousCellTower"));
		assertEquals("66000", dt.getFieldValue("previousTimeUTC"));

		dt = buildEvent(99, 99, 84);
		assertTrue(underTest.accept(dt));
		LOG.info("dt3 {}", dt);		
		assertEquals("99", dt.getFieldValue("lac"));
		assertEquals("99", dt.getFieldValue("cellTower"));
		assertEquals("84000", dt.getFieldValue("timeUTC"));
		assertEquals("10", dt.getFieldValue("previousLac"));
		assertEquals("1", dt.getFieldValue("previousCellTower"));
		assertEquals("67000", dt.getFieldValue("previousTimeUTC"));

		dt = buildEvent(999, 999, 90);
		assertFalse(underTest.accept(dt));
		
		dt = buildEvent(10, 1, 95);		
		assertTrue(underTest.accept(dt));
		LOG.info("dt4 {}", dt);
		
		dt = buildEvent(999, 999, 100);
		assertTrue(underTest.accept(dt));
		LOG.info("dt5 {}", dt);
		
		dt = buildEvent(11, 11, 110);
		assertTrue(underTest.accept(dt));
		assertEquals("11", dt.getFieldValue("lac"));
		assertEquals("11", dt.getFieldValue("cellTower"));
		assertEquals("110000", dt.getFieldValue("timeUTC"));
		assertNull(dt.getFieldValue("previousLac"));
		assertNull(dt.getFieldValue("previousCellTower"));
		assertNull(dt.getFieldValue("previousTimeUTC"));
	}
	
	private DataTransporter buildEvent(int lac, int cell, int timeUTC) {
		Map<String, Object> fields = new HashMap<String, Object>();
		DataTransporter dt = new DataTransporter(fields, "unused");
		fields.put("imsi", "123456789012345");
		fields.put("lac", String.valueOf(lac));
		fields.put("cellTower", String.valueOf(cell));
		fields.remove("lastLac");
		fields.remove("lastCellTower");
		fields.put("timeUTC", buildUTC(timeUTC));
		return dt;

	}

	private Object buildUTC(int sec) {
		return String.valueOf(sec) + "000";
	}
}