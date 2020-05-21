package com.dell.rti4t.xd.filter;

import org.springframework.beans.factory.InitializingBean;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.locker.LockerByValue;

public class LacCellReductionFilterImpl implements EventFilter, InitializingBean {
	
	private EventFilter lacCellFilter;
	private EventFilter dataReductionFilter;
	private LockerByValue lockerByValue = LockerByValue.buildLocker("imsi");
	
	public void setLacCellFilter(EventFilter lacCellFilter) {
		this.lacCellFilter = lacCellFilter;
	}
	
	public void setDataReductionFilter(EventFilter dataReductionFilter) {
		this.dataReductionFilter = dataReductionFilter;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
	}

	@Override
	public boolean accept(DataTransporter dt) {
		String imsi = dt.getFieldValue("imsi");
		synchronized(lockerByValue.lock(imsi)) {
			return lacCellFilter.accept(dt) && dataReductionFilter.accept(dt);
		}
	}

	@Override
	public String description() {
		return "lac/cell and reduction filter";
	}
}
