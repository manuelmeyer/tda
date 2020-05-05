package com.dell.rti4t.xd.process.eventhandler;

import static java.lang.System.currentTimeMillis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.context.Lifecycle;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.enrich.EventEnricher;
import com.dell.rti4t.xd.eventhandler.AbstractDataTransporterEventHandler;
import com.dell.rti4t.xd.filter.EventFilter;
import com.dell.rti4t.xd.transformer.MapFieldReducer;
import com.dell.rti4t.xd.transformer.ObjectListToDataTransporter;

public class AccumulatorEventHandler extends AbstractDataTransporterEventHandler {
	
	class Context {
		final String marker;
		final StringBuilder builder;
		int accumulated = 0;
		long watchDog = 0;
		public Context(String marker) {
			this.marker = marker;
			this.builder = new StringBuilder(32 * 1024);
		}
	}
	
	final String filter;
	Map<String, Context> contextMap = new HashMap<String, Context>();

	public AccumulatorEventHandler(String handlerName, Lifecycle lifeCycle, MessageChannel outputChannel, int batchSize, int batchTimeout, MapFieldReducer reducer, ObjectListToDataTransporter transformer, List<EventFilter> eventFilters, List<EventEnricher> eventEnrichers) {
		super(handlerName, lifeCycle, outputChannel, batchSize, batchTimeout, reducer, transformer, eventFilters, eventEnrichers);
		this.filter = (String)handlerName;
	}

	public void flushOnTimeout() {
		long now = System.currentTimeMillis();
		for(Context context : contextMap.values()) {
			if(context.accumulated > 0) {
				if((context.watchDog + batchTimeout - now) < 0) {
					LOG.debug("Timeout expired for {}", Thread.currentThread().getName());
					fflush(context);
				}
			}
		}
	}
	
	@Override
	protected void accumulate(DataTransporter dt) {
		Context context = getContext(dt.filter());
		context.builder.append(reducer.transform(dt)).append("\n");
		context.watchDog = currentTimeMillis();
		context.accumulated++;
		if (context.accumulated == batchSize) {
			fflush(context);
		}
	}

	private Context getContext(String filter) {
		Context builder = contextMap.get(filter);
		if(builder == null) {
			LOG.debug("Creating builder for {}", filter);
			builder = new Context(filter);
			contextMap.put(filter, builder);
		}
		return builder;
	}

	private void fflush(Context context) {
		try {
			LOG.debug("Flushing {} for {}", context.marker, Thread.currentThread().getName());
			if(context.accumulated > 0) {
				long t0 = currentTimeMillis();
				Message<String> msg = MessageBuilder.withPayload(context.builder.toString()).setHeader("data-type", context.marker).build();
				context.builder.setLength(0);
				if(isRunning()) {
					outputChannel.send(msg);
					if(LOG.isDebugEnabled()) {
						long t1 = currentTimeMillis();
						LOG.debug("Flushing '{}', {} events in {} ms", filter, context.accumulated, t1 - t0);
					}
				} else {
					LOG.debug("Discarding '{}', {} events in {} ms - component is not running.", filter, context.accumulated);
				}
				context.accumulated = 0;
			}
		} catch(Exception e) {
			LOG.error("Cannot flush message", e);
		}
	}
}
