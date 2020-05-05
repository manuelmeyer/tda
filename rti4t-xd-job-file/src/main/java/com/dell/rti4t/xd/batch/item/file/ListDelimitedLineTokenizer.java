package com.dell.rti4t.xd.batch.item.file;

import java.util.List;

import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;

class ListDelimitedLineTokenizer extends DelimitedLineTokenizer {
	
	@SuppressWarnings("rawtypes")
	public List tokenizeToList(String line) {
		return doTokenize(line);
	}
}
