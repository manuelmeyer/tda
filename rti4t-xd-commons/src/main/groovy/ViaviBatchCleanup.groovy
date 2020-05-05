import net.openhft.chronicle.map.ChronicleMap
import net.openhft.chronicle.map.ChronicleMapBuilder

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.dell.rti4t.xd.domain.ViaviBatchInfo



class ViaviBatchCleanup {
	
	static final private Logger LOG = LoggerFactory.getLogger(ViaviBatchCleanup.class);

	static public void main(String[] args) {
		new ViaviBatchCleanup().run(args);
	}
	
	private void run(args) {
		ViaviChronicleMap.createPersistentMap();
		ViaviChronicleMap.cleanupEntries();
	}
}
