package com.dell.rti4t.xd.kafkasource;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.integration.monitor.ExponentialMovingAverageRate;

public class TestKafka {
	public static void main(String args[]) throws Exception {
		new TestKafka().init();
	}
	
	int totalThreads = 8;
	//KafkaConsumer<String, String> consumer;
    ExponentialMovingAverageRate average = new ExponentialMovingAverageRate(1, 60, 10);
    
    CountDownLatch latch = new CountDownLatch(totalThreads);
    AtomicLong total = new AtomicLong(0);
	
	Properties props = new Properties();
    List<String> topics = new ArrayList<String>();

	private void init() throws Exception {
		props.put("bootstrap.servers", "localhost:9092");
	    props.put("group.id", "kafka-test-2");
	    props.put("key.deserializer", StringDeserializer.class.getName());
	    props.put("value.deserializer", ByteArrayDeserializer.class.getName());
	    
	    topics.add("adr");
	    topics.add("gb");
	    
//	    consumer = new KafkaConsumer<>(props);
//	    consumer.subscribe(topics);
	    for(int index = 0; index < totalThreads; index++) {
	    	new Thread(new MyConsumer()).start();
	    }
	    latch.await();
	    long t0 = System.nanoTime();
	    for(;;) {
	    	Thread.sleep(5000);
//	    	long t1 = System.nanoTime();
//	    	double n = (double)total.longValue();
//	    	double mean = n * (1_000_000_000.00) / (t1 - t0);
	    	System.out.println(String.format("%1$,.2f", average.getMean()));
	    	//Thread.sleep(5000);
	    }
	}

    class MyConsumer implements Runnable {
		public void run() {
			latch.countDown();
			KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(props);
		    consumer.subscribe(topics);
			for(;;) {
				ConsumerRecords<String, byte[]> records = consumer.poll(Long.MAX_VALUE);
				for(int record = 0; record < records.count(); record++) {
					total.incrementAndGet();
					average.increment();
				}
			}
		}
    }
}
