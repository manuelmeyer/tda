package com.dell.rti4t.xd.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AsciiToNumber {
	
	private static final Logger LOG = LoggerFactory.getLogger(AsciiToNumber.class);
	
	static public long atol(String number) {
		long result = 0;
		if(number != null) {
			int maxlength = number.length();
			for(int index = 0; index < maxlength; index++) {
				int value = number.charAt(index) - 0x30;
				result *= 10;
				result += value;
			}
		}
		return result;
	}
	
	static public long atotime(String time) {
		if(time == null || time.length() < 4) {
			LOG.error("time {} too short to be converted, assuming it is 0", time);
			return 0;
		}
		return atol(time.substring(0, time.length() - 3));
	}

}
