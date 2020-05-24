package com.dell.rti4t.xd.common;

import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class ImsiHistory implements Serializable {
	
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
	}
	
	public abstract boolean isTimeValid(long time);
	
	public abstract boolean isReductable(long lac, long cellTower, long now);
		
	protected abstract void enterGeofence();
	
	protected abstract void followGeofence();

	public void isGeoFence(boolean isInGeofence) {	
		if(isInGeofence) {
			if(!inGeoFence) {
				inGeoFence = true;
				enterGeofence();
//				previousLac = -1;
//				previousCellTower = -1;
//				previousTimeUTC = -1;
				return;
			}
		}
		followGeofence();
//		previousLac = lac;
//		previousCellTower = cellTower;
//		previousTimeUTC = eventTime;
		inGeoFence = isInGeofence;
	}
	
	public boolean inGeoFence() {
		return inGeoFence;
	}
}
