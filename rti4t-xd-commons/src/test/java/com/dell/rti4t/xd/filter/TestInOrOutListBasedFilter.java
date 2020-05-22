package com.dell.rti4t.xd.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dell.rti4t.xd.domain.DataTransporter;

public class TestInOrOutListBasedFilter {
	
	static private final Logger LOG = LoggerFactory.getLogger(TestInOrOutListBasedFilter.class);

	int linesPerFile = 50; //2_061_541; //20_061_541;
	
	@Test // turn it off as the big file cannot be loaded in github
	@Ignore
	public void canLoadBigFile() throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter("/tmp/big-file.csv"));
		for(int index = 0; index < linesPerFile; index++) {
				//String added = String.format("234159153836%08d\n", index);
				String added = String.format("2341511%08d\n", index);
				writer.write(added);
		}
		writer.flush();
		writer.close();

		InOrOutListBasedFilterImpl filter = new InOrOutListBasedFilterImpl();
		filter.setInMode(true);
		filter.setInOrOutFilePath("/tmp/big-file.csv");
		filter.setRefresh(100000);
		filter.afterPropertiesSet();

		Map<String, Object> fields = new HashMap<String, Object>();
		DataTransporter dt = new DataTransporter(fields, "no-filter");
		
		long totalNano = 0;
		for(int index = 0; index < linesPerFile - 20; index++) {
			String imsi = String.format("2341511%08d", index);
			fields.put("imsi", imsi);
			long n1 = System.nanoTime();
			boolean rc = filter.accept(dt);
			totalNano += System.nanoTime() - n1;
			Assert.assertTrue(rc);
		}
		
		LOG.info("{} checked in {} ms", linesPerFile, totalNano / 1000000);
		
		for(int index = 0; index < 10; index++) {
			String imsi = String.format("2341561%08d", index);
			fields.put("imsi", imsi);
			Assert.assertFalse(filter.accept(dt));
		}
		
		for(int index = 0; index < 10; index++) {
			String imsi = String.format("4341711%08d", index);
			fields.put("imsi", imsi);
			Assert.assertFalse(filter.accept(dt));
		}

		Thread.sleep(60000);
		Files.delete(Paths.get("/tmp/big-file.csv"));
	}
	
	@Test
	public void canFilterDT() throws Exception {
		InOrOutListBasedFilterImpl filter = new InOrOutListBasedFilterImpl();
		filter.setInOrOutFilePath("imsi.txt");
		filter.setInMode(false);
		filter.afterPropertiesSet();
		
		Map<String, Object> fields = new HashMap<String, Object>();
		DataTransporter dt = new DataTransporter(fields, "no-filter"); // no imsi field, cannot accept
		
		assertFalse(filter.accept(dt)); // no field, no acceptance
		
		filter.setInMode(false); // in false -> opt out
		fields.put("imsi", "1111111111111"); // opted out imsi, should not be accepted
		assertFalse(filter.accept(dt)); 

		filter.setInMode(true); // opt in mode
		assertTrue(filter.accept(dt));
		
		fields.put("imsi", "4111111111110"); // opted in imsi, should not be accepted
		assertFalse(filter.accept(dt)); 
		
		filter.setInMode(false); // in false -> opt out
		fields.put("imsi", "9911111111111"); // opted out imsi, should be accepted
		assertTrue(filter.accept(dt));
		filter.setInMode(true); // opt in mode
		assertFalse(filter.accept(dt)); 
	}
}
