package com.dell.rti4t.xd.common;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

@SuppressWarnings("serial")
public class ImsiHistory implements Serializable {
	
	static private final Logger LOG = LoggerFactory.getLogger(ImsiHistory.class);
	
	volatile public short accessed = 0;
	volatile public long eventTime;
	volatile public long lac;
	volatile public long cellTower;
	volatile public long firstSeen;
	volatile private boolean inGeoFence;
	volatile public long previousLac = -1;
	volatile public long previousCellTower = -1;
	volatile public long previousTimeUTC = -1;
	
	public ImsiHistory(long lac, long cellTower, long now) {
		this.lac = lac;
		this.cellTower = cellTower;
		this.eventTime = now;
		this.previousTimeUTC = now;
		this.firstSeen = now;
	}
	
	public boolean isTimeValid(long time) {
		return time > eventTime;
	}
	
	public boolean isReductable(long lac, long cellTower, long now) {
		accessed++;
		if(now <= eventTime) {
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
		
		this.lac = lac;
		this.cellTower = cellTower;
		this.eventTime = now;
		this.firstSeen = now;
		
		return false;
	}

	public void isGeoFence(boolean isInGeofence) {	
		if(isInGeofence) {
			if(!inGeoFence) {
				inGeoFence = true;
				previousLac = -1;
				previousCellTower = -1;
				previousTimeUTC = -1;
				return;
			}
		}
		previousLac = lac;
		previousCellTower = cellTower;
		previousTimeUTC = eventTime;
		inGeoFence = isInGeofence;
	}
	
	public boolean inGeoFence() {
		return inGeoFence;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
							.add("eventTime", eventTime)
							.add("lac", lac)
							.add("cellTower", cellTower)
							.add("inGeoFence", inGeoFence)
							.add("previousLac", previousLac )
							.add("previousCellTower", previousCellTower) 
							.add("previousTimeUTC", previousTimeUTC) 
							.toString();
	}
}
