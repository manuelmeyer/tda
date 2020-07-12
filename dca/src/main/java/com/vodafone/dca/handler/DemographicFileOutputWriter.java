package com.vodafone.dca.handler;

import static com.vodafone.dca.common.FileUtils.fileFromPath;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.concurrent.FutureTask;

import org.assertj.core.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

import com.google.common.collect.Lists;
import com.vodafone.dca.common.FileUtils;
import com.vodafone.dca.domain.DataTransporter;
import com.vodafone.dca.domain.properties.DemographicsOutputProperties;
import com.vodafone.dca.filter.InOrOutListBasedFilter;
import com.vodafone.dca.shell.SpawnProcess;
import com.vodafone.dca.transformer.MapFieldReducer;
import com.vodafone.dca.transformer.ParsedElementListToDataTransporter;

public class DemographicFileOutputWriter {
	
	private static final Logger LOG = LoggerFactory.getLogger(DemographicFileOutputWriter.class);
	
	@Autowired
	private InOrOutListBasedFilter blackListFilter;
	
	@Autowired
	private InOrOutListBasedFilter whiteListFilter;
	
	private DelimitedLineTokenizer lineTokenizer;
	
	private ParsedElementListToDataTransporter parser;
	
	private MapFieldReducer reducer;
	
	private DemographicsOutputProperties outputProperties;
	
	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
	
	public File generateOutputFile(File input) {
		LOG.info("Creating demographics from {}", input);
		List<DataTransporter> dispatchList = Lists.newArrayList();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(input), 4 * 1024 * 1024)) {
			String line = null;
			
			File output = FileUtils.changeSuffix(input, ".processing");
			FlatFileItemWriter<DataTransporter> writer = newFlatFileItemWriter(output);
			
			while((line = reader.readLine()) != null) {
				writeLineToOutput(dispatchList, line, writer);
			}
			
			if(!dispatchList.isEmpty()) {
				writer.write(dispatchList);
			}
			writer.close();
			runEndScript(input, output);
		} catch(Exception e) {
			LOG.error("Exception while processing file", e);
		}
		return null;
	}

	private void runEndScript(File input, File output) throws Exception {
		String[] environmentParams = new String[] { 
				String.format("FILEIN=%s", input.getAbsolutePath()),
				String.format("FILEOUT=%s", output.getAbsolutePath())
		};
		File workingDirectory = null;
		FutureTask<Integer> systemCommandTask = 
				new FutureTask<Integer>(new SpawnProcess(fileFromPath(outputProperties.getEndScript()).getAbsolutePath(), 
																							environmentParams, 
																							workingDirectory));
		taskExecutor.execute(systemCommandTask);
		int status = systemCommandTask.get();
		LOG.debug("{} exit status {}", outputProperties.getEndScript(), status);
	}

	private void writeLineToOutput(List<DataTransporter> dispatchList, String line, 
									FlatFileItemWriter<DataTransporter> writer) throws Exception {
		DataTransporter dataTransporter = 
				parser.buildFromObjectList(Arrays.asList(lineTokenizer.tokenize(line).getValues()));
		if(blackListFilter.accept(dataTransporter) && whiteListFilter.accept(dataTransporter)) {
			dispatchList.add(dataTransporter);
			if(dispatchList.size() == outputProperties.getBatchSize()) {
				LOG.info("1.writing {} items", dispatchList.size());
				writer.write(dispatchList);
				dispatchList.clear();
			}
		}
	}

	private FlatFileItemWriter<DataTransporter> newFlatFileItemWriter(File targetFile) throws Exception {
		LOG.info("Creating FlatFileWriter for {}", targetFile);
		FlatFileItemWriter<DataTransporter> writer = new FlatFileItemWriter<>();
		Resource resource = new FileSystemResource(targetFile);
		writer.setResource(resource);
		writer.setSaveState(false);
		writer.setLineAggregator(dataTransporter -> reducer.convert(dataTransporter));
		writer.afterPropertiesSet();
		writer.open(new ExecutionContext());
		return writer;
	}
	
	static public Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private DelimitedLineTokenizer lineTokenizer;
		private ParsedElementListToDataTransporter parser;
		private MapFieldReducer reducer;
		private DemographicsOutputProperties outputProperties;

		public Builder withLineTokeniserAndParser(DelimitedLineTokenizer lineTokenizer, ParsedElementListToDataTransporter parser) {
			this.lineTokenizer = lineTokenizer;
			this.parser = parser;
			return this;
		}
		
		public Builder withReducer(MapFieldReducer reducer) {
			this.reducer = reducer;
			return this;
		}
		
		public Builder withOutputProperties(DemographicsOutputProperties outputProperties) {
			this.outputProperties = outputProperties;
			return this;
		}
		
		public DemographicFileOutputWriter build() {
			DemographicFileOutputWriter built = new DemographicFileOutputWriter();
			built.lineTokenizer = this.lineTokenizer;
			built.outputProperties = this.outputProperties;
			built.parser = this.parser;
			built.reducer = this.reducer;
			return built;
		}
	}
}
