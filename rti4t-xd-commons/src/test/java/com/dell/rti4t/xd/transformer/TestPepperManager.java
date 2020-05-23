package com.dell.rti4t.xd.transformer;

import static com.dell.rti4t.xd.testutil.EventTestBuilder.generateEvent;
import static com.dell.rti4t.xd.testutil.EventTestBuilder.generateIntInRange;
import static com.dell.rti4t.xd.testutil.EventTestBuilder.generateString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.google.common.collect.Sets;

public class TestPepperManager {
	
	static private final Logger LOG = LoggerFactory.getLogger(TestPepperManager.class);
	
	@Test
	public void canGetNextDateAndDelta() {
		String expression = "30 * * * * *";
		CronTrigger cronTrigger = new CronTrigger(expression);
		Date nextExecutionTime = cronTrigger.nextExecutionTime(new SimpleTriggerContext());
		Date now = new Date();
		LOG.debug("Date is {}, next execution time is {}", 
					now.getTime(), 
					nextExecutionTime.getTime());
		Date nextNextExecutionTime = cronTrigger.nextExecutionTime(new SimpleTriggerContext(nextExecutionTime, nextExecutionTime, nextExecutionTime));
		LOG.info("Date is {}\nnext next execution time is {}\ndiff is {}", 
				now.getTime(), 
				nextNextExecutionTime.getTime(),
				nextNextExecutionTime.getTime() - nextExecutionTime.getTime());
	}
	
	@Test
	public void canSaltChangeEncryption() throws Exception {
		PepperManagerImpl pepperManager = new PepperManagerImpl();
		pepperManager.afterPropertiesSet();
		
		Set<String> peppers = Sets.newHashSet();
		int maxIndex = generateIntInRange(100, 2000);
		for(int index = 0; index < maxIndex; index++) {
			peppers.add(pepperManager.getSaltForTime(generateEvent()));
		}
		assertEquals(1, peppers.size());
		
		pepperManager.setPepper(generateString());
		
		maxIndex = generateIntInRange(100, 2000);
		for(int index = 0; index < maxIndex; index++) {
			peppers.add(pepperManager.getSaltForTime(generateEvent()));
		}
		assertEquals(2, peppers.size());
	}
	
	@Test
	public void testInterval() throws Exception {
		PepperManagerImpl pepperManager = new PepperManagerImpl();
		pepperManager.setCronTrigger("*/5 * * * * *");
		pepperManager.afterPropertiesSet();
		
		Map<String, Object> valuesBefore = new HashMap<String, Object>();
		DataTransporter dtBefore = new DataTransporter(valuesBefore);
		valuesBefore.put("timeUTC", "0");
		String salt1 = pepperManager.getSaltForTime(dtBefore);
		
		Map<String, Object> valuesAfter = new HashMap<String, Object>();
		DataTransporter dtAfter = new DataTransporter(valuesAfter);
		valuesAfter.put("timeUTC", String.valueOf(Long.MAX_VALUE));
		String salt2 = pepperManager.getSaltForTime(dtAfter);
		
		assertNotEquals(salt1,  salt2);

		Thread.sleep(10000);
		
		String newSalt1 = pepperManager.getSaltForTime(dtBefore);
		assertEquals(newSalt1,  salt2);

		String newSalt2 = pepperManager.getSaltForTime(dtAfter);
		assertNotEquals(newSalt2,  salt2);
	}
}
