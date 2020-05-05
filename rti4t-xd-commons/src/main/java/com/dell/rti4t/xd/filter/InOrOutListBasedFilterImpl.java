package com.dell.rti4t.xd.filter;

import static com.dell.rti4t.xd.utils.FileUtils.fileFromPath;
import static reactor.util.StringUtils.isEmpty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.NumberFormat;
import java.util.HashSet;
import java.util.Set;

import net.openhft.chronicle.set.ChronicleSetBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.util.StringUtils;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.files.AbstractFileChangeWatchdog;
import com.dell.rti4t.xd.files.OnFileChange;

public class InOrOutListBasedFilterImpl extends InOrOutBaseFilter {
	
	private static final int BUFFER_SIZE = 32 * 1024 * 1024;
	private static final int FILTERED_SET_SIZE = 32 * 1024 * 1024;
	private static final int SLEEP_BETWEEN_STATS = 30_000;
	
	private static final String VFUK_PREFIX = "23415";
	
	static private final Logger LOG = LoggerFactory.getLogger(InOrOutListBasedFilterImpl.class);

	private String inOrOutFilePath;
	private Set<String> inOrOutSet;
	private Set<String> filteredOutSet;
	private Set<String> acceptedInSet;
	private int frequency;
	int totalEntries;
	
	private boolean useChronicle = true;
	
	public void setUseOffHeapSet(boolean useChronicle) {
		this.useChronicle = useChronicle;
	}
	
	public String toString() {
		return String.format("Filtering on file [%s], keep if present is %s, filter field %s",
				(StringUtils.isEmpty(inOrOutFilePath) ? "<none>" : inOrOutFilePath),
				(in == null ? "<?>" : in.toString()),
				filterField);
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
		boolean accepted = !(in ^ inOrOutSet.contains(field)); // xnor -> true if both are the same
		addToFilteredOut(field, accepted ? acceptedInSet : filteredOutSet);
		return accepted;
	}

	private void addToFilteredOut(String field, Set<String> set) {
		try {
			if (field != null && field.startsWith(VFUK_PREFIX)) {
				set.add(field);
			}
		} catch(Exception e) {
			LOG.error("Cannot add {} to filteredout map", field);
		}
	}

	@Override
	protected void doAfterPropertiesSet() throws Exception {
		if(!isEmpty(inOrOutFilePath)) {
			LOG.info("Filter for {} is {}", inOrOutFilePath, (in == true ? "whitelist" : "blacklist"));
			inOrOutSet = loadFileInfo();
			startFileWatchDog();
			filteredOutSet = createFilteredHashSet();
			acceptedInSet = createAcceptedHashSet();
			startFilterOutSetDumper();
		}
	}
	
	private Set<String> loadFileInfo() throws Exception {
		long t0 = System.currentTimeMillis();
		Set<String> inOrOutSet = createHashSet(fileFromPath(inOrOutFilePath).getAbsolutePath());
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
		long t1 = System.currentTimeMillis();
		LOG.info("Total read {} in {} ms", inOrOutSet.size(), (t1 - t0));
		return inOrOutSet;
	}
	
	private Set<String> createAcceptedHashSet() {
		if(!useChronicle) {
			return new HashSet<String>(totalEntries);
		}
		LOG.info("Creating a chronicle set of {} entries for accepted, key size 20", totalEntries);

		return ChronicleSetBuilder
				.of(String.class)
				.averageKeySize(20)
				.entries(FILTERED_SET_SIZE)
				.create();
		
	}
	
	private Set<String> createFilteredHashSet() {
		if(!useChronicle) {
			return new HashSet<String>(totalEntries);
		}
		LOG.info("Creating a chronicle set of {} entries for filtered, key size 20", totalEntries);

		return ChronicleSetBuilder
				.of(String.class)
				.averageKeySize(20)
				.entries(FILTERED_SET_SIZE)
				.create();
		
	}
	
	private Set<String> createHashSet(String fileToLoad) throws Exception {
		if(!useChronicle) {
			return new HashSet<String>();
		}
		int keySize = 0;
		totalEntries = 0;
		try(BufferedReader reader = new BufferedReader(new FileReader(fileToLoad), BUFFER_SIZE)) {
			String line;
			while((line = reader.readLine()) != null) {
				if(!isEmpty(line)) {
					int newSize = line.length();
					if(newSize > keySize) {
						keySize = newSize;
					}
					totalEntries++;
				}
			}
		}
		LOG.info("Creating a chronicle set of {} entries, key size {}", totalEntries, keySize);
		return ChronicleSetBuilder
					.of(String.class)
					.averageKeySize(keySize)
					.entries(totalEntries + 500)
					.create();
	}

	private void startFilterOutSetDumper() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				NumberFormat integerInstance = NumberFormat.getIntegerInstance();
				for(;;) {
					try {
						Thread.sleep(SLEEP_BETWEEN_STATS);
						if (inOrOutSet != null) {
							int totalEntries = inOrOutSet.size();
							int totalAccepted = acceptedInSet.size();
							if (totalEntries > 0) {
								int totalFiltered = filteredOutSet.size();
								LOG.info("Accepted {} filtered {}", totalAccepted, totalFiltered);
								double dblPercentFilteredInList = 100.0 * (double)totalFiltered/(double)totalEntries;
								double percentAccepted = 100.00;
								if(totalFiltered + totalAccepted > 0) {
									double dblFiltered = (double)totalFiltered;
									percentAccepted = 100.0 * dblFiltered/((double)totalAccepted + dblFiltered);
								}
								LOG.info(String.format("%s total in list[%s], filtered(in)[%s] (%.2f%% of list), total(in)[%s], p(filtered/total) %.2f%% out (%.2f%% ok)", 
										(in ? "[white list]" : "[black list]"),
										integerInstance.format(totalEntries),
										integerInstance.format(totalFiltered), 
										dblPercentFilteredInList, 
										integerInstance.format(totalFiltered + totalAccepted),
										percentAccepted,
										100.00 - percentAccepted
										));
								
							}
						}
					} catch (Exception e) {
						LOG.info("Getting exception, waiting before generating stats");
					}
				}
			}
		}).start();
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
}
