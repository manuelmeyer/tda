package com.dell.rti4t.xd.filter;

import static com.dell.rti4t.xd.filter.DataReductionImpl.ReductionMode.IMSIS_CHANGE_CELL;
import static com.dell.rti4t.xd.filter.DataReductionImpl.ReductionMode.IMSIS_CHANGE_CELL_ONLY;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.Thread.currentThread;

import java.io.Serializable;
import java.util.Map;

import net.openhft.chronicle.map.ChronicleMapBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.dell.rti4t.xd.domain.DataTransporter;

public class DataReductionImpl implements EventFilter, InitializingBean {
	
	static private final int cachExpiration = 2 * 3600 + 1; // 2h 1mn
	
	private static final Logger LOG = LoggerFactory.getLogger(DataReductionImpl.class);
	
	public enum ReductionMode {
		NONE,
		IMSIS_CHANGE_CELL, // a change of cell or an event out of the clock minute
		IMSIS_CHANGE_CELL_ONLY, // cell change only
		MARK_IMSIS_CHANGE_CELL
	}
	
	static private long delta = 60;
	private ReductionMode mode = IMSIS_CHANGE_CELL;

	public void setDeltaTime(int delta) {
		this.delta = delta;
	}
	
	public void setReductionMode(ReductionMode mode) {
		this.mode = mode;
		if(mode == IMSIS_CHANGE_CELL_ONLY) {
			delta = cachExpiration;
		}
	}
	
	@SuppressWarnings("serial")
	public static class ImsiHistory implements Serializable {
		public long accessed = 0;
		public long[] eventTime = new long[2];
		public long[] lac = new long[2];
		public long[] cellTower = new long[2];
		public long lastSeen;
		
		public ImsiHistory(long lac, long cellTower, long now) {
			this.lac[0] = lac;
			this.cellTower[0] = cellTower;
			this.eventTime[0] = now;
			this.eventTime[1] = -1L;
			this.lastSeen = now;
		}
		
		public void setLastSeen(long now) {
			if(this.lastSeen < now) {
				this.lastSeen = now;
			}
		}
		
		public synchronized boolean isReductable(long lac, long cellTower, long now) {
			accessed++;
			
			boolean isCell0 = (this.lac[0] == lac && this.cellTower[0] == cellTower);
			boolean isCell1 = (this.lac[1] == lac && this.cellTower[1] == cellTower);
			
			if(isCell0 || isCell1) {
				if(isCell0 && now > (this.eventTime[0] + delta)) {
					this.eventTime[0] = now;
					return false;
				} else if(isCell1 && now > (this.eventTime[1] + delta)) {
					this.eventTime[1] = now;
					return false;
				}
				return true;
			}
			
			if((this.eventTime[0] > this.eventTime[1]) && (now > this.eventTime[1])) {
				this.eventTime[1] = now;
				this.lac[1] = lac;
				this.cellTower[1] = cellTower;
				return false;
			}
			if((this.eventTime[1] > this.eventTime[0]) && (now > this.eventTime[0])) {
				this.eventTime[0] = now;
				this.lac[0] = lac;
				this.cellTower[0] = cellTower;
				return false;
			}
			return true;
		}

		public void reset(long lac, long cellTower, long now) {
			this.lac[0] = lac;
			this.cellTower[0] = cellTower;
			this.eventTime[0] = now;
			this.lac[1] = 0L;
			this.cellTower[1] = 0L;
			this.eventTime[1] = 0L;
		}
		
		public boolean isInSameClockMinute(long now) {
			return  (now - lastSeen) < 60;
		}
	};

	private Map<String, ImsiHistory> imsiHistory;

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

		long lac = atol(lacStr);
		long cellTower = atol(cellTowerStr);
		long eventTimeUTC = atol(eventTimeUTCStr);

		long now = (eventTimeUTC - (eventTimeUTC % 1000)) / 1000;

		ImsiHistory history = imsiHistory.get(imsi);
		
		if(history == null) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("No entry, adding one and returning true");
			}
			if(mode == ReductionMode.MARK_IMSIS_CHANGE_CELL) {
				dt.putFieldValue("accessed", "0");
				dt.putFieldValue("reducted", "OK-FIRSTSEEN");
			}
			history = new ImsiHistory(lac, cellTower, now);
			imsiHistory.put(imsi, history);
			return true;
		}
		
		if(!history.isReductable(lac, cellTower, now)) {
			if(LOG.isDebugEnabled()) {
				LOG.debug("Change of cell tower, returning true");
			}
			if(mode == ReductionMode.MARK_IMSIS_CHANGE_CELL) {
				dt.putFieldValue("accessed", valueOf(history.accessed));
				dt.putFieldValue("reducted", format("OK-LACCELLCHANGE-%s", currentThread().getName()));
			} else if(mode == ReductionMode.IMSIS_CHANGE_CELL_ONLY) {
				if(history.eventTime[0] > history.eventTime[1]) {
					dt.putFieldValue("lastLac", valueOf(history.lac[1]));
					dt.putFieldValue("lastCellTower", valueOf(history.cellTower[1]));
					dt.putFieldValue("lastSeen", Long.toString(history.eventTime[1]));
				} else {
					dt.putFieldValue("lastLac", valueOf(history.lac[0]));
					dt.putFieldValue("lastCellTower", valueOf(history.cellTower[0]));
					dt.putFieldValue("lastSeen", Long.toString(history.eventTime[0]));
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

	static public long atol(String number) {
		long result = 0;
		if(number != null) {
			int maxlength = number.length();
			for(int index = 0; index < maxlength; index++) {
				int value = number.charAt(index) - 0x30;
				result *= 10;
				result += value;
			}
		}
		return result;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(mode == ReductionMode.NONE) {
			LOG.info("NO data reduction in place");
			return;
		}
		LOG.info("Data reduction mode {} based on a change of celltower or a delta of {} seconds", mode.toString(), delta);
		imsiHistory = ChronicleMapBuilder.of(String.class, ImsiHistory.class)
				.averageKeySize(15)
				//.averageValueSize(256)
				.entries(50 * 1024 * 1024)
				.create();
//				(Map<String, ImsiHistory>) CacheBuilder.newBuilder()
//				.expireAfterAccess(cachExpiration, TimeUnit.SECONDS)
//				.initialCapacity(500_000)
//				.build();
		LOG.info("Chronical map created");
	}

	@Override
	public String description() {
		return "filter events seen in the last minute in the same lac/cell";
	}
}
