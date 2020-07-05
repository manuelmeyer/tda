package com.vodafone.dca.reduction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;


@SuppressWarnings("serial")
public class ImsiHistorySimpleDedup extends ImsiHistory {
	
	static private final Logger LOG = LoggerFactory.getLogger(ImsiHistorySimpleDedup.class);
	
	volatile public long eventTime;
	volatile public long lac;
	volatile public long cellTower;
	volatile public long firstSeen;
	volatile private boolean inGeoFence;
	volatile public long previousLac = -1;
	volatile public long previousCellTower = -1;
	volatile public long previousTimeUTC = -1;
	
	public ImsiHistorySimpleDedup(long lac, long cellTower, long now, int delayBeforeDuplicate) {
		super(lac, cellTower, now, delayBeforeDuplicate);
		this.lac = lac;
		this.cellTower = cellTower;
		this.eventTime = now;
		this.previousTimeUTC = now;
		this.firstSeen = now;
	}
	
	public boolean isTimeValid(long time) {
		return time > eventTime;
	}
	
	public synchronized boolean isDuplicated(long lac, long cellTower, long now) {
		accessed++;
		if(now <= eventTime) {
			return true;
		}

		boolean isSameLacCell = (this.lac == lac && this.cellTower == cellTower);
		
		if(isSameLacCell) {
			eventTime = now;
			if(now > (firstSeen + delayBeforeDuplicate)) {
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

	@Override
	protected void enterGeofence() {
		previousLac = -1;
		previousCellTower = -1;
		previousTimeUTC = -1;
	}

	@Override
	protected void followGeofence() {
		previousLac = lac;
		previousCellTower = cellTower;
		previousTimeUTC = eventTime;
	}
	
	@Override
	public long previousLac() {
		return previousLac;
	}

	@Override
	public long previousCellTower() {
		return previousCellTower;
	}

	@Override
	public long previousTimeUTC() {
		return previousTimeUTC;
	}

	@Override
	public long lac() {
		return lac;
	}

	@Override
	public long cellTower() {
		return cellTower;
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
