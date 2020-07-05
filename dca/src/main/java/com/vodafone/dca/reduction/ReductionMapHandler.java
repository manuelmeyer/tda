package com.vodafone.dca.reduction;

import static com.vodafone.dca.reduction.AsciiToNumber.atotime;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ReductionMapHandler {
	
	static private Logger LOG = LoggerFactory.getLogger(ReductionMapHandler.class);
	
	// we resend events for the same <lac, cell> after delayBeforeDuplicate on the same cell
	public int delayBeforeDuplicate = 60;
	private int initialCapacity = 500_000;
	private long expireAfterInSec = 7201L;
	private int concurrencyLevel = 32;
	protected boolean useSimpleDedup = true;
	
	private Cache<String, ImsiHistory> ismiHistoryMap;
	
	public void useSimpleDedup() {
		useSimpleDedup = true;
	}
	
	public void useStrongDedup() {
		useSimpleDedup = false;
	}
	
	public void setDelayBeforeDuplicate(int delta) {
		this.delayBeforeDuplicate = delta;
	}
	
	public void setInitialCapacity(int initialCapacity) {
		this.initialCapacity = initialCapacity;
	}
	
	@PostConstruct
	public void buildMap() {
		LOG.info("Cache before building is {}", ismiHistoryMap);
		ismiHistoryMap = CacheBuilder.newBuilder()
				.initialCapacity(initialCapacity)
				.expireAfterAccess(expireAfterInSec, TimeUnit.SECONDS)
				//.concurrencyLevel(concurrencyLevel)
				.build();
		LOG.info("Creating a cache for {} dedup, of initial capacity {}, unaccessed data expires after {} sec(s)",
									useSimpleDedup 
											? "simple" 
											: " strong",
									initialCapacity,
									concurrencyLevel,
									expireAfterInSec);
	}
	
	public ImsiHistory getImsiHistory(String imsi) {
		return ismiHistoryMap.getIfPresent(imsi);
	}

	public void isInGeofence(String imsi, String timeUTC) {
		ImsiHistory imsiHistory = ismiHistoryMap.getIfPresent(imsi);
		if (imsiHistory != null && imsiHistory.isTimeValid(atotime(timeUTC))) {
			imsiHistory.isGeoFence(true);
		}
	}

	public boolean isLeavingGeofence(String imsi, String timeUTC) {
		ImsiHistory imsiHistory = ismiHistoryMap.getIfPresent(imsi);
		if (imsiHistory != null && imsiHistory.isTimeValid(atotime(timeUTC))) {
			if(imsiHistory.inGeoFence()) {
				imsiHistory.isGeoFence(false);
				return true;
			}
		}
		return false;
	}

	public ImsiHistory newImsiHistory(String imsi, long lac, long cellTower, long now) {
		ImsiHistory newEntry = useSimpleDedup 
				? new ImsiHistorySimpleDedup(lac, cellTower, now, delayBeforeDuplicate)
				: new ImsiHistoryStrongDedup(lac, cellTower, now, delayBeforeDuplicate);
		newEntry.isGeoFence(true);
		ismiHistoryMap.put(imsi, newEntry);
		return newEntry;
	}

	public boolean isDuplicated(ImsiHistory history, long lac, long cellTower, long now) {
		return history.isDuplicated(lac, cellTower, now);
	}
}
