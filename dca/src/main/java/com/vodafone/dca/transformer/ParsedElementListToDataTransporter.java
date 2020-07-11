package com.vodafone.dca.transformer;

import static com.vodafone.dca.common.FileUtils.getFieldsFromFile;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import com.google.common.collect.Maps;
import com.vodafone.dca.domain.DataTransporter;
import com.vodafone.dca.transformer.CsvBytesToOffsetParser.Offset;

public class ParsedElementListToDataTransporter {
	
	private static final Logger LOG = LoggerFactory.getLogger(ParsedElementListToDataTransporter.class);
	
	private String defaultFilterValue = "data";
	private String filterField;
	private List<String> names;
		
	public void setFilterField(String filterField) {
		if(!StringUtils.isEmpty(filterField)) {
			this.filterField = filterField;
		}
	}	

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
	
	protected void setFieldNames(String[] names) {
		if(names != null && names.length > 0) {
			this.names = Arrays.asList(names);
			LOG.info("Names for DataTransporter are [{}]", this.names);
		}
	}
	
	@PostConstruct
	public void checkHasNames() {
		Assert.notEmpty(names, "field name collection cannot be empty");
	}
	
	public DataTransporter buildFromObjectList(List<Object> objects) {
		return buildFromList(objects, o -> o);
	}
	
	public DataTransporter buildFromOffsetList(List<Offset> objects) {
		return buildFromList(objects, (o) -> o.extractContent());
	}
	
	public <T> DataTransporter buildFromList(List<T> objects, Function<T, Object> transform) {
		Map<String, Object>  map = Maps.newHashMap();	
		IntStream.range(0, names.size())
			.boxed()
			.filter(index -> !names.get(index).startsWith("-"))
			.forEach(index -> addToMap(names.get(index), transform.apply(objects.get(index)), map));
		return new DataTransporter(map, filterValueFromMap(map));
	}
			
	@SuppressWarnings("unchecked")
	private void addToMap(String name, Object value, Map<String, Object> map) {
		if(value instanceof Map) {
			for(Map.Entry<String, Object> entry : ((Map<String, Object>)value).entrySet()) {
				map.put(name + "." + entry.getKey(), entry.getValue());
			}
		} else {
			map.put(name, value);
		}
	}

	protected String filterValueFromMap(Map<String, Object>  map) {
		if(filterField == null) {
			return defaultFilterValue;
		}
		String filterValue = (String)map.get(filterField);
		return (filterValue == null) ? defaultFilterValue : filterValue;
	}
}
