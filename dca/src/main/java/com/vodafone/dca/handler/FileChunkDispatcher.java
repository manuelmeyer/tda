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

public class FileChunkDispatcher {
	
	private static final Logger LOG = LoggerFactory.getLogger(FileChunkDispatcher.class);
	
	@Autowired
	private DelimitedLineTokenizer delimitedLineTokenizer;
	
	@Autowired
	private InOrOutListBasedFilter blackListFilter;
	
	@Autowired
	private InOrOutListBasedFilter whiteListFilter;
	
	@Autowired
	private ParsedElementListToDataTransporter demographicsOffsetListToDataTransporter;
	
	@Autowired
	private MapFieldReducer demographicsFieldReducer;
	
	@Autowired
	private DemographicsOutputProperties demographicsOutputProperties;
	
	private TaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
	
	public File generateOutputFile(File input) {
		LOG.info("Dispatching {}", input);
		List<DataTransporter> dispatchList = Lists.newArrayList();
		
		try (BufferedReader reader = new BufferedReader(new FileReader(input), 4 * 1024 * 1024)) {
			String line = null;
			
			File output = FileUtils.changeSuffix(input, ".processing");
			FlatFileItemWriter<DataTransporter> writer = newFlatFileItemWriter(output);
			
			while((line = reader.readLine()) != null) {
				writeLineToOutput(dispatchList, line, writer);
			}
			
			if(!dispatchList.isEmpty()) {
				LOG.info("flushing {} items", dispatchList.size());
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
				new FutureTask<Integer>(new SpawnProcess(fileFromPath(demographicsOutputProperties.getEndScript()).getAbsolutePath(), 
																							environmentParams, 
																							workingDirectory));
		taskExecutor.execute(systemCommandTask);
		int status = systemCommandTask.get();
		LOG.info("{} exit status {}", demographicsOutputProperties.getEndScript(), status);
	}

	private void writeLineToOutput(List<DataTransporter> dispatchList, String line, 
									FlatFileItemWriter<DataTransporter> writer) throws Exception {
		DataTransporter dataTransporter = 
				demographicsOffsetListToDataTransporter.buildFromObjectList(Arrays.asList(delimitedLineTokenizer.tokenize(line).getValues()));
		if(blackListFilter.accept(dataTransporter) && whiteListFilter.accept(dataTransporter)) {
			dispatchList.add(dataTransporter);
			if(dispatchList.size() == demographicsOutputProperties.getBatchSize()) {
				LOG.info("1.writing {} items", dispatchList.size());
				writer.write(dispatchList);
				dispatchList.clear();
			}
		}
	}

	private FlatFileItemWriter<DataTransporter> newFlatFileItemWriter(File targetFile) throws Exception {
		FlatFileItemWriter<DataTransporter> writer = new FlatFileItemWriter<>();
		Resource resource = new FileSystemResource(targetFile);
		writer.setResource(resource);
		writer.setSaveState(false);
		writer.setLineAggregator(dataTransporter -> demographicsFieldReducer.convert(dataTransporter));
		writer.afterPropertiesSet();
		writer.open(new ExecutionContext());
		return writer;
	}
}
