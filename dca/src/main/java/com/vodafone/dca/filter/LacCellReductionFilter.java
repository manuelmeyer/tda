package com.vodafone.dca.filter;

import java.util.concurrent.locks.Lock;

import javax.annotation.PostConstruct;

import org.springframework.integration.core.GenericSelector;

import com.google.common.util.concurrent.Striped;
import com.vodafone.dca.domain.DataTransporter;

public class LacCellReductionFilter implements GenericSelector<DataTransporter> {
	
	private GenericSelector<DataTransporter> lacCellFilter;
	private GenericSelector<DataTransporter> dataReductionFilter;
	private Striped<Lock> locks;
	
	public void setLacCellFilter(GenericSelector<DataTransporter> lacCellFilter) {
		this.lacCellFilter = lacCellFilter;
	}
	
	public void setDataReductionFilter(GenericSelector<DataTransporter> dataReductionFilter) {
		this.dataReductionFilter = dataReductionFilter;
	}

	@PostConstruct
	public void buildLocks() throws Exception {
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
}
