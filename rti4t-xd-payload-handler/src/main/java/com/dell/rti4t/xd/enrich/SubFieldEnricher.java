package com.dell.rti4t.xd.enrich;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.enrich.EventEnricher;
/*
 * extract the country code from the imsi and add it as a field
 */
public class SubFieldEnricher implements EventEnricher {
	// hard wired members so far as we are using it specifically for imsis
	private int subFieldLength = 5;
	private int subFieldOffset = 0;
	private int minFieldLength = (subFieldLength + subFieldOffset);
	
	private String sourceField = "imsi";
	private String targetField = "countryCode";

	public void setSourceField(String sourceField) {
		this.sourceField = sourceField;
	}
	public void setTargetField(String targetField) {
		this.targetField = targetField;
	}
	public void setSubFieldLength(int countryCodeLength) {
		this.subFieldLength = countryCodeLength;
		this.minFieldLength = countryCodeLength + subFieldOffset;
	}
	public void setSubFieldOffset(int subFieldOffset) {
		this.subFieldOffset = subFieldOffset;
		this.minFieldLength = subFieldLength + subFieldOffset;
	}

	@Override
	public DataTransporter enrich(DataTransporter dt) {
		String imsi = dt.getFieldValue(sourceField);
		if(imsi == null || imsi.length() < minFieldLength) {
			return dt;
		}
		dt.putFieldValue(targetField, imsi.substring(subFieldOffset, subFieldLength));
		return dt;
	}
}
