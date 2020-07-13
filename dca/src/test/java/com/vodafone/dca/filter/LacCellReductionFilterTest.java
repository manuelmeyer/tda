package com.vodafone.dca.filter;

import static com.vodafone.dca.utils.DataTransporterAssert.assertDtEquals;
import static com.vodafone.dca.utils.RandomBasedTestObjectGenerator.buildEvent;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vodafone.dca.domain.DataTransporter;
import com.vodafone.dca.filter.DataReductionFilter.ReductionMode;
import com.vodafone.dca.reduction.ReductionMapHandler;

public class LacCellReductionFilterTest {
	
	static private final Logger LOG = LoggerFactory.getLogger(LacCellReductionFilterTest.class);
	
	ReductionMapHandler reductionMapHandler;
	
	@Before
	public void setSimpleRedact() {
		//ReductionMapHandler.useSimpleDedup();
	}
	
	@Test
	public void canFilterAndFollowExitWithStrongRedact() throws Exception {
		LacCellReductionFilter underTest = createFilter("test-refdata/lac-cells-100p-20200521.csv");
		reductionMapHandler.useStrongDedup();

		DataTransporter dt;
		dt = buildEvent(1590067404, 24610, 1339412); // enter
		assertTrue(underTest.accept(dt));
		LOG.info("dt {}", dt);
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
	public void canFilterAndFollowExitProdIssueDupEnteringCells() throws Exception {
		LacCellReductionFilter underTest = createFilter("test-refdata/lac-cells-100p-20200521.csv");
		
		LOG.info(" -- Reduction delay is {}", reductionMapHandler.delayBeforeDuplicate);
		
		DataTransporter dt;
		boolean check;
		
		dt = buildEvent(1590224216, 24606, 1500190);
		assertTrue(underTest.accept(dt));
		assertDtEquals(dt, 1590224216, 24606, 1500190);
		LOG.info("dt {}", dt);
		
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
		LacCellReductionFilter underTest = createFilter("test-refdata/lac-cells-100p-20200521.csv");
		
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
		LacCellReductionFilter underTest = createFilter("test-refdata/lac-and-cells.txt");
		
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
	
	private LacCellReductionFilter createFilter(String lacCellFile) throws Exception {
		reductionMapHandler = new ReductionMapHandler();
		reductionMapHandler.buildMap();
		
		DataReductionFilter dataReduction = new DataReductionFilter();
		dataReduction.setReductionMode(ReductionMode.IMSIS_CHANGE_CELL_ONLY);
		dataReduction.setReductionMapHandler(reductionMapHandler);
		dataReduction.initReductionFilter();

		LacCellFilter lacCellFilter = new LacCellFilter();
		lacCellFilter.setLacCellFilePath(lacCellFile);
		lacCellFilter.setFollowExit(true);
		lacCellFilter.setReductionMapHandler(reductionMapHandler);
		lacCellFilter.firstLoadLacAndCells();
		
		LacCellReductionFilter underTest = new LacCellReductionFilter();
		underTest.setDataReductionFilter(dataReduction);
		underTest.setLacCellFilter(lacCellFilter);
		underTest.buildLocks();
		return underTest;
	}
}
