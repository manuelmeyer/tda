package com.dell.rti4t.xd.locker;

import org.springframework.util.ConcurrentReferenceHashMap;


public class LockerByValue {
	
	private static ConcurrentReferenceHashMap<String, XLock> lockMap = new ConcurrentReferenceHashMap<>();
	
	static class XLock {
	}

	public static Object lock(String lockValue) {
		XLock xLock = lockMap.putIfAbsent(lockValue, createXLock());
		if (xLock == null) {
			xLock = lockMap.get(lockValue);
		}
		return xLock;
	}

	private static XLock createXLock() {
		return new XLock();
	}
}
