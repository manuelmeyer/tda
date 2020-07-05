package com.vodafone.dca.file;

import static com.vodafone.dca.file.GenericFileNameGenerator.ThresholdType.NONE;
import static com.vodafone.dca.file.GenericFileNameGenerator.ThresholdType.SIZE;
import static com.vodafone.dca.file.GenericFileNameGenerator.ThresholdType.TIME;
import static org.springframework.util.StringUtils.isEmpty;

import java.io.File;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.messaging.Message;

import com.google.common.io.Files;

public class GenericFileNameGenerator implements FileNameGenerator {

	static final Logger LOG = LoggerFactory.getLogger(GenericFileNameGenerator.class);
	
	private Map<String, FileInfo> fileNameMap = new ConcurrentHashMap<String, FileInfo>();
	
	private long fileSizeThreshold;
	private String tempPrefix = ".writing";
	private String finalPrefix = ".csv";
	private long fileTimeThreshold;
	private String filePrefix = "";
	private String directory;
	
	private long moveDelay = 200;
	
	public enum ThresholdType {
		NONE,
		SIZE,
		TIME
	}
	
	ThresholdType mode = NONE;
	
	public void setFilePrefix(String filePrefix) {
		if(isValidPrefix(filePrefix)) { //!StringUtils.isEmpty(filePrefix) && filePrefix.contains("/")){
			LOG.info("Adding file prefix {} on file name", filePrefix);
			this.filePrefix = filePrefix;
		}
	}
	
	private final String[] forbiddenChars = { "/", " "};
	
	private boolean isValidPrefix(String filePrefix) {
		if(isEmpty(filePrefix)) {
			return false;
		}
		
		for(String forbiddenChar : forbiddenChars) {
			if(filePrefix.contains(forbiddenChar)) {
				LOG.error("File prefix '{}' contains forbidden char '{}'", forbiddenChar);
				return false;
			}
		}
		return true;
	}

	public void setMoveDelay(long delay) {
		if(delay > 0) {
			moveDelay = delay;
		}
		LOG.info("Move file delay set to {} ms", moveDelay);
	}
	
	public void setFileSizeThreshold(long fileSizeThreshold) {
		this.fileSizeThreshold = fileSizeThreshold;
		if(fileSizeThreshold > 0) {
			LOG.info("File size threshold mode set with a limit of {} bytes", fileSizeThreshold);
			mode = SIZE;
		}
	}
	
	public void setFileTimeThreshold(long fileTimeThreshold) {
		this.fileTimeThreshold = fileTimeThreshold * 1000;
		if(fileTimeThreshold > 0) {
			LOG.info("File time threshold mode set with a limit of {} seconds", fileTimeThreshold);
			mode = TIME;
		}
	}
	
	public void setDirectory(String directory) {
		this.directory = directory;
		if(!this.directory.endsWith("/")) {
			this.directory += "/";
		}
		LOG.info("File name base directory is {}", this.directory);
	}
	
	Timer moveFileTimer = new Timer();
	
	class MoveFileTimerTask extends TimerTask {
		final String fromPath;
		final String toPath;
		
		public MoveFileTimerTask(String fromPath, String toPath) {
			this.fromPath = fromPath;
			this.toPath = toPath;
		}
		
		@Override
		public void run() {
			if(LOG.isDebugEnabled()) {
				LOG.debug("Moving {} to {}", fromPath, toPath);
			}
			try {
				File from = new File(fromPath);
				File to = new File(toPath);
				if(from.exists()) {
					Files.move(from, to);
				}
			} catch (IOException e) {
				LOG.error("Cannot move {} to {}", fromPath, toPath);
			}
		}
	}

	
	class FileInfo {
		String currentName;
		String currentBaseName;
		long currentSize = 0;
		AtomicLong fileIndex = new AtomicLong(0);
		long currentTime = 0;
		
		public FileInfo(String type) {
			newFile(type);
		}
		
		public String getFileName(String type, int addedSize) {
			// for SIZE and TIME we modify FileInfo class members, hence the sync() 
			switch(mode) {
			case SIZE :
				synchronized(this) {
					return getFileNameSize(type, addedSize);
				}
			case TIME :
				synchronized(this) {
					getFileNameTime(type);
				}
			default :
				return getFileName(type);
			}
		}
		
		protected String getFileName(String type) {
			String newName = newBaseName(type);
			newName = newName + finalPrefix;
			LOG.debug("Newly generated file is {}", newName);
			return newName;
		}

		private String newBaseName(String type) {
			long currentIndex = fileIndex.incrementAndGet() & 0x0fff;
			Format formatter = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
			String newName = String.format("%s%s-%s-%03x",
										filePrefix,
										type, 
										formatter.format(new Date()),
										currentIndex);
			return newName;
		}
		
		protected String getFileNameTime(String type) {
			long time = System.currentTimeMillis();
			if((time - currentTime) > fileTimeThreshold) {
				String lastBase = currentBaseName;
				if(LOG.isDebugEnabled()) {
					LOG.debug("New file needed, current file name is {}, current time is {}, threshold is {}, file time is {}", 
										new Object[] {
											currentName, time, fileTimeThreshold, currentTime
										});
				}
				newFile(type);
				move(lastBase);
			}
			currentTime = time;
			return currentName;
		}

		protected String getFileNameSize(String type, int addedSize) {
			if((currentSize + addedSize) > fileSizeThreshold) {
				String lastBase = currentBaseName;
				if(LOG.isDebugEnabled()) {
					LOG.debug("New file needed, current file name is {}, current size is {}, threshold is {}, adding {}", 
											new Object[] {
												currentName, currentSize, fileSizeThreshold, addedSize
											});
				}
				newFile(type);
				move(lastBase);
				currentSize = addedSize;
			} else {
				currentSize += addedSize;
			}
			return currentName;
		}
		
		protected void move(String lastBase) {
			String fromPath = directory + lastBase + tempPrefix;
			String toPath = directory + lastBase + finalPrefix;
			moveFileTimer.schedule(new MoveFileTimerTask(fromPath, toPath), moveDelay);
		}
		
		protected void newFile(String type) {
			currentBaseName = newBaseName(type);
			currentName = currentBaseName + tempPrefix;
			if(LOG.isDebugEnabled()) {
				LOG.debug("Newly generated file is {}", currentName);
			}
		}
	}
	
	@Override
	public String generateFileName(Message<?> message) {
		Object header = message.getHeaders().get("data-type");
		String type = (header == null) ? "data" : header.toString();
		FileInfo fileInfo = fileNameMap.get(type);
		if(fileInfo == null) {
			synchronized(fileNameMap) {
				fileInfo = fileNameMap.get(type);
				if(fileInfo == null) {
					fileInfo = new FileInfo(type);
					fileNameMap.put(type, fileInfo);
				}
			}
		}
		String msg = (String) message.getPayload();
		return fileInfo.getFileName(type, msg.length());
	}
}
