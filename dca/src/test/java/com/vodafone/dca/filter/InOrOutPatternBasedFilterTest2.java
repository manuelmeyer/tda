package com.vodafone.dca.filter;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.vodafone.dca.domain.DataTransporter;
import com.vodafone.dca.filter.InOrOutPatternBasedFilter.DiscriminationMode;
import com.vodafone.dca.utils.RandomBasedTestObjectGenerator;

public class InOrOutPatternBasedFilterTest2 {
	
	@Test
	public void canExcludeStartsWith() {
		List<String> patterns = Lists.newArrayList("23425", "23427", "23415", "00000");
		InOrOutPatternBasedFilter underTest = new InOrOutPatternBasedFilter();
		underTest.setInMode(false);
		underTest.setDiscriminationMode(DiscriminationMode.STARTS_WITH);
		underTest.setPatterns(patterns);
		
		List<String> imsis = Lists.newArrayList(
			"302720601834081",
			"310410472280147",
			"310410608266621",
			"310410669256392",
			"310410781848323",
			"334020447865277",
			"404662200191679",
			"425030025363587",
			"427021801022022",
			"505029490141855",
			"530011104835757",
			"722310215986749");

		imsis.stream().forEach(imsi -> {
			DataTransporter input = RandomBasedTestObjectGenerator.generateImsiEvent(imsi);
			Assert.assertFalse(underTest.accept(input));
		});
	}
}
