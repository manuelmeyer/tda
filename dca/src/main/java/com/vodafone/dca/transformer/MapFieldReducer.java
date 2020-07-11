package com.vodafone.dca.transformer;

import static org.assertj.core.util.Arrays.isNullOrEmpty;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.assertj.core.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;

import com.vodafone.dca.common.FileUtils;
import com.vodafone.dca.domain.DataTransporter;
import com.vodafone.dca.service.PepperManager;

public class MapFieldReducer implements Converter<DataTransporter, String> {
	
	private static final Logger LOG = LoggerFactory.getLogger(MapFieldReducer.class);
	
	private List<String> fieldsOutNames = new ArrayList<String>();
	private Set<String> fieldsToEpoch = new HashSet<String>();
	private Set<String> fieldsToDecimal = new HashSet<String>();
	private Set<String> anonymiseSet = new HashSet<String>();
	
	private PepperManager pepperManager;

	public void setPepperManager(PepperManager pepperManager) {
		this.pepperManager = pepperManager;
	}
	
	protected void setFieldsOutNames(String[] fields) {
		addToCollection(fieldsOutNames, fields);
		LOG.info("fieldsOutNames is {}", fieldsOutNames);
	}

	protected void setFieldsToEpoch(String[] fields) {
		addToCollection(fieldsToEpoch, fields);
		LOG.info("fieldsToEpoch is {}", fieldsToEpoch);
	}
	
	protected void setFieldsToDecimal(String[] fields) {
		addToCollection(fieldsToDecimal, fields);
		LOG.info("fieldsToDecimal is {}", fieldsToDecimal);
	}

	public void setAnonymiseSet(String[] fields) {
		buildAnonymiseLogic(fields);
		LOG.info("anonymiseSet is {}", anonymiseSet);
	}
	
	private void buildAnonymiseLogic(String[] fields) {
		if(!isNullOrEmpty(fields)) {
			for(String field : fields) {
				anonymiseSet.add(field.trim());
			}
		}
	}

	public void setFieldsOutDefinitionFile(String path) {
		setFieldsOutNames(FileUtils.getFieldsFromFile(path));
	}
	
	private void addToCollection(Collection<String> list, String[] fields) {
		for(String value : fields) {
			list.add(value);
		}
	}
	
	@Override
	public String convert(DataTransporter dt) {
		StringBuilder sb = new StringBuilder();
		String delimiter = "";

		for(String name : fieldsOutNames) {
			String token = dt.getFieldValue(name);
			if(token == null || "null".equals(token)) {
				token = "";
			}
			if(anonymiseSet.contains(name)) {
				token = anonymiseToken(token, pepperManager.getSaltForTime(dt));
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

	@PostConstruct
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
