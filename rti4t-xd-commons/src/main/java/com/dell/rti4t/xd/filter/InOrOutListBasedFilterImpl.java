package com.dell.rti4t.xd.filter;

import static com.dell.rti4t.xd.utils.FileUtils.fileFromPath;
import static reactor.util.StringUtils.isEmpty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.util.StringUtils;

import com.dell.rti4t.xd.common.VersionedSet;
import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.files.AbstractFileChangeWatchdog;
import com.dell.rti4t.xd.files.OnFileChange;
import com.google.common.base.Stopwatch;

public class InOrOutListBasedFilterImpl extends InOrOutBaseFilter {
	
	private static final int BUFFER_SIZE = 32 * 1024 * 1024;
	
	static private final Logger LOG = LoggerFactory.getLogger(InOrOutListBasedFilterImpl.class);

	private String inOrOutFilePath;
	private Set<String> inOrOutSet;
	private int frequency;
	int totalEntries;
	
	public void setInOrOutFilePath(String inOrOutFilePath) {
		this.inOrOutFilePath = inOrOutFilePath;
	}
	
	public void setRefresh(int frequency) {
		this.frequency = frequency * 1000;
	}
	
	@Override
	public boolean accept(DataTransporter dt) {
		if(inOrOutSet == null) { // what to do ??
			return true;
		}
		String field = dt.getFieldValue(filterField);
		if(field == null) { // no field, decision is not to keep the item
			return false;
		}
		boolean accepted = !(in ^ inOrOutSet.contains(field)); // xnor -> true if both are the same
		return accepted;
	}

	@Override
	protected void doAfterPropertiesSet() throws Exception {
		if(!isEmpty(inOrOutFilePath)) {
			inOrOutSet = new VersionedSet<String>();
			LOG.info("Filter for '{}' is '{}'", inOrOutFilePath, (in == true ? "whitelist" : "blacklist"));
			inOrOutSet = loadFileInfo();
			startFileWatchDog();
		}
	}
	
	private Set<String> loadFileInfo() throws Exception {
        Set<String> inOrOutSet = new HashSet<String>();

		Stopwatch stopWatch = Stopwatch.createStarted();
		
		File inOrOutFile = fileFromPath(inOrOutFilePath);
		LOG.info("InMode is '{}', field is '{}'", in, filterField);
		LOG.info("Loading {}", inOrOutFile.getAbsolutePath());

		try(BufferedReader reader = new BufferedReader(new FileReader(inOrOutFile), BUFFER_SIZE)) {
			String line;
			while((line = reader.readLine()) != null) {
				if(!isEmpty(line)) {
					inOrOutSet.add(line);
				}
			}
		}
		
		stopWatch.stop();
		
		LOG.info("Total read {} in {} ms", 
				inOrOutSet.size(), 
				stopWatch.elapsed(TimeUnit.MILLISECONDS));
		return inOrOutSet;
	}
	
	private void startFileWatchDog() {
		new AbstractFileChangeWatchdog(inOrOutFilePath, new OnFileChange() {
			@Override
			public void onFileChange() {
				try {
					inOrOutSet = loadFileInfo();
				} catch(InterruptedException ie) {
					return;
				} catch(Exception e) {
					LOG.error("Error while watching/reloading file {}", inOrOutFilePath);
				}
			}
		}, frequency);
	}

	@Override
	public String description() {
		return "in/out filter based on lists";
	}
	
	public String toString() {
		return String.format("Filtering on file [%s], keep if present is %s, filter field %s",
				(StringUtils.isEmpty(inOrOutFilePath) ? "<none>" : inOrOutFilePath),
				(in == null ? "<?>" : in.toString()),
				filterField);
	}
}
