package com.dell.rti4t.xd.filter;

import static com.dell.rti4t.xd.testutil.EventTestBuilder.buildEvent;
import static java.lang.String.format;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestInOrOutListBasedFilter {
	
	static private final Logger LOG = LoggerFactory.getLogger(TestInOrOutListBasedFilter.class);

	@Test 
	public void canSeeFileChanges() throws Exception {
		String inFile = "/tmp/change-file.csv";
		writeNLinesForFilteringWithPrefix(inFile, 10, "2341511");
		
		InOrOutListBasedFilterImpl underTest = createFilterBasedOnList(inFile, true, 1);

		for(int index = 0; index < 10; index++) {
			assertTrue(underTest.accept(buildEvent(format("2341511%08d", index))));
		}
		
		Thread.sleep(1000); // switch to the next second before setting last modified date on the file.
		writeNLinesForFilteringWithPrefix(inFile, 10, "1141511");
		Thread.sleep(2000);
		
		for(int index = 0; index < 10; index++) {
			assertFalse(underTest.accept(buildEvent(format("2341511%08d", index))));
		}
		
		Files.delete(Paths.get(inFile));
	}

	int linesPerFile = 1_000_000;
	
	@Test 
	public void canLoadBigFile() throws Exception {
		String inFile = "/tmp/big-file.csv";

		writeNLinesForFilteringWithPrefix(inFile, linesPerFile, "2341511");
		
		InOrOutListBasedFilterImpl underTest = createFilterBasedOnList(inFile, true, 10000);
		
		for(int index = 0; index < linesPerFile - 20; index++) {
			assertTrue(underTest.accept(buildEvent(format("2341511%08d", index))));
		}
		
		for(int index = 0; index < linesPerFile; index++) {
			assertFalse(underTest.accept(buildEvent(format("2341561%08d", index))));
		}
		
		for(int index = 0; index < linesPerFile; index++) {
			assertFalse(underTest.accept(buildEvent(format("4341711%08d", index))));
		}

		Files.delete(Paths.get("/tmp/big-file.csv"));
	}
	
	@Test
	public void canFilterDT() throws Exception {
		InOrOutListBasedFilterImpl underTest = createFilterBasedOnList("imsi.txt", false, 10000);
		
		assertFalse(underTest.accept(buildEvent(null))); // no field, no acceptance
		
		underTest.setInMode(false); // in false -> opt out
		assertFalse(underTest.accept(buildEvent("1111111111111"))); 

		underTest.setInMode(true); // opt in mode
		assertTrue(underTest.accept(buildEvent("1111111111111")));
		
		assertFalse(underTest.accept(buildEvent("4111111111110"))); 
		
		underTest.setInMode(false); // in false -> opt out
		assertTrue(underTest.accept(buildEvent("9911111111111")));

		underTest.setInMode(true); // opt in mode
		assertFalse(underTest.accept(buildEvent("9911111111111"))); 
	}
	
	private void writeNLinesForFilteringWithPrefix(String fileName, int total, String prefix) throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
		for(int index = 0; index < total; index++) {
				String added = String.format("%s%08d\n", prefix, index);
				writer.write(added);
		}
		writer.flush();
		writer.close();
		Long modifiedDate = new Date().getTime();
		LOG.info("Setting last modified date to {}", modifiedDate);
		new File(fileName).setLastModified(modifiedDate);		
	}

	private InOrOutListBasedFilterImpl createFilterBasedOnList(String inFile, boolean inMode, int refreshFrequency) throws Exception {
		InOrOutListBasedFilterImpl filter = new InOrOutListBasedFilterImpl();
		filter.setInMode(inMode);
		filter.setInOrOutFilePath(inFile);
		filter.setRefresh(refreshFrequency);
		filter.afterPropertiesSet();
		return filter;
	}
}
