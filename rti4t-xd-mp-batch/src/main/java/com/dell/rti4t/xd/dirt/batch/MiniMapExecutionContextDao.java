package com.dell.rti4t.xd.dirt.batch;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.repository.dao.ExecutionContextDao;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.util.Assert;
import org.springframework.util.SerializationUtils;

/**
 * In-memory implementation of {@link ExecutionContextDao} backed by maps.
 *
 * @author Robert Kasanicky
 * @author Dave Syer
 * @author David Turanski
 */
@SuppressWarnings("serial")
public class MiniMapExecutionContextDao implements ExecutionContextDao {
	
	static final private Logger LOG = LoggerFactory.getLogger(MiniMapExecutionContextDao.class);

	private final ConcurrentMap<ContextKey, ExecutionContext> contexts = new ConcurrentHashMap<ContextKey, ExecutionContext>();

	private static final class ContextKey implements Comparable<ContextKey>, Serializable {

		private static enum Type { STEP, JOB; }

		private final Type type;
		private final long id;

		private ContextKey(Type type, long id) {
			if(type == null) {
				throw new IllegalStateException("Need a non-null type for a context");
			}
			this.type = type;
			this.id = id;
		}

		@Override
		public int compareTo(ContextKey them) {
			if(them == null) {
				return 1;
			}
			final int idCompare = new Long(this.id).compareTo(new Long(them.id)); // JDK6 Make this Long.compare(x,y)
			if(idCompare != 0) {
				return idCompare;
			}
			final int typeCompare = this.type.compareTo(them.type);
			if(typeCompare != 0) {
				return typeCompare;
			}
			return 0;
		}

		@Override
		public boolean equals(Object them) {
			if(them == null) {
				return false;
			}
			if(them instanceof ContextKey) {
				return this.equals((ContextKey)them);
			}
			return false;
		}

		public boolean equals(ContextKey them) {
			if(them == null) {
				return false;
			}
			return this.id == them.id && this.type.equals(them.type);
		}

		@Override
		public int hashCode() {
			int value = (int)(id^(id>>>32));
			switch(type) {
			case STEP: return value;
			case JOB: return ~value;
			default: throw new IllegalStateException("Unknown type encountered in switch: " + type);
			}
		}

		public static ContextKey step(long id) { return new ContextKey(Type.STEP, id); }

		public static ContextKey job(long id) { return new ContextKey(Type.JOB, id); }
	}

	public void clear() {
		contexts.clear();
	}

	private static ExecutionContext copy(ExecutionContext original) {
		return (ExecutionContext) SerializationUtils.deserialize(SerializationUtils.serialize(original));
	}

	@Override
	public ExecutionContext getExecutionContext(StepExecution stepExecution) {
		return copy(contexts.get(ContextKey.step(stepExecution.getId())));
	}

	@Override
	public void updateExecutionContext(StepExecution stepExecution) {
		ExecutionContext executionContext = stepExecution.getExecutionContext();
		if (executionContext != null) {
			contexts.put(ContextKey.step(stepExecution.getId()), copy(executionContext));
		}
	}

	@Override
	public ExecutionContext getExecutionContext(JobExecution jobExecution) {
		return copy(contexts.get(ContextKey.job(jobExecution.getId())));
	}

	@Override
	public void updateExecutionContext(JobExecution jobExecution) {
		ExecutionContext executionContext = jobExecution.getExecutionContext();
		if (executionContext != null) {
			contexts.put(ContextKey.job(jobExecution.getId()), copy(executionContext));
		}
	}

	@Override
	public void saveExecutionContext(JobExecution jobExecution) {
		updateExecutionContext(jobExecution);
	}

	@Override
	public void saveExecutionContext(StepExecution stepExecution) {
		updateExecutionContext(stepExecution);
	}


	@Override
	public void saveExecutionContexts(Collection<StepExecution> stepExecutions) {
		Assert.notNull(stepExecutions,"Attempt to save a nulk collection of step executions");
		for (StepExecution stepExecution: stepExecutions) {
			saveExecutionContext(stepExecution);
			saveExecutionContext(stepExecution.getJobExecution());
		}
	}

	public void delete(JobExecution jobExecution) {
		if(jobExecution != null && jobExecution.getId() != null) {
			contexts.remove(ContextKey.job(jobExecution.getId()));
			LOG.debug("Removing job context {} map size is {}", jobExecution.getId(), contexts.size());
			if(jobExecution.getStepExecutions() != null) {
				for(StepExecution stepExecution : jobExecution.getStepExecutions()) {
					ExecutionContext executionContext = stepExecution.getExecutionContext();
					if (executionContext != null) {
						contexts.remove(ContextKey.step(stepExecution.getId()));
						LOG.debug("Removing step context {} map size is {}", stepExecution.getId(), contexts.size());
					}
				}
			}
		}
	}
}
