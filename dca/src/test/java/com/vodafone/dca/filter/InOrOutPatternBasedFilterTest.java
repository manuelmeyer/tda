package com.vodafone.dca.filter;

import static com.vodafone.dca.filter.InOrOutPatternBasedFilter.DiscriminationMode.CONTAINS;
import static com.vodafone.dca.filter.InOrOutPatternBasedFilter.DiscriminationMode.ENDS_WITH;
import static com.vodafone.dca.filter.InOrOutPatternBasedFilter.DiscriminationMode.HAS_MINIMUM_LENGTH;
import static com.vodafone.dca.filter.InOrOutPatternBasedFilter.DiscriminationMode.NONE;
import static com.vodafone.dca.filter.InOrOutPatternBasedFilter.DiscriminationMode.STARTS_WITH;
import static com.vodafone.dca.utils.RandomBasedTestObjectGenerator.generateImsiEvent;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;
import com.vodafone.dca.domain.DataTransporter;
import com.vodafone.dca.filter.InOrOutPatternBasedFilter.DiscriminationMode;

public class InOrOutPatternBasedFilterTest {
	
	@Test(expected=RuntimeException.class)
	public void throwExceptionWhenMissingBaseProperties() throws Exception {
		InOrOutPatternBasedFilter filter = new InOrOutPatternBasedFilter();
		filter.initFilter();
	}
	
	@Test(expected=RuntimeException.class)
	public void throwExceptionWhenMissingFilterProperties() throws Exception {
		InOrOutPatternBasedFilter filter = new InOrOutPatternBasedFilter();
		filter.setInMode(true);
		filter.setFilterField("a name");
		filter.setDiscriminationMode(ENDS_WITH);
		filter.initFilter();
	}

	@Test
	public void canFilterOnNoneFilter() throws Exception {
		InOrOutPatternBasedFilter filter = new InOrOutPatternBasedFilter();
		filter.setInMode(true);
		filter.setFilterField("name");
		filter.setDiscriminationMode(NONE);
		filter.initFilter();
		
		DataTransporter dt = new DataTransporter();
		dt.setFields(new HashMap<String, Object>());
		dt.putFieldValue("name", "hello");
		assertTrue(filter.accept(dt));
	}
	
	@Test
	public void canFilterOnMinimumLengthFilter() throws Exception {
		InOrOutPatternBasedFilter filter = createInFilterOfMode(HAS_MINIMUM_LENGTH);
		
		DataTransporter dt = new DataTransporter();
		dt.setFields(new HashMap<String, Object>());

		dt.putFieldValue("name", "123456");
		assertTrue(filter.accept(dt));

		dt.putFieldValue("name", "1234");
		assertFalse(filter.accept(dt));
		
		filter.setInMode(false);

		dt.putFieldValue("name", "123456");
		assertFalse(filter.accept(dt));

		dt.putFieldValue("name", "1234");
		assertTrue(filter.accept(dt));
	}
	
	@Test
	public void canFilterOnStartsWithFilter() throws Exception {
		InOrOutPatternBasedFilter filter = createInFilterOfMode(STARTS_WITH);
		
		DataTransporter dt = new DataTransporter();
		dt.setFields(new HashMap<String, Object>());

		dt.putFieldValue("name", "hello, how are you ?");
		assertTrue(filter.accept(dt));
		
		dt.putFieldValue("name", "bonjour and hello, how are you ?");
		assertFalse(filter.accept(dt));

		dt.putFieldValue("name", "I am fine !");
		assertFalse(filter.accept(dt));
		
		filter.setInMode(false);
		
		dt.putFieldValue("name", "hello, how are you ?");
		assertFalse(filter.accept(dt));

		dt.putFieldValue("name", "bonjour and hello, how are you ?");
		assertTrue(filter.accept(dt));
		
		dt.putFieldValue("name", "I am still fine !");
		assertTrue(filter.accept(dt));
	}	
	
	@Test
	public void canFilterOnContainsFilter() throws Exception {
		InOrOutPatternBasedFilter filter = createInFilterOfMode(CONTAINS);
		
		DataTransporter dt = new DataTransporter();
		dt.setFields(new HashMap<String, Object>());

		dt.putFieldValue("name", "bonjour and hello, how are you ?");
		assertTrue(filter.accept(dt));
		
		dt.putFieldValue("name", "I am fine !");
		assertFalse(filter.accept(dt));
		
		filter.setInMode(false);
		
		dt.putFieldValue("name", "bonjour and hello, how are you ?");
		assertFalse(filter.accept(dt));
		
		dt.putFieldValue("name", "I am still fine !");
		assertTrue(filter.accept(dt));
	}
	
	@Test
	public void canFilterOnEndsWithFilter() throws Exception {
		InOrOutPatternBasedFilter filter = createInFilterOfMode(ENDS_WITH);
		
		DataTransporter dt = new DataTransporter();
		dt.setFields(new HashMap<String, Object>());

		dt.putFieldValue("name", "bonjour and hello, how are you ?");
		assertFalse(filter.accept(dt));

		dt.putFieldValue("name", "bonjour, how are you ?hello");
		assertTrue(filter.accept(dt));
		
		dt.putFieldValue("name", "I am fine !");
		assertFalse(filter.accept(dt));
		
		filter.setInMode(false);
		
		dt.putFieldValue("name", "bonjour, how are you ?hello");
		assertFalse(filter.accept(dt));
		
		dt.putFieldValue("name", "I am still fine !");
		assertTrue(filter.accept(dt));
	}
	
	@Test
	public void canExcludeStartsWith() {
		List<String> patterns = Lists.newArrayList("23425", "23427", "23415", "00000");
		InOrOutPatternBasedFilter underTest = new InOrOutPatternBasedFilter();
		underTest.setInMode(false);
		underTest.setDiscriminationMode(STARTS_WITH);
		underTest.setPatterns(patterns);
		
		List<String> imsis = Lists.newArrayList("302720601834081", "310410472280147",
											"310410608266621", "310410669256392",
											"310410781848323", "334020447865277",
											"404662200191679", "425030025363587",
											"427021801022022", "505029490141855",
											"530011104835757", "722310215986749");

		imsis.stream().forEach(imsi -> {
			DataTransporter input = generateImsiEvent(imsi);
			assertTrue(underTest.accept(input));
		});
	}

	private InOrOutPatternBasedFilter createInFilterOfMode(DiscriminationMode mode) throws Exception {
		InOrOutPatternBasedFilter filter = new InOrOutPatternBasedFilter();
		filter.setInMode(true);
		filter.setFilterField("name");
		filter.setDiscriminationMode(mode);
		filter.setPatterns(Lists.newArrayList("hello"));
		filter.initFilter();
		return filter;
	}
}
