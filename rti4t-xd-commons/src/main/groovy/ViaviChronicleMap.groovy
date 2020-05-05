import net.openhft.chronicle.map.ChronicleMap;
import net.openhft.chronicle.map.ChronicleMapBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dell.rti4t.xd.domain.ViaviBatchInfo;


public class ViaviChronicleMap {
	
	static final private Logger LOG = LoggerFactory.getLogger(ViaviChronicleMap.class);

	static private ChronicleMap<CharSequence, ViaviBatchInfo> batchMap;
	static private String chronicleMapPath = "/opt/SP/pivotal/output/capture/instance_5/data/chronicle/batch-download.cmap";

	static private long twoDays = 3600 * 48 * 1000;
	static private long now = new Date().getTime();
	static private long older = now - twoDays;

	public static void createPersistentMap() {
		ChronicleMapBuilder<CharSequence, ViaviBatchInfo> builder = ChronicleMapBuilder
				.of(CharSequence, ViaviBatchInfo)
				.name("batch-id-download")
				.averageKeySize("geosubscribermobilit_130464ded9cf4a348b283676617b86b7".length())
				.averageValueSize(2048)
				.entries(1_000_000);

		batchMap = builder.createPersistedTo(new File(chronicleMapPath));
	}
	
	public static void delete(CharSequence entry) {
		batchMap.remove(entry);
	}
	
	public static ViaviBatchInfo getOrCreate(CharSequence entry) {
		ViaviBatchInfo info = batchMap.get(entry);
		if(info == null) {
			info = new ViaviBatchInfo();
			info.name = entry;
			info.created = new Date();
			batchMap.put(entry, info);
		}
		return info;
	}
	
	public static void cleanupEntries() {
		Set<CharSequence> toBeDeleted = new HashSet<CharSequence>();
		for(CharSequence batchInfoKey : batchMap.keySet()) {
			if(batchMap.get(batchInfoKey).created.getTime() < older) {
				toBeDeleted.add(batchInfoKey);
			}
		}
		LOG.info("Removing {} entries older than {} ms", toBeDeleted.size(), new Date(older));
		for(CharSequence batchInfoKey : toBeDeleted) {
			batchMap.remove(batchInfoKey);
		}
	}
}
