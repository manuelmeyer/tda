package com.dell.rti4t.xd.transformer;

import static java.lang.Thread.sleep;
import static java.util.UUID.randomUUID;
import static org.springframework.util.StringUtils.isEmpty;

import java.util.Date;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.scheduling.support.SimpleTriggerContext;

import com.dell.rti4t.xd.domain.DataTransporter;

public class PepperManagerImpl implements InitializingBean, PepperManager {
	
	private static final Logger LOG = LoggerFactory.getLogger(PepperManagerImpl.class);
		
	class PepperContext {
		long threshold = 0;
		String[] saltedIntervals = new String[2];
		
		PepperContext() {
			saltedIntervals[0] = ":" + randomUUID().toString();
			saltedIntervals[1] = ":" + randomUUID().toString();
		}
		public PepperContext(long threshold, PepperContext pepperContext) {
			this.threshold = threshold;
			this.saltedIntervals[0] = pepperContext.saltedIntervals[1];
			saltedIntervals[1] = ":" + randomUUID().toString();
		}
		
		public PepperContext newThresholdContext(long threshold) {
			return new PepperContext(threshold, this);
		}
		
		String getPepperFor(long timeUTC) {
			LOG.info("value {} threshold {} saltedIntervals {}", timeUTC, threshold, timeUTC < threshold ? 0 : 1);
			return timeUTC < threshold ? saltedIntervals[0] : saltedIntervals[1];
		}
		
		public String toString() {
			return new StringBuffer()
				.append("Threshold=[").append(new DateTime(threshold).toString()).append("],")
				.append(" in " + diffSeconds() + " sec(s) ")
				.append("key0=[").append(blur(saltedIntervals[0])).append("],")
				.append("key1=[").append(blur(saltedIntervals[1])).append("]")
				.toString();
		}
		
		private String diffSeconds() {
			 long ms = ((new DateTime(threshold).getMillis() - DateTime.now().getMillis()));
			 ms /= 1000;
			 return String.valueOf(ms);
		}
		private Object blur(String value) {
			return value.substring(0, 5) + "***********************";
		}
	}

	class PepperUpdater implements Runnable {
		@Override
		public void run() {
			for(;;) {
				try {
					sleep(1000);
					DateTime now = new DateTime();
					if(now.isAfter(nextChange)) {
						LOG.info("Rebuilding the interval");
						buildIntervals();
					}
				} catch(InterruptedException e) {
					return;
				}
			}
		}
	}

	private String pepper = ":envoituresimone";
	private String timeField = "timeUTC";
	private boolean isDynamic = false;
	private DateTime nextChange;
	private CronTrigger trigger;
	private PepperContext pepperContext;

	
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
	
	@Override
	public void afterPropertiesSet() throws Exception {
		if(trigger != null) {
			isDynamic = true;
			pepperContext = new PepperContext();
			buildIntervals();
			new Thread(new PepperUpdater()).start();
		}
	}
	
	private void buildIntervals() {
		Date nextExecutionTime = trigger.nextExecutionTime(new SimpleTriggerContext());
		long threshold = nextExecutionTime.getTime();
		pepperContext = pepperContext.newThresholdContext(threshold);
		LOG.info("PepperConext is now {}", pepperContext);
		nextExecutionTime = trigger.nextExecutionTime(new SimpleTriggerContext(nextExecutionTime, nextExecutionTime, nextExecutionTime));
		long duration = nextExecutionTime.getTime() - threshold;
		nextChange = new DateTime(threshold + (duration - duration/4));
		if(LOG.isDebugEnabled()) {
			LOG.debug("Duration={}, Next key change={}, Next buildIntervals()={}", 
					duration, 
					nextExecutionTime, 
					nextChange);
		}
	}

	@Override
	public String getSaltForTime(DataTransporter dt) {
		if(!isDynamic) {
			return pepper;
		}
		String timeUTCField = dt.getFieldValue(timeField);
		LOG.info(" -- Value is {}", timeField);
		long timeUTC = Long.valueOf(timeUTCField);
		return pepperContext.getPepperFor(timeUTC);
	}
}
