package com.dell.rti4t.xd.filter;

import java.util.concurrent.locks.Lock;

import org.springframework.beans.factory.InitializingBean;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.google.common.util.concurrent.Striped;

public class LacCellReductionFilterImpl implements EventFilter, InitializingBean {
	
	private EventFilter lacCellFilter;
	private EventFilter dataReductionFilter;
	Striped<Lock> locks;
	//private LockerByValue lockerByValue = LockerByValue.buildLocker("imsi");
	
	public void setLacCellFilter(EventFilter lacCellFilter) {
		this.lacCellFilter = lacCellFilter;
	}
	
	public void setDataReductionFilter(EventFilter dataReductionFilter) {
		this.dataReductionFilter = dataReductionFilter;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		locks = Striped.lock(16 * 1024);
	}

	@Override
	public boolean accept(DataTransporter dt) {
		String imsi = dt.getFieldValue("imsi");
		Lock lock = locks.get(imsi);
		try {
			lock.lock();
			return lacCellFilter.accept(dt) && dataReductionFilter.accept(dt);
		} finally {
			if (lock != null) {
				lock.unlock();
			}
		}
	}

	@Override
	public String description() {
		return "lac/cell and reduction filter";
	}
}
