package com.vodafone.dca.service;

import static java.util.UUID.randomUUID;
import static org.springframework.util.StringUtils.isEmpty;

import java.util.Date;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import com.vodafone.dca.domain.DataTransporter;

public class PepperManager {
	
	private static final Logger LOG = LoggerFactory.getLogger(PepperManager.class);
		
	@Autowired
	private TaskScheduler taskScheduler;

	private CronTrigger trigger;
	private ScheduledFuture<?> task;
	
	long threshold = 0;
	String[] saltedIntervals = new String[2];
	
	private String pepper = ":envoituresimone";
	private String timeField = "timeUTC";
	private boolean isDynamic = false;
	
	/* 
	 *          t0        t1 
	 * ---------|---------|-----------
	 *    k0       k1
	 * t1 = t0 + delay
	 * e(t) -> t0 + d < delay k = k1
	 * 
	 * 
	 *  t0       t1       t2 
	 * -|--------|--------|-----------
	 *     k1        k2
	 * e(t) -> t0 + d < delay k = k1
	 * 
	 */
	
	public void setCronTrigger(String cronTrigger) {
		if(!isEmpty(cronTrigger)) {
			LOG.info("Setting cronTrigger to {}", cronTrigger);
			trigger = new CronTrigger(cronTrigger);
		}
	}
	
	public void setPepper(String pepper) {
		if(!isEmpty(pepper)) {
			LOG.debug("Setting salt to {}", pepper);
			this.pepper = pepper;
		}
	}
	
	public void setTimeField(String timeField) {
		if(!isEmpty(timeField)) {
			LOG.info("Setting timeField to {}", timeField);
			this.timeField = timeField;
		}
	}
	
	@PostConstruct
	public void startUpdaterIfDynamic() throws Exception {
		if(trigger != null) {
			isDynamic = true;
			task = taskScheduler.schedule(() -> changeKeys(), trigger);
			changeKeys();
		}
	}
	
	private void changeKeys() {
		threshold = now() + task.getDelay(TimeUnit.SECONDS);
		saltedIntervals[0] = ":" + randomUUID().toString();
		saltedIntervals[1] = ":" + randomUUID().toString();
	}
	
	private long now() {
		return new Date().getTime();
	}

	public String getSaltForTime(DataTransporter dt) {
		if(!isDynamic) {
			return pepper;
		}
		return getPepperFor(Long.valueOf(dt.getFieldValue(timeField)));
	}

	private String getPepperFor(Long time) {
		return time < threshold ? saltedIntervals[0] : saltedIntervals[1];
	}
}
