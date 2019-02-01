package com.chy.mebook.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class ConfigUtil {

	private static Log log = LogFactory.getLog(ConfigUtil.class);
	private static Properties properties = new Properties();

	static {
		log.debug("ready to load config.properties");
		InputStream input = null;
		try {
			input = new FileInputStream(new File("./conf/config.properties"));
			properties.load(input);
		} catch (FileNotFoundException e) {
			log.error("can not find config file: config.properties", e);
			System.exit(-1);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		}
	}

	public static String getValue(String key) {
		return properties.getProperty(key);
	}

}
