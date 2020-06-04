package com.dell.rti4t.xd.common;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestVersionedSet {
	
	private static final Logger LOG = LoggerFactory.getLogger(TestVersionedSet.class);
	VersionedSet<String> underTest = new VersionedSet<>();
	
	@Test
	public void canLoadAndPrune() {
		LOG.info(" ----- load an prune for 10");
		underTest.clear();
		loadAndPrune(10);
//		LOG.info(" ----- load an prune for big number");
//		for(int index = 0; index < 100; index++) {
//			LOG.info("Clear {}", index);
//			underTest.clear();
//			loadAndPrune(20_000_000);
//		}
	}
	
	void loadAndPrune(int totalSize) {
		
		underTest.incrementVersion();
		LOG.info("Load1");
		for(int index = 0; index < totalSize; index++) {
			String toAdd = String.format("%015d", index);
			underTest.add(toAdd);
		}
		LOG.info("/Load1");
		assertTrue(underTest.size() == totalSize);
		for(int index = 0; index < totalSize; index++) {
			String toCheck = String.format("%015d", index);
			assertTrue(underTest.contains(toCheck));
		}
		underTest.incrementVersion();
		LOG.info("Load2");

		for(int index = 0; index < totalSize; index +=2) {
			String toAdd = String.format("%015d", index);
			underTest.add(toAdd);
		}
		LOG.info("/Load2");
		underTest.prune();
		LOG.info("/PruneLoad2");
		assertTrue(underTest.size() == totalSize / 2);
		
		for(int index = 0; index < totalSize; index++) {
			String toCheck = String.format("%015d", index);
			assertTrue(index % 2 != 0 ^ underTest.contains(toCheck));
		}
	}
}
