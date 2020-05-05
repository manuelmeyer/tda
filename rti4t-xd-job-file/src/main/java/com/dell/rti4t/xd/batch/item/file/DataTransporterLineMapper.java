package com.dell.rti4t.xd.batch.item.file;

import org.springframework.batch.item.file.LineMapper;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.transformer.ObjectListToDataTransporter;

public class DataTransporterLineMapper implements LineMapper<DataTransporter> {

	private ListDelimitedLineTokenizer tokenizer;
	private ObjectListToDataTransporter toDataTransporter;
	
	@SuppressWarnings("unchecked")
	@Override
	public DataTransporter mapLine(String line, int lineNumber) throws Exception {
		return toDataTransporter.buildFromObjectList(tokenizer.tokenizeToList(line));
	}
	
	public void setLineTokenizer(ListDelimitedLineTokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}
	
	public void setObjectListToDataTransporter(ObjectListToDataTransporter toDataTransporter) {
		this.toDataTransporter = toDataTransporter;
	}
}