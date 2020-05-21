package com.dell.rti4t.xd.filter;

import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import net.openhft.chronicle.map.ChronicleMapBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.locker.LockerByValue;

public class FraudDetectionImpl implements EventFilter, InitializingBean {
	
	private static final Logger LOG = LoggerFactory.getLogger(FraudDetectionImpl.class);
	
	private static final int IMSI_TOTAL_MAX = 30 * 1024 * 1024;
	private static final int IMSI_SIZE_IN_CHAR = 15;
	
	public static final String SWAPPED_MSISDN_KEY = "swappedMsisdn";
	public static final String SWAPPED_IMEI_KEY = "swappedImei";
	
	@SuppressWarnings("serial")
	static class FraudInformation implements Serializable {
		String msisdn;
		String imei;
		
		public FraudInformation(String msisdn, String imei) {
			this.msisdn = msisdn;
			this.imei = imei;
		}
		
		@Override
		public boolean equals(Object obj) {
			if(this == obj) {
				return true;
			}
			if (obj == null || obj.getClass() != getClass()) {
				return false;
			}
			FraudInformation other = (FraudInformation)obj;
			return Objects.equals(msisdn, other.msisdn)
					&& Objects.equals(imei, other.imei);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(imei, msisdn);
		}

		public boolean isFraudulent(String msisdn, String imei) {
			if (imei != null && this.imei != null) {
				if(!Objects.equals(this.imei, imei)) {
					return true;
				}
			}
			
			if (msisdn != null && this.msisdn != null) {
				if(!Objects.equals(this.msisdn, msisdn)) {
					return true;
				}
			}
			
			return false;
		}
		
		public FraudInformation assignIfNotNull(String msisdn, String imei) {
			if(msisdn != null) {
				this.msisdn = msisdn;
			}
			if(imei != null) {
				this.imei = imei;
			}
			return this;
		}
		
		protected String emptyIfNull(String value) {
			return value == null ? "" : value;
		}

		public String msisdn() {
			return emptyIfNull(msisdn);
		}

		public String imei() {
			return emptyIfNull(imei);
		}
		
		@Override
		public String toString() {
			return msisdn + "/" + imei;
		}
	}
	
	private Map<String, FraudInformation> imsiToFraud;
	private boolean isFraudDetectionOn = false;
	private LockerByValue lockerByValue = LockerByValue.buildLocker("imsi");

	@Override
	public void afterPropertiesSet() throws Exception {
		if(isFraudDetectionOn) {
			buildImsiToMsisdnMap();
		}
	}
	
	public void setFraudDetection(boolean isFraudDetectionOn) {
		this.isFraudDetectionOn = isFraudDetectionOn;
		LOG.info("Fraud detection is {}", this.isFraudDetectionOn);
	}
	
	private void buildImsiToMsisdnMap() {
		LOG.info("Building imsi to (msisdn/imei) map");
		imsiToFraud = ChronicleMapBuilder.of(String.class, FraudInformation.class)
				.averageKeySize(IMSI_SIZE_IN_CHAR)
				.entries(IMSI_TOTAL_MAX)
				.create();
	}

	@Override
	public boolean accept(DataTransporter dt) {
		if (isFraudDetectionOn) {
			return detectFraud(dt);
		}
		return true;
	}

	private boolean detectFraud(DataTransporter dt) {
		String imsi = dt.getFieldValue("imsi");
		String msisdn = dt.getFieldValue("msisdn");
		String imei = dt.getFieldValue("imei");
		if(nullField(imsi) || (nullField(msisdn) && nullField(imei))) {
			LOG.debug("No imsi [{}] or (msisdn [{}], imei [{}]) - no fraud - returning false", 
					imsi,
					msisdn,
					imei);
			return false;
		}
		
		FraudInformation storeFraud = null;
		
		synchronized(lockerByValue.lock(imsi)) {
			storeFraud = imsiToFraud.get(imsi);
			if (storeFraud == null) {
				imsiToFraud.put(imsi, new FraudInformation(msisdn, imei));
				return false;
			}
		
			// fraud is different msisdn or imei for the same imsi.
			boolean isFraud = storeFraud.isFraudulent(msisdn, imei); 
			if(isFraud) {
				if(LOG.isDebugEnabled()) {
					LOG.debug("Fraud for {}, changed to {}/{}", storeFraud, msisdn, imei);
				}
				dt.putFieldValue(SWAPPED_MSISDN_KEY, storeFraud.msisdn());
				dt.putFieldValue(SWAPPED_IMEI_KEY, storeFraud.imei());
				imsiToFraud.put(imsi, storeFraud.assignIfNotNull(msisdn, imei));
			}
			return isFraud;
		}
	}

	private boolean nullField(String field) {
		return field == null || "null".equals(field);
	}

	@Override
	public String description() {
		return "filter sending out only when a fraud is detected";
	}
}
