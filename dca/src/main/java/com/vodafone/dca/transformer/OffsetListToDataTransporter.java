package com.vodafone.dca.transformer;

import static com.vodafone.dca.common.FileUtils.getFieldsFromFile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.vodafone.dca.domain.DataTransporter;
import com.vodafone.dca.transformer.CSVToOffsetParser.Offset;

public class OffsetListToDataTransporter implements DataInputParser<Offset, byte[]> {
	
	private static final Logger LOG = LoggerFactory.getLogger(OffsetListToDataTransporter.class);
	
	@Value("${dca.input.filter-value:data}")
	private String defaultFilterValue;

	@Value("${dca.input.filter-field:}")
	public void setFilterField(String filterField) {
		if(!StringUtils.isEmpty(filterField)) {
			this.filterField = filterField;
		}
	}	
	private String filterField;
	
	private List<String> names;
		
	@Value("${dca.input.field-definition:}")
	public void setFieldNamesDefinitionFile(String path) {
		if(!StringUtils.isEmpty(path)) {
			try {
				LOG.info("Loading field names defintion from {}", path);
				setFieldNames(getFieldsFromFile(path));
			} catch(Exception e) {
				LOG.error("Cannot get the field definition from {}", path);
				throw new RuntimeException(e);
			}
		}
	}
	
	@Value("${dca.input.field-names:}")
	public void setFieldNames(String[] names) {
		if(names != null && names.length > 0) {
			this.names = Arrays.asList(names);
			LOG.info("Names for DataTransporter are [{}]", this.names);
		}
	}
	
	@PostConstruct
	public void checkHasNames() {
		Assert.notEmpty(names, "field name collection cannot be empty");
	}

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
}
