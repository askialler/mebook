package com.chy.mebook;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
//import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PageParser {

	private static HttpClient client;
	private static Log log = LogFactory.getLog(PageParser.class);

	public static void getHttpClient() {
		// if (client == null) {

		SSLContext sslCxt = null;
		try {

			sslCxt = SSLContexts.custom().loadTrustMaterial(null, new TrustStrategy() {
				// @Override
				public boolean isTrusted(X509Certificate[] chain, String authType)
						throws CertificateException {
					return true;
				}
			}).build();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyStoreException e) {
			e.printStackTrace();
		}
		// HostnameVerifier verifier = new HostnameVerifier() {
		//
		// public boolean verify(String hostname, SSLSession session) {
		// return true;
		// }
		// };
		HttpHost proxy = new HttpHost("10.126.3.161", 56000);

		SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslCxt,
				new HostnameVerifier() {

					public boolean verify(String hostname, SSLSession session) {
						return true;
					}
				});
		// SSLConnectionSocketFactory sslsf = new
		// SSLConnectionSocketFactory(sslCxt,
		// SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		Registry<ConnectionSocketFactory> registry = RegistryBuilder
				.<ConnectionSocketFactory> create()
				.register("http", PlainConnectionSocketFactory.getSocketFactory())
				.register("https", sslsf).build();
		PoolingHttpClientConnectionManager connMgr = new PoolingHttpClientConnectionManager(
				registry);
		connMgr.setMaxTotal(50);
		connMgr.setDefaultMaxPerRoute(50);
//		 use proxy
//		RequestConfig config = RequestConfig.custom().setConnectTimeout(10000)
//				.setConnectionRequestTimeout(10000).setSocketTimeout(60000).setProxy(proxy).build();
		RequestConfig config = RequestConfig.custom().setConnectTimeout(10000)
				.setConnectionRequestTimeout(10000).setSocketTimeout(60000).build();
		client = HttpClients.custom().setConnectionManager(connMgr).setDefaultRequestConfig(config)
				.build();
		if (log.isInfoEnabled()) {
			log.info("httpclient initialize successfully");
		}

		// }
		// return client;
	}

	static {
		getHttpClient();
	}

	public PageParser() {

	}

	public static String getHtmlPage(HttpRequestBase request) {

		// HttpGet httpget = new HttpGet(uri);
		// httpget.setURI(uri);
		StringBuilder sb = new StringBuilder();
		BufferedInputStream ins = null;

		HttpResponse resp = null;
		try {
			resp = client.execute(request);
			int statusCode = resp.getStatusLine().getStatusCode();

			if (statusCode == HttpStatus.SC_OK) {
				ins = new BufferedInputStream(resp.getEntity().getContent());

				int temp = 0;
				byte[] buffer = new byte[8192];
				while (-1 != (temp = ins.read(buffer, 0, 8192))) {
					sb.append(new String(buffer, 0, temp, "utf-8"));
				}
				// Header[] hs= resp.getAllHeaders();
				// for(Header hd:hs){
				// log.info(hd.toString());
				// }

			} else {
				log.error("request failed,http status: " + resp.getStatusLine());
			}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			if (log.isErrorEnabled()) {
				log.error("request webpage error: " + request.getURI().toString());
				e.printStackTrace();
			}
		} finally {
			log.debug("gethtmlpage end, httprequest end...");
			if (ins != null) {
				try {
					ins.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			request.abort();
		}
		return sb.toString();
	}

	public static HttpResponse getResponse(HttpRequestBase request) {

		// HttpGet httpget = new HttpGet(uri);
		// httpget.setURI(uri);
		// StringBuilder sb = new StringBuilder();
		// BufferedInputStream ins = null;

		// httpget.addHeader(new BasicHeader("Connection", "Keep-Alive"));
		// Header[] reqhs= httpget.getAllHeaders();
		// for(Header hd:reqhs){
		// log.info("request header: "+hd.toString());
		// }
		HttpResponse resp = null;
		try {
			resp = client.execute(request);

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			// e.printStackTrace();
			if (log.isErrorEnabled()) {
				log.error("request webpage error: " + request.getURI().toString());
				e.printStackTrace();
			}
		} finally {
			if (log.isInfoEnabled()) {
				log.info("gethtmlpage end, httpget end...");
			}

			request.abort();
		}
		return resp;
	}

	public static Document parseWebPage(String html) {
		return Jsoup.parse(html);
	}

	public static void writeFile(String filepath, String html) {

		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(filepath);
			fos.write(html.getBytes("utf-8"));

			fos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (fos != null) {
					fos.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String url = "http://mebook.cc/";
		String html = getHtmlPage(new HttpGet(url));
		System.out.println(html);
		// URI baseUri = null;
		// URI uri = null;
		// try {
		// baseUri = new URI("http://www.example.org/domains");
		// uri = new URI("http://www.example.org/domains");
		// } catch (URISyntaxException e) {
		// e.printStackTrace();
		// }
		//
		// String html = getHtmlPage(uri);
		// System.out.println(html);
		// List<URI> array = parseWebPage(html);
		//
		// for (int i = 0; i < array.size(); i++) {
		// System.out.println(array.get(i));
		// System.out.println(baseUri.resolve(array.get(i)));
		//
		// }

		// writeFile("D:\\temp\\test.html", html);
	}

}
