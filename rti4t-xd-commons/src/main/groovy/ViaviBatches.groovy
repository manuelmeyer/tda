import java.util.concurrent.FutureTask

import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonToken
import org.codehaus.jackson.map.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.task.SimpleAsyncTaskExecutor

import com.dell.rti4t.xd.domain.ViaviBatchInfo
import com.dell.rti4t.xd.external.SpawnProcess


class ViaviBatches {
	
	static class RunningBatch {
		ViaviBatchInfo batchInfo;
		FutureTask task;
	}
	
	static final private Logger LOG = LoggerFactory.getLogger(ViaviBatches.class);
	
	static public void main(String[] args) {
		new ViaviBatches().run(args);
	}
	
	private SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
	private String downloadBatch = "/opt/SP/pivotal/output/capture/instance_5/bin/getbatch.sh ";
	
	int concurrencyLimit = 10;
	String token;
	
	private void run(String[] args) {
		Long.
		setProperties(args);
		ViaviChronicleMap.createPersistentMap();
		runAll(getObjectsFromStdin());
	}
	
	private void runAll(List<Map> runs) {
		List<RunningBatch> runningBatches = new ArrayList<RunningBatch>();
		String[] env = new String[3];
		env[0] = "CODEDTOKEN=" + token;
		runs.each { map -> 
			String batchId = map.get('BatchId');
			if(batchId == null) {
				return;
			}
			if(map.get("Partition").startsWith("RH")) {
				return;
			}
			if(!"null".equals(map.get("Deleted"))) {
				ViaviChronicleMap.delete(batchId);
				return;
			}
			ViaviBatchInfo batchInfo = ViaviChronicleMap.getOrCreate(batchId);
			if(batchInfo.downloaded == null) {
				//LOG.info("Getting bach {}", map);
				env[1] = "PARTITION=" + map.get("Partition");
				env[2] = "TECHNOLOGY=" + map.get("Technology");
				SpawnProcess process = new SpawnProcess(String.format("%s %s %s", downloadBatch, map.get('FileLink'), map.get('BatchId')),
										env,
										new File("/tmp"),
										new OutputStreamWriter(System.out),
										new OutputStreamWriter(System.err),
										null);
				FutureTask task = new FutureTask<Integer>(process);
				taskExecutor.execute(task);
				RunningBatch runningBatch = new RunningBatch();
				runningBatch.batchInfo = batchInfo;
				runningBatch.task = task;
				runningBatches.add(runningBatch);
			}
		}
		
		LOG.info ("{} download tasks running", runningBatches.size())
		runningBatches.each { runningBatch ->
			Integer status = runningBatch.task.get();
			runningBatch.batchInfo.status = status;
			if(status == 0) {
				runningBatch.batchInfo.downloaded = new Date();
			}
			batchMap.put(runningBatch.batchInfo.name, runningBatch.batchInfo);
		}
		LOG.info ("Terminated")
	}
	
	private void setProperties(String[] args) {
		taskExecutor.setConcurrencyLimit(concurrencyLimit);
		token = System.getenv("CODEDTOKEN");
	}
	
	private List<Map> getObjectsFromStdin() {
		JsonFactory jsonFactory = new JsonFactory();
		JsonParser jsonParser = jsonFactory.createJsonParser(System.in);
		JsonToken token;
		
		List<Map> objects = new ArrayList<Map>();

		ObjectMapper mapper = new ObjectMapper();
		int total = 0;

		while((token = jsonParser.nextToken()) != null) {
			if(token == JsonToken.START_OBJECT) {
				Map objectMap = mapper.readValue(jsonParser, HashMap.class);
				objects.add(objectMap);
			}
		}
		LOG.info ("${objects.size()} objects read.")
		return objects;
	}
}

