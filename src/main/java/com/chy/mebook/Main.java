package com.chy.mebook;

import java.io.File;
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

public class Main {
	private static Log log = LogFactory.getLog(Main.class);

	private Set<String> bookUrls;
	private String homepage;
	private String mebookFilePath;
	private String tyy189Path;

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
		File mebookFile = new File(mebookFilePath);
		File tyy189File = new File(tyy189Path);
		if (mebookFile.exists()) {
			log.info("mebookFile exists, remove it");
			mebookFile.delete();
		}
		if (tyy189File.exists()) {
			log.info("tyy189File exists, remove it");
			tyy189File.delete();
		}
	}

	public void crawleSite() {
		HtmlPage monthPage = null;
		try {
			monthPage = new HtmlPage(new URI(homepage));
			getBooksByMonth(monthPage);
		} catch (URISyntaxException e) {
			e.printStackTrace();
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
		while (iter.hasNext()) {
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
