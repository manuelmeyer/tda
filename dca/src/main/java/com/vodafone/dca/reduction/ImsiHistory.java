package com.vodafone.dca.reduction;

import java.io.Serializable;

@SuppressWarnings("serial")
public abstract class ImsiHistory implements Serializable {
	
	volatile protected boolean inGeoFence;
	volatile public short accessed;
	protected int delayBeforeDuplicate;
	
	public abstract long previousLac();
	public abstract long previousCellTower();
	public abstract long previousTimeUTC();

	public abstract boolean isTimeValid(long time);	
	public abstract boolean isDuplicated(long lac, long cellTower, long now);
		
	protected abstract void enterGeofence();
	protected abstract void followGeofence();
	
	public ImsiHistory(long lac, long cellTower, long now, int delayBeforeDuplicate) {
		this.inGeoFence = true;
		this.accessed = 0;
		this.delayBeforeDuplicate = delayBeforeDuplicate;
	}
	
	public void isGeoFence(boolean isInGeofence) {	
		if(isInGeofence) {
			if(!inGeoFence) {
				inGeoFence = true;
				enterGeofence();
				return;
			}
		}
		inGeoFence = isInGeofence;
		followGeofence();
	}
	
	public boolean inGeoFence() {
		return inGeoFence;
	}
	
	public abstract long lac();
	public abstract long cellTower();
	
}
