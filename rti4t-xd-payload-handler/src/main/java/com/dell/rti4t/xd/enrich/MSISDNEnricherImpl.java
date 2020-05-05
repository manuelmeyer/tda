package com.dell.rti4t.xd.enrich;

import static org.apache.commons.lang.StringUtils.isNumeric;
import static org.springframework.util.StringUtils.isEmpty;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.enrich.EventEnricher;
import com.dell.rti4t.xd.utils.FileUtils;

public class MSISDNEnricherImpl implements EventEnricher, InitializingBean {
	
	static private final Logger LOG = LoggerFactory.getLogger(MSISDNEnricherImpl.class);
	private String dataSourceFilePath;
	private HashMap<String, String> dataSourceToTargetMap;
	
	private String fieldSource = "imsi";
	private String fieldTarget = "msisdn";
	
	public void setDataSourceFilePath(String dataSourceFilePath) {
		this.dataSourceFilePath = dataSourceFilePath;
	}
	
	public void setFieldSource(String fieldSource) {
		this.fieldSource = fieldSource;
	}
	
	public void setFieldTarget(String fieldTarget) {
		this.fieldTarget = fieldTarget;
	}

	@Override
	public DataTransporter enrich(DataTransporter dt) {
		if(dataSourceToTargetMap == null) {
			return dt;
		}
		dt.putFieldValue(fieldTarget, dataSourceToTargetMap.get(dt.getFieldValue(fieldSource)));
		return dt;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if(!isEmpty(dataSourceFilePath)) {
			File dataSourceFile = FileUtils.fileFromPath(dataSourceFilePath);
			LOG.info("Loading {}", dataSourceFile.getAbsolutePath());
			dataSourceToTargetMap = new HashMap<String, String>();
			try(BufferedReader reader = new BufferedReader(new FileReader(dataSourceFile), 16 * 1024 * 1024)) {
				String line;
				while((line = reader.readLine()) != null) {
					String[] data = line.split(",");
					if(data.length == 2 && isNumeric(data[0]) && isNumeric(data[1])) {
						dataSourceToTargetMap.put(data[0], data[1]);
					}
				}
			}
		}
	}
}
