package com.dell.rti4t.xd.dirt.batch;

import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.AbstractJobExplorerFactoryBean;
import org.springframework.batch.core.explore.support.SimpleJobExplorer;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.core.repository.dao.JobExecutionDao;
import org.springframework.batch.core.repository.dao.JobInstanceDao;
import org.springframework.batch.core.repository.dao.StepExecutionDao;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;

/**
 * A {@link FactoryBean} that automates the creation of a
 * {@link SimpleJobExplorer} using in-memory DAO implementations.
 *
 * @author Dave Syer
 * @since 2.0
 */
public class MiniMapJobExplorerFactoryBean extends AbstractJobExplorerFactoryBean implements InitializingBean {

	private MiniMapJobRepositoryFactoryBean repositoryFactory;

	/**
	 * Create an instance with the provided {@link MapJobRepositoryFactoryBean}
	 * as a source of Dao instances.
	 * @param repositoryFactory provides the used {@link org.springframework.batch.core.repository.JobRepository}
	 */
	public MiniMapJobExplorerFactoryBean(MiniMapJobRepositoryFactoryBean repositoryFactory) {
		this.repositoryFactory = repositoryFactory;
	}

	/**
	 * Create a factory with no {@link MapJobRepositoryFactoryBean}. It must be
	 * injected as a property.
	 */
	public MiniMapJobExplorerFactoryBean() {
	}

	/**
	 * The repository factory that can be used to create daos for the explorer.
	 *
	 * @param repositoryFactory a {@link MiniMapJobExplorerFactoryBean}
	 */
	public void setRepositoryFactory(MiniMapJobRepositoryFactoryBean repositoryFactory) {
		this.repositoryFactory = repositoryFactory;
	}

	/**
	 * @throws Exception
	 * @see org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.state(repositoryFactory != null, "A MapJobRepositoryFactoryBean must be provided");
		repositoryFactory.afterPropertiesSet();
	}

	@Override
	protected JobExecutionDao createJobExecutionDao() throws Exception {
		return repositoryFactory.getJobExecutionDao();
	}

	@Override
	protected JobInstanceDao createJobInstanceDao() throws Exception {
		return repositoryFactory.getJobInstanceDao();
	}

	@Override
	protected StepExecutionDao createStepExecutionDao() throws Exception {
		return repositoryFactory.getStepExecutionDao();
	}

	@Override
	protected ExecutionContextDao createExecutionContextDao() throws Exception {
		return repositoryFactory.getExecutionContextDao();
	}

	@Override
	public JobExplorer getObject() throws Exception {
		return new SimpleJobExplorer(createJobInstanceDao(), createJobExecutionDao(), createStepExecutionDao(),
				createExecutionContextDao());
	}
}
