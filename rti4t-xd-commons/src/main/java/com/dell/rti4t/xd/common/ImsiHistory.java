package com.dell.rti4t.xd.common;

import static com.dell.rti4t.xd.common.ReductionMapHandler.expirationDelta;

import java.io.Serializable;
import java.util.Arrays;

@SuppressWarnings("serial")
public class ImsiHistory implements Serializable {
	
	public long accessed = 0;
	public long[] eventTime = new long[2];
	public long[] lac = new long[2];
	public long[] cellTower = new long[2];
	public long lastSeen;
	public boolean inGeoFence;
	
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
			if(isCell0 && now > (this.eventTime[0] + expirationDelta)) {
				this.eventTime[0] = now;
				return false;
			} else if(isCell1 && now > (this.eventTime[1] + expirationDelta)) {
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
				+ lastSeen + ", inGeoFence=" + inGeoFence + "]";
	}
}
