package com.chy.mebook.utils;


import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileUtils {

	public static void writeFile(String filename, String content) {
		BufferedWriter bw=null;
		try {
			// FileWriter fw=new FileWriter(filename, true);
			bw = new BufferedWriter(new FileWriter(filename, true));
			bw.write("********************************************************\r\n");
			bw.write("********************************************************\r\n");
			bw.write(content);

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if(bw !=null){
					bw.flush();
					bw.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}
		
//	public static void main(String[] args) {
//		SimpleDateFormat sdf=new SimpleDateFormat("yyMMddHHmm");
//		Date date= new Date();
//		System.out.println(sdf.format(date));
//		
//	}
}
