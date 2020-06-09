package com.dell.rti4t.xd.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.testutil.EventTestBuilder;

public class TestLacAndCellFilter {
	
	static private final Logger LOG = LoggerFactory.getLogger(TestLacAndCellFilter.class);
	
	@Test
	public void canFilterOnLacAndCellsWhenNoFileSet() {
		LacCellFilterImpl filter = new LacCellFilterImpl();
		assertTrue(filter.accept(EventTestBuilder.buildEvent(10, 10, 1000000)));
	}
	
	@Test
	public void canFilterOnInvalidValues() {
		LacCellFilterImpl filter = new LacCellFilterImpl();
		assertFalse(filter.accept(EventTestBuilder.buildEvent(0, 0, 0)));
		assertFalse(filter.accept(EventTestBuilder.buildEvent(0, 65535, 65535)));
	}

	
	@Test
	@Ignore // more a visual test to check sets are correctly built
	public void canLoadFileWithMultipleLinesSameLac() throws Exception {
		LacCellFilterImpl filter = new LacCellFilterImpl();
		filter.setLacCellFilePath("lac-cells-100p-20200521.csv");
		filter.afterPropertiesSet();
		
	}
	
	@Test
	public void canFilterOnLacAndCells() throws Exception {
		LacCellFilterImpl filter = new LacCellFilterImpl();
		filter.setLacCellFilePath("lac-and-cells.txt");
		filter.afterPropertiesSet();
		
		Map<String, Set<String>> lacCellsStore = filter.accessLacCellsStore();
		assertNotNull(lacCellsStore);
		assertEquals(4, lacCellsStore.size());
		
		assertNull(lacCellsStore.get("10000"));
		
		Set<String> lac10 = lacCellsStore.get("10");
		assertNotNull(lac10);
		assertTrue(lac10.contains("1"));
		assertTrue(lac10.contains("2"));
		assertTrue(lac10.contains("3"));
		assertTrue(lac10.contains("4"));
		assertFalse(lac10.contains("9999"));

		Set<String> lac11 = lacCellsStore.get("11");
		assertNotNull(lac11);
		assertTrue(lac11.isEmpty());
		
		Set<String> lac12 = lacCellsStore.get("12");
		assertNotNull(lac12);
		assertTrue(lac12.contains("5"));
		assertTrue(lac12.contains("6"));
		assertTrue(lac12.contains("7"));
		assertFalse(lac12.contains("9999"));
		
		Map<String, Object> fields = new HashMap<String, Object>();
		DataTransporter dt = new DataTransporter(fields, "unused");
		
		fields.put("lac", "10");
		fields.put("cellTower", "1");
		assertTrue(filter.accept(dt));

		fields.put("lac", "10");
		fields.put("cellTower", "100");
		assertFalse(filter.accept(dt));

		fields.put("lac", "11"); // 11,* accept all cells
		fields.put("cellTower", String.valueOf((int)(Math.random() * 10000)));
		assertTrue(filter.accept(dt));
	}	
	
	@Test(expected = FileNotFoundException.class)
	public void throwAnExceptionOnBadFile() throws Exception {
		
		LacCellFilterImpl filter = new LacCellFilterImpl();
		filter.setLacCellFilePath("no-way-I-exists!");
		filter.afterPropertiesSet();
	}
}
