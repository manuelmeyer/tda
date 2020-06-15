package com.dell.rti4t.xd.csv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a string typical of RTI output format including multiple lines
 * will decode it into a list of lists, with each inner list representing a line
 * The elements in the inner list maybe be strings or hashmaps.
 * 
 * @author manuelmeyer
 *
 */
public class CSVToObjectParser {
	static private final Logger LOG = LoggerFactory.getLogger(CSVToObjectParser.class);

	private static final int START_OF_VALUE = 0;
	private static final int IN_MAP = 1;
	private static final int IN_MAP_VALUE = 2;
	private static final int OUT_MAP = 4;

	private static final int NO_OF_FIELDS = 250;
	private static final int NO_OF_EVENTS = 2048;

	static private ThreadLocal<StringBuilder> threadLocalValue = new ThreadLocal<StringBuilder>();
	static private ThreadLocal<StringBuilder> threadLocalName = new ThreadLocal<StringBuilder>();
	
	/**
	 * Give a string will parse it placing sets within {} in a hashmap and other
	 * values in the list
	 * This is threadsafe
	 * 
	 * @param input
	 * @return
	 */
	public static List<List<Object>> parse(String input) {
		int state = START_OF_VALUE;

		StringBuilder value = threadLocalValue.get();
		if (value == null) {
			value = new StringBuilder();
			threadLocalValue.set(value);
		}

		StringBuilder name = threadLocalName.get();
		if (name == null) {
			name = new StringBuilder();
			threadLocalName.set(name);
		}

		Map<String, String> currentMap = null;
		List<List<Object>> currentListOfList = new ArrayList<List<Object>>(NO_OF_EVENTS);
		List<Object> currentList = new ArrayList<Object>(NO_OF_FIELDS);

		int length = input.length();
		for (int index = 0; index < length; index++) {
			char c = input.charAt(index);
			switch (c) {
			case ',':
				if (state == IN_MAP || state == IN_MAP_VALUE) {
					currentMap.put(name.toString(), value.toString());
					name.setLength(0);
					state = IN_MAP;
				} else {
					if (state == OUT_MAP) {
						state = START_OF_VALUE;
					} else {
						currentList.add(value.toString());
					}
				}
				value.setLength(0);
				continue;
			case '{':
				if ((index == 0) || (input.charAt(index - 1) == ',')) {
					state = IN_MAP;
					// name = new StringBuilder();
					name.setLength(0);
					currentMap = new HashMap<String, String>();
					continue;
				}
				break;
			case '}':
				if (state == IN_MAP || state == IN_MAP_VALUE) {
					currentMap.put(name.toString(), value.toString());
					value.setLength(0);
					state = OUT_MAP;
					currentList.add(currentMap);
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
				if (value.length() > 0) {
					currentList.add(value.toString());
					value.setLength(0);
				} else if (currentList.size() > 0) { // add empty string if a
														// line ends with a ,
					currentList.add("");
				}
				if (currentList.size() > 0) {
					if (LOG.isDebugEnabled()) {
						LOG.debug("CR found, adding a list of {} to the list of list", currentList.size());
					}
					currentListOfList.add(currentList);
				}
				currentList = new ArrayList<Object>(NO_OF_FIELDS);
				continue;
			}

			switch (state) {
			case IN_MAP:
				if (c != ' ') {
					name.append(c);
				}
				break;
			case IN_MAP_VALUE:
			default:
				value.append(c);
			}
		} // end of for loop

		// check if string ends with an empty value
		if (input.charAt(length - 1) == ',') {
			currentList.add("");
		}
		if (value.length() > 0) {
			currentList.add(value.toString());
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
		value.setLength(0);
		name.setLength(0);
		return currentListOfList;
	}
}
