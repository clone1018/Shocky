package pl.shockah;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

public class HTTPQuery {
	public enum Method{GET,POST,HEAD}
	
	public final URL url;
	public final Method method;
	protected HttpURLConnection c;
	protected Charset charset = Helper.utf8;
	
	public static HTTPQuery create(String addr) throws MalformedURLException {
		return create(addr, Method.GET);
	}
	
	public static HTTPQuery create(String addr, Method method) throws MalformedURLException {
		URL url = new URL(addr);
		return new HTTPQuery(url,method);
	}
	
	public HTTPQuery(URL url) {
		this(url,Method.GET);
	}
	
	public HTTPQuery(URL url, Method method) {
		this.url = url;
		this.method = method;
	}
	
	public void connect(boolean input, boolean output) {connect(false,input,output);}
	public void connect(boolean cache, boolean input, boolean output) {
		try {
			c = (HttpURLConnection)url.openConnection();
			c.setRequestMethod(method.name());
			if (method == Method.POST)
				c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
			
			c.setUseCaches(cache);
			c.setDoInput(input);
			c.setDoOutput(output);
			c.setConnectTimeout(60000);
			c.setReadTimeout(60000);
		} catch (Exception e) {e.printStackTrace();}
	}
	public HttpURLConnection getConnection() {
		return c;
	}
	public void close() {
		if (c != null) c.disconnect();
		c = null;
	}
	
	public void setUserAgentFirefox() {setUserAgent("Mozilla/5.0 (Windows NT 6.1; WOW64; rv:10.0) Gecko/20100101 Firefox/10.0");}
	public void setUserAgent(String s) {
		System.setProperty("http.agent","");
		c.setRequestProperty("User-Agent",s);
	}
	
	public void setHeaderProperty(String header, String value) {
		c.setRequestProperty(header,value);
	}
	
	public String getHeaderProperty(String header) {
		return c.getRequestProperty(header);
	}
	
	public Charset getCharset() {
		return charset;
	}

	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	public void write(String s) {
		write(s.getBytes(charset));
	}
	
	public void write(byte[] bytes) {
		OutputStream os = null;
		try {
			c.setRequestProperty("Content-Length",Integer.toString(bytes.length));
			os = c.getOutputStream();
			os.write(bytes);
		} catch (Exception e) {e.printStackTrace();}
	}
	
	public ArrayList<String> readLines() {
		ArrayList<String> ret = new ArrayList<String>();
		BufferedReader br = null;
		try {
			br = new BufferedReader(new InputStreamReader(c.getInputStream(),charset));
			String line;
			while ((line = br.readLine()) != null)
				ret.add(line);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null)
					br.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}
	public String readWhole() throws IOException {
		char[] buffer = new char[256];
		StringBuilder sb = new StringBuilder(buffer.length);
		InputStreamReader is = null;
		try {
			is = new InputStreamReader(c.getInputStream(),charset);
			int count;
			while ((count=is.read(buffer))>0)
				sb.append(buffer, 0, count);
		} finally {
				try {
					if (is != null)
						is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return sb.toString();
	}
	
	public static String parseArgs(Map<String,String> args) {
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> pair : args.entrySet()) {
			if (sb.length() != 0) sb.append('&');
			try {
				sb.append(URLEncoder.encode(pair.getKey(),"UTF-8"));
				sb.append('=');
				sb.append(URLEncoder.encode(pair.getValue(),"UTF-8"));
			} catch (Exception e) {e.printStackTrace();}
		}
		return sb.toString();
	}
	public static String parseArgs(String... args) {
		StringBuilder sb = new StringBuilder();
		try {
			for (int i = 0; i < args.length; i += 2) {
				if (i > 0)
					sb.append('&');
				sb.append(URLEncoder.encode(args[i],"UTF-8"));
				sb.append('=');
				sb.append(URLEncoder.encode(args[i+1],"UTF-8"));
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return sb.toString();
	}
}