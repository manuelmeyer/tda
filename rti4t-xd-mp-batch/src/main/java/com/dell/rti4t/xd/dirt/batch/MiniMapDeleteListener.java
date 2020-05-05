package com.dell.rti4t.xd.dirt.batch;

import org.springframework.batch.core.JobExecution;

public interface MiniMapDeleteListener {
	void delete(JobExecution jobExecution);
}
