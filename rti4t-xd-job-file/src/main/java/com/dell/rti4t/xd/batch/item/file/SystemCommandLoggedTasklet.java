package com.dell.rti4t.xd.batch.item.file;

import java.io.File;
import java.util.concurrent.FutureTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobInterruptedException;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.SimpleSystemProcessExitCodeMapper;
import org.springframework.batch.core.step.tasklet.StoppableTasklet;
import org.springframework.batch.core.step.tasklet.SystemProcessExitCodeMapper;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.util.Assert;

import com.dell.rti4t.xd.external.SpawnProcess;
/** 
 * 
 * Based on the batch one, but very simplified.
 *
 */
public class SystemCommandLoggedTasklet extends StepExecutionListenerSupport implements StoppableTasklet, InitializingBean {

	protected static final Logger LOG = LoggerFactory.getLogger(SystemCommandLoggedTasklet.class);
	private String command;
	private String[] environmentParams = null;
	private File workingDirectory = null;
	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();

	@Override
	public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
		FutureTask<Integer> systemCommandTask = new FutureTask<Integer>(new SpawnProcess(command, environmentParams, workingDirectory));
		taskExecutor.execute(systemCommandTask);
		int status = systemCommandTask.get();
		
		LOG.debug("{} terminated with {}", command, status);
		contribution.setExitStatus((status == 0) ? ExitStatus.COMPLETED : ExitStatus.FAILED);
		return RepeatStatus.FINISHED;
		/*
		while (true) {
			Thread.sleep(checkInterval); //moved to the end of the logic

			if(stoppable) {
				JobExecution jobExecution = jobExplorer.getJobExecution(
													chunkContext
														.getStepContext()
														.getStepExecution()
														.getJobExecutionId());
				if(jobExecution.isStopping()) {
					stopped = true;
				}
			}

			if (systemCommandTask.isDone()) {
				contribution.setExitStatus(systemProcessExitCodeMapper.getExitStatus(systemCommandTask.get()));
				return RepeatStatus.FINISHED;
			}
			else if (System.currentTimeMillis() - t0 > timeout) {
				systemCommandTask.cancel(interruptOnCancel);
				throw new SystemCommandException("Execution of system command did not finish within the timeout");
			}
			else if (execution.isTerminateOnly()) {
				systemCommandTask.cancel(interruptOnCancel);
				throw new JobInterruptedException("Job interrupted while executing system command '" + command + "'");
			}
			else if (stopped) {
				systemCommandTask.cancel(interruptOnCancel);
				contribution.setExitStatus(ExitStatus.STOPPED);
				return RepeatStatus.FINISHED;
			}
		}
		*/
	}

	/**
	 * @param command command to be executed in a separate system process
	 */
	public void setCommand(String command) {
		this.command = command;
	}

	/**
	 * @param envp environment parameter values, inherited from parent process
	 * when not set (or set to null).
	 */
	public void setEnvironmentParams(String[] envp) {
		this.environmentParams = envp;
	}

	/**
	 * @param dir working directory of the spawned process, inherited from
	 * parent process when not set (or set to null).
	 */
	public void setWorkingDirectory(String dir) {
		if (dir == null) {
			this.workingDirectory = null;
			return;
		}
		this.workingDirectory = new File(dir);
		Assert.isTrue(workingDirectory.exists(), "working directory must exist");
		Assert.isTrue(workingDirectory.isDirectory(), "working directory value must be a directory");

	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.hasLength(command, "'command' property value is required");
		Assert.notNull(taskExecutor, "taskExecutor is required");
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
//		this.execution = stepExecution;
	}

	@Override
	public void stop() {
	}
}
