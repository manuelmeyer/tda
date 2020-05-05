package com.dell.rti4t.xd.locker;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dell.rti4t.xd.locker.LockerByValue.XLock;
import com.google.common.base.Stopwatch;

public class TestLockerByValue {
	
	private static final Logger LOG = LoggerFactory.getLogger(TestLockerByValue.class);
	
	class T1 implements Runnable { public void run() { thread1(); } }
	
	class T2 implements Runnable { public void run() { thread2(); } }
	
	class T3 implements Runnable { public void run() { thread3(); } }
	
	CountDownLatch latch = new CountDownLatch(3);
	
	long thread1Time = 0;
	long thread2Time = 0;
	long thread3Time = 0;
	
	@Test
	public void canLockByValue() throws Exception {
		new Thread(new T1()).start();
		new Thread(new T2()).start();
		new Thread(new T3()).start();
		
		assertTrue(latch.await(10L, TimeUnit.SECONDS));
		assertTrue(Math.abs(thread2Time + thread1Time) >= 1000);
		assertTrue(thread3Time < 200);
	}
	
	int totalLoop = 300;
	int totalSleep = 10;
	
	@Test
	public void canBeEquals() {
		XLock l1 = new LockerByValue.XLock();
		XLock l2 = new LockerByValue.XLock();
		
		assertEquals(l1, l1);
		assertNotEquals(l1, l2);
		
		assertEquals(l1.hashCode(), l1.hashCode());
		assertNotEquals(l1.hashCode(), l2.hashCode());
		
		l2 = l1;
		
		assertEquals(l1, l2);
		assertEquals(l1.hashCode(), l2.hashCode());
	}
	
	@Test
	public void performances() throws Exception {
		long with = fwith();
		long without = fwithout();
		
		long delta = with - without;
		double msPerCall = (double)delta/(double)totalLoop;
		
		LOG.info("delta with/without is {}, {} ms per call, {} % of overhead", with - without, 
				String.format("%.5f", msPerCall),
				String.format("%.2f", 100.00 * (double)with/(double)without - 100.0)
				);
		
		assertTrue(msPerCall <= 0.5); // a bit random...
	}
	
	long fwith() throws Exception {
		LOG.info("starting with");
		Stopwatch stopWatch = Stopwatch.createStarted();
		for(int index = 0; index < totalLoop; index++) {
			synchronized(LockerByValue.lock("2222" + index)) {
				Thread.sleep(totalSleep);
			}
		}
		LOG.info("TotalSpent {}", stopWatch.elapsed(TimeUnit.MILLISECONDS));
		return stopWatch.elapsed(TimeUnit.MILLISECONDS);
	}
	
	long fwithout() throws Exception {
		LOG.info("starting without");
		Stopwatch stopWatch = Stopwatch.createStarted();
		for(int index = 0; index < totalLoop; index++) {
			Thread.sleep(totalSleep);
		}
		LOG.info("TotalSpent {}", stopWatch.elapsed(TimeUnit.MILLISECONDS));
		return stopWatch.elapsed(TimeUnit.MILLISECONDS);
	}

	
	public void thread1() {
		Stopwatch stopWatch = Stopwatch.createStarted();
		LOG.info("1 - Waiting to get in");
		synchronized(LockerByValue.lock("1234")) {
			LOG.info("1 - In the thread");
			try { Thread.sleep(500); } catch(Exception e) {}
			LOG.info("1 - Leaving the thread");
		}
		thread1Time = stopWatch.elapsed(TimeUnit.MILLISECONDS);
		latch.countDown();
	}
	
	public void thread2() {
		Stopwatch stopWatch = Stopwatch.createStarted();
		LOG.info("2 - Waiting to get in");
		synchronized(LockerByValue.lock("1234")) {
			LOG.info("2 - In the thread");
			try { Thread.sleep(500); } catch(Exception e) {}
			LOG.info("2 - Leaving the thread");
		}
		thread2Time = stopWatch.elapsed(TimeUnit.MILLISECONDS);
		latch.countDown();
	}
	
	public void thread3() {
		Stopwatch stopWatch = Stopwatch.createStarted();
		LOG.info("3 - Waiting to get in");
		synchronized(LockerByValue.lock("0000")) {
			LOG.info("3 - In the thread");
			try { Thread.sleep(100); } catch(Exception e) {}
			LOG.info("3 - Leaving the thread");
		}
		thread3Time = stopWatch.elapsed(TimeUnit.MILLISECONDS);
		latch.countDown();
	}
}
