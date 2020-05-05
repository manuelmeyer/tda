package com.dell.rti4t.xd.filter;

import com.dell.rti4t.xd.domain.DataTransporter;

public class InOrOutPatternBasedFilterImpl extends InOrOutBaseFilter {
	
	private String pattern;
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
	private DiscriminationMode discriminationMode = DiscriminationMode.NONE;

	public void setMinimumLength(int minimumLength) {
		this.minimumLength = minimumLength;
	}

	public void setMaximumLength(int maximumLength) {
		this.maximumLength = maximumLength;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
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
			case STARTS_WITH:
				return startsWith(field);
			case CONTAINS:
				return contains(field);
			case HAS_MINIMUM_LENGTH:
				return hasMinimumLength(field);
			case HAS_MAXIMUM_LENGTH:
				return hasMaximumLength(field);
			case ENDS_WITH:
				return endsWith(field);
			case REGEXP:
				return regexp(field);
		}
		return false; // just to please the compiler...
	}
	
	private boolean regexp(String field) {
		// not yes implemented
		throw new RuntimeException("REGEXP is not yet implemented...");
	}

	private boolean hasMinimumLength(String field) {
		return !(in ^ (field.length() >= minimumLength));
	}

	private boolean hasMaximumLength(String field) {
		return !(in ^ (field.length() <= maximumLength));
	}

	private boolean endsWith(String field) {
		return !(in ^ field.endsWith(pattern));
	}

	private boolean contains(String field) {
		return !(in ^ field.contains(pattern));
	}

	private boolean startsWith(String field) {
		return !(in ^ field.startsWith(pattern));
	}

	@Override
	protected void doAfterPropertiesSet() throws Exception {
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
			if(pattern == null) {
				throw new RuntimeException("Pattern cannot be null.");
			}
		}

		LOG.info("Discriminating[(include {})] '{}' {} '{}' '{}'", 
				new Object[] {
				in, 
				filterField, 
				discriminationMode.toString(), 
				pattern, 
				minimumLength});
		if(discriminationMode == DiscriminationMode.REGEXP) {
			throw new RuntimeException("REGEXP is not yet implemented...");
		}
	}

	@Override
	public String description() {
		return "in/out filter based on patters";
	}
}
