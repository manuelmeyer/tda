package com.dell.rti4t.xd.common;

import java.io.Serializable;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class ImsiHistory implements Serializable {
	
	static private final Logger LOG = LoggerFactory.getLogger(ImsiHistory.class);
	
	public long accessed = 0;
	public long[] eventTime = new long[2];
	public long[] lac = new long[2];
	public long[] cellTower = new long[2];
	public long lastSeen;
	private boolean inGeoFence;
	public long lastLac = -1;
	public long lastCellTower = -1;
	
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
			if(isCell0 && now > (this.eventTime[0] + ReductionMapHandler.delayBeforeDuplicate)) {
				this.eventTime[0] = now;
				return false;
			} else if(isCell1 && now > (this.eventTime[1] + ReductionMapHandler.delayBeforeDuplicate)) {
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

	@Override
	public String toString() {
		return "ImsiHistory [accessed=" + accessed + ", eventTime="
				+ Arrays.toString(eventTime) + ", lac=" + Arrays.toString(lac)
				+ ", cellTower=" + Arrays.toString(cellTower) + ", lastSeen="
				+ lastSeen + ", inGeoFence=" + inGeoFence + ", lastLac=" + lastLac + ", lastCellTower=" + lastCellTower + "]";
	}

	public void isGeoFence(boolean isInGeofence) {
		
		if(isInGeofence) {
			if(!this.inGeoFence) {
				this.inGeoFence = true;
				lastLac = -1;
				lastCellTower = -1;
				return;
			}
		}
		if(eventTime[0] > eventTime[1]) {
			lastLac = lac[0];
			lastCellTower = cellTower[0];
		} else {
			lastLac = lac[1];
			lastCellTower = cellTower[1];
		}
		this.inGeoFence = isInGeofence;
	}
	
	public boolean inGeoFence() {
		return inGeoFence;
	}
}
