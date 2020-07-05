package com.vodafone.dca.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vodafone.dca.common.FileUtils;

public class AbstractFileChangeWatchdog {
	
	static final Logger LOG = LoggerFactory.getLogger(AbstractFileChangeWatchdog.class);

	private long lastModified = 0;
	
	public AbstractFileChangeWatchdog(final String filePath, final OnFileChange callback, final int frequency) {
		LOG.info("Starting an AbstractFileChangeWatchdog on {} every {} sec", filePath, frequency/1000);
		lastModified = FileUtils.lastModified(filePath);
		new Thread(new Runnable() {
			long currentModified = lastModified;
			@Override
			public void run() {
				for(;;) {
					try {
						Thread.sleep(frequency);
						lastModified = FileUtils.lastModified(filePath);
						if(lastModified > currentModified) {
							currentModified = lastModified;
							LOG.info("File {} has changed - calling callback", filePath);
							callback.onFileChange();
						}
					} catch(InterruptedException ie) {
						return;
					} catch(Exception e) {
						LOG.error("Error while watching/reloading file {}", filePath);
					}
				}
			}
		}).start();
	}	
}
