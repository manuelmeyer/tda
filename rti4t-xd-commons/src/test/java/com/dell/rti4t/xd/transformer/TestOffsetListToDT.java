package com.dell.rti4t.xd.transformer;

import static org.junit.Assert.assertEquals;

import java.lang.ref.WeakReference;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dell.rti4t.xd.csv.CSVToOffsetParser.Offset;
import com.dell.rti4t.xd.domain.DataTransporter;
import com.google.common.collect.Lists;

public class TestOffsetListToDT {
	
	private static final Logger LOG = LoggerFactory.getLogger(TestOffsetListToDT.class);
	
	@Test
	public void testCanHandleFilter() {
		OffsetListToDataTransporterImpl oltodt = new OffsetListToDataTransporterImpl();
		oltodt.setFieldNames(new String[] {"f1","f2","f3"});

		String toParse = "v1,v2,v3";
		
		WeakReference<byte[]> weakToParseReference = new WeakReference<>(toParse.getBytes());
		List<Offset> offsets = Lists.newArrayList();
		offsets.add(new Offset(weakToParseReference, 0, 2));
		offsets.add(new Offset(weakToParseReference, 3, 5));
		offsets.add(new Offset(weakToParseReference, 6, 8));
		
		DataTransporter dt = oltodt.buildFromList(offsets);
		LOG.info("dt1 {}", dt);
		assertEquals(oltodt.getDefaultFilterValue(), dt.filter());
		
		oltodt.setFilterField("not-in");
		dt = oltodt.buildFromList(offsets);
		assertEquals(oltodt.getDefaultFilterValue(), dt.filter());

		oltodt.setFilterField("f2");
		dt = oltodt.buildFromList(offsets);
		assertEquals("v2", dt.filter());
	}
}
