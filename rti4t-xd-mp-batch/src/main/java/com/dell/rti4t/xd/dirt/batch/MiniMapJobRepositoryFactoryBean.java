package com.dell.rti4t.xd.dirt.batch;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.repository.support.AbstractJobRepositoryFactoryBean;
import org.springframework.batch.core.repository.support.SimpleJobRepository;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * A {@link FactoryBean} that automates the creation of a
 * {@link SimpleJobRepository} using non-persistent in-memory DAO
 * implementations. This repository is only really intended for use in testing
 * and rapid prototyping. In such settings you might find that
 * {@link ResourcelessTransactionManager} is useful (as long as your business
 * logic does not use a relational database). Not suited for use in
 * multi-threaded jobs with splits, although it should be safe to use in a
 * multi-threaded step.
 * 
 * @author Robert Kasanicky
 */
public class MiniMapJobRepositoryFactoryBean extends AbstractJobRepositoryFactoryBean implements MiniMapDeleteListener {

	private MiniMapJobExecutionDao jobExecutionDao;
	private MiniMapJobInstanceDao jobInstanceDao;
	private MiniMapStepExecutionDao stepExecutionDao;
	private MiniMapExecutionContextDao executionContextDao;

	/**
	 * Create a new instance with a {@link ResourcelessTransactionManager}.
	 */
	public MiniMapJobRepositoryFactoryBean() {
		this(new ResourcelessTransactionManager());
	}

	/**
	 * Create a new instance with the provided transaction manager.
	 * 
	 * @param transactionManager {@link org.springframework.transaction.PlatformTransactionManager}
	 */
	public MiniMapJobRepositoryFactoryBean(PlatformTransactionManager transactionManager) {
		setTransactionManager(transactionManager);
	}

	public JobExecutionDao getJobExecutionDao() {
		return jobExecutionDao;
	}

	public JobInstanceDao getJobInstanceDao() {
		return jobInstanceDao;
	}

	public StepExecutionDao getStepExecutionDao() {
		return stepExecutionDao;
	}

	public ExecutionContextDao getExecutionContextDao() {
		return executionContextDao;
	}

	/**
	 * Convenience method to clear all the map DAOs globally, removing all
	 * entities.
	 */
	public void clear() {
		jobInstanceDao.clear();
		jobExecutionDao.clear();
		stepExecutionDao.clear();
		executionContextDao.clear();
	}

	@Override
	protected JobExecutionDao createJobExecutionDao() throws Exception {
		jobExecutionDao = new MiniMapJobExecutionDao(this);
		return jobExecutionDao;
	}

	@Override
	protected JobInstanceDao createJobInstanceDao() throws Exception {
		jobInstanceDao = new MiniMapJobInstanceDao();
		return jobInstanceDao;
	}

	@Override
	protected StepExecutionDao createStepExecutionDao() throws Exception {
		stepExecutionDao = new MiniMapStepExecutionDao();
		return stepExecutionDao;
	}

	@Override
	protected ExecutionContextDao createExecutionContextDao() throws Exception {
		executionContextDao = new MiniMapExecutionContextDao();
		return executionContextDao;
	}

	@Override
	public void delete(JobExecution jobExecution) {
		executionContextDao.delete(jobExecution);
		stepExecutionDao.delete(jobExecution);
		jobInstanceDao.delete(jobExecution);
	}
}
