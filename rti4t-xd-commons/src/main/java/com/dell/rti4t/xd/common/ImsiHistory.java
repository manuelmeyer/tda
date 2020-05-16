package com.dell.rti4t.xd.common;

import java.io.Serializable;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
public class ImsiHistory implements Serializable {
	
	static private final Logger LOG = LoggerFactory.getLogger(ImsiHistory.class);
	
	public short accessed = 0;
	public long eventTime;
	public long lac;
	public long cellTower;
	public long firstSeen;
	private boolean inGeoFence;
	public long previousLac = -1;
	public long previousCellTower = -1;
	public long previousTimeUTC = -1;
	
	public ImsiHistory(long lac, long cellTower, long now) {
		this.lac = lac;
		this.cellTower = cellTower;
		this.eventTime = now;
		this.previousTimeUTC = now;
		this.firstSeen = now;
	}
	
	public boolean isReductable(long lac, long cellTower, long now) {
		accessed++;
		if(now < eventTime) {
			return true;
		}

		boolean isSameLacCell = (this.lac == lac && this.cellTower == cellTower);
		
		if(isSameLacCell) {
			eventTime = now;
			if(now > (firstSeen + ReductionMapHandler.delayBeforeDuplicate)) {
				firstSeen = now;
				return false;
			}
			return true;
		}
//		this.previousLac = this.lac;
//		this.previousCellTower = this.cellTower;
//		this.previousTimeUTC = eventTime;
		
		this.lac = lac;
		this.cellTower = cellTower;
		this.eventTime = now;
		this.firstSeen = now;
		
		return false;
	}

	public void reset(long lac, long cellTower, long now) {
	}

	@Override
	public String toString() {
		return "ImsiHistory [accessed=" + accessed 
				+ ", eventTime=" + eventTime
				+ ", lac=" + lac
				+ ", cellTower=" + cellTower
				+ ", inGeoFence=" + inGeoFence 
				+ ", previousLac=" + previousLac 
				+ ", previousCellTower=" + previousCellTower 
				+ ", previousTimeUTC=" + previousTimeUTC 
				+ "]";
	}

	public void isGeoFence(boolean isInGeofence) {	
		if(isInGeofence) {
			if(!this.inGeoFence) {
				this.inGeoFence = true;
				previousLac = -1;
				previousCellTower = -1;
				previousTimeUTC = -1;
				return;
			}
		}
		this.previousLac = this.lac;
		this.previousCellTower = this.cellTower;
		this.previousTimeUTC = eventTime;
		this.inGeoFence = isInGeofence;
	}
	
	public boolean inGeoFence() {
		return inGeoFence;
	}
}
