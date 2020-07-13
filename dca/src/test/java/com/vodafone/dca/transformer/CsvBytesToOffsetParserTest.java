package com.vodafone.dca.transformer;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vodafone.dca.transformer.CsvBytesToOffsetParser.Offset;

public class CsvBytesToOffsetParserTest {
	
	private static final Logger LOG = LoggerFactory.getLogger(CsvBytesToOffsetParser.class);

	@Test
	public void canCreateOffsets() {
		String simple = "1a,2b,3c,4d,5e,{informationElements=303=012F030A0080000000000000040055BA06A700067840;303=012F030A0046000000000002830055BA06A700067840;303=012F030A0044400000000000D90055BA06A70006D218;303=012F030A001B400000000000810055BA06A7000A2D78, iuSignalingConnectionId=4294967295, mmCause=255, opc=671091413, lac=205, endTimeUSec=176000, sccpAlcapCause=256, ccCause=255, dpc=671092644, cellId=63430, realImsi=, callNumber=753873991, equipmentId=5, longitude=180.0, mnc=15, firstTmsi=712F805E, processorId=4494, status=1879573144, ranapCause=83, applicationProtocol=238, realMsisdn=, radii90=-1.0, timeoutBits=0, transactionType=65535, pagingCause=FF, mccmnc=23415, latitude=90.0, conditionIndicator=16384, firstLac=-1, subscriberInfo=0, realImei=, callType=2093},{a=2, b=12345},6f\n";
		simple += "11a,21b,31c,41d,51e,{a=2, b=12345, c.d=0-1-2-3-4},61f,\n";
		simple += "aaa\n";
		simple += "\n";
		simple += ",,\n";
		simple += "211a,221b,231c,241d,251e,{a=2, b=12345},261f";
		//String simple = "1a";
		byte[] simpleBytes = simple.getBytes();
		
		List<List<Offset>> offsets = CsvBytesToOffsetParser.parse(simpleBytes);
		LOG.info("Offsets {}", offsets.size());
		int index = 0;
		for(List<Offset> objs : offsets) {
			LOG.info(" -- {} ----------------------------------------------------------------------- ", index++);
			for(Offset offset : objs) {
				Object result = offset.extractContent();
				if(result instanceof Map) {
					LOG.info("Map --- ");
					Map<String, Object> map = (Map)result;
					for(Entry<String, Object> entry : map.entrySet()) {
						LOG.info("{}={}", entry.getKey(), entry.getValue());
					}
					LOG.info("EndMap --- ");
				} else {
					LOG.info("Result = '{}'='{}'", result.getClass().getName(), result);
				}
			}
		}
	}
}
