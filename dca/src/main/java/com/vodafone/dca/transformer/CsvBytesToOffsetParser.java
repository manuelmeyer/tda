package com.vodafone.dca.transformer;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Maps;

/**
 * Given a string typical of RTI output format including multiple lines
 * will decode it into a list of lists, with each inner list representing a line
 * The elements in the inner list maybe be strings or hashmaps.
 * 
 * @author manuelmeyer
 *
 */
public class CsvBytesToOffsetParser {
	static private final Logger LOG = LoggerFactory.getLogger(CsvBytesToOffsetParser.class);

	private static final int START_OF_VALUE = 0;
	private static final int IN_MAP = 1;
	private static final int IN_MAP_VALUE = 2;
	private static final int OUT_MAP = 4;

	private static final int EXTEND_WHEN_OVERFLOW = 10;
	private static final int NO_OF_FIELDS = 70;
	private static final int NO_OF_EVENTS = 2048;
	
	static class OffsetPool {
		List<Offset> offsets;
		int poolOffset = 0;
		
		public OffsetPool() {
			offsets = new ArrayList<Offset>(NO_OF_FIELDS * NO_OF_EVENTS);
			extendPool(NO_OF_FIELDS * NO_OF_EVENTS);
			LOG.debug("Created {} offset in local", NO_OF_FIELDS * NO_OF_EVENTS);
		}
		
		public void reset() {
			poolOffset = 0;
		}
		
		private void extendPool(int moreToAdd) {
			for(int index = 0; index < moreToAdd; index++) {
				offsets.add(new Offset());
			}
		}
		
		public Offset nextAndSet(WeakReference<byte[]> input, int start, int end) {
			Offset offset;
			if(poolOffset >= offsets.size()) {
				extendPool(EXTEND_WHEN_OVERFLOW);
			} 
			offset = offsets.get(poolOffset++);
			offset.start = start;
			offset.end = end;
			offset.input = input;
			return offset;
		}
	}
	
	static class OffsetKeeper extends ThreadLocal<OffsetPool> {
		@Override
		protected OffsetPool initialValue() {
			return new OffsetPool();
		}
	}
	
	static private OffsetKeeper offsetKeeper = new OffsetKeeper();
	
	public static class Offset {
		
		private static Pattern mapPattern = Pattern.compile("[{ ,]([a-zA-Z.]+)=([^},]*)");
		
		WeakReference<byte[]> input;
		int start;
		int end;
		
		public Offset(WeakReference<byte[]> input, int start, int end) {
			this.input = input;
			this.start = start;
			this.end = end;
		}
		
		public Offset() {
			this.start = 0;
			this.end = 0;
		}
		
		public Object extractContent() {
			if (start >= end) {
				return "";
			}
			return input.get()[start] == '{' 
					? buildMap()
					: buildString();
		}
		
		private String buildString() {
			return new String(input.get(), start, end - start);
		}
		
		private Map<String, Object> buildMap() {
			HashMap<String, Object> map = Maps.newHashMap();
			Matcher matcher = mapPattern.matcher(buildString());
			while(matcher.find()) {
				if(matcher.groupCount() == 2) {
					map.put(matcher.group(1), matcher.group(2));
				}
			}
			return map;
		}
		
		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
					.add("start", start)
					.add("end", end)
					//.add("input", inputToString())
					.toString();
		}
		
		String inputToString() {
			return input == null
					? "<null-input>"
					: input.get() == null 
						? "<null-ref>" 
						: new String(input.get());
		}
	}
	
	/**
	 * Give a csv byte[] will parse it by returning a list of offset(s) (input, start, end) per line.
	 * 
	 * Offset is of type (WeakReference(input), start, end).
	 * 
	 * If input[start] is '{' then it is a map and offset will extract all key=value fields in a map.
	 * Otherwise it is considered as a string.
	 * 
	 */
	public static List<List<Offset>> parse(byte[] input) {
		int state = START_OF_VALUE;
		
		OffsetPool offsetPool = offsetKeeper.get();
		offsetPool.reset();
		
		List<List<Offset>> currentListOfList = new ArrayList<>(NO_OF_EVENTS);
		List<Offset> currentList = new ArrayList<>(NO_OF_FIELDS);
		
		int lastOffset = 0;
		int currentOffset = 0;

		int inputLength = input.length;
		WeakReference<byte[]> inputReference = new WeakReference<>(input);
		
		for (int index = 0; index < inputLength; index++, currentOffset++) {
			byte c = input[index];
			switch (c) {
			case ',':
				if (state == IN_MAP || state == IN_MAP_VALUE) {
					state = IN_MAP;
				} else {
					if (state == OUT_MAP) {
						state = START_OF_VALUE;
					}
					currentList.add(offsetPool.nextAndSet(inputReference, lastOffset, currentOffset));
					lastOffset = currentOffset + 1;
				}
				continue;
			case '{':
				if ((index == 0) || (input[index - 1] == ',')) {
					state = IN_MAP;
					continue;
				}
				break;
			case '}':
				if (state == IN_MAP || state == IN_MAP_VALUE) {
					state = OUT_MAP;
					continue;
				}
			case '=':
				if (state == IN_MAP) {
					state = IN_MAP_VALUE;
					continue;
				}
				break;
			case '\n':
			case '\r':
				currentList.add(offsetPool.nextAndSet(inputReference, lastOffset, currentOffset));
				lastOffset = currentOffset + 1;
				if (currentList.size() > 0) {
					currentListOfList.add(currentList);
					currentList = new ArrayList<>(NO_OF_FIELDS);
				}
				continue;
			}
		} // end of for loop

		// check if string ends with an empty value
		if (input[inputLength - 1] == ',') {
			currentList.add(offsetPool.nextAndSet(inputReference, 0, 0));
		}
		if (lastOffset != currentOffset) {
			currentList.add(offsetPool.nextAndSet(inputReference, lastOffset, currentOffset));
		}
		if (currentList.size() > 0) {
			currentListOfList.add(currentList);
		}
		if (LOG.isDebugEnabled()) {
			for (int index = 0; index < currentListOfList.size(); index++) {
				LOG.debug("List {}/{} contains {} elts", index, currentListOfList.size(),
						currentListOfList.get(index)
								.size());
			}
		}
		return currentListOfList;
	}
}
