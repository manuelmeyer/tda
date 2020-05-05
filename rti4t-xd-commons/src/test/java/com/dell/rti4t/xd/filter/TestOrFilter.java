package com.dell.rti4t.xd.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.dell.rti4t.xd.domain.DataTransporter;

public class TestOrFilter {
	
	@Test
	public void canFilterWithOr() throws Exception {
		DataTransporter dt = new DataTransporter();
		Map<String, Object> fields = new HashMap<String, Object>();
		fields.put("imsi", "11111111");
		dt.setFields(fields);
		
		OrEventFilter orEventFilter = new OrEventFilter();
		assertFalse(orEventFilter.accept(dt));
		
		List<EventFilter> filters = new ArrayList<EventFilter>();
		InOrOutPatternBasedFilterImpl filter1 = new InOrOutPatternBasedFilterImpl();
		filter1.setFilterField("imsi");
		filter1.setInMode(true);
		filter1.setDiscriminationMode(InOrOutPatternBasedFilterImpl.DiscriminationMode.HAS_MINIMUM_LENGTH);
		filter1.setMinimumLength(12);
		filter1.afterPropertiesSet();
		assertFalse(filter1.accept(dt));
		
		filters.add(filter1);
		orEventFilter.setFilters(filters);
		assertFalse(orEventFilter.accept(dt));
		
		InOrOutPatternBasedFilterImpl filter2 = new InOrOutPatternBasedFilterImpl();
		filter2.setFilterField("imsi");
		filter2.setInMode(true);
		filter2.setDiscriminationMode(InOrOutPatternBasedFilterImpl.DiscriminationMode.CONTAINS);
		filter2.setPattern("111");
		filter2.afterPropertiesSet();
		assertTrue(filter2.accept(dt));

		filters.add(filter2);
		assertTrue(orEventFilter.accept(dt));
	}
}
