package com.dell.rti4t.xd.filter;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.dell.rti4t.xd.domain.DataTransporter;

public class TestLacAndCellFilter {
	
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
	}

}
