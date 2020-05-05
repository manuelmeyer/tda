package com.dell.rti4t.xd.transformer;

import static com.google.common.base.Splitter.on;
import static java.lang.String.format;
import static java.nio.ByteBuffer.wrap;
import static java.util.Collections.sort;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.enrich.EventEnricher;

public class IEParserImpl implements EventEnricher {
	
	static final private Logger LOG = LoggerFactory.getLogger(IEParserImpl.class);

	static final private String IE_FIELD = "protocolDetailMap.informationElements";
    private static final char[] HEX = {
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    class IE implements Comparable<IE> {
    	String transactionType;
    	long timestamp;
    	boolean hasTimestamp1 = false;
    	boolean hasTimestamp2 = false;
		String ranapCause;
		String ccCause;
		String mmCause;
		String transStatsSmsCpCause;
		String transStatsSmsRpCause;

		@Override
		public int compareTo(IE o) {
			return Long.compare(this.timestamp, o.timestamp); // we want from oldest to newest
		}
    }

    public static char[] encode(byte b) {
        char[] result = new char[2];
        result[0] = HEX[(0xF0 & b) >>> 4 ];
        result[1] = HEX[(0x0F & b)];
        return result;
    }

	@Override
	public DataTransporter enrich(DataTransporter dt) {
		String ies = dt.getFieldValue(IE_FIELD);
		if(ies == null) {
			return dt;
		}
		List<IE> extracted = extractAllIEs(dt, ies);
		fillDT(dt, extracted);
		return dt;
	}
	
	private void fillDT(DataTransporter dt, List<IE> extracted) { 
		// List is order by oldest first so the transaction type is last
		if(extracted.size() > 0) {
			dt.putFieldValue("protocolDetailMap.transactionType", extracted.get(extracted.size() - 1).transactionType);
			for(IE ie : extracted) {
				if(ie.ranapCause != null) {
					dt.putFieldValue("protocolDetailMap.ranapCause", ie.ranapCause);
				}
				if(ie.ccCause != null) {
					dt.putFieldValue("protocolDetailMap.ccCause", ie.ccCause);
				}
				if(ie.mmCause != null) {
					dt.putFieldValue("protocolDetailMap.mmCause", ie.mmCause);
				}
				if(ie.transStatsSmsCpCause != null) {
					dt.putFieldValue("protocolDetailMap.transStatsSmsCpCause", ie.transStatsSmsCpCause);
				}
				if(ie.transStatsSmsRpCause != null) {
					dt.putFieldValue("protocolDetailMap.transStatsSmsRpCause", ie.transStatsSmsRpCause);
				}
			}
		}		
	}

	private List<IE> extractAllIEs(DataTransporter dt, String ies) {
		List<IE> ieList = new ArrayList<IE>();
		Iterator<String> iterator = on(';').split(ies).iterator();
		while(iterator.hasNext()) {
			String ieStr = iterator.next();
			if(!isValidIE(dt, ieStr)) {
				continue;
			}
			IE ie = ieExtracted(dt, ieStr);
			ieList.add(ie);
		}
		sort(ieList);
		return ieList;
	}

	private IE ieExtracted(DataTransporter dt, String ieStr) {
		try {
			LOG.debug("Looking at {}", ieStr);
			ByteBuffer buff = wrap(decode(ieStr, 4));
			return extract(buff);
		} catch(Exception e) {
			String error = format("Error while extracting fields from [%s], callType [%s]", ieStr, dt.getFieldValue("protocolDetailMap.callType"));
			LOG.error(error, e);
			throw new RuntimeException(error);
		}
	}
	
	protected IE extract(ByteBuffer buff) {
		IE ie = new IE();
		Short ieSet = buff.getShort(); // we already know this info is 301,302 or 303
		LOG.debug("IE is {}", ieSet);
		byte bitMask = buff.get();
		LOG.debug("Bitmak is {}", bitMask);
		if((bitMask & 0x0001) == 0x0001) {
			ie.hasTimestamp1 = true;
		}
		if((bitMask & 0x0002) == 0x0002) {
			ie.hasTimestamp2 = true;
		}
		
		short dataFieldSize = (short)buff.get();
		LOG.debug("dataFieldSize is {}", dataFieldSize);
		return extractDataFields(ie, buff);
	}

	private IE extractDataFields(IE ie, ByteBuffer buff) {
		short transactionType = buff.getShort();
		short status = buff.getShort();
		/*short padding = */buff.get();
		int latency = buff.getInt();
		short ranapMask = (short)buff.get();
		/*
		protocolDetailMap.transactionType,
		protocolDetailMap.ranapCause,
		protocolDetailMap.ccCause,
		protocolDetailMap.mmCause,
		protocolDetailMap.transStatsSmsCpCause,
		protocolDetailMap.transStatsSmsRpCause,
		*/
		//dt.putFieldValue("protocolDetailMap.transactionType", Integer.toHexString(transactionType));
		ie.transactionType = Integer.toHexString(transactionType);

		getRANAPCause(ie, ranapMask, buff);
		getCCCause(ie, ranapMask, buff);
		getMMCause(ie, ranapMask, buff);
		getSMSCPCause(ie, ranapMask, buff);
		getSMSRPCause(ie, ranapMask, buff);
		getTimestamp(ie, buff);

		if(LOG.isDebugEnabled()) {
			LOG.debug("transactionType {}", Integer.toHexString(transactionType));
			LOG.debug("status {}", Integer.toHexString(status));
			LOG.debug("latency {}", Integer.toHexString(latency));
			LOG.debug("mask {}", Integer.toBinaryString((int)ranapMask));
		}
		return ie;
	}

	private void getTimestamp(IE ie, ByteBuffer buff) {
		if(ie.hasTimestamp1) {
			ie.timestamp = (long)buff.getInt() << 32;
		}
		if(ie.hasTimestamp2) {
			ie.timestamp += buff.getInt();
		}
		if(LOG.isDebugEnabled()) {
			LOG.debug("Timestamp is '{}'", String.format("%x", ie.timestamp));
		}
	}

	private void getSMSRPCause(IE ie, short ranapMask, ByteBuffer buff) {
		if((ranapMask & 0x10) != 0x00) {
			//dt.putFieldValue("protocolDetailMap.transStatsSmsRpCause", new String(encode(buff.get())));
			ie.transStatsSmsRpCause = new String(encode(buff.get()));
		}
	}

	private void getSMSCPCause(IE ie, short ranapMask, ByteBuffer buff) {
		if((ranapMask & 0x08) != 0x00) {
			ie.transStatsSmsCpCause = new String(encode(buff.get()));
		}
	}

	private void getMMCause(IE ie, short ranapMask, ByteBuffer buff) {
		if((ranapMask & 0x04) != 0x00) {
			ie.mmCause = new String(encode(buff.get()));
		}
	}

	private void getCCCause(IE ie, short ranapMask, ByteBuffer buff) {
		if((ranapMask & 0x02) != 0x00) {
			ie.ccCause = new String(encode(buff.get()));
		}
	}

	private void getRANAPCause(IE ie, short ranapMask, ByteBuffer buff) {
		if((ranapMask & 0x01) != 0x00) {
			ie.ranapCause = new String(encode(buff.get()));
		}
	}

	protected boolean isValidIE(DataTransporter dt, String ieStr) {
		if(ieStr == null || ieStr.length() < 4 || ieStr.charAt(0) != '3' || ieStr.charAt(1) != '0' && ieStr.charAt(3) != '=') {
			return false;
		}
		//
	    // Possible dataId, callType (dec value) combined
	    // 301 0x821 (2081)
	    // 302 0x82b (2091)
	    // 303 0x82d (2093)
	    //
		String callType = dt.getFieldValue("protocolDetailMap.callType");

		char ie3 = ieStr.charAt(2);
		
		switch(ie3) {
		case '1' :
			return "2081".equals(callType);
		case '2' :
			return "2091".equals(callType);
		case '3' :
			return "2093".equals(callType);
		default :
			return false;
		}
	}
	
	protected byte[] decode(String s, int offset) {
		String toDecode = s.substring(offset);
        int nChars = toDecode.length();
        if (nChars % 2 != 0) {
        	LOG.error("String {} has a invalid number of chars", toDecode);
            throw new IllegalArgumentException("Hex-encoded string must have an even number of characters");
        }
        byte[] result = new byte[nChars/ 2];
        for (int i = 0, j = 0; i < nChars; i += 2, j++) {
            int msb = Character.digit(toDecode.charAt(i), 16);
            int lsb = Character.digit(toDecode.charAt(i + 1), 16);

            if (msb < 0 || lsb < 0) {
                throw new IllegalArgumentException("Non-hex character in input: " + s);
            }
            result[j] = (byte) ((msb << 4) | lsb);
        }
        return result;
	}
}
