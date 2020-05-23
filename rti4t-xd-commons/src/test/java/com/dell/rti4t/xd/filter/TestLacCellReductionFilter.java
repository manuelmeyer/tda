package com.dell.rti4t.xd.filter;

import static com.dell.rti4t.xd.testutil.DataTransporterAssert.assertDtEquals;
import static com.dell.rti4t.xd.testutil.EventTestBuilder.buildEvent;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dell.rti4t.xd.common.ImsiHistory;
import com.dell.rti4t.xd.common.ReductionMapHandler;
import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.filter.DataReductionImpl.ReductionMode;

public class TestLacCellReductionFilter {
	
	static private final Logger LOG = LoggerFactory.getLogger(TestLacCellReductionFilter.class);
	
	@Test
	public void canDedupEventsAndResetOnEnterGF() throws Exception {
		/*
		 lac, cell in
			56,12670
			56,13121
			56,13127
			56,14399
		 lac, cell out
		    9999,*
		 */
		
		LacCellReductionFilterImpl underTest = createFilter("lac-cells-100p-20200521.csv");
		
		DataTransporter dt;
		dt = buildEvent(10000, 56, 12670);
		
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 10000, 56, 12670);

		dt = buildEvent(10001, 56, 13121);
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 10001, 56, 13121, 10000, 56, 12670);
		
		for(int index = 2; index < 20; index += 2) {
			dt = buildEvent(10000 + index, 56, 13121);
			assertFalse(underTest.accept(dt));
			dt = buildEvent(10000 + index + 1, 56, 12670);
			assertFalse(underTest.accept(dt));
		}
		
		dt = buildEvent(10030, 56, 14399);
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 10030, 56, 14399, 10019, 56, 12670);
		
		dt = buildEvent(10031, 9999, 0);
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 10031, 9999, 0, 10030, 56, 14399);
		
		dt = buildEvent(10040, 56, 14399);
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 10040, 56, 14399);
		
		dt = buildEvent(10041, 9999, 0);
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 10041, 9999, 0, 10040, 56, 14399);
		
		dt = buildEvent(10050, 56, 14399);
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 10050, 56, 14399);
	}
	
	@Test
	public void canFilterAndFollowExitProdIssueDupEnteringCells() throws Exception {
		LacCellReductionFilterImpl underTest = createFilter("lac-cells-100p-20200521.csv");
		
		DataTransporter dt;
		boolean check;
		
		dt = buildEvent(1590224216, 24606, 1500190);
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 1590224216, 24606, 1500190);
		LOG.info("dt {}", dt);
		ImsiHistory imsiHistory = ReductionMapHandler.getImsiHistory("123456789012345");
		LOG.info("imsiHistory {}", imsiHistory);
		
		dt = buildEvent(1590224218, 24606, 812574);
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 1590224218, 24606, 812574, 1590224216, 24606, 1500190);
		
		dt = buildEvent(1590224219, 24606, 1500190);
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 1590224219, 24606, 1500190);

		dt = buildEvent(1590224219, 24606, 812574);
		check = underTest.accept(dt);
		LOG.info("Check1 {} dt {}", check, dt);

		dt = buildEvent(1590224222, 24606, 1500190);
		check = underTest.accept(dt);
		LOG.info("Check3 {} dt {}", check, dt);
	}
	
	@Test
	public void canFilterAndFollowExitProdIssueEnteringCellNotInLacCell() throws Exception {
		LacCellReductionFilterImpl underTest = createFilter("lac-cells-100p-20200521.csv");
		
		DataTransporter dt;
		dt = buildEvent(1590067404, 24610, 1339412); // enter
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 1590067404, 24610, 1339412);
		
		dt = buildEvent(1590069280, 24610, 1339412); // same
		assertFalse(underTest.accept(dt));
		dt = buildEvent(1590069281, 24610, 1339412); // same	
		assertFalse(underTest.accept(dt));
		dt = buildEvent(1590069282, 24610, 1339412); // same	
		assertFalse(underTest.accept(dt));
		
		dt = buildEvent(1590069284, 9999, 9999); // exit point
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 1590069284, 9999, 9999, 1590069282, 24610, 1339412);

		dt = buildEvent(1590069282, 24610, 1339412); // event in the past
		assertFalse(underTest.accept(dt));

		dt = buildEvent(1590069299, 24611, 834324); // out of GF
		assertFalse(underTest.accept(dt));
		
		dt = buildEvent(1591069299, 24610, 1339412); // enter
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 1591069299, 24610, 1339412);

		dt = buildEvent(1591079299, 9999, 0); // exit point
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 1591079299, 9999, 0, 1591069299, 24610, 1339412);
	}

	@Test
	public void canFilterAndFollowExit() throws Exception {
		LacCellReductionFilterImpl underTest = createFilter("lac-and-cells.txt");
		
		DataTransporter dt;
		
		dt = buildEvent(60, 10, 1);	// enter
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 60, 10, 1);

		dt = buildEvent(62, 12, 5);	// move inside GF	
		assertTrue(underTest.accept(dt));		
		assertDtEquals(dt, 62, 12, 5, 60, 10, 1);

		dt = buildEvent(63, 12, 5); // same
		assertFalse(underTest.accept(dt));

		dt = buildEvent(64, 12, 5); // same
		assertFalse(underTest.accept(dt));

		dt = buildEvent(65, 12, 5); // same
		assertFalse(underTest.accept(dt));
		
		dt = buildEvent(66, 12, 5); // same
		assertFalse(underTest.accept(dt));
		
		dt = buildEvent(67, 10, 1);	// move inside GF
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 67, 10, 1, 66, 12, 5);

		dt = buildEvent(84, 99, 99);
		assertTrue(underTest.accept(dt)); // exit point
		assertDtEquals(dt, 84, 99, 99, 67, 10, 1);

		dt = buildEvent(90, 999, 999); // out of GF
		assertFalse(underTest.accept(dt));
		
		dt = buildEvent(95, 10, 1);	// enter
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 95, 10, 1);
		
		dt = buildEvent(100, 999, 999);
		assertTrue(underTest.accept(dt)); // exit point
		assertDtEquals(dt, 100, 999, 999, 95, 10, 1);
		
		dt = buildEvent(110, 11, 11); // enter
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 110, 11, 11);
		
		dt = buildEvent(120, 100, 0); // move inside GF
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 120, 100, 0, 110, 11, 11);

		dt = buildEvent(130, 999, 999); // exit point
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 130, 999, 999, 120, 100, 0);
		
		dt = buildEvent(140, 999, 999); // out of GF
		assertFalse(underTest.accept(dt));
	}
	
	private LacCellReductionFilterImpl createFilter(String lacCellFile) throws Exception {
		DataReductionImpl dataReduction = new DataReductionImpl();
		dataReduction.setReductionMode(ReductionMode.IMSIS_CHANGE_CELL_ONLY);
		dataReduction.afterPropertiesSet();

		LacCellFilterImpl lacCellFilter = new LacCellFilterImpl();
		lacCellFilter.setLacCellFilePath(lacCellFile);
		lacCellFilter.setFollowExit(true);
		lacCellFilter.afterPropertiesSet();
		
		LacCellReductionFilterImpl underTest = new LacCellReductionFilterImpl();
		underTest.setDataReductionFilter(dataReduction);
		underTest.setLacCellFilter(lacCellFilter);
		return underTest;
	}
}
