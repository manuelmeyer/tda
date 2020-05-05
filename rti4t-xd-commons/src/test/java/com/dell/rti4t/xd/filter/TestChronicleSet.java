package com.dell.rti4t.xd.filter;

import java.util.Set;

import net.openhft.chronicle.set.ChronicleSetBuilder;

public class TestChronicleSet {
	
	//@Test just to see an exception is thrown is the set is under sized.
	public void canCreateWithlessEntries() throws Exception {
		Set<String> mySet = ChronicleSetBuilder
				.of(String.class)
				.constantKeySizeBySample("234159153836386")
				.entries(200)
				.create();
		
		System.out.println("Key size is " + "234159153836386".length());
		
		for(int index = 0; index < 300; index++) {
			try {
				
				String added = String.format("234159153836%03d", index);
				mySet.add(added);
			} catch(Exception e) {
				System.out.println("exception with index=" + index);
				throw e;
			}
		}
	}
}
