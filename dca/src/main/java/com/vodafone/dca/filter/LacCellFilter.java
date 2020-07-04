package com.vodafone.dca.filter;

import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.springframework.util.StringUtils.isEmpty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.GenericSelector;

import com.google.common.collect.Sets;
import com.vodafone.dca.common.FileUtils;
import com.vodafone.dca.domain.DataTransporter;
import com.vodafone.dca.reduction.ReductionMapHandler;

public class LacCellFilter implements GenericSelector<DataTransporter> {
	
	static private final Logger LOG = LoggerFactory.getLogger(LacCellFilter.class);
	static Set<String> forbiddenValues = Sets.newHashSet("0", "65535");

	static final private String LAC_SEPARATOR = ",";
	static final private String CELL_SEPARATOR = ";";

	Map<String, Set<String>> lacCellsStore;
	
	private String lacCellFilePath;
	private String lacField = "lac";
	private String cellField = "cellTower";
	
	private ReductionMapHandler reductionMapHandler;
	
	private int frequency;
	private boolean followExit;
	
	public Map<String, Set<String>> accessLacCellsStore() {
		return lacCellsStore;
	}
	
	public void setReductionMapHandler(ReductionMapHandler reductionMapHandler) {
		this.reductionMapHandler = reductionMapHandler;
	}
	
	public void setFollowExit(boolean followExit) {
		this.followExit = followExit;
	}
	
	public void setRefresh(int frequency) {
		this.frequency = frequency * 1000;
	}
	
	public void setLacField(String lacField) {
		this.lacField = lacField;
	}

	public void setCellField(String cellField) {
		this.cellField = cellField;
	}

	public void setLacCellFilePath(String lacCellFilePath) {
		this.lacCellFilePath = lacCellFilePath;
	}
	
	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		private String lacCellFilePath;
		private String lacField = "lac";
		private String cellField = "cellTower";
		
		private int frequency;
		private boolean followExit = false;
		private ReductionMapHandler reductionMapHandler;
		
		public Builder withReductionMapHandler(ReductionMapHandler reductionMapHandler) {
			this.reductionMapHandler = reductionMapHandler;
			return this;
		}
		
		public Builder withFileScanFrequency(int frequency) {
			this.frequency = frequency;
			return this;
		}
		
		public Builder withLacCellFields(String lacField, String cellField) {
			this.lacField = lacField;
			this.cellField = cellField;
			return this;
		}
		
		public Builder withLacCellFilePath(String lacCellFilePath) {
			this.lacCellFilePath = lacCellFilePath;
			return this;
		}
		
		public Builder withFollowExit(boolean followExit) {
			this.followExit = followExit;
			return this;
		}
		
		public LacCellFilter build() {
			LacCellFilter built = new LacCellFilter();
			built.setCellField(cellField);
			built.setLacField(lacField);
			built.setFollowExit(followExit);
			built.setRefresh(frequency);
			built.setLacCellFilePath(lacCellFilePath);
			built.setReductionMapHandler(reductionMapHandler);
			return built;
		}
	}

	@Override
	public boolean accept(DataTransporter dt) {
		String lac = dt.getFieldValue(lacField);
		String cell = dt.getFieldValue(cellField);
		if(areForbiddenValues(lac, cell)) {
			return false;
		}
		if(lacCellsStore == null) {
			return true;
		}
		if(lac == null || cell == null) {
			return false;
		}
		Set<String> cells = lacCellsStore.get(lac);
		
		boolean inGeoFence = cells != null && (cells.isEmpty() || cells.contains(cell));
		
		String imsi = dt.getFieldValue("imsi");

		if(inGeoFence) {
			if(followExit) {
				reductionMapHandler.isInGeofence(imsi, dt.getFieldValue("timeUTC"));
			}
			return true;
		} else {
			if(followExit) {
				return reductionMapHandler.isLeavingGeofence(imsi, dt.getFieldValue("timeUTC"));
			}
		}
		return false;
	}
	
	private boolean areForbiddenValues(String lac, String cell) {
		if(lac == null || cell == null) {
			return true;
		}
		for(String forbiddenValue : forbiddenValues) {
			if(forbiddenValue.equals(lac) && forbiddenValue.equals(cell)) {
				return true;
			}
		}
		return false;
	}

	@PostConstruct
	public void firstLoadLacAndCells() throws Exception {
		if(!isEmpty(lacCellFilePath)) {
			lacCellsStore = loadLacAndCells();
			LOG.info("{}", toString());
			startFileWatchDog();
		}
	}
	
	private void startFileWatchDog() {
		new AbstractFileChangeWatchdog(lacCellFilePath, new OnFileChange() {
			@Override
			public void onFileChange() {
				try {
					lacCellsStore = loadLacAndCells();
				} catch(InterruptedException ie) {
					return;
				} catch(Exception e) {
					LOG.error("Error while watching/reloading file {}", lacCellFilePath);
				}
			}
		}, frequency);
	}

	private HashMap<String, Set<String>> loadLacAndCells() throws Exception {
		File lacFile = FileUtils.fileFromPath(lacCellFilePath);
		LOG.info("Loading {}", lacFile.getAbsolutePath());
		long t0 = System.currentTimeMillis();
		HashMap<String, Set<String>> lacCellsStore = new HashMap<String, Set<String>>();
		try(BufferedReader reader = new BufferedReader(new FileReader(lacFile), 16 * 1024 * 1024)) {
			String line;
			while((line = reader.readLine()) != null) {
				addLacAndCellsToList(line, lacCellsStore);
			}
		}
		long t1 = System.currentTimeMillis();
		LOG.info("Loaded {} lacs in {} ms", lacCellsStore.size(), (t1 - t0));
		return lacCellsStore;
	}

	private void addLacAndCellsToList(String line, Map<String, Set<String>> lacCellsStore) {
		// line is lac,c1;c2;c3 or lac,* for all the cells in the lac
		String[] lacCells = line.split(LAC_SEPARATOR);
		if(lacCells.length != 2) {
			return;
		}
		String lac = lacCells[0].trim();
		if(StringUtils.isNumeric(lac)) {
			String allCells = lacCells[1];
			Set<String> cellsSet = lacCellsStore.get(lac);
			if(cellsSet == null) {
				cellsSet = new HashSet<String>();
				lacCellsStore.put(lac, cellsSet);
			}
			String[] cells = allCells.split(CELL_SEPARATOR);
			for(String cell : cells) {
				cell = cell.trim();
				if("*".equals(cell)) {
					cellsSet.clear();
				} else if(isNumeric(cell)) {
					cellsSet.add(cell);
				}
			}
		}
	}

	public String toString() {
		return String.format("Filtering on file [%s], filter fields are [%s,%s] - follow exit '%s'",
				(StringUtils.isEmpty(lacCellFilePath) ? "<none>" : lacCellFilePath),
				lacField, cellField,
				followExit);
	}
}
