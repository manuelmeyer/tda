package com.dell.rti4t.xd.common;

import static com.dell.rti4t.xd.common.AsciiToNumber.atotime;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ReductionMapHandler {
	
	static private Logger LOG = LoggerFactory.getLogger(ReductionMapHandler.class);
	
	// we resend events for the same <lac, cell> after delayBeforeDuplicate on the same cell
	static public long delayBeforeDuplicate = 60;
	static private int initialCapacity = 500_000;
	static private long expireAfterInSec = 7201L;
	static private int concurrencyLevel = 32;
	
	static private Cache<String, ImsiHistory> ismiHistoryMap = emptyCache();
	
	static public void setDelayBeforeDuplicate(int delta) {
		ReductionMapHandler.delayBeforeDuplicate = delta;
	}
	
	private static Cache<String, ImsiHistory> emptyCache() {
		return CacheBuilder.newBuilder().build(); // default cache.
	}

	static public void setInitialCapacity(int initialCapacity) {
		ReductionMapHandler.initialCapacity = initialCapacity;
	}
	
	public static void buildMap() {
		LOG.info("Cache before building is {}", ismiHistoryMap);
		ismiHistoryMap = CacheBuilder.newBuilder()
				.initialCapacity(initialCapacity)
				.expireAfterAccess(expireAfterInSec, TimeUnit.SECONDS)
				//.concurrencyLevel(concurrencyLevel)
				.build();
		LOG.info("Creating a cache of initial capacity {}, concurrency level {}, expire after {} sec(s)", 
									initialCapacity,
									concurrencyLevel,
									expireAfterInSec);
	}
	
	public static ImsiHistory getImsiHistory(String imsi) {
		return ismiHistoryMap.getIfPresent(imsi);
	}

	public static void isInGeofence(String imsi, String timeUTC) {
		ImsiHistory imsiHistory = ismiHistoryMap.getIfPresent(imsi);
		if (imsiHistory != null && imsiHistory.isTimeValid(atotime(timeUTC))) {
			imsiHistory.isGeoFence(true);
		}
	}

	public static boolean isLeavingGeofence(String imsi, String timeUTC) {
		ImsiHistory imsiHistory = ismiHistoryMap.getIfPresent(imsi);
		if (imsiHistory != null && imsiHistory.isTimeValid(atotime(timeUTC))) {
			if(imsiHistory.inGeoFence()) {
				imsiHistory.isGeoFence(false);
				return true;
			}
		}
		return false;
	}

	public static void newImsiHistory(String imsi, long lac, long cellTower, long now) {
		ImsiHistory newEntry = new ImsiHistory(lac, cellTower, now);
		newEntry.isGeoFence(true);
		ismiHistoryMap.put(imsi, newEntry);
	}

	public static boolean isReductable(ImsiHistory history, long lac, long cellTower, long now) {
		return history.isReductable(lac, cellTower, now);
	}
}
