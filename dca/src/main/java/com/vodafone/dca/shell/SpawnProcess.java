package com.vodafone.dca.shell;

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpawnProcess implements Callable<Integer> {
	
	private final static Logger LOG = LoggerFactory.getLogger(SpawnProcess.class);
	
	final private String command;
	final private String[] environmentParams;
	final private File workingDirectory;
	final private Writer stdOut;
	final private Writer stdErr;
	
	public SpawnProcess(String command, String[] environmentParams, File workingDirectory) {
		this(command, environmentParams, workingDirectory, null, null);
	}

	public SpawnProcess(String command, String[] environmentParams, File workingDirectory, Writer stdErr) {
		this(command, environmentParams, workingDirectory, null, stdErr);
	}

	public SpawnProcess(String command, String[] environmentParams, File workingDirectory, Writer stdOut, Writer stdErr) {
		this.command = command;
		this.environmentParams = environmentParams;
		this.workingDirectory = workingDirectory;
		this.stdOut = stdOut;
		this.stdErr = stdErr;
	}
	
	protected Thread redirectStdErr(final Process process) {
		LOG.debug("Redirecting stderr");
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					BufferedReader processStdErr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
					String out;
					while((out = processStdErr.readLine()) != null) {
						if(stdErr != null) {
							stdErr.write(out);
							stdErr.write("\n");
							stdErr.flush();
						}
						LOG.error(out);
					}
				} catch(Exception e) {
					LOG.error("Error while reading stderr of {}", command);
				}
			}
		});
		thread.start();
		return thread;
	}

	protected Thread redirectStdOut(final Process process) {
		LOG.debug("Redirecting stdout");
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					BufferedReader processStdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
					String out;
					while((out = processStdOut.readLine()) != null) {
						if(stdOut != null) {
							stdOut.write(out);
							stdOut.write("\n");
							stdOut.flush();
							LOG.debug("[Writting ({})\n]",  out);
						} else {
							LOG.info(out);
						}
					}
				} catch(Exception e) {
					LOG.error("Error while reading stdin of {}", command);
				}
			}
		});
		thread.start();
		return thread;
	}
	
	@Override
	public Integer call() throws Exception {
		LOG.debug("Starting command {}", command);

		File execFile = buildShellFile(command);
		Process process = Runtime.getRuntime().exec(execFile.getAbsolutePath(), environmentParams, workingDirectory);
		try {
				redirectStdOut(process);
				redirectStdErr(process);
				return process.waitFor();
		} catch(InterruptedException ie) {
			LOG.error("Interrupted - stopping process {}", command);
			process.destroy();
			throw ie;
		} finally {
			if(execFile != null) {
				execFile.delete();
			}
		}
	}
	
	private File buildShellFile(String command) {
		try {
			String fileName = "shell-proc-" + UUID.randomUUID().toString();
			File execFile = File.createTempFile(fileName, ".sh");
			try(FileWriter writer = new FileWriter(execFile)) {
				writer.write(command);
				writer.flush();
				writer.close();
			}
			Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
			perms.add(OWNER_EXECUTE);
			perms.add(OWNER_READ);
			Files.setPosixFilePermissions(Paths.get(execFile.getAbsolutePath()), perms);
			return execFile;
		} catch(Exception e) {
			LOG.error("Error while building shell file", e);
			throw new RuntimeException(e);
		}
	}
}
