package com.dell.rti4t.xd.transformer;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.dell.rti4t.xd.domain.DataTransporter;

public class TestObjectListToDT {
	@Test
	public void testCanHandleFilter() {
		ObjectListToDataTransporterImpl oltodt = new ObjectListToDataTransporterImpl();
		oltodt.setFieldNames(new String[] {"f1","f2","f3"});

		List<Object> inList = new ArrayList<Object>();
		inList.add("v1");
		inList.add("v2");
		inList.add("v3");
		
		DataTransporter dt = oltodt.buildFromObjectList(inList);
		Assert.assertEquals(oltodt.getDefaultFilterValue(), dt.filter());
		
		oltodt.setFilterField("not-in");
		dt = oltodt.buildFromObjectList(inList);
		Assert.assertEquals(oltodt.getDefaultFilterValue(), dt.filter());

		oltodt.setFilterField("f2");
		dt = oltodt.buildFromObjectList(inList);
		Assert.assertEquals("v2", dt.filter());

	}
}
