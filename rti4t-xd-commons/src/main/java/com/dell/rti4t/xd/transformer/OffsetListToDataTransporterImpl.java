package com.dell.rti4t.xd.transformer;

import static com.dell.rti4t.xd.utils.FileUtils.getFieldsFromFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import com.dell.rti4t.xd.csv.CSVToOffsetParser;
import com.dell.rti4t.xd.csv.CSVToOffsetParser.Offset;
import com.dell.rti4t.xd.domain.DataTransporter;

public class OffsetListToDataTransporterImpl implements DataInputParser<Offset, byte[]> {
	
	private static final Logger LOG = LoggerFactory.getLogger(OffsetListToDataTransporterImpl.class);
	
	private List<String> names = new ArrayList<String>();
	private String filterField;
	private String defaultFilterValue = "data";
	
	@Override
	public List<List<Offset>> parse(byte[] input) {
		return CSVToOffsetParser.parse(input);
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public DataTransporter buildFromList(List<Offset> objects) {
		Map<String, Object>  map = new HashMap<String, Object>();
		
		int nameSize = names.size();
		for(int index = 0; index < nameSize; index++) {
			String name = names.get(index);
			if(!name.startsWith("-")) {
				Object value = objects.get(index).extractContent();
				if(value instanceof Map) {
					for(Map.Entry<String, Object> entry : ((Map<String, Object>)value).entrySet()) {
						map.put(name + "." + entry.getKey(), entry.getValue());
					}
				} else {
					map.put(name, value);
				}
			}
		}
		return new DataTransporter(map, filterValueFromMap(map));
	}
	
	protected String filterValueFromMap(Map<String, Object>  map) {
		if(filterField == null) {
			return defaultFilterValue;
		}
		String filterValue = (String)map.get(filterField);
		return (filterValue == null) ? defaultFilterValue : filterValue;
	}
	
	public void setDefaultFilterValue(String defaultFilterValue) {
		if(!StringUtils.isEmpty(defaultFilterValue)) {
			this.defaultFilterValue = defaultFilterValue;
		}
	}
	public String getDefaultFilterValue() {
		return this.defaultFilterValue;
	}
	
	public void setFilterField(String filterField) {
		if(!StringUtils.isEmpty(filterField)) {
			this.filterField = filterField;
		}
	}
	
	public void setFieldNames(String[] names) {
		this.names = Arrays.asList(names);
		LOG.info("Names for DataTransporter are [{}]", this.names);
	}
	
	public void setFieldNamesDefinitionFile(String path) {
		LOG.info("Loading field names defintion from {}", path);
		if(!StringUtils.isEmpty(path)) {
			try {
				setFieldNames(getFieldsFromFile(path));
			} catch(Exception e) {
				LOG.error("Cannot get the field definition from {}", path);
				throw new RuntimeException(e);
			}
		}
	}
}
