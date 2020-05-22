package com.dell.rti4t.xd.filter;

import static com.dell.rti4t.xd.filter.DataReductionImpl.ReductionMode.IMSIS_CHANGE_CELL;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.Thread.currentThread;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.dell.rti4t.xd.common.AsciiToNumber;
import com.dell.rti4t.xd.common.ImsiHistory;
import com.dell.rti4t.xd.common.ReductionMapHandler;
import com.dell.rti4t.xd.domain.DataTransporter;

public class DataReductionImpl implements EventFilter, InitializingBean {
	
	private int delayBeforeDuplicate = -1; // 8 * 60 + 1; // 2h 1mn
	
	private static final Logger LOG = LoggerFactory.getLogger(DataReductionImpl.class);
	
	public enum ReductionMode {
		NONE,
		IMSIS_CHANGE_CELL, // a change of cell or an event out of the clock minute
		IMSIS_CHANGE_CELL_ONLY, // cell change only
		MARK_IMSIS_CHANGE_CELL
	}
	
	private ReductionMode mode = IMSIS_CHANGE_CELL;

	public void setDelayBeforeDuplicate(int delay) {
		LOG.info("Setting delay before duplicating on the same cell to {} sec", delay);
		delayBeforeDuplicate = delay;
		ReductionMapHandler.setDelayBeforeDuplicate(delay);
	}
	
	public void setReductionMode(ReductionMode mode) {
		this.mode = mode;
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
		
		if(LOG.isDebugEnabled()) {
			LOG.debug("Checking event imsi={}, lac={}, cellTower={}, timeUTC={}", 
					new Object[] {imsi, lacStr, cellTowerStr, eventTimeUTCStr});
		}
		
		if(lacStr == null | cellTowerStr == null || eventTimeUTCStr == null) {
			LOG.debug("Null fields found - returning false");
			if(mode == ReductionMode.MARK_IMSIS_CHANGE_CELL) {
				dt.putFieldValue("reducted", "NOK-CANTCHECK");
			}
			return false;
		}

		long lac = AsciiToNumber.atol(lacStr);
		long cellTower = AsciiToNumber.atol(cellTowerStr);
		long now = AsciiToNumber.atotime(eventTimeUTCStr);

		ImsiHistory history = ReductionMapHandler.getImsiHistory(imsi);
		
		if(history == null) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("No entry, adding one and returning true");
			}
			if(mode == ReductionMode.MARK_IMSIS_CHANGE_CELL) {
				dt.putFieldValue("accessed", "0");
				dt.putFieldValue("reducted", "OK-FIRSTSEEN");
			}
			ReductionMapHandler.newImsiHistory(imsi, lac, cellTower, now);
			return true;
		}
		
		if(!ReductionMapHandler.isReductable(history, lac, cellTower, now)) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("Change of cell tower, returning true");
			}
			if(mode == ReductionMode.MARK_IMSIS_CHANGE_CELL) {
				dt.putFieldValue("accessed", valueOf(history.accessed));
				dt.putFieldValue("reducted", format("OK-LACCELLCHANGE-%s", currentThread().getName()));
			} else if(mode == ReductionMode.IMSIS_CHANGE_CELL_ONLY) {
				if(history.previousLac != -1 && history.previousCellTower != -1) {
					dt.putFieldValue("previousLac", valueOf(history.previousLac));
					dt.putFieldValue("previousCellTower", valueOf(history.previousCellTower));
					dt.putFieldValue("previousTimeUTC", valueOf(history.previousTimeUTC) + "000");
				}
			}
			return true;
		}
		
		if(mode == ReductionMode.MARK_IMSIS_CHANGE_CELL) {
			dt.putFieldValue("accessed", valueOf(history.accessed));
			dt.putFieldValue("reducted", format("REDUCTED-%s", currentThread().getName()));
			return true;
		}
		
		return false;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(mode == ReductionMode.NONE) {
			LOG.info("NO data reduction in place");
			return;
		}
		
		switch(mode) {
			case IMSIS_CHANGE_CELL_ONLY :
				setDelayBeforeDuplicate(8 * 3600); // 8h on the same cell before duplicating
				break;
			default:
				setDelayBeforeDuplicate(60); // 1mn before duplicating in other mode
				break;
		}
		
		LOG.info("Data reduction mode {} based on a change of celltower or a delta of {} seconds", 
				mode, 
				ReductionMapHandler.delayBeforeDuplicate);

		ReductionMapHandler.buildMap();
	}

	@Override
	public String description() {
		return "filter events seen in the last minute in the same lac/cell";
	}
}
