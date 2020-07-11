package com.vodafone.dca.misc;

import static com.vodafone.dca.it.TestUtils.cleanGeneratedFiles;
import static com.vodafone.dca.it.TestUtils.moveDemoSampleFileToTmp;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

public class FileReadMessageSourceTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(FileReadMessageSourceTest.class);
	
	String rootDir = "/tmp/filereadmessage";
	File rootScan = new File(rootDir);
	
	@Before
	public void cleanRootDir() throws Exception {
		cleanGeneratedFiles(rootDir, ".dat");
	}
	
	@Test
	public void canScanDirectory() throws Exception {
		FileReadingMessageSource underTest = new FileReadingMessageSource();
		
		underTest.setDirectory(rootScan);
		underTest.setFilter((files) -> Arrays.stream(files)
										.filter(file -> file.isFile() && file.getName().endsWith(".dat"))
										.collect(Collectors.toList())); 
		
		underTest.afterPropertiesSet();
		underTest.start();
		
		PollerMetadata poller = Pollers.fixedRate(1000).get();
		poller.setMaxMessagesPerPoll(1000);

		DirectChannel outputChannel = mock(DirectChannel.class);
		BeanFactory beanFactory = mock(BeanFactory.class);
		
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();


		SourcePollingChannelAdapter sourcePoller = new SourcePollingChannelAdapter();
		sourcePoller.setMaxMessagesPerPoll(200);
		sourcePoller.setSource(underTest);
		sourcePoller.setOutputChannel(outputChannel);
		sourcePoller.setBeanFactory(beanFactory);
		sourcePoller.setTaskScheduler(taskScheduler);
		
		sourcePoller.afterPropertiesSet();
		sourcePoller.start();
		
		
		for(int index = 0; index < 10; index++) {
			moveDemoSampleFileToTmp(rootDir, "demographics/demo1/simple.dat", "test-" + index + ".dat");
		}
		
//		for(;;) {
//			Thread.sleep(1000);
//			Message<File> newFile = null;
//			LOG.info(" --- starting receive -------------- ");
//			do {
//				newFile = underTest.receive();
//				if(newFile != null) {
//					LOG.info("Received {}", newFile);
//				}
//			} while(newFile != null);
//			LOG.info(" --- ending receive -------------- ");
//		}
	}
}
