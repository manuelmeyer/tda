package com.vodafone.dca.reduction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

@SuppressWarnings("serial")
public class ImsiHistoryStrongDedup extends ImsiHistory {
	
	static private final Logger LOG = LoggerFactory.getLogger(ImsiHistoryStrongDedup.class);
	
	enum DuplicateState {
		// imsi was seen on the last 2 recorded <lac, cell>
		DUPLICATED,
		// imsi was not seen on the last 2 recorded <lac, cell>
		NON_DUPLICATED,
		// imsi was seen on the last 2 recorded <lac, cell> but kept the same position 
		// for more than ReductionMapHandler.delayBeforeDuplicate seconds.
		DUPLICATED_WITH_EXPIRED_IDLE_TIME
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
		
		public DuplicateState isRedactable(long lac, long cellTower, long now) {
			if(isEquals(lac, cellTower)) {
				eventTime = now;
				// TODO reference the map handler
//				if(now > (firstSeen + ReductionMapHandler.delayBeforeDuplicate)) {
//					firstSeen = now;
//					return DuplicateState.DUPLICATED_WITH_EXPIRED_IDLE_TIME;
//				}
				return DuplicateState.DUPLICATED;
			}
			return DuplicateState.NON_DUPLICATED;
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

		public void setLacCell(int lac, int cell) {
			this.lac = lac;
			this.cellTower = cell;
		}
	}
	
	volatile LacCellHistory[] lacCellHistory;
	volatile private boolean firstEventInGeoFence;
	
	public ImsiHistoryStrongDedup(long lac, long cellTower, long now) {
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
	public synchronized boolean isDuplicated(long lac, long cellTower, long now) {
		accessed++;
		if(now <= eventTime()) {
			return true;
		}

		DuplicateState reductableState = lacCellHistory[0].isRedactable(lac, cellTower, now);
		
		if(reductableState == DuplicateState.DUPLICATED_WITH_EXPIRED_IDLE_TIME) {
			if(firstEventInGeoFence) {
				lacCellHistory[1].setLacCell(-1 , -1);
				firstEventInGeoFence = false;
			}
			return false;
		}
		if(reductableState == DuplicateState.DUPLICATED) {
			return true;
		}
		
		reductableState = lacCellHistory[1].isRedactable(lac, cellTower, now);

		if(reductableState == DuplicateState.DUPLICATED_WITH_EXPIRED_IDLE_TIME) {
			if(firstEventInGeoFence) {
				//lacCellHistory[0].setLacCell(-1 , -1);
				firstEventInGeoFence = false;
			}
			return false;
		}
		if(reductableState == DuplicateState.DUPLICATED) {
			return true;
		}
		
		int assigned = assignLacCellHistory(lac, cellTower, now); // new assignment
		if(firstEventInGeoFence) {
			//lacCellHistory[assigned == 0 ? 1 : 0].setLacCell(-1 , -1);
			firstEventInGeoFence = false;
		}
		return false;
	}
	
	private int assignLacCellHistory(long lac, long cellTower, long now) {
		int nextIndex = lacCellHistory[1].eventTime > lacCellHistory[0].eventTime ? 0 : 1;
		lacCellHistory[nextIndex].setFields(lac, cellTower, now);
		return nextIndex;
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
	public long lac() {
		return firstEventInGeoFence ? -1 : lacCellHistory[0].eventTime > lacCellHistory[1].eventTime ? 
				lacCellHistory[0].lac :
				lacCellHistory[1].lac;
	}

	@Override
	public long cellTower() {
		return firstEventInGeoFence ? -1 : lacCellHistory[0].eventTime > lacCellHistory[1].eventTime ? 
				lacCellHistory[0].cellTower :
				lacCellHistory[1].cellTower;
	}


	@Override
	protected void enterGeofence() {
		firstEventInGeoFence = true;
	}

	@Override
	protected void followGeofence() {
		//firstEventInGeoFence = false;
	}
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
							.add("inGeoFence", inGeoFence)
							.add("firstEventInGeoFence", firstEventInGeoFence)
							.add("lacCellHistory[0]", lacCellHistory[0])
							.add("lacCellHistory[1]", lacCellHistory[1])
							.toString();
	}
}
