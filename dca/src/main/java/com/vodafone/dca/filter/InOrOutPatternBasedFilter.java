package com.vodafone.dca.filter;

import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.core.GenericSelector;
import org.springframework.util.CollectionUtils;

import com.google.common.collect.Lists;
import com.vodafone.dca.domain.DataTransporter;

public class InOrOutPatternBasedFilter implements GenericSelector<DataTransporter> {
	
	private static final Logger LOG = LoggerFactory.getLogger(InOrOutPatternBasedFilter.class);
	
	private List<String> patterns = Lists.newArrayList();
	private int minimumLength = 5;
	private int maximumLength = 15;
	
	public enum DiscriminationMode {
			NONE,
			HAS_MINIMUM_LENGTH,
			HAS_MAXIMUM_LENGTH,
			STARTS_WITH,
			CONTAINS,
			ENDS_WITH,
			REGEXP
	};
	
	private DiscriminationMode discriminationMode = null; //DiscriminationMode.NONE;
	
	protected boolean in;
	protected String filterField = "imsi";

	public void setInMode(boolean in) {
		this.in = in;
	}

	public void setFilterField(String filterField) {
		this.filterField = filterField;
	}

	public void setMinimumLength(int minimumLength) {
		this.minimumLength = minimumLength;
	}

	public void setMaximumLength(int maximumLength) {
		this.maximumLength = maximumLength;
	}

	public void setPatterns(List<String> patterns) {
		this.patterns = patterns;
	}

	public void setDiscriminationMode(DiscriminationMode discriminationMode) {
		this.discriminationMode = discriminationMode;
	}

	@Override
	public boolean accept(DataTransporter dt) {
		String field = dt.getFieldValue(filterField);
		if(field == null) { // no field, decision is not to keep the item, same for list based filter
			return false;
		}
		switch(discriminationMode) {
			case NONE:
				return true;
			case HAS_MINIMUM_LENGTH:
				return hasMinimumLength(field);
			case HAS_MAXIMUM_LENGTH:
				return hasMaximumLength(field);
			default:
				break;
		}
		return !patterns.stream().anyMatch(pattern -> filterUsingMode(discriminationMode, pattern, field));
	}
	
	private boolean filterUsingMode(DiscriminationMode discriminationMode, String pattern, String field) {
		switch(discriminationMode) {
			case STARTS_WITH:
				return startsWith(field, pattern);
			case CONTAINS:
				return contains(field, pattern);
			case ENDS_WITH:
				return endsWith(field, pattern);
			case REGEXP:
				return regexp(field, pattern);
			default:
				throw new RuntimeException("Invalid mode " + discriminationMode);
		}
	}

	private boolean regexp(String field, String pattern) {
		// not yes implemented
		throw new RuntimeException("REGEXP is not yet implemented...");
	}

	private boolean hasMinimumLength(String field) {
		return !(in ^ (field.length() >= minimumLength));
	}

	private boolean hasMaximumLength(String field) {
		return !(in ^ (field.length() <= maximumLength));
	}

	private boolean endsWith(String field, String pattern) {
		return (in ^ field.endsWith(pattern));
	}

	private boolean contains(String field, String pattern) {
		return (in ^ field.contains(pattern));
	}

	private boolean startsWith(String field, String pattern) {
		return (in ^ field.startsWith(pattern));
	}

	@PostConstruct
	public void initFilter() {
		if(discriminationMode == null) {
			throw new RuntimeException("Discrimination mode and pattern or minimumLength properties must be set");
		}
		switch(discriminationMode) {
		case HAS_MINIMUM_LENGTH:
		case HAS_MAXIMUM_LENGTH:
		case NONE:
			break;
		case STARTS_WITH:
		case CONTAINS:
		case ENDS_WITH:
		case REGEXP:
			if(CollectionUtils.isEmpty(patterns)) {
				throw new RuntimeException("Pattern(s) cannot be empty null.");
			}
		}

		LOG.info("Discriminating[(include {})] '{}' {} '{}' '{}'", 
				new Object[] {
				in, 
				filterField, 
				discriminationMode.toString(), 
				patterns, 
				minimumLength});
		if(discriminationMode == DiscriminationMode.REGEXP) {
			throw new RuntimeException("REGEXP is not yet implemented...");
		}
	}
}
