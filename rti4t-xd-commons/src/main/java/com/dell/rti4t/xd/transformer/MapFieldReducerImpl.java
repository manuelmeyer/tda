package com.dell.rti4t.xd.transformer;

import static com.dell.rti4t.xd.utils.FileUtils.getFieldsFromFile;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.filter.FieldBasedFilter;
import com.dell.rti4t.xd.filter.FieldBasedFilterAccepAll;
import com.dell.rti4t.xd.filter.FieldBasedFilterLengthLessThan;

public class MapFieldReducerImpl implements InitializingBean, MapFieldReducer {
	
	private static final Logger LOG = LoggerFactory.getLogger(MapFieldReducerImpl.class);
	
	private List<String> fieldsOutNames = new ArrayList<String>();
	private Set<String> fieldsToEpoch = new HashSet<String>();
	private Set<String> fieldsToDecimal = new HashSet<String>();
	private Set<String> anonymiseSet = new HashSet<String>();
	private Map<String, FieldBasedFilter> anonymiseRules = new HashMap<String, FieldBasedFilter>();
	private PepperManager pepperManager;

	public void setPepperManager(PepperManager pepperManager) {
		this.pepperManager = pepperManager;
	}
	
	public void setFieldsOutNames(String[] fields) {
		addToCollection(fieldsOutNames, fields);
		LOG.info("fieldsOutNames is {}", fieldsOutNames);
	}

	public void setFieldsToEpoch(String[] fields) {
		addToCollection(fieldsToEpoch, fields);
		LOG.info("fieldsToEpoch is {}", fieldsToEpoch);
	}
	
	public void setFieldsToDecimal(String[] fields) {
		addToCollection(fieldsToDecimal, fields);
		LOG.info("fieldsToDecimal is {}", fieldsToDecimal);
	}

	public void setAnonymiseSet(String[] fields) {
		buildAnonymiseLogic(fields);
		LOG.info("anonymiseSet is {}", anonymiseSet);
		LOG.info("anonymiseRules is {}", anonymiseRules);
	}
	
	private void buildAnonymiseLogic(String[] fields) {
		for(String field : fields) {
			String[] parts = field.split("\\(");
			if(parts.length == 1) {
				anonymiseSet.add(parts[0]);
				anonymiseRules.put(parts[0], new FieldBasedFilterAccepAll());
				continue;
			}
			parts[1] = parts[1].replace(")", "");
			addWithRules(parts);
		}
	}

	private void addWithRules(String[] parts) {
		String name = parts[0].trim();
		anonymiseSet.add(name);
		anonymiseRules.put(name, findRuleBased(parts[1]));
	}

	private FieldBasedFilter findRuleBased(String rule) {
		String original = rule;
		try {
			if(rule.startsWith("length")) {
				rule = rule.substring("length".length()).trim();
				if(rule.startsWith("<")) {
					rule = rule.substring(1).trim();
					return new FieldBasedFilterLengthLessThan(Integer.valueOf(rule));
				}
			}
		} catch(Exception e) {
		}
		
		String errorMsg = String.format("Rule \"%s\" cannot be parsed", original);
		LOG.error(errorMsg);
		throw new RuntimeException(errorMsg);
	}

	public void setFieldsOutDefinitionFile(String path) {
		setFieldsOutNames(getFieldsFromFile(path));
	}
	
	private void addToCollection(Collection<String> list, String[] fields) {
		for(String value : fields) {
			list.add(value);
		}
	}
	
	public String transform(DataTransporter dt) {
		StringBuilder sb = new StringBuilder();
		String delimiter = "";

		for(String name : fieldsOutNames) {
			String token = dt.getFieldValue(name);
			if(token == null || "null".equals(token)) {
				token = "";
			}
			if(anonymiseSet.contains(name)) {
				if(anonymiseRules.get(name).accept(token)) {
					token = anonymiseToken(token, pepperManager.getSaltForTime(dt));
				}
			} else if(fieldsToEpoch.contains(name)) {
				token = toEpoch(token);
			} else if(fieldsToDecimal.contains(name)) {
				token = toDecimal(token);
			}
			sb.append(delimiter).append(token);
			delimiter = ",";
		}
		return sb.toString();
	}

	protected String toEpoch(String token) {
		if(token == null || token.length() < 3) {
			return token;
		}
		return token.substring(0, token.length() - 3);
	}

	private String toDecimal(String field) {
		if(field == null || field.length() == 0) {
			return field;
		}
		Integer result = 0;
		for(byte it : field.getBytes()) {
			int current =  it & 0xff;
			result *= 256;
			result += current;		
		}
		return result.toString();
	}

	public String anonymiseToken(String token, String pepper) {
		try {
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		String anonymised = token + pepper;
		byte[] hash = digest.digest(anonymised.getBytes("UTF-8"));
		return toHex(hash);
		} catch(Exception e) {
			LOG.error("Cannot anonymise {} using {}", token, "SHA-256", e);
			throw new RuntimeException(e);
		}
	}

	private String toHex(byte[] hash) {
		StringBuffer hexString = new StringBuffer();
		for (int index = 0; index < hash.length; index++) {
			hexString.append(Integer.toHexString(0xFF & hash[index]));
		}
		return hexString.toString();
	}

	private void intersect(Collection<String> reference, Collection<String> target) {
		List<String> excluded = new ArrayList<String>();
		for(String check : target) {
			if(!reference.contains(check)) {
				excluded.add(check);
			}
		}
		if(excluded.size() > 0) {
			target.removeAll(excluded);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		intersect(fieldsOutNames, anonymiseSet);
		intersect(fieldsOutNames, fieldsToDecimal);
		intersect(fieldsOutNames, fieldsToEpoch);
		
		LOG.info("fieldsOutNames untouched is {}", fieldsOutNames);
		LOG.info("fieldsToEpoch is {}", fieldsToEpoch);
		LOG.info("fieldsToDecimal is {}", fieldsToDecimal);
		LOG.info("anonymiseSet is {}", anonymiseSet);
	}
}
