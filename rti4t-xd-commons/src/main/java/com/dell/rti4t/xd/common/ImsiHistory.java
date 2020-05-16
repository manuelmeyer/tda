package com.dell.rti4t.xd.common;

import java.io.Serializable;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class ImsiHistory implements Serializable {
	
	static private final Logger LOG = LoggerFactory.getLogger(ImsiHistory.class);
	
	public short accessed = 0;
	public long[] eventTime = new long[2];
	public long[] lac = new long[2];
	public long[] cellTower = new long[2];
	public long[] previousTimesUTC = new long[2];
	private boolean inGeoFence;
	public long previousLac = -1;
	public long previousCellTower = -1;
	public long previousTimeUTC = -1;
	
	public ImsiHistory(long lac, long cellTower, long now) {
//		LOG.info(" --- created with {} {} {}", lac, cellTower, now);
		this.lac[0] = lac;
		this.cellTower[0] = cellTower;
		this.eventTime[0] = now;
		this.previousTimesUTC[0] = now;
		this.eventTime[1] = -1L;
		this.previousTimeUTC = now;
	}
	
	public synchronized boolean isReductable(long lac, long cellTower, long now) {
		accessed++;
		
//		LOG.info(" --- isReductable called with {} {} {}", lac, cellTower, now);
		
		boolean isCell0 = (this.lac[0] == lac && this.cellTower[0] == cellTower);
		boolean isCell1 = (this.lac[1] == lac && this.cellTower[1] == cellTower);
//		LOG.info(" --- isCell0 {} isCell1 {}", isCell0, isCell1);
		
		if(isCell0 || isCell1) {
			if(isCell0) {
				previousTimesUTC[0] = now;
				if(now > (this.eventTime[0] + ReductionMapHandler.delayBeforeDuplicate)) {
					this.eventTime[0] = now;
//					LOG.info(" --- returning false in 1");
					return false;
				} else {
					if(eventTime[1] > eventTime[0]) {
						this.eventTime[0] = now;
//						LOG.info(" --- returning false in 1.1");
						return false;
					}
				}
			} else if(isCell1) {
				previousTimesUTC[1] = now;
				if(now > (this.eventTime[1] + ReductionMapHandler.delayBeforeDuplicate)) {
					this.eventTime[1] = now;
//					LOG.info(" --- returning false in 2");
					return false;
				} else {
					if(eventTime[0] > eventTime[1]) {
						this.eventTime[1] = now;
//						LOG.info(" --- returning false in 2.1");
						return false;
					}
				}
			}
//			LOG.info(" --- returning true in 1");
			return true;
		}
		
		if((this.eventTime[0] > this.eventTime[1]) && (now > this.eventTime[1])) {
			this.previousTimesUTC[1] = now;
			this.eventTime[1] = now;
			this.lac[1] = lac;
			this.cellTower[1] = cellTower;
//			LOG.info(" --- returning false in 3");
			return false;
		}
		if((this.eventTime[1] > this.eventTime[0]) && (now > this.eventTime[0])) {
			this.previousTimesUTC[0] = now;
			this.eventTime[0] = now;
			this.lac[0] = lac;
			this.cellTower[0] = cellTower;
			//LOG.info(" --- returning false in 4");
			return false;
		}
		//LOG.info(" --- returning true in 2");
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

	@Override
	public String toString() {
		return "ImsiHistory [accessed=" + accessed 
				+ ", previousTimesUTC=" + Arrays.toString(previousTimesUTC) 
				+ ", eventTime=" + Arrays.toString(eventTime) 
				+ ", lac=" + Arrays.toString(lac)
				+ ", cellTower=" + Arrays.toString(cellTower)
				+ ", inGeoFence=" + inGeoFence 
				+ ", lastLac=" + previousLac 
				+ ", lastCellTower=" + previousCellTower + "]";
	}

	public synchronized void isGeoFence(boolean isInGeofence) {
		
		if(isInGeofence) {
			if(!this.inGeoFence) {
				this.inGeoFence = true;
				previousLac = -1;
				previousCellTower = -1;
				return;
			}
		}
		if(eventTime[0] > eventTime[1]) {
			previousLac = lac[0];
			previousCellTower = cellTower[0];
			previousTimeUTC = previousTimesUTC[0];
		} else {
			previousLac = lac[1];
			previousCellTower = cellTower[1];
			previousTimeUTC = previousTimesUTC[1];
		}
		this.inGeoFence = isInGeofence;
	}
	
	public boolean inGeoFence() {
		return inGeoFence;
	}
}
