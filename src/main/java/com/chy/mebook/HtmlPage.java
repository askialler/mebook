package com.chy.mebook;

import static com.chy.mebook.PageParser.*;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpGet;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class HtmlPage {
	private static Log log = LogFactory.getLog(HtmlPage.class);
	private String html = null;
	private URI uri = null;

	public HtmlPage(URI uri) {
		this.uri = uri;
		this.html = getHtmlPage(new HttpGet(uri));
	}

	public URI getUri() {
		return uri;
	}

	public List<URI> getAllLinks() {
		Document rootDoc = parseWebPage(html);
		List<URI> list = new LinkedList<URI>();
		Elements eles = rootDoc.getElementsByTag("a");
		if (eles != null) {
			Iterator<Element> it = eles.iterator();
			while (it.hasNext()) {
				String temp = it.next().attr("href").trim();
				if (temp.matches("^http.*") || temp.matches("^[/.].*")) {
					try {
						list.add(new URI(temp));
					} catch (URISyntaxException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return list;
	}

	@Override
	public String toString() {
		return html;
	}

	public static void main(String[] args) {
		String mebook = "http://mebook.cc/";
		// String homepage = getHtmlPage(new HttpGet(mebook));

		HtmlPage home = null;
		try {
			home = new HtmlPage(new URI(mebook));
			// if(log.isDebugEnabled()){
			// log.debug(homepage);
			// }
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		if (home != null) {
			List<URI> list = home.getAllLinks();
			if (log.isDebugEnabled()) {
				for (URI uri : list) {
					log.debug(uri.toString());
				}
			}
		}
	}

}
