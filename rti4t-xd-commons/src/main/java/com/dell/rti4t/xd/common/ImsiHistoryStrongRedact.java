package com.dell.rti4t.xd.common;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

@SuppressWarnings("serial")
public class ImsiHistoryStrongRedact extends ImsiHistory {
	
	static private final Logger LOG = LoggerFactory.getLogger(ImsiHistoryStrongRedact.class);
	
	enum RedactableState {
		REDACTABLE,
		NON_REDACTABLE,
		NON_REDACTABLE_DELAY_EXPIRED
	}
	
	class LacCellHistory {
		volatile public long eventTime;
		volatile public long lac;
		volatile public long cellTower;
		volatile public long firstSeen;
		
		public boolean isEquals(long lac, long cellTower) {
			return this.lac == lac && this.cellTower == cellTower;
		}

		public void setFields(long lac, long cellTower, long eventTime) {
			this.lac = lac;
			this.cellTower = cellTower;
			this.eventTime = eventTime;
			this.firstSeen = eventTime;
		}
		
		public RedactableState isRedactable(long lac, long cellTower, long now) {
			if(isEquals(lac, cellTower)) {
				eventTime = now;
				if(now > (firstSeen + ReductionMapHandler.delayBeforeDuplicate)) {
					firstSeen = now;
					return RedactableState.NON_REDACTABLE_DELAY_EXPIRED;
				}
				return RedactableState.REDACTABLE;
			}
			return RedactableState.NON_REDACTABLE;
		}
		
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
								.add("eventTime", eventTime)
								.add("lac", lac)
								.add("cellTower", cellTower)
								.add("firstSeen", firstSeen)
								.toString();
		}
	}
	
	volatile LacCellHistory[] lacCellHistory;
	volatile public short accessed = 0;
	volatile private boolean inGeoFence;
	volatile private boolean firstEventInGeoFence;
	
	public ImsiHistoryStrongRedact(long lac, long cellTower, long now) {
		super(lac, cellTower, now);
		this.lacCellHistory = new LacCellHistory[2];
		this.lacCellHistory[0] = new LacCellHistory();
		this.lacCellHistory[1] = new LacCellHistory();
		this.lacCellHistory[0].lac = lac;
		this.lacCellHistory[0].cellTower = cellTower;
		this.lacCellHistory[0].eventTime = now;
		this.lacCellHistory[0].firstSeen = now;
		this.inGeoFence = true;
		this.firstEventInGeoFence = true;
	}
	
	public boolean isTimeValid(long time) {
		return time > eventTime();
	}
	
	private long eventTime() {
		return Math.max(lacCellHistory[0].eventTime, lacCellHistory[1].eventTime);
	}
	
	//
	// true -> event is discarded
	// false -> event is kept
	//
	public boolean isReductable(long lac, long cellTower, long now) {
		accessed++;
		if(now <= eventTime()) {
			return true;
		}

		RedactableState reductableState = lacCellHistory[0].isRedactable(lac, cellTower, now);
		
		if(reductableState == RedactableState.NON_REDACTABLE_DELAY_EXPIRED) {
			return false;
		}
		if(reductableState == RedactableState.REDACTABLE) {
			return true;
		}
		
		reductableState = lacCellHistory[1].isRedactable(lac, cellTower, now);

		if(reductableState == RedactableState.NON_REDACTABLE_DELAY_EXPIRED) {
			return false;
		}
		if(reductableState == RedactableState.REDACTABLE) {
			return true;
		}
		
		assignLacCellHistory(lac, cellTower, now); // new assignment
		return false;
	}
	
	private void assignLacCellHistory(long lac, long cellTower, long now) {
		int nextIndex = lacCellHistory[1].eventTime > lacCellHistory[0].eventTime ? 0 : 1;
		lacCellHistory[nextIndex].setFields(lac, cellTower, now);
	}

	public long previousLac() {
		return firstEventInGeoFence ? -1 : lacCellHistory[0].eventTime > lacCellHistory[1].eventTime ? 
					lacCellHistory[1].lac :
					lacCellHistory[0].lac;

	}

	public long previousCellTower() {
		return firstEventInGeoFence ? -1 : lacCellHistory[0].eventTime > lacCellHistory[1].eventTime ? 
				lacCellHistory[1].cellTower :
				lacCellHistory[0].cellTower;
	}

	public long previousTimeUTC() {
		return firstEventInGeoFence ? -1 : lacCellHistory[0].eventTime > lacCellHistory[1].eventTime ? 
				lacCellHistory[1].eventTime :
				lacCellHistory[0].eventTime;
	}

	@Override
	protected void enterGeofence() {
		firstEventInGeoFence = true;
	}

	@Override
	protected void followGeofence() {
		firstEventInGeoFence = false;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
							.add("inGeoFence", inGeoFence)
							.add("lacCellHistory[0]", lacCellHistory[0])
							.add("lacCellHistory[1]", lacCellHistory[1])
							.toString();
	}
}
