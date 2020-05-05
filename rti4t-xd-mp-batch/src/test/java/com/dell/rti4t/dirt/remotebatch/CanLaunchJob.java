package com.dell.rti4t.dirt.remotebatch;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URI;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.xd.rest.client.impl.SpringXDTemplate;

public class CanLaunchJob {
	
	static private final Logger LOG = LoggerFactory.getLogger("nodell"); //CanLaunchJob.class);
	
	// We suppose XD is running and a batch called 'viavi-batch' has been installed.
	// More a visual test than anything else.
	
	int totalFiles = 200;
	int linesPerFile = 100_000;
	
	@Test
	public void canLaunchJob() throws Exception {
//		LOG.info("Starting...creating files");
//		for(int index = 0; index < totalFiles; index++) {
//			createFile("/Users/manuelmeyer/GitHub/rti4t-xd-modules/modules/files/test" + index + ".csv", index);
//		}
//		LOG.info("Files created");
		
		for(;;) {
			LOG.info("sending batches");
			SpringXDTemplate xdTemplate = new SpringXDTemplate(new URI("http://localhost:9393"));
			for(int index = 0; index < totalFiles; index++) {
				LOG.info("sending batch {}", index);
				//createFile("/Users/manuelmeyer/GitHub/rti4t-xd-modules/modules/files/test" + index + ".csv", index);
				xdTemplate.jobOperations().launchJob("viavi-batch", 
						"{\"absoluteFilePath\": \"/Users/manuelmeyer/GitHub/rti4t-xd-modules/modules/files/test" + index + ".csv\"}");
				LOG.info("sent batch {}", index);
				Thread.sleep(50);
			}
		}
	}

	private void createFile(String fileName, int fileIndex) throws Exception {
		BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
		for(int index = 0; index < linesPerFile; index++) {
			writer.write(
				String.format("aaaaaaaaaaaaaaaaaa-%d,%d-bbbbbbbbbbbbbbbbbbb-%d,%d-ccccccccccccccccc-%d\n", 
												fileIndex, 
												index,
												fileIndex, 
												index,
												fileIndex)
			);
		}
		writer.flush();
		writer.close();
	}
}
