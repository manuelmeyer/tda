package com.dell.rti4t.xd.batch;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.dell.rti4t.xd.utils.FileUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"/test-full-batch.xml"})
public class TestFullBatch {
	
    @Autowired
    private JobLauncherTestUtils launcher;

	@Test
	public void testFullBatch() throws Exception {
		String path = FileUtils.fileFromPath("demolike.txt").getAbsolutePath();
		JobParameter param = new JobParameter(path);
		
		Map<String, JobParameter> paramMap = new HashMap<>();
		paramMap.put("absoluteFilePath", param);
		
		JobParameters params = new JobParameters(paramMap);
		JobExecution jobExecution = launcher.launchJob(params);
		
		Assert.assertEquals("COMPLETED", jobExecution.getExitStatus().getExitCode());
	}
}
