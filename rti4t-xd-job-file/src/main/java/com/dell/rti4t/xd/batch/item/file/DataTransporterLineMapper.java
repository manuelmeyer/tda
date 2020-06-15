package com.dell.rti4t.xd.batch.item.file;

import org.springframework.batch.item.file.LineMapper;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.transformer.ListToDataTransporter;

public class DataTransporterLineMapper implements LineMapper<DataTransporter> {

	private ListDelimitedLineTokenizer tokenizer;
	private ListToDataTransporter toDataTransporter;
	
	@SuppressWarnings("unchecked")
	@Override
	public DataTransporter mapLine(String line, int lineNumber) throws Exception {
		return toDataTransporter.buildFromList(tokenizer.tokenizeToList(line));
	}
	
	public void setLineTokenizer(ListDelimitedLineTokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}
	
	public void setObjectListToDataTransporter(ListToDataTransporter toDataTransporter) {
		this.toDataTransporter = toDataTransporter;
	}
}