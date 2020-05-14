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
		
		Map<String, Object> fields = new HashMap<String, Object>();
		DataTransporter dt = new DataTransporter(fields, "unused");
		fields.put("imsi", "123456789012345");
		
//		fields.put("lac", "66");
//		fields.put("cellTower", "66");
//		fields.remove("lastLac");
//		fields.remove("lastCellTower");
//
//		Assert.assertFalse(lacCellFfilter.accept(dt));
//		LOG.info("dt0 {}", dt);
		
		fields.put("lac", "10");
		fields.put("cellTower", "1");
		fields.remove("lastLac");
		fields.remove("lastCellTower");
		fields.put("timeUTC", "10000 00 61 000".replaceAll(" ", ""));
		
		Assert.assertTrue(lacCellFfilter.accept(dt));
		Assert.assertTrue(dataReduction.accept(dt));
		LOG.info("dt1 {}", dt);

		fields.put("lac", "12");
		fields.put("cellTower", "5");
		fields.remove("lastLac");
		fields.remove("lastCellTower");
		fields.put("timeUTC", "10000 00 62 000".replaceAll(" ", ""));
		
		Assert.assertTrue(lacCellFfilter.accept(dt));
		Assert.assertTrue(dataReduction.accept(dt));
		LOG.info("dt2 {}", dt);

		fields.put("timeUTC", "10000 00 63 000".replaceAll(" ", ""));

		Assert.assertTrue(lacCellFfilter.accept(dt));
		Assert.assertFalse(dataReduction.accept(dt));
		
		fields.put("lac", "99");
		fields.put("cellTower", "99");
		fields.remove("lastLac");
		fields.remove("lastCellTower");
		fields.put("timeUTC", "10000 00 64 000".replaceAll(" ", ""));

		Assert.assertTrue(lacCellFfilter.accept(dt));
		Assert.assertTrue(dataReduction.accept(dt));
		LOG.info("dt3 {}", dt);

		fields.put("lac", "999");
		fields.put("cellTower", "999");
		fields.remove("lastLac");
		fields.remove("lastCellTower");
		fields.put("timeUTC", "10000 00 65 000".replaceAll(" ", ""));
		Assert.assertFalse(lacCellFfilter.accept(dt));
		
		fields.put("lac", "10");
		fields.put("cellTower", "1");
		fields.remove("lastLac");
		fields.remove("lastCellTower");
		fields.put("timeUTC", "10000 00 66 000".replaceAll(" ", ""));
		
		Assert.assertTrue(lacCellFfilter.accept(dt));
		Assert.assertTrue(dataReduction.accept(dt));
		LOG.info("dt4 {}", dt);
	}
}
