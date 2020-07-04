package com.dell.rti4t.xd.integration;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

@ContextConfiguration(locations = { "classpath:test-module-amqp.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
public class TestModuleAggAmqp {
	
	private static Logger LOG = LoggerFactory.getLogger(TestModuleAggAmqp.class);

	@Autowired
	QueueChannel output;
	
	int totalSent = 0;
	int totalReceived = 0;
	
	int totalImsiInList = 100_000;
	int totalImsiNotInList = 10_000;
	
	String tmpWhiteList = "/tmp/wl";
	String xdWhiteList = "/opt/SP/pivotal/output/capture/instance_30/refdata/white-list.test.csv";
	String xdLacCellFile = "/opt/SP/pivotal/output/capture/instance_30/refdata/lac-cells.csv";
	
	class LacCell {
		String lac;
		String cell;
		LacCell(String lac, String cell) {
			this.lac = lac;
			this.cell = cell;
		}
		@Override
		public String toString() {
			return "<LacCell lac=" + lac + ", cell=" + cell + ">";
		}
	}
	
	Set<String> setImsiInList = Sets.newHashSet();
	Set<String> setImsiNotInList = Sets.newHashSet();
	Random random = new Random();
	
	Map<String, Collection<String>> lacCells;
	
	String newImsi() {
		return String.format("23415%015d", random.nextInt());
	}
		
	protected void runRandom(String[] args) throws Exception {
		buildImsiInList();
		buildImsiNotInList();
		writeImsiInList();
		moveImsmFileToRefData();
		loadRefDataLacCells();
		scenario1();
	}
	
	private void scenario1() throws Exception {
		List<String> allImsis = getInList(1024);
		allImsis.addAll(getOutOfList(1024));
		List<LacCell> lacCells = getLacCell(2048);

		String timeUTC = System.currentTimeMillis() / 1000 + "000";
		StringBuffer toSend = new StringBuffer();
		for(int index = 0; index < allImsis.size(); index++) {
			String line = formatLine(allImsis.get(index), lacCells.get(index), timeUTC);
			toSend.append(line);
		}
		
		String payload = toSend.toString();
		Channel channel = createAMQPChannel();
		byte[] bytes = payload.getBytes();
		
		LOG.info("Sending {} bytes", bytes.length);
		channel.basicPublish("DECODER.STREAM", "TEST.LORS.TEST", null, bytes);
	}
	
	private String formatLine(String imsi, LacCell lacCell, String timeUTC) {
//		protocolName,imsi,lac,cellTower,startTimeUTC,timeUTC,latitude,longitude,mccmnc,msisdn,imei,transactionTarget,firstLac,firstCellTower,eventType,eventStatus,protocolDetailMap,ingestTime,processedTime
		return String.format("iups,%s,%s,%s,%s,%s,,,,,,,,,,,,,\n", imsi, lacCell.lac, lacCell.cell, timeUTC, timeUTC);
	}


	private List<LacCell> getLacCell(int count) {
		List<LacCell> testLacCell = Lists.newArrayList();
		int lacCount = lacCells.size();
		List<String> lacs = Lists.newArrayList();
		lacs.addAll(lacCells.keySet());
		for(int index = 0; index < count; index++) {
			String lac = lacs.get(genarateIntBoundedBy(lacCount - 1));
			List<String> cells = Lists.newArrayList();
			cells.addAll(lacCells.get(lac));
			String cell = cells.size() == 1 ? cells.get(0) : cells.get(genarateIntBoundedBy(cells.size() - 1));
			testLacCell.add(new LacCell(lac, cell));
		}
		return testLacCell;
	}


	int generateIntInRange(int low, int high) {
		return low + genarateIntBoundedBy(high - low);
	}
	
	int genarateIntBoundedBy(int bound) {
		return random.nextInt(bound);
	}

	private List<String> getInList(int count) {
		return getFromSet(setImsiInList, count);
	}

	private List<String> getOutOfList(int count) {
		return getFromSet(setImsiNotInList, count);
	}

	private List<String> getFromSet(Set<String> imsiSet, int count) {
		List<String> testList = Lists.newArrayList();
		Iterator<String> iterator = imsiSet.iterator();
		int index = 0;
		while(iterator.hasNext()) {
			if(index++ < count) {
				testList.add(iterator.next());
			} else {
				break;
			}
		}
		return testList;
	}

	private void loadRefDataLacCells() throws Exception {
		SetMultimap<String, String> multimapSet = HashMultimap.create();
		try(BufferedReader reader = new BufferedReader(new FileReader(xdLacCellFile), 16 * 1024 * 1024)) {
			String line;
			while((line = reader.readLine()) != null) {
				String[] fields = line.split(",");
				if (fields != null && fields.length == 2) {
					multimapSet.put(fields[0], fields[1]);
				}
			}
		}
		lacCells = multimapSet.asMap();
	}

	private void moveImsmFileToRefData()  throws Exception {
		LOG.info("Target {}", Paths.get(xdWhiteList));
		Files.move(Paths.get(tmpWhiteList), Paths.get(xdWhiteList), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
	}

	private void writeImsiInList()  throws Exception {
		BufferedWriter fileWriter = new BufferedWriter(new FileWriter(tmpWhiteList, false));
		for(String imsi : setImsiInList) {
			fileWriter.write(String.format("%s\n", imsi));
		}
		fileWriter.flush();
		fileWriter.close();
	}

	private void buildImsiInList() {
		for (int index = 0; index < totalImsiInList; index++) {
			String newImsi = newImsi();
			setImsiInList.add(newImsi);
		}
		while(setImsiInList.size() < totalImsiInList) {
			String newImsi = newImsi();
			setImsiInList.add(newImsi);
		}
		LOG.info("Total imsi in list {}", setImsiInList.size());
	}

	private void buildImsiNotInList() {
		for (int index = 0; index < totalImsiNotInList; index++) {
			setImsiNotInList.add(newImsi());
		}
		while(setImsiNotInList.size() < totalImsiNotInList) {
			setImsiNotInList.add(newImsi());
		}

		LOG.info("Total imsi in test {}", setImsiNotInList.size());
	}
	
	protected static void createImsiFile() throws Exception {
		BufferedWriter fileWriter = new BufferedWriter(new FileWriter("/tmp/wl", false));
		Random random = new Random();
		for(int index = 0; index < 20_000_000; index++) {
			fileWriter.write(String.format("2315%010d\n", Math.abs(random.nextInt())));
		}
		fileWriter.flush();
		fileWriter.close();
	}
	
	public static void main(String[] args) throws Exception {
		new TestModuleAggAmqp().run();
		System.exit(0);
	}

	private void run() throws Exception {
		LOG.info("Loading test data");
		//final String payload = loadTestData("simple.line"); 
		final String payload = loadTestData("networkrail.simple"); // sample.all.protocols");
		//final String payload = loadTestData("sample.all.protocols"); //networkrail.simple");
		final Channel channel = createAMQPChannel();
		
		final int totalMsg = 1;
		final int delay = 100;
		
		sendMessages(payload, totalMsg, channel, delay);
	}
	
	private void sendMessages(String payload, int total, Channel channel, int delay) {
		try {
			byte[] bytes = payload.getBytes();
			for(;;) {
				channel.basicPublish("DECODER.STREAM", "TEST.LORS.TEST", null, bytes);
				Thread.sleep(delay);
				totalSent++;
				if(totalSent % 1000 == 0) {
					LOG.info("Total sent is {}", totalSent);
				}
				if(total > 0 && totalSent == total) {
					break;
				}
			}
			LOG.info("Finish writting");
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	public void testAggregateWithFlatFormat() throws Exception {
		LOG.info("Loading test data");
		final String payload = loadTestData("sample.all.protocols");
		final Channel channel = createAMQPChannel();
		
		final int totalMsg = 2;
		Thread.sleep(10000); // time for everything to start
		
		new Thread(new Runnable() {
			@Override
			public void run() {
				sendMessages(payload, totalMsg, channel, 10);
			}
		}).start();
		
		long t0 = System.currentTimeMillis();
		for(int index = 0;; index++) {
			Message<String> msg = (Message<String>) output.receive(Long.MAX_VALUE);
			if(msg == null) {
				break;
			}
			String body = msg.getPayload();
			int max = Math.min(20, body.length());
			
//			if(index % 10 == 0) {
				LOG.info("Received message {}\n\twith headers {}\n\tand body {}", 
							index, 
							msg.getHeaders(),
							body.substring(0, max) + "...");
//			}
			totalReceived++;
		}
		long t1 = System.currentTimeMillis();
//		LOG.info("Total sent {}, total received {}", totalSent, totalReceived);
//		LOG.info("Total message red {} in {} ms, {} msg/s", totalMsg, (t1 - t0), (totalMsg * 1000)/(t1 - t0));
//		assertTrue(totalSent > 0 && totalSent == totalReceived);
	}

	static private String loadTestData(String file) throws Exception {
		ClassPathResource resource = new ClassPathResource(file);
		byte[] encoded = Files.readAllBytes(Paths.get(resource.getURI()));
		return new String(encoded);
	}
	
	static private Channel createAMQPChannel() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        
       factory.setUsername("decoder");
       factory.setPassword("decoder");
       factory.setVirtualHost("/");
       factory.setHost("localhost");
       factory.setPort(5672);
       Connection connection = factory.newConnection();

       Channel channel = connection.createChannel();
       return channel;

	}
}
