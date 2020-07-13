package com.vodafone.dca.domain;

import static com.vodafone.dca.utils.RandomBasedTestObjectGenerator.generateIntInRange;
import static com.vodafone.dca.utils.RandomBasedTestObjectGenerator.generateRandomMap;
import static com.vodafone.dca.utils.RandomBasedTestObjectGenerator.generateString;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataTransporterTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(DataTransporterTest.class);
	
	@Test
	public void canSetDataSameThread() throws Exception {
		
		int totalThreads = generateIntInRange(32, 64);
		CountDownLatch synchroStart = new CountDownLatch(totalThreads);
		CountDownLatch synchroEnd = new CountDownLatch(totalThreads);
		
		DataTransporter underTest = new DataTransporter();
		Map<String, Object> initialMap = generateRandomMap();
		underTest.setFields(initialMap);

		String mainThreadId = String.valueOf(currentThread().getId());
		underTest.putFieldValue("threadId", mainThreadId);
		Map<String, Object> mainMerged = underTest.mergeFields();
		
		
		for(int index = 0; index < totalThreads; index++) {
			new Thread(() -> {
				try {
					synchroStart.countDown();
					synchroStart.await();
					changeLocally(underTest);
				} catch(Exception e) {
					LOG.error("Exception in thread test...", e);
				} finally {
					synchroEnd.countDown();
				}
			}).start();
		}
		
		synchroEnd.await();
		
		assertEquals(mainThreadId, underTest.getFieldValue("threadId"));
		assertTrue(underTest.getFieldValue("toBeChanged") == null);
		assertEquals(mainMerged, underTest.mergeFields());
	}

	private void changeLocally(DataTransporter underTest) throws InterruptedException {
		String threadId = String.valueOf(currentThread().getId());
		underTest.putFieldValue("threadId", threadId);
		
		for(int changes = 0; changes < generateIntInRange(4, 33); changes++) {
			String toBeChanged = generateString();
			underTest.putFieldValue("toBeChanged", toBeChanged);
			
			Map<String, Object> merged = underTest.mergeFields();
			sleep(generateIntInRange(10, 20));

			assertEquals(threadId, underTest.getFieldValue("threadId"));
			assertEquals(toBeChanged, underTest.getFieldValue("toBeChanged"));
			assertEquals(merged, underTest.mergeFields());
		}
	}
}