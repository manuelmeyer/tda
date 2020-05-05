package com.dell.rti4t.processor;


import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.dell.rti4t.processor.ShellProcessor;
import com.dell.rti4t.xd.utils.FileUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:test-module-shell-processor.xml"})
public class TestShellProcessor {
	
	@Autowired
	MessageChannel input;
	
	@Autowired
	QueueChannel output;
	
	@Autowired
	ShellProcessor shellExecutor;
	
	@BeforeClass
	static public void createTestShell() throws Exception {
		copyFromToTemp("commandok.sh", "commandok.sh");
	}
	
	static String tempDir = System.getProperty("java.io.tmpdir");
	
	static void copyFromToTemp(String source, String dest) throws Exception {
		String shell = FileUtils.fileFromPath(source).getAbsolutePath();
		String path = tempDir + dest;
		Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
		Files.copy(Paths.get(shell), Paths.get(path), REPLACE_EXISTING);
		perms.add(OWNER_EXECUTE);
		perms.add(OWNER_READ);
		Files.setPosixFilePermissions(Paths.get(path), perms);
	}
	
	@Test
	public void canRunScript() throws Exception {
		Message<String> msg = MessageBuilder.withPayload("hello\n").build();
		
		String shell = tempDir + "commandok.sh";
		shellExecutor.setCommand(shell);
		shellExecutor.setPropagateOutput(true);
		
		input.send(msg);
		
		Message<?> received = output.receive(60000);
		
		Assert.assertNotNull(received);
		String payload = received.getPayload().toString();
		// look at the commandok.sh file to see why those should be true
		Assert.assertTrue(payload.contains("hello\n"));
		Assert.assertTrue(payload.contains("PARAM1=hello1\n"));
		Assert.assertTrue(payload.contains("PARAM2=hello2\n"));
	}
	
	@Test
	public void canRunDirectCommand() throws Exception {
		Message<String> msg = MessageBuilder.withPayload("hello\n").build();
		StringBuffer shell = new StringBuffer();
		shell.append("\nenv\n")
				.append("echo hello\n")
				.append("if [[ $PARAM1 == \"hello1\" ]]\n")
				.append("then\n")
				.append("\texit 0\n")
				.append("fi\n")
				.append("exit 1\n");
		shellExecutor.setCommand(shell.toString());
		shellExecutor.setPropagateOutput(true);

		input.send(msg);
		
		Message<?> received = output.receive(60000);
		
		Assert.assertNotNull(received);
		String payload = received.getPayload().toString();
		// look at the commandok.sh file to see why those should be true
		Assert.assertTrue(payload.contains("hello\n"));
		Assert.assertTrue(payload.contains("PARAM1=hello1\n"));
		Assert.assertTrue(payload.contains("PARAM2=hello2\n"));
	}

	@Test
	public void canTurnoffOutput() throws Exception {
		Message<String> msg = MessageBuilder.withPayload("hello\n").build();
		StringBuffer shell = new StringBuffer();
		shell.append("\nenv\n")
				.append("echo hello\n")
				.append("if [[ $PARAM1 == \"hello1\" ]]\n")
				.append("then\n")
				.append("\texit 0\n")
				.append("fi\n")
				.append("exit 1\n");
		shellExecutor.setCommand(shell.toString());
		shellExecutor.setPropagateOutput(false);
		
		input.send(msg);
		
		Message<?> received = output.receive(5000);
		
		Assert.assertNull(received);
		Assert.assertFalse(shellExecutor.hasRunningCommand());
	}

}
