package com.chy.mebook;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.chy.mebook.utils.FileUtils;

public class Book {
	public Book(){
		
	}
	
	private static Log log = LogFactory.getLog(PageParser.class);
	private String title;
	private String introduction;
	private String downloadAddr;
	private String downloadPassword;
	private String mebookAddr;
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the introduction
	 */
	public String getIntroduction() {
		return introduction;
	}
	/**
	 * @param introduction the introduction to set
	 */
	public void setIntroduction(String introduction) {
		this.introduction = introduction;
	}
	/**
	 * @return the downloadAddr
	 */
	public String getDownloadAddr() {
		return downloadAddr;
	}
	/**
	 * @param downloadAddr the downloadAddr to set
	 */
	public void setDownloadAddr(String downloadAddr) {
		this.downloadAddr = downloadAddr;
	}
	/**
	 * @return the downloadPassword
	 */
	public String getDownloadPassword() {
		return downloadPassword;
	}
	/**
	 * @param downloadPassword the downloadPassword to set
	 */
	public void setDownloadPassword(String downloadPassword) {
		this.downloadPassword = downloadPassword;
	}
	/**
	 * @return the title
	 */
	public String getMebookAddr() {
		return mebookAddr;
	}
	/**
	 * @param title the title to set
	 */
	public void setMebookAddr(String mebookAddr) {
		this.mebookAddr = mebookAddr;
	}

	public static void writeBook(String filename,Book book){
		StringBuilder content=new StringBuilder();
		content.append("********************************************************\r\n");
		content.append("********************************************************\r\n");
		content.append("title: "+book.title+"\r\n");
		content.append("mebook address: "+book.mebookAddr+"\r\n");
		content.append("introduction: "+book.introduction+"\r\n");
		content.append("download link:\r\n"+book.downloadAddr+"\r\n");
		content.append("download password: "+book.downloadPassword+"\r\n");
		content.append("\r\n\r\n");
		FileUtils.writeFile(filename, content.toString());
		
	}
	
	public static void write189(String filename,Book book){
		String link189 = null;
		BufferedReader reader = new BufferedReader(new StringReader(book.downloadAddr));
		boolean hasLink189=false;
		try {
			while((link189 = reader.readLine()) != null) {
				if(link189.startsWith("https://cloud.189.cn")) {
					FileUtils.writeFile(filename, link189 + System.lineSeparator());
					hasLink189 = true;
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}finally {
			if(!hasLink189) {
				log.warn("this book has no tyy189 download link:  "+book.getTitle());
			}
			try {
				reader.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
