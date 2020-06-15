package com.dell.rti4t.xd.process.eventhandler;

import java.util.List;

import org.springframework.context.Lifecycle;
import org.springframework.messaging.MessageChannel;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.eventhandler.AbstractDataTransporterEventHandler;
import com.dell.rti4t.xd.filter.EventFilter;
import com.dell.rti4t.xd.jmx.VFROInputOutputMetrics;
import com.dell.rti4t.xd.transformer.DataInputParser;
import com.dell.rti4t.xd.transformer.MapFieldReducer;
import com.dell.rti4t.xd.transformer.ListToDataTransporter;
import com.google.common.util.concurrent.AtomicLongMap;

public class CellTowerStatsEventHandler extends AbstractDataTransporterEventHandler {
	
	public CellTowerStatsEventHandler(String handlerName,
			VFROInputOutputMetrics inputOutputMetrics, Lifecycle lifeCycle,
			MessageChannel outputChannel, int batchSize, int batchTimeout,
			MapFieldReducer reducer, DataInputParser objectTransformer,
			List eventFilters) {
		super(handlerName, inputOutputMetrics, lifeCycle, outputChannel, batchSize,
				batchTimeout, reducer, objectTransformer, eventFilters);
		// TODO Auto-generated constructor stub
	}

	private AtomicLongMap<String> cellIn;
	private AtomicLongMap<String> cellOut;

//	public CellTowerStatsEventHandler(String handlerName, VFROInputOutputMetrics inputOutputMetrics, Lifecycle lifeCycle, MessageChannel outputChannel, int batchSize, int batchTimeout, MapFieldReducer reducer, ListToDataTransporter transformer, List<EventFilter> eventFilters) {
////		super(handlerName, inputOutputMetrics, lifeCycle, outputChannel, batchSize, batchTimeout, reducer, transformer, eventFilters);
//	}

	@Override
	public void flushOnTimeout() {
	}

	@Override
	protected void accumulate(DataTransporter dt) {
		String keyIn = getInLacCellKey(dt);
		incrementIn(keyIn);
		String keyOut = getOutLastLacCellKey(dt);
		if(keyOut != null) {
			incrementOut(keyOut);
		}
	}

	private void incrementOut(String keyOut) {
		cellOut.addAndGet(keyOut, 1);
	}

	private String getOutLastLacCellKey(DataTransporter dt) {
		String lastLac = dt.getFieldValue("lastLac");
		String lastCellTower = dt.getFieldValue("lastCellTower");
		if(lastLac != null && lastCellTower != null) {
			return lastLac + "," + lastCellTower;
		}
		return null;
	}

	private void incrementIn(String keyIn) {
		cellIn.addAndGet(keyIn, 1);
	}

	private String getInLacCellKey(DataTransporter dt) {
		return dt.getFieldValue("lac") + "," + dt.getFieldValue("cellTower");
	}

	public void setCellMaps(AtomicLongMap<String> cellIn, AtomicLongMap<String> cellOut) {
		this.cellIn = cellIn;
		this.cellOut = cellOut;
	}
}
