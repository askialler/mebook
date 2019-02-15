package com.chy.mebook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.chy.mebook.utils.ConfigUtil;
import com.chy.mebook.utils.FileUtils;

public class Main {
	private static Log log = LogFactory.getLog(Main.class);

	private Set<String> bookUrls;
	private String homepage;
	private String mebookFilePath;
	private String tyy189Path;
	private int last_crawel_day = 0;
	private boolean done = false;

	public boolean isDone() {
		return done;
	}

	public void setDone(boolean done) {
		this.done = done;
	}

	public Main(String homepage, String yearAndMonth, String fileSavePath) {
		this.homepage = homepage;
		this.bookUrls = new HashSet<String>();
		File savePath = new File(fileSavePath);
		if (!savePath.exists()) {
			savePath.mkdirs();
		}
		mebookFilePath = fileSavePath + File.separator + "mebookcn_" + yearAndMonth + ".txt";
		tyy189Path = fileSavePath + File.separator + "tyy189_" + yearAndMonth + ".txt";
		log.info("mebookcn file: " + mebookFilePath);
		log.info("tyy189 file: " + tyy189Path);

	}

	public void crawleSite() {

		// 备份旧文件，获取上次爬取的位置
		File mebookFile = new File(mebookFilePath);
		File tyy189File = new File(tyy189Path);
		if (mebookFile.exists()) {
			log.info("mebookFile exists, move it to .bak, and record last crawel day");
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(mebookFile));
				br.readLine();
				br.readLine();
				String pubDate = br.readLine();
				this.last_crawel_day = getPubDay(pubDate);
				log.info("last crawel day is: " + this.last_crawel_day);
			} catch (FileNotFoundException e) {
				log.error(e.getMessage(), e);
			} catch (IOException e) {
				log.error(e.getMessage(), e);
			} finally {
				if (br != null) {
					try {
						br.close();
					} catch (IOException e) {
						log.error(e.getMessage(), e);
					}
				}
				mebookFile.renameTo(new File(mebookFilePath + ".bak"));
				log.debug("move file to: " + mebookFilePath + ".bak");
			}
			if (tyy189File.exists()) {
				tyy189File.renameTo(new File(tyy189Path + ".bak"));
				log.debug("move file to: " + tyy189Path + ".bak");
			}
		}

		// 开始本次爬取
		HtmlPage monthPage = null;
		try {
			monthPage = new HtmlPage(new URI(homepage));
			getBooksByMonth(monthPage);
		} catch (URISyntaxException e) {
			log.error(e.getMessage(), e);
		}
		// 读取上次爬取的内容追加到本次爬取文件后，并删除备份文件
		File mebookBakFile =new File(mebookFilePath + ".bak");
		if(mebookBakFile.exists()) {
			String bakStr = FileUtils.readFile(mebookBakFile);
			FileUtils.writeFile(mebookFilePath, bakStr);
			mebookBakFile.delete();
			File tyy189BakFile =new File(tyy189Path + ".bak");
			if(tyy189BakFile.exists()) {
				String bakStr2 = FileUtils.readFile(tyy189BakFile);
				FileUtils.writeFile(tyy189Path, bakStr2);
				tyy189BakFile.delete();
			}
		}
	}

	public void getBooksByMonth(HtmlPage monthPage) throws URISyntaxException {

		if (monthPage != null) {
			Document page1 = PageParser.parseWebPage(monthPage.toString());
			getBookPage(page1);
			String strPageNum = page1.select(".page-numbers").text().split(" ")[5];
			log.debug("current month page number: " + strPageNum);
			int pageNum = Integer.parseInt(strPageNum);
			for (int i = 2; i <= pageNum; i++) {
				URI pageUrl = null;
				try {
					if (isDone()) {
						log.info("crawel job is done this time!");
						return;
					}
					pageUrl = new URI(monthPage.getUri().toString() + "/page/" + i);
					log.debug("pageUrl: " + pageUrl);
					HtmlPage pagei = new HtmlPage(pageUrl);
					getBookPage(PageParser.parseWebPage(pagei.toString()));

				} catch (URISyntaxException e) {
					e.printStackTrace();
				}

			}

		}
	}

	public void getBookPage(Document dateDoc) throws URISyntaxException {
		String bookUrl = null;
		String bookTitle = null;
		Elements eles = dateDoc.select("ul.list h2>a[title]");
		Iterator<Element> iter = eles.iterator();
		while (iter.hasNext() && !isDone()) {
			Element el = iter.next();
			bookUrl = el.attr("href");
			bookTitle = el.text();
			if (log.isDebugEnabled()) {
				log.debug(bookTitle);
				log.debug(bookUrl);
			}
			if (!this.bookUrls.contains(bookUrl)) {
				bookUrls.add(bookUrl);
				getBookItem(bookUrl);
			} else {
				if (log.isWarnEnabled()) {
					log.warn(bookUrl + "has been downloaded.  discard!");
				}
			}

		}
	}

	private void getBookItem(String uri) throws URISyntaxException {
		HtmlPage bookPage = new HtmlPage(new URI(uri));

		Document bookDoc = PageParser.parseWebPage(bookPage.toString());
		Elements eles = bookDoc.select("div#primary>h1.sub");
		Book book = new Book();
		try {
			book.setTitle(eles.first().text());
			book.setMebookAddr(uri.toString());
			String pubDate = bookDoc.select("div#primary>div.postinfo>div.left").text()
					.split("发表于 ")[1].split(" ")[0];
			if (getPubDay(pubDate) <= this.last_crawel_day) {
				setDone(true);
				return;
			}
			book.setPubDate(pubDate);
			StringBuilder strb = new StringBuilder();
			eles = bookDoc.select("#content p");
			Iterator<Element> iter = eles.iterator();
			while (iter.hasNext()) {
				strb.append(iter.next().text());
				strb.append("\r\n");
			}
			book.setIntroduction(strb.toString());

			String dlAddr = bookDoc.select("p.downlink a.downbtn").first().attr("href");
			if (log.isDebugEnabled()) {
				log.debug("book[" + book.getTitle() + ", dlAddr: " + dlAddr);
			}
			String[] bdys = getBDYun(dlAddr);
			book.setDownloadAddr(bdys[0]);
			book.setDownloadPassword(bdys[1]);
			if (log.isDebugEnabled()) {
				log.debug("*******************************************************************");
				log.debug("publication date: " + book.getPubDate());
				log.debug("book[" + book.getTitle() + "]");
				log.debug("bdyun addr: " + book.getDownloadAddr());
				log.debug("bdy password: " + book.getDownloadPassword());
				log.debug("*******************************************************************");
			}

			Book.writeBook(mebookFilePath, book);
			Book.write189(tyy189Path, book);
		} catch (NullPointerException e) {
			log.error(e.getMessage());
			log.error("can not find the download information for thi	s book:" + book.getTitle());
			log.error("  " + book.getTitle());
			log.error("  " + book.getMebookAddr());
		}

	}

	private String[] getBDYun(String dlAddr) throws URISyntaxException {
		String[] bdyStrs = new String[2];
		HtmlPage dlPage = new HtmlPage(new URI(dlAddr));
		Document dlDoc = PageParser.parseWebPage(dlPage.toString());

		Elements eles1 = dlDoc.select("div.desc");
		Iterator<Element> iter = eles1.iterator();
		while (iter.hasNext()) {
			Element passwdEle = iter.next();
			if (!passwdEle.select("h3").isEmpty()) {
				Elements elePS = passwdEle.select("p");
				Iterator<Element> iter3 = elePS.iterator();
				while (iter3.hasNext()) {
					Element p = iter3.next();
					if (p.text().contains("密码")) {
						bdyStrs[1] = p.text();
					}
				}
			}
		}

		Elements eles2 = dlDoc.select("div.list>a");
		StringBuilder sb = new StringBuilder();
		Iterator<Element> iter2 = eles2.iterator();
		while (iter2.hasNext()) {
			Element dlink = iter2.next();
			sb.append(dlink.attr("href")).append("\r\n");

		}
		bdyStrs[0] = sb.toString();
		return bdyStrs;
	}

	private int getPubDay(String pubDate) {
		String pubDateN = pubDate.replaceAll("[年月日]", " ");
		String[] pieces = pubDateN.split(" ");
		return Integer.parseInt(pieces[2]);
	}

	public static void main(String[] args) {
		// PropertyConfigurator.configure("./conf/log4j.properties");
		String fileSavePath = "";
		String yearAndMonth = "";
		if (args.length == 2) {
			fileSavePath = args[0];
			yearAndMonth = args[1];
		} else {
			log.info("no args, read config file.");
			fileSavePath = ConfigUtil.getValue("file.save.path");
			yearAndMonth = ConfigUtil.getValue("mebook.books.month");
		}
		log.info("fileSavePath: " + fileSavePath);
		log.info("yearAndMonth: " + yearAndMonth);

		Main mainTask = new Main("http://mebook.cc/date/" + yearAndMonth.substring(0, 4) + "/"
				+ yearAndMonth.substring(4), yearAndMonth, fileSavePath);
		mainTask.crawleSite();

	}

}
