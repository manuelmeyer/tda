package com.vodafone.dca.filter;

import static java.lang.String.valueOf;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.GenericSelector;

import com.vodafone.dca.domain.DataTransporter;
import com.vodafone.dca.reduction.AsciiToNumber;
import com.vodafone.dca.reduction.ImsiHistory;
import com.vodafone.dca.reduction.ReductionMapHandler;

public class DataReductionFilter implements GenericSelector<DataTransporter> {
	
	private static final Logger LOG = LoggerFactory.getLogger(DataReductionFilter.class);
	
	public enum ReductionMode {
		NONE,
		/*
		 * Simple de-duplication (dedup) is to let pass only the first event when several are sent in a row for the same <lac, cell>.
		 * Therefore receiving a row of <time, lac, cell> events like
		 * t0, l1, c1,
		 * t1, l1, c1,
		 * ...
		 * tn, l1, c1, with t0 < t1 < ... < tn, will only let pass t0, l1, c1
		 * 
		 * Strong dedup is to avoid letting pass too much events when an imsi bounces back and forth 
		 * from a cell tower to another identical cell tower.
		 * Considering t0 < t1 < ... < tn, we can have a row of <time, lac, cell> events like:
		 * 
		 * t0, l1, c1
		 * t1, l2, c2
		 * t2, l1, c1
		 * t3, l2, c2
		 * 
		 * Simple dedup consider each above event as a cell change, back and forth from <l1, c1> to <l2, c2>, 
		 * strong dedup consider those events are a row on <l1, c1> followed by a row on <l2, c2>, marked with incorrect timestamp.
		 * 
		 * Therefore simple dedup let pass all the above events while
		 * strong dedup let pass only the 2 first, the 2 others being considered redundant.
		 * 
		 */
		MARK_IMSIS_CHANGE_CELL_ONLY_STRONG_DEDUP, // cell change or same cell every 8h, strong dedup.
		MARK_IMSIS_CHANGE_CELL_ONLY, // cell change or same cell every 8h, simple dedup

		IMSIS_CHANGE_CELL_ONLY_STRONG_DEDUP, // cell change or same cell every 8h, strong dedup.
		IMSIS_CHANGE_CELL_ONLY, // cell change or same cell every 8h, simple dedup
		IMSIS_CHANGE_CELL, // cell change or an event out of the clock minute, + strong dedup
		MARK_IMSIS_CHANGE_CELL
	}
	
	private ReductionMode mode = ReductionMode.IMSIS_CHANGE_CELL;
	private boolean traceModeOn = false;
	private ReductionMapHandler reductionMapHandler;

	public void setReductionMode(ReductionMode mode) {
		this.mode = mode;
	}
	
	public void setReductionMapHandler(ReductionMapHandler reductionMapHandler) {
		this.reductionMapHandler = reductionMapHandler;
	}

	@Override
	public boolean accept(DataTransporter dt) {
		if(mode == ReductionMode.NONE) {
			return true;
		}
		
		String imsi = dt.getFieldValue("imsi");
		if(imsi == null) {
			LOG.trace("No imsi - returning false");
			return false;
		}
		
		String lacStr = (dt.getFieldValue("lac"));
		String cellTowerStr = (dt.getFieldValue("cellTower"));
		String eventTimeUTCStr = (dt.getFieldValue("timeUTC"));
		
		if(lacStr == null | cellTowerStr == null || eventTimeUTCStr == null) {
			return false;
		}

		long lac = AsciiToNumber.atol(lacStr);
		long cellTower = AsciiToNumber.atol(cellTowerStr);
		long now = AsciiToNumber.atotime(eventTimeUTCStr);

		ImsiHistory history = reductionMapHandler.getImsiHistory(imsi);
		
		if(history == null) {
			ImsiHistory newImsiHistory = reductionMapHandler.newImsiHistory(imsi, lac, cellTower, now);
			if(isTraceModeOn()) {
				addTraceInfo(dt, newImsiHistory, "__OK_FIRSTSEEN__");
			}
			return true;
		}
		
		if(!reductionMapHandler.isDuplicated(history, lac, cellTower, now)) {
			if(isTraceModeOn()) {
				addTraceInfo(dt, history, "__OK_LACCELLCHANGE__");
			} else if(mode == ReductionMode.IMSIS_CHANGE_CELL_ONLY ||
					mode == ReductionMode.IMSIS_CHANGE_CELL_ONLY_STRONG_DEDUP) {
				if(history.previousLac() != -1 && history.previousCellTower() != -1) {
					dt.putFieldValue("previousLac", valueOf(history.previousLac()));
					dt.putFieldValue("previousCellTower", valueOf(history.previousCellTower()));
					dt.putFieldValue("previousTimeUTC", valueOf(history.previousTimeUTC()) + "000");
				}
			}
			return true;
		}
		
		if(isTraceModeOn()) {
			addTraceInfo(dt, history, "__OK_REDUCTED__");
			return true;
		}		
		return false;
	}

	private void addTraceInfo(DataTransporter dt, ImsiHistory history, String reductionType) {
		dt.putFieldValue("_accessed", valueOf(history.accessed));
		dt.putFieldValue("_reducted", reductionType);
		dt.putFieldValue("_thread", Thread.currentThread().getName());
		dt.putFieldValue("_time", String.valueOf(System.currentTimeMillis()));
		dt.putFieldValue("_previousLac", valueOf(history.previousLac()));
		dt.putFieldValue("_previousCellTower", valueOf(history.previousCellTower()));
		dt.putFieldValue("_previousTimeUTC", valueOf(history.previousTimeUTC()) + "000");
		dt.putFieldValue("_traceMarker", "-");
	}

	private boolean isTraceModeOn() {
		return traceModeOn;
	}

	@PostConstruct // TODO useless
	public void afterPropertiesSet() throws Exception {
		if(mode == ReductionMode.NONE) {
			LOG.info("NO data reduction in place");
			return;
		}
		
		switch(mode) {
			case IMSIS_CHANGE_CELL_ONLY_STRONG_DEDUP:
			case MARK_IMSIS_CHANGE_CELL_ONLY_STRONG_DEDUP:
				reductionMapHandler.useStrongDedup();
			case IMSIS_CHANGE_CELL_ONLY :
			case MARK_IMSIS_CHANGE_CELL_ONLY:
				// TODO
				//setDelayBeforeDuplicate(8 * 3600); // 8h on the same cell before duplicating
				break;
			default:
				reductionMapHandler.useStrongDedup();
				// TODO
				//setDelayBeforeDuplicate(60); // 1mn before duplicating in other mode
				break;
		}
		
		traceModeOn = (mode == ReductionMode.MARK_IMSIS_CHANGE_CELL) ||
				(mode == ReductionMode.MARK_IMSIS_CHANGE_CELL_ONLY) ||
				(mode == ReductionMode.MARK_IMSIS_CHANGE_CELL_ONLY_STRONG_DEDUP);
		
		LOG.info("Data reduction mode '{}' based on a change of celltower or a delta of {} seconds, trace mode is '{}'",
				mode, reductionMapHandler.delayBeforeDuplicate, traceModeOn ? "on" : "off");

	}
}
