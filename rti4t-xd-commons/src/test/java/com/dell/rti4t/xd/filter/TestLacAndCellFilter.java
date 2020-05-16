package com.dell.rti4t.xd.filter;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.filter.DataReductionImpl.ReductionMode;

public class TestLacAndCellFilter {
	
	static private final Logger LOG = LoggerFactory.getLogger(TestLacAndCellFilter.class);
	
	@Test
	public void canFilterOnLacAndCellsWhenNoFileSet() {
		LacCellFilterImpl filter = new LacCellFilterImpl();
		DataTransporter dt = new DataTransporter();
		Assert.assertTrue(filter.accept(dt));
	}
	
	@Test
	public void canFilterOnLacAndCells() throws Exception {
		LacCellFilterImpl filter = new LacCellFilterImpl();
		filter.setLacCellFilePath("lac-and-cells.txt");
		filter.afterPropertiesSet();
		
		Map<String, Object> fields = new HashMap<String, Object>();
		DataTransporter dt = new DataTransporter(fields, "unused");
		
		fields.put("lac", "10");
		fields.put("cellTower", "1");
		Assert.assertTrue(filter.accept(dt));

		fields.put("lac", "10");
		fields.put("cellTower", "100");
		Assert.assertFalse(filter.accept(dt));

		fields.put("lac", "11"); // 11,* accept all cells
		fields.put("cellTower", String.valueOf((int)(Math.random() * 10000)));
		Assert.assertTrue(filter.accept(dt));
	}
	
	
	@Test(expected = FileNotFoundException.class)
	public void throwAnExceptionOnBadFile() throws Exception {
		
		LacCellFilterImpl filter = new LacCellFilterImpl();
		filter.setLacCellFilePath("no-way-I-exists!");
		filter.afterPropertiesSet();
		
		Map<String, Object> fields = new HashMap<String, Object>();
		DataTransporter dt = new DataTransporter(fields, "unused");
		fields.put("imsi", "123456789012345");
		fields.put("lac", "10");
		fields.put("cellTower", "1");
		Assert.assertTrue(filter.accept(dt));
		
		fields.put("lac", "10");
		fields.put("cellTower", "1");
		Assert.assertTrue(filter.accept(dt));

	}
	
	@Test
	public void canFilterAndFollowExit() throws Exception {
		DataReductionImpl dataReduction = new DataReductionImpl();
		dataReduction.setReductionMode(ReductionMode.IMSIS_CHANGE_CELL_ONLY);
		dataReduction.afterPropertiesSet();

		LacCellFilterImpl lacCellFfilter = new LacCellFilterImpl();
		lacCellFfilter.setLacCellFilePath("lac-and-cells.txt");
		lacCellFfilter.setFollowExit(true);
		lacCellFfilter.afterPropertiesSet();
		
		DataTransporter dt;
		
		dt = buildEvent(10, 1, 60);		
		Assert.assertTrue(lacCellFfilter.accept(dt));
		Assert.assertTrue(dataReduction.accept(dt));
		LOG.info("dt1 {}", dt);

		dt = buildEvent(12, 5, 62);		
		Assert.assertTrue(lacCellFfilter.accept(dt));
		Assert.assertTrue(dataReduction.accept(dt));
		LOG.info("dt2 {}", dt);

		dt = buildEvent(12, 5, 63);
		Assert.assertTrue(lacCellFfilter.accept(dt));
		Assert.assertFalse(dataReduction.accept(dt));

		dt = buildEvent(12, 5, 64);
		Assert.assertTrue(lacCellFfilter.accept(dt));
		Assert.assertFalse(dataReduction.accept(dt));

		dt = buildEvent(12, 5, 65);
		Assert.assertTrue(lacCellFfilter.accept(dt));
		Assert.assertFalse(dataReduction.accept(dt));
		
		dt = buildEvent(12, 5, 66);
		Assert.assertTrue(lacCellFfilter.accept(dt));
		Assert.assertFalse(dataReduction.accept(dt));
		
		dt = buildEvent(10, 1, 67);		
		Assert.assertTrue(lacCellFfilter.accept(dt));
		Assert.assertTrue(dataReduction.accept(dt));
		LOG.info("dt-second {}", dt);

		dt = buildEvent(99, 99, 84);
		Assert.assertTrue(lacCellFfilter.accept(dt));
		Assert.assertTrue(dataReduction.accept(dt));
		LOG.info("dt3 {}", dt);

		dt = buildEvent(999, 999, 90);
		Assert.assertFalse(lacCellFfilter.accept(dt));
		
		dt = buildEvent(10, 1, 95);		
		Assert.assertTrue(lacCellFfilter.accept(dt));
		Assert.assertTrue(dataReduction.accept(dt));
		LOG.info("dt4 {}", dt);
		
		dt = buildEvent(999, 999, 100);
		Assert.assertTrue(lacCellFfilter.accept(dt));
		Assert.assertTrue(dataReduction.accept(dt));
		LOG.info("dt5 {}", dt);
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
