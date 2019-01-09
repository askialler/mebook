package com.chy.mebook;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.PropertyConfigurator;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Main {
	private static Log log = LogFactory.getLog(Main.class);

	private Set<String> bookUrls;
	private String homepage;
	private String year_month;

	public Main(String homepage, String year_month) {
		this.homepage = homepage;
		this.year_month = year_month;
		this.bookUrls = new HashSet<String>();
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
			// HtmlPage datePage=new HtmlPage(uri);
			// System.out.println(catagery);
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
		String bookTitle=null;
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
			if(!this.bookUrls.contains(bookUrl)) {
				bookUrls.add(bookUrl);
				getBookItem(bookUrl);
			} else {
				if (log.isWarnEnabled()) {
					log.warn(bookUrl  + "has been downloaded.  discard!");
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
			// if (log.isDebugEnabled()) {
			// log.debug("book[" + book.getTitle() + ", introduction: " +
			// book.getIntroduction());
			// }

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
				// log.debug("introduction: "+ book.getIntroduction());
				log.debug("bdyun addr: " + book.getDownloadAddr());
				log.debug("bdy password: " + book.getDownloadPassword());
				log.debug("*******************************************************************");
			}
			SimpleDateFormat sdf = new SimpleDateFormat("yyMMdd");

			Book.writeBook("e:\\ResilioSync\\mebookcn" + this.year_month + ".txt", book);
		} catch (NullPointerException e) {
			// e.printStackTrace();
			log.error(e.getMessage());
			log.error("can not find the download information for this book:" + book.getTitle());
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
			// if (dlink.text().equals("百度网盘")) {
			// bdyStrs[0] = dlink.attr("href");
			// }

		}
		bdyStrs[0] = sb.toString();
		return bdyStrs;
	}

	public static void main(String[] args) {
		PropertyConfigurator.configure("./conf/log4j.properties");
		// Properties props=System.getProperties();
		// System.out.println(props.toString());
		// Set<Object> keys= props.keySet();
		// Iterator<Object> iter=keys.iterator();
		// while(iter.hasNext()) {
		// String key=(String)iter.next();
		// System.out.println(key+": "+props.getProperty(key));
		// }
		String year_month = "201812";
		Main mainTask = new Main("http://mebook.cc/date/" + year_month.substring(0, 4) + "/"
				+ year_month.substring(4), year_month);
		mainTask.crawleSite();

	}
}
