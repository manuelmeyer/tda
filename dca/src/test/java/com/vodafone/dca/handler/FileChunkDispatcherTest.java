package com.vodafone.dca.handler;

import java.io.File;

import org.junit.Test;
import org.springframework.core.io.Resource;

import com.vodafone.dca.it.TestUtils;

public class FileChunkDispatcherTest {
	
	@Test
	public void canDispacthFileChunks() throws Exception {
		FileChunkDispatcher underTest = new FileChunkDispatcher();
		Resource resource = TestUtils.fileSystemOrClassPathResource("demographics/analytics.smpl");
		File input = resource.getFile();
		underTest.generateOutputFile(input);
	}
}
