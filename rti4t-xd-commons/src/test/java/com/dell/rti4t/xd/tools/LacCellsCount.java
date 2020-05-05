package com.dell.rti4t.xd.tools;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.dell.rti4t.xd.filter.LacCellFilterImpl;

public class LacCellsCount {
	
	static public void main(String[] args) throws Exception {
		if(args.length == 0) {
			System.out.println("Usage LacCellCount file-path");
			System.exit(1);
		}
		new LacCellsCount().run(args[0]);
	}

	private void run(String path) throws Exception {
		LacCellFilterImpl filter = new LacCellFilterImpl();
		filter.setLacCellFilePath(path);
		filter.afterPropertiesSet();
		Map<String, Set<String>> store = filter.accessLacCellsStore();
		int totalLac = 0;
		int totalCells = 0;
		for(Entry<String, Set<String>> entry : store.entrySet()) {
			totalLac++;
			totalCells += entry.getValue().size();
			System.out.println("Lac " + entry.getKey() + " contains " + entry.getValue());
		}
		System.out.println("Total LAC : " + totalLac);
		System.out.println("Total CELLS : " + totalCells);
	}
}
