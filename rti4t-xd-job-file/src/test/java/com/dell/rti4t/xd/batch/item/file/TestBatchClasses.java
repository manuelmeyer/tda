package com.dell.rti4t.xd.batch.item.file;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.core.io.FileSystemResource;

import com.dell.rti4t.xd.batch.item.file.DataTranporterWriterAggregator;
import com.dell.rti4t.xd.batch.item.file.DataTransporterLineMapper;
import com.dell.rti4t.xd.domain.DataTransporter;
import com.dell.rti4t.xd.transformer.MapFieldReducerImpl;
import com.dell.rti4t.xd.transformer.OffsetListToDataTransporterImpl;
import com.dell.rti4t.xd.utils.FileUtils;

public class TestBatchClasses {
	
	@Test
	public void canUseDataTransporter() throws Exception {
		FlatFileItemReader<DataTransporter> itemReader = buildItemReader();
		
		ExecutionContext ec = new ExecutionContext();
		itemReader.open(ec);
		
		FlatFileItemWriter<DataTransporter> itemWriter = buildItemWriter();
		itemWriter.open(ec);
		
		for(;;) {
			DataTransporter dt = itemReader.read();
			if(dt == null) {
				break;
			}
			System.out.println(String.format("read %s", dt.toString()));

			List<DataTransporter> items = new ArrayList<DataTransporter>();
			items.add(dt);
			itemWriter.write(items);
		}
	}

	private FlatFileItemWriter<DataTransporter> buildItemWriter() throws Exception {

		DataTranporterWriterAggregator lineAggregator = new DataTranporterWriterAggregator();
		MapFieldReducerImpl fieldReducer = new MapFieldReducerImpl();
		fieldReducer.setAnonymiseSet(new String[] {"A"});
		fieldReducer.setFieldsOutNames(new String[] {"C", "A"});
		fieldReducer.afterPropertiesSet();
		lineAggregator.setMapFieldReducer(fieldReducer);
		
		FlatFileItemWriter<DataTransporter> writer = new FlatFileItemWriter<DataTransporter>();
		writer.setLineAggregator(lineAggregator);
		writer.setResource(new FileSystemResource("/temp/out/test.csv"));
		writer.afterPropertiesSet();
		
		return writer;
	}

	private FlatFileItemReader<DataTransporter> buildItemReader() throws Exception {
		FlatFileItemReader<DataTransporter> itemReader =  new FlatFileItemReader<DataTransporter>();
		
		ListDelimitedLineTokenizer tokenizer = new ListDelimitedLineTokenizer();
		tokenizer.setDelimiter("|");
		
		OffsetListToDataTransporterImpl toDataTransporter = new OffsetListToDataTransporterImpl();
		toDataTransporter.setFieldNames(new String[] {"A", "B", "C", "D"});
		
		DataTransporterLineMapper lineMapper = new DataTransporterLineMapper();
		lineMapper.setLineTokenizer(tokenizer);
		lineMapper.setObjectListToDataTransporter(toDataTransporter);
		
		itemReader.setLineMapper(lineMapper);
		
		String demoFile = FileUtils.fileFromPath("demolike.txt").getAbsolutePath();
		itemReader.setResource(new FileSystemResource(demoFile));
		
		itemReader.afterPropertiesSet();
		return itemReader;
	}
}
