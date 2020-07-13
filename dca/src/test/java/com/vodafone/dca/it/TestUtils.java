package com.vodafone.dca.it;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class TestUtils {
	
	private static final Logger LOG = LoggerFactory.getLogger(TestUtils.class);

	public static String moveDemoSampleFileToTmp(String tmpDir, String file) throws Exception {
		return moveDemoSampleFileToTmp(tmpDir, file, null);
	}

	public static String moveDemoSampleFileToTmp(String tmpDir, String file, String fileNameTarget) throws Exception {
		Resource source = fileSystemOrClassPathResource(file);
		String targetName = fileNameTarget == null
					? source.getFile().getName()
					: fileNameTarget;
		Path targetFile = Paths.get(tmpDir, targetName);
		Path copiedFile = Paths.get(tmpDir, targetName + ".cp");
		Files.copy(Paths.get(source.getURI()), copiedFile, REPLACE_EXISTING);
		Files.move(copiedFile, targetFile, REPLACE_EXISTING);
		return targetFile.toString();
	}

	public static String loadFileData(String file) throws Exception {
		Resource resource = fileSystemOrClassPathResource(file);
		byte[] encoded = Files.readAllBytes(Paths.get(resource.getURI()));
		return new String(encoded);
	}
	
	public static Resource fileSystemOrClassPathResource(String file) {
		FileSystemResource resource = new FileSystemResource(file);
		if(resource.exists() && resource.isFile()) {
			return resource;
		}
		return new ClassPathResource(file);
	}

	public static Channel createAMQPChannel() throws Exception {
       ConnectionFactory factory = new ConnectionFactory();        
       factory.setUsername("decoder");
       factory.setPassword("decoder");
       factory.setVirtualHost("/");
       factory.setHost("localhost");
       factory.setPort(5672);
       Connection connection = factory.newConnection();

       Channel channel = connection.createChannel();
       return channel;
	}
	
	public static void sendMessages(String payload, int total, Channel channel, int delay) {
		try {
			int totalSent = 0;
			byte[] bytes = payload.getBytes();
			for(;;) {
				channel.basicPublish("DECODER.STREAM", "TEST.LORS.TEST", null, bytes);
				Thread.sleep(delay);
				totalSent++;
				if(totalSent % 1000 == 0) {
					LOG.info("Total sent is {}", totalSent);
				}
				if(total > 0 && totalSent == total) {
					break;
				}
			}
			LOG.info("Finish writting {} message(s)", totalSent);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static Stream<Path> generatedFiles(String tmpDir, String qualifier) {
		try {
			return Stream
					.concat(
						Files.find(Paths.get(tmpDir), 1, (path,  a) -> path.getFileName().toString().startsWith(qualifier)),
						Files.find(Paths.get(tmpDir), 1, (path,  a) -> path.getFileName().toString().endsWith(qualifier))
					);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static void cleanGeneratedFiles(String tmpDir, String... prefixes) throws Exception {
		Arrays.stream(prefixes)
			.forEach(prefix -> generatedFiles(tmpDir, prefix).forEach(file -> safeDelete(file)));
	}

	private static void safeDelete(Path file) {
		try {
			LOG.info(" > deleting {}", file);
			Files.deleteIfExists(file);
		} catch(Exception e) {}
	}

	public static void assertGeneratedIsExpected(String tmpDir, String prefix, String referenceFile) throws Exception {
		Path generated = generatedFiles(tmpDir, prefix).findFirst().get();
		String generatedContent = loadFileData(generated.toString());
		String expectedContent = loadFileData(referenceFile);
		
		if(!expectedContent.equals(generatedContent)) {
			// see the difference on screen.
			LOG.info("\nExpected ------------ \n{}\n generated -------------------\n{}\n---------------", expectedContent, generatedContent);
		}
		assertEquals(expectedContent, generatedContent);		
	}

	public static boolean fileExists(String expectedName) {
		return fileSystemOrClassPathResource(expectedName).exists();
	}
}
