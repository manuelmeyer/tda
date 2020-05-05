package com.dell.rti4t.xd.process;

import static com.dell.rti4t.xd.filter.DataReductionImpl.ReductionMode.MARK_IMSIS_CHANGE_CELL;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

import com.dell.rti4t.xd.csv.CSVToObjectParser;
import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.filter.DataReductionImpl;
import com.dell.rti4t.xd.transformer.MapFieldReducerImpl;
import com.dell.rti4t.xd.transformer.ObjectListToDataTransporterImpl;

public class FileChecker {
	
	void run(String[] args) throws Exception {
		ObjectListToDataTransporterImpl oldtd = new ObjectListToDataTransporterImpl();
		oldtd.setFieldNamesDefinitionFile(System.getProperty("input.def"));
		
		MapFieldReducerImpl reducer = new MapFieldReducerImpl();
		reducer.setFieldsOutDefinitionFile(System.getProperty("output.def"));
		reducer.afterPropertiesSet();
		
		DataReductionImpl reduction = new DataReductionImpl();
		reduction.setReductionMode(MARK_IMSIS_CHANGE_CELL);
		
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
		String line;
		while((line = reader.readLine()) != null) {
			List<List<Object>> parseds = CSVToObjectParser.parse(line);
			for(List<Object> parsed : parseds) {
				DataTransporter dt = oldtd.buildFromObjectList(parsed);
				if(reduction.accept(dt)) {
					System.out.println(reducer.transform(dt));
				}
			}
		}
		} catch(Exception e) {
			System.err.println("Error while checking stdin");
		}
	}
	public static void main(String[] args) throws Exception {
		new FileChecker().run(args);
	}
}
