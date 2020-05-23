package com.dell.rti4t.xd.filter;

import static com.dell.rti4t.xd.testutil.DataTransporterAssert.assertDtEquals;
import static com.dell.rti4t.xd.testutil.EventTestBuilder.buildEvent;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.filter.DataReductionImpl.ReductionMode;

public class TestLacCellReductionFilter {
	
	static private final Logger LOG = LoggerFactory.getLogger(TestLacCellReductionFilter.class);
	
	@Test
	public void canFilterAndFollowExitProdIssue() throws Exception {
		DataReductionImpl dataReduction = new DataReductionImpl();
		dataReduction.setReductionMode(ReductionMode.IMSIS_CHANGE_CELL_ONLY);
		dataReduction.afterPropertiesSet();

		LacCellFilterImpl lacCellFilter = new LacCellFilterImpl();
		lacCellFilter.setLacCellFilePath("lac-cells-100p-20200521.csv");
		lacCellFilter.setFollowExit(true);
		lacCellFilter.afterPropertiesSet();
		
		LacCellReductionFilterImpl underTest = new LacCellReductionFilterImpl();
		underTest.setDataReductionFilter(dataReduction);
		underTest.setLacCellFilter(lacCellFilter);
		
		DataTransporter dt;
		dt = buildEvent(24610, 1339412, 1590067404); // enter
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 24610, 1339412, 1590067404);
		
		dt = buildEvent(24610, 1339412, 1590069280); // same
		assertFalse(underTest.accept(dt));
		dt = buildEvent(24610, 1339412, 1590069281); // same	
		assertFalse(underTest.accept(dt));
		dt = buildEvent(24610, 1339412, 1590069282); // same	
		assertFalse(underTest.accept(dt));
		
		dt = buildEvent(9999, 9999, 1590069284); // exit point
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 9999, 9999, 1590069284, 24610, 1339412, 1590069282);

		dt = buildEvent(24610, 1339412, 1590069282); // event in the past
		assertFalse(underTest.accept(dt));

		dt = buildEvent(24611, 834324, 1590069299); // out of GF
		assertFalse(underTest.accept(dt));
		
		dt = buildEvent(24610, 1339412, 1591069299); // enter
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 24610, 1339412, 1591069299);

		dt = buildEvent(9999, 0, 1591079299); // exit point
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 9999, 0, 1591079299, 24610, 1339412, 1591069299);
	}

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
		
		dt = buildEvent(10, 1, 60);	// enter
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 10, 1, 60);

		dt = buildEvent(12, 5, 62);	// move inside GF	
		assertTrue(underTest.accept(dt));		
		assertDtEquals(dt, 12, 5, 62, 10, 1, 60);

		dt = buildEvent(12, 5, 63); // same
		assertFalse(underTest.accept(dt));

		dt = buildEvent(12, 5, 64); // same
		assertFalse(underTest.accept(dt));

		dt = buildEvent(12, 5, 65); // same
		assertFalse(underTest.accept(dt));
		
		dt = buildEvent(12, 5, 66); // same
		assertFalse(underTest.accept(dt));
		
		dt = buildEvent(10, 1, 67);	// move inside GF
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 10, 1, 67, 12, 5, 66);

		dt = buildEvent(99, 99, 84);
		assertTrue(underTest.accept(dt)); // exit point
		assertDtEquals(dt, 99, 99, 84, 10, 1, 67);

		dt = buildEvent(999, 999, 90); // out of GF
		assertFalse(underTest.accept(dt));
		
		dt = buildEvent(10, 1, 95);	// enter
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 10, 1, 95);
		
		dt = buildEvent(999, 999, 100);
		assertTrue(underTest.accept(dt)); // exit point
		assertDtEquals(dt, 999, 999, 100, 10, 1, 95);
		
		dt = buildEvent(11, 11, 110); // enter
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 11, 11, 110);
		
		dt = buildEvent(100, 0, 120); // move inside GF
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 100, 0, 120, 11, 11, 110);

		dt = buildEvent(999, 999, 130); // exit point
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 999, 999, 130, 100, 0, 120);
		
		dt = buildEvent(999, 999, 140); // out of GF
		assertFalse(underTest.accept(dt));
	}	
}
