package com.dell.rti4t.xd.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Test;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.filter.InOrOutPatternBasedFilterImpl.DiscriminationMode;

public class TestInOrOutPatternBasedFilter {
	
	@Test(expected=RuntimeException.class)
	public void throwExceptionWhenMissingBaseProperties() throws Exception {
		InOrOutPatternBasedFilterImpl filter = new InOrOutPatternBasedFilterImpl();
		filter.afterPropertiesSet();
	}
	
	@Test(expected=RuntimeException.class)
	public void throwExceptionWhenMissingFilterProperties() throws Exception {
		InOrOutPatternBasedFilterImpl filter = new InOrOutPatternBasedFilterImpl();
		filter.setInMode(true);
		filter.setFilterField("a name");
		filter.setDiscriminationMode(DiscriminationMode.ENDS_WITH);
		filter.afterPropertiesSet();
	}

	@Test
	public void canFilterOnNoneFilter() throws Exception {
		InOrOutPatternBasedFilterImpl filter = new InOrOutPatternBasedFilterImpl();
		filter.setInMode(true);
		filter.setFilterField("name");
		filter.setDiscriminationMode(DiscriminationMode.NONE);
		filter.afterPropertiesSet();
		
		DataTransporter dt = new DataTransporter();
		dt.setFields(new HashMap<String, Object>());
		dt.putFieldValue("name", "hello");
		assertTrue(filter.accept(dt));
	}
	
	@Test
	public void canFilterOnMinimumLengthFilter() throws Exception {
		InOrOutPatternBasedFilterImpl filter = createInFilterOfMode(DiscriminationMode.HAS_MINIMUM_LENGTH);
		
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
		InOrOutPatternBasedFilterImpl filter = createInFilterOfMode(DiscriminationMode.STARTS_WITH);
		
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
		InOrOutPatternBasedFilterImpl filter = createInFilterOfMode(DiscriminationMode.CONTAINS);
		
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
		InOrOutPatternBasedFilterImpl filter = createInFilterOfMode(DiscriminationMode.ENDS_WITH);
		
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


	private InOrOutPatternBasedFilterImpl createInFilterOfMode(DiscriminationMode mode) throws Exception {
		InOrOutPatternBasedFilterImpl filter = new InOrOutPatternBasedFilterImpl();
		filter.setInMode(true);
		filter.setFilterField("name");
		filter.setDiscriminationMode(mode);
		filter.setPattern("hello");
		filter.afterPropertiesSet();
		return filter;
	}
}
