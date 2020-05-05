package com.dell.rti4t.xd.sink;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.expression.Expression;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import com.dell.rti4t.xd.sink.GenericFileNameGenerator;

public class TestFileNameGenerator {
	
	private final static Logger LOG = LoggerFactory.getLogger(TestFileNameGenerator.class);
	
	Message<?> msg = MessageBuilder
			.withPayload("12345")
			.setHeader("h-data-type", "h")
			.build();
	
	TemporaryFolder folder;

	@Before
	public void createTempFolder() throws Exception {
		folder = new TemporaryFolder();
		folder.create();
		LOG.info("Created folder {}", folder.getRoot().getAbsolutePath());
	}
	
	@After
	public void deleteTempFolder() throws Exception {
		LOG.info("Deleting folder {}", folder.getRoot().getAbsolutePath());
		folder.delete();
	}

	@Test
	public void canGenerateUniqFileNameBasedOnSize() throws Exception {
		Set<String> names = new HashSet<String>();
		String rootPath = folder.getRoot().getAbsolutePath();
		LOG.info("Rootpath is {}", rootPath);
		
		GenericFileNameGenerator nameGenerator = new GenericFileNameGenerator();
		nameGenerator.setDirectory(rootPath);
		nameGenerator.setFileSizeThreshold(10);
		
		int maxfile = 0x0fff;
		
		for(int index = 0; index < maxfile; index++) {
			// generate a file every 2 rounds, as the payload length is 5 for a threshold of 10
			String name = nameGenerator.generateFileName(msg);
			names.add(name);
		}
		assertEquals((maxfile/2) + 1, names.size());
	}
	
	@Test
	public void canGenerateUniqFileNameBasedOnTime() throws Exception {
		Set<String> names = new HashSet<String>();
		String rootPath = folder.getRoot().getAbsolutePath();
		LOG.info("Rootpath is {}", rootPath);
		
		GenericFileNameGenerator nameGenerator = new GenericFileNameGenerator();
		nameGenerator.setDirectory(rootPath);
		nameGenerator.setFileTimeThreshold(3); // this is in sec, turn into ms.
		
		int maxfile = 3;
		
		for(int index = 0; index < maxfile; index++) {
			String name = nameGenerator.generateFileName(msg);
			names.add(name);
			Thread.sleep(4000);
		}
		assertEquals(maxfile, names.size());
	}
	
	@Test
	public void canGenerateUniqFileNameOnEachCall() throws Exception {
		TemporaryFolder folder = new TemporaryFolder();
		try {
			Set<String> names = new HashSet<String>();
			folder.create();
			String rootPath = folder.getRoot().getAbsolutePath();
			LOG.info("Rootpath is {}", rootPath);
			
			GenericFileNameGenerator nameGenerator = new GenericFileNameGenerator();
			nameGenerator.setDirectory(rootPath);
			
			int maxfile = 0x0fff;

			for(int index = 0; index < maxfile; index++) {
				String name = nameGenerator.generateFileName(msg);
				names.add(name);
			}
			assertEquals(maxfile, names.size());
		} finally {
			folder.delete();
		}
	}
}
