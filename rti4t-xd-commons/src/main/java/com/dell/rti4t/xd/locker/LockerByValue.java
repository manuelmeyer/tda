package com.dell.rti4t.xd.locker;

import java.util.Map;

import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.util.ConcurrentReferenceHashMap.ReferenceType;

import com.google.common.collect.Maps;


public class LockerByValue {
	
	private static Map<String, LockerByValue> lockerMap = Maps.newHashMap();
	private static final int DEFAULT_INITIAL_CAPACITY = 500_000;
	private static final ReferenceType DEFAULT_REFERENCE_TYPE = ReferenceType.SOFT;
	
	private ConcurrentReferenceHashMap<String, XLock> lockMap;
	
	private LockerByValue() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_REFERENCE_TYPE);
	}

	private LockerByValue(ReferenceType referenceType) {
		lockMap = new ConcurrentReferenceHashMap<>(DEFAULT_INITIAL_CAPACITY, referenceType);
	}

	private LockerByValue(int initialCapacity, ReferenceType referenceType) {
		lockMap = new ConcurrentReferenceHashMap<>(initialCapacity, referenceType);
	}
	
	public static LockerByValue buildLocker(String lockerName) {
		LockerByValue lockerByValue = lockerMap.get(lockerName);
		if(lockerByValue == null) {
			lockerByValue = new LockerByValue();
			lockerMap.put(lockerName, lockerByValue);
		}
		return lockerByValue;	
	}
	
	static class XLock {
	}

	public Object lock(String lockValue) {
		XLock xLock = lockMap.putIfAbsent(lockValue, createXLock());
		if (xLock == null) {
			xLock = lockMap.get(lockValue);
		}
		return xLock;
	}

	private static XLock createXLock() {
		return new XLock();
	}

	public void clear() {
		lockMap.clear();
	}
	
	public int size() {
		return lockMap.size();
	}

	public XLock get(String key) {
		return lockMap.get(key);
	}
}
