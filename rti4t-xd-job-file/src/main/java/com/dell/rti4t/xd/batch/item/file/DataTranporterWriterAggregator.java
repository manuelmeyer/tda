package com.dell.rti4t.xd.batch.item.file;

import org.springframework.batch.item.file.transform.LineAggregator;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.transformer.MapFieldReducer;

public class DataTranporterWriterAggregator  implements LineAggregator<DataTransporter> {
	
	private MapFieldReducer fieldReducer;

	@Override
	public String aggregate(DataTransporter item) {
		return fieldReducer.transform(item);
	}
	
	public void setMapFieldReducer(MapFieldReducer fieldReducer) {
		this.fieldReducer = fieldReducer;
	}
}
