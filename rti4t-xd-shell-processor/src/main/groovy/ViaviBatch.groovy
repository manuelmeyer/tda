import java.util.concurrent.FutureTask

import net.openhft.chronicle.map.ChronicleMap
import net.openhft.chronicle.map.ChronicleMapBuilder

import org.codehaus.jackson.JsonFactory
import org.codehaus.jackson.JsonParser
import org.codehaus.jackson.JsonToken
import org.codehaus.jackson.map.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.task.SimpleAsyncTaskExecutor

import com.dell.rti4t.xd.domain.ViaviBatchInfo
import com.dell.rti4t.xd.external.SpawnProcess


class ViaviBatch {
	
	static class RunningBatch {
		ViaviBatchInfo batchInfo;
		FutureTask task;
	}
	
	static final private Logger LOG = LoggerFactory.getLogger(ViaviBatch.class);
	
	static public void main(String[] args) {
		new ViaviBatch().run(args);
	}
	
	private SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
	private ChronicleMap<CharSequence, ViaviBatchInfo> batchMap;
	
	int concurrencyLimit = 10;
	String token;
	
	private void run(String[] args) {
		setProperties(args);
		createPersistentMap();
		runAll(getObjectsFromStdin());
	}
	
	private void createPersistentMap() {
		ChronicleMapBuilder<CharSequence, ViaviBatchInfo> builder = ChronicleMapBuilder
					.of(CharSequence, ViaviBatchInfo)
					.name("batch-id-download")
					.averageKeySize("geosubscribermobilit_130464ded9cf4a348b283676617b86b7".length())
					.averageValueSize(2048)
					.entries(50_000);

		batchMap = builder.createPersistedTo(new File("/Users/manuelmeyer/GitHub/rti4t-xd-modules/modules/batch-download.cmap"));
	}
	
	private void runAll(List<Map> runs) {
		List<RunningBatch> runningBatches = new ArrayList<RunningBatch>();
		String[] env = new String[1];
		env[0] = "TOKEN=" + token;
		runs.each { map -> 
			String batchId = map.get('BatchId')
			if(batchId != null) {
				ViaviBatchInfo batchInfo = getOrCreate(batchId);
				if(!"null".equals(map.get("Deleted")) && batchInfo.downloaded == null) {
					SpawnProcess process = new SpawnProcess("/Users/manuelmeyer/GitHub/rti4t-xd-modules/modules/getbatch.sh " + map.get('FileLink'),
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
		}
		
		LOG.info ("{} tasks to check", runningBatches.size())
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
	
	private ViaviBatchInfo getOrCreate(String batchId) {
		ViaviBatchInfo info = batchMap.get(batchId);
		if(info == null) {
			info = new ViaviBatchInfo();
			info.name = batchId;
			batchMap.put(batchId, info);
		}
		return info;
	}

	private void setProperties(String[] args) {
		taskExecutor.setConcurrencyLimit(concurrencyLimit);
		token = System.getenv("TOKEN");
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
		LOG.info ("${objects.get(0)}")
		LOG.info ("FileLink=${objects.get(0).get('FileLink')}")
		LOG.info ("BachId=${objects.get(0).get('BatchId')}")
		return objects;
	}
}
