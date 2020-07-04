package com.vodafone.dca.filter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.GenericSelector;
import org.springframework.util.StringUtils;

import com.google.common.base.Stopwatch;
import com.vodafone.dca.common.FileUtils;
import com.vodafone.dca.domain.DataTransporter;

public class InOrOutListBasedFilter implements GenericSelector<DataTransporter> {
	
	static private final Logger LOG = LoggerFactory.getLogger(InOrOutListBasedFilter.class);

	private static final int BUFFER_SIZE = 32 * 1024 * 1024;
	
	private String inOrOutFilePath;
	private Set<String> inOrOutSet;
	
	private String filterField = "imsi";
	
	private Boolean in;
	private int frequency;
	
	public void setInMode(boolean in) {
		this.in = in;
	}
	
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
		return !(in ^ inOrOutSet.contains(field)); // xnor -> true if both are the same
	}

	@PostConstruct
	public void loadListFromFileAndStartWatcher() throws Exception {
		if(!StringUtils.isEmpty(inOrOutFilePath)) {
			LOG.info("Filter for '{}' is '{}'", inOrOutFilePath, (in == true ? "whitelist" : "blacklist"));
			inOrOutSet = loadListFromFile();
			startFileWatchDog();
		}
	}
	
	private Set<String> loadListFromFile() throws Exception {
        Set<String> inOrOutSet = new HashSet<String>();

		Stopwatch stopWatch = Stopwatch.createStarted();
		
		File inOrOutFile = FileUtils.fileFromPath(inOrOutFilePath);
		LOG.info("Loading {}, InMode is '{}', field is '{}'", inOrOutFile.getAbsolutePath(), in, filterField);

		try(BufferedReader reader = new BufferedReader(new FileReader(inOrOutFile), BUFFER_SIZE)) {
			String line;
			while((line = reader.readLine()) != null) {
				if(!StringUtils.isEmpty(line)) {
					inOrOutSet.add(line);
				}
			}
		}
		
		stopWatch.stop();		
		LOG.info("Total read {} in {} ms", inOrOutSet.size(), stopWatch.elapsed(TimeUnit.MILLISECONDS));
		return inOrOutSet;
	}
	
	private void startFileWatchDog() {
		new AbstractFileChangeWatchdog(inOrOutFilePath, () -> {
				try {
					inOrOutSet = loadListFromFile();
				} catch(InterruptedException ie) {
					return;
				} catch(Exception e) {
					LOG.error("Error while watching/reloading file {}", inOrOutFilePath);
				}
			}, 
		frequency);
	}
	
	static public class Builder {
		private boolean inMode;
		private String filePath;
		private int frequency;
		private String filterField;
		
		public Builder withInMode(boolean inMode) {
			this.inMode = inMode;
			return this;
		}
		public Builder withFileScanFrequency(int frequency) {
			this.frequency = frequency;
			return this;
		}
		public Builder withListFilePath(String filePath) {
			this.filePath = filePath;
			return this;
		}
		public Builder withFilterField(String filterField) {
			this.filterField = filterField;
			return this;
		}
		
		public InOrOutListBasedFilter build() {
			InOrOutListBasedFilter built = new InOrOutListBasedFilter();
			built.frequency = frequency;
			if (!StringUtils.isEmpty(filterField)) { // leave to default.
				built.filterField = filterField;
			}
			built.inOrOutFilePath = this.filePath;
			built.in = inMode;
			return built;
		}
	}
	
	public static Builder newBuilder() {
		return new Builder();
	}

	public String toString() {
		return String.format("Filtering on file [%s], keep if present is %s, filter field %s",
				(StringUtils.isEmpty(inOrOutFilePath) ? "<none>" : inOrOutFilePath),
				(in == null ? "<?>" : in.toString()),
				filterField);
	}
}
