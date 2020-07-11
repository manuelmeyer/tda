package com.vodafone.dca.common;

import static java.nio.file.Files.readAllLines;
import static java.nio.file.Paths.get;
import static org.springframework.util.StringUtils.commaDelimitedListToStringArray;
import static org.springframework.util.StringUtils.isEmpty;
import static org.springframework.util.StringUtils.trimAllWhitespace;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

public class FileUtils  {
	
	static public File changeSuffix(File source, String newSuffix) {
		String targetName = source.getName();
		int index = targetName.lastIndexOf(".");
		if(index == -1) {
			targetName += newSuffix;
		} else {
			targetName = targetName.substring(0, index) + newSuffix;
		}
		return new File(source.getParent() + "/" + targetName);
	}
	
	static public File fileFromPath(String path) throws FileNotFoundException {
		File file = new File(path);
		boolean exists = file.isFile();
		if(exists) {
			return file;
		}
		URL url = FileUtils.class.getClassLoader().getResource(path);
		if(url != null) {
			return new File(url.getPath());
		}
		throw new FileNotFoundException(path);
	}
	
	static public long lastModified(String path) {
		try {
			return fileFromPath(path).lastModified();
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	static public String[] getFieldsFromFile(String path) {
		try {
			File file = fileFromPath(path);
			List<String> lines = readAllLines(get(file.getCanonicalPath()), StandardCharsets.UTF_8);
			for(String line : lines) {
				line = trimAllWhitespace(line);
				if(isEmpty(line)) {
					continue;
				}
				if(line.startsWith("#")) {
					continue;
				}
				return commaDelimitedListToStringArray(line);
			}
			return new String[0];
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static Properties readProperties(String path)  throws FileNotFoundException, IOException {
		File propertiesFile = fileFromPath(path);
		Properties props = new Properties();
		props.load(new FileReader(propertiesFile));
		return props;
	}
}
