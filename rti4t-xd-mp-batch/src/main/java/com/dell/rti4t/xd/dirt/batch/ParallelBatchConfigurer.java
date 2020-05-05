package com.dell.rti4t.xd.dirt.batch;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

public class ParallelBatchConfigurer implements BatchConfigurer {
	
	static final private Logger LOG = LoggerFactory.getLogger(ParallelBatchConfigurer.class);

	private int concurrencyLimit = 1;
	private PlatformTransactionManager transactionManager = new ResourcelessTransactionManager();
	private JobRepository jobRepository;
	private JobLauncher jobLauncher;
	private JobExplorer jobExplorer;
	private MiniMapJobRepositoryFactoryBean jobRepositoryFactory;
	
	protected ParallelBatchConfigurer() {
	}

	public void setConcurrencyLimit(int concurrencyLimit) {
		this.concurrencyLimit = concurrencyLimit;
	}

	@Override
	public JobRepository getJobRepository() {
		return jobRepository;
	}

	@Override
	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	@Override
	public JobLauncher getJobLauncher() {
		return jobLauncher;
	}

	@Override
	public JobExplorer getJobExplorer() {
		return jobExplorer;
	}

	@PostConstruct
	public void initialize() {
		try {
			this.jobRepositoryFactory = createJobRepositoryFactory();
			this.jobRepository = createJobRepository();
			this.jobExplorer = createJobExplorer();
			this.jobLauncher = createJobLauncher();
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to initialize Spring Batch", ex);
		}
	}

	private JobExplorer createJobExplorer() throws Exception {
		MiniMapJobExplorerFactoryBean jobExplorerFactoryBean = new MiniMapJobExplorerFactoryBean();
		jobExplorerFactoryBean.setRepositoryFactory(jobRepositoryFactory);
		jobExplorerFactoryBean.afterPropertiesSet();
		return jobExplorerFactoryBean.getObject();
	}

	private JobLauncher createJobLauncher() throws Exception {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		LOG.info("Job launcher concurrencyLimit is {}", concurrencyLimit);
		if(concurrencyLimit > 1) {
			SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
			taskExecutor.setConcurrencyLimit(concurrencyLimit);
			jobLauncher.setTaskExecutor(taskExecutor);
		}
		jobLauncher.setJobRepository(jobRepositoryFactory.getObject());
		jobLauncher.afterPropertiesSet();
		return jobLauncher;
	}

	protected MiniMapJobRepositoryFactoryBean createJobRepositoryFactory() {
		MiniMapJobRepositoryFactoryBean jobRepositoryFactory = new MiniMapJobRepositoryFactoryBean();
		jobRepositoryFactory.setTransactionManager(transactionManager);
		return jobRepositoryFactory;
	}

	protected JobRepository createJobRepository() throws Exception {
		return jobRepositoryFactory.getObject();
	}
}
