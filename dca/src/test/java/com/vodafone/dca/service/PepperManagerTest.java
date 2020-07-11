package com.vodafone.dca.service;

import static com.vodafone.dca.utils.RandomDataGenerator.generateIntInRange;
import static com.vodafone.dca.utils.RandomDataGenerator.generateString;
import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.assertj.core.util.Sets;
import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

import com.vodafone.dca.domain.DataTransporter;
import com.vodafone.dca.utils.RandomDataGenerator;

public class PepperManagerTest {
	
	@Test
	public void canChangeKeysOnTrigger() throws Exception {
		PepperManager underTest = new PepperManager();
		
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		ReflectionTestUtils.setField(underTest, "taskScheduler", taskScheduler);
		
		underTest.setCronTrigger("*/2 * * * * *");
		underTest.startUpdaterIfDynamic();

		DataTransporter input = RandomDataGenerator.generateEvent();

		Set<String> peppers = Sets.newHashSet();
		
		int testCount = 10;
		for(int index = 0; index < testCount; index++) {
			peppers.add(underTest.getSaltForTime(input));
			Thread.sleep(2100);
		}
		assertEquals(testCount, peppers.size());
	}
	
	@Test
	public void canStayTheSameOnNoDynamic() throws Exception {
		PepperManager underTest = new PepperManager();
		String pepper = generateString();
		underTest.setPepper(pepper);
		underTest.startUpdaterIfDynamic();
		
		int testCount = generateIntInRange(40, 100);
		DataTransporter input = RandomDataGenerator.generateEvent();

		for(int index = 0; index < testCount; index++) {
			assertEquals(pepper, underTest.getSaltForTime(input));
		}
	}
}
