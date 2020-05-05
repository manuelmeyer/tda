import java.net.URI;

import org.springframework.xd.rest.client.impl.SpringXDTemplate;

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ViaviRemoteBatch {
	
	static final private Logger LOG = LoggerFactory.getLogger(ViaviRemoteBatch.class);
	
	String url;
	String batchName;
	String fileName;

	static public void main(String[] args) {
		try {
			new ViaviRemoteBatch().run(args);
			System.exit(0);
		} catch(Exception e) {
			LOG.error("Cannot start remote batch", e);
			System.exit(1);
		}
	}
	
	private void run(String[] args) {
		checkArgs(args);
		fireBatch(args);
	}

	private void fireBatch(String[] args) {
		LOG.info("Calling batch {} on {} for {}", batchName, url, fileName);
		SpringXDTemplate xdTemplate = new SpringXDTemplate(new URI(url));
		xdTemplate.jobOperations().launchJob(
						batchName,
						String.format("{\"absoluteFilePath\": \"%s\"}", fileName)
					);
	}
	
	private void checkArgs(String[] args) {
		if(args.length != 3) {
			LOG.error("Usage ViaviRemoteBatch http://xd-main-url batchName fileName")
			System.exit(1);
		}
		url = args[0];
		batchName = args[1];
		fileName = args[2];
	}
}
