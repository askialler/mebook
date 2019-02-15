package com.chy.mebook.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FileUtils {

	private static Log log = LogFactory.getLog(FileUtils.class);

	public static void writeFile(String filename, String content) {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(filename, true));
			bw.write(content);

		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} finally {
			try {
				if (bw != null) {
					bw.flush();
					bw.close();
				}
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			}
		}
	}

	public static String readFile(File filename) {
		FileReader reader = null;
		StringBuilder sb = new StringBuilder();
		try {
			reader = new FileReader(filename);
			int maxLen = 4096;
			char[] buf = new char[maxLen];
			int num = -1;
			while ((num = reader.read(buf, 0, maxLen)) != -1) {
				sb.append(buf, 0, num);
			}

		} catch (FileNotFoundException e) {
			log.error(e.getMessage(), e);
		} catch (IOException e) {
			log.error(e.getMessage(), e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					log.error(e.getMessage(), e);
				}
			}
		}
		return sb.toString();
	}

	public static String readFile(String filename) {
		File file = new File(filename);
		return readFile(file);
	}
}
