package com.dell.rti4t.xd.common;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class ReductionMapHandler {
	
	static private Logger LOG = LoggerFactory.getLogger(ReductionMapHandler.class);
	
	static public long expirationDelta = 60_000;
	static private int initialCapacity = 500_000;
	static private long expireAfterInSec = 7201L;
	static private int concurrencyLevel = 32;
	
	static private Cache<String, ImsiHistory> ismiHistoryMap = emptyCache();
	
	static public void setExpirationDelta(int delta) {
		ReductionMapHandler.expirationDelta = delta;
	}
	
	private static Cache<String, ImsiHistory> emptyCache() {
		return CacheBuilder.newBuilder().build(); // default cache.
	}

	static public void setInitialCapacity(int initialCapacity) {
		ReductionMapHandler.initialCapacity = initialCapacity;
	}
	
	public static void buildMap() {
		ismiHistoryMap = CacheBuilder.newBuilder()
				.initialCapacity(initialCapacity)
				.expireAfterAccess(expireAfterInSec, TimeUnit.SECONDS)
				.concurrencyLevel(concurrencyLevel)
				.build();
		LOG.info("Creating a cache of initial capacity {}, concurrency level {}, expire after {} sec(s)", 
									initialCapacity,
									concurrencyLevel,
									expireAfterInSec);
	}
	
	public static ImsiHistory getImsiHistory(String imsi) {
		return ismiHistoryMap.getIfPresent(imsi);
	}

	public static void isInGeofence(String imsi) {
		ImsiHistory imsiHistory = ismiHistoryMap.getIfPresent(imsi);
		if (imsiHistory != null) {
			imsiHistory.inGeoFence = true;
		}
	}

	public static boolean isLeavingGeofence(String imsi) {
		ImsiHistory imsiHistory = ismiHistoryMap.getIfPresent(imsi);
		if (imsiHistory != null) {
			if(imsiHistory.inGeoFence) {
				imsiHistory.inGeoFence = false;
				return true;
			}
		}
		return false;
	}

	public static void newImsiHistory(String imsi, long lac, long cellTower, long now) {
		ismiHistoryMap.put(imsi, new ImsiHistory(lac, cellTower, now));
	}

	public static boolean isReductable(ImsiHistory history, long lac, long cellTower, long now) {
		return history.isReductable(lac, cellTower, now);
	}
}
