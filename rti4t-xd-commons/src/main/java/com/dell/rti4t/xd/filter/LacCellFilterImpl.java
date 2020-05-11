package com.dell.rti4t.xd.filter;

import static com.gs.collections.impl.tuple.Tuples.pair;
import static org.apache.commons.lang.StringUtils.isNumeric;
import static org.springframework.util.StringUtils.isEmpty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import reactor.util.StringUtils;

import com.dell.rti4t.xd.common.ReductionMapHandler;
import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.files.AbstractFileChangeWatchdog;
import com.dell.rti4t.xd.files.OnFileChange;
import com.dell.rti4t.xd.utils.FileUtils;
import com.gs.collections.api.tuple.Pair;

public class LacCellFilterImpl implements EventFilter, InitializingBean {
	
	static private final Logger LOG = LoggerFactory.getLogger(LacCellFilterImpl.class);
	static List<Pair<String, String>> forbiddenValues = new ArrayList<Pair<String, String>>();
	static {
		forbiddenValues.add(pair("65535", "65535"));
		forbiddenValues.add(pair("0", "0"));
	};
	
	static final private String LAC_SEPARATOR = ",";
	static final private String CELL_SEPARATOR = ";";

	Map<String, Set<String>> lacCellsStore;// = new HashMap<String, Set<String>>();
	
	private String lacCellFilePath;
	private String lacField = "lac";
	private String cellField = "cellTower";
	
	private int frequency;
	private boolean followExit;
	
	public String toString() {
		return String.format("Filtering on file [%s], filter fields are [%s,%s] - follow exit '%s'",
				(StringUtils.isEmpty(lacCellFilePath) ? "<none>" : lacCellFilePath),
				lacField, cellField,
				followExit);
	}
	
	public Map<String, Set<String>> accessLacCellsStore() {
		return lacCellsStore;
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

	@Override
	public boolean accept(DataTransporter dt) {
		String lac = dt.getFieldValue(lacField);
		String cell = dt.getFieldValue(cellField);
		if(lacCellsStore == null) {
			return checkForbiddenValues(lac, cell);
		}
		if(lac == null || cell == null) {
			return false;
		}
		Set<String> cells = lacCellsStore.get(lac);
		
		boolean inGeoFence = cells != null && (cells.isEmpty() || cells.contains(cell));
		
		String imsi = dt.getFieldValue("imsi");

		if(inGeoFence) {
			if(followExit) {
				ReductionMapHandler.isInGeofence(imsi);
			}
			return true;
		} else {
			if(followExit) {
				return ReductionMapHandler.isLeavingGeofence(imsi);
			}
		}
		return false;
	}
	
	private boolean checkForbiddenValues(String lac, String cell) {
		for(Pair<String, String> pair : forbiddenValues) {
			if(pair.getOne().equals(lac) && pair.getOne().equals(cell)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
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
		String lac = lacCells[0];
		if(isNumeric(lac)) {
			String allCells = lacCells[1];
			Set<String> cellsSet = lacCellsStore.get(lac);
			if(cellsSet == null) {
				cellsSet = new HashSet<String>();
				lacCellsStore.put(lac, cellsSet);
			}
			String[] cells = allCells.split(CELL_SEPARATOR);
			for(String cell : cells) {
				if("*".equals(cell)) {
					cellsSet.clear();
				} else if(isNumeric(cell)) {
					cellsSet.add(cell);
				}
			}
		}
	}

	@Override
	public String description() {
		return "filter based on lac/cell";
	}
}
