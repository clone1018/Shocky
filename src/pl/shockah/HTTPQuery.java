package pl.shockah;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;

public class HTTPQuery {
	protected URL url = null;
	protected String method;
	protected HttpURLConnection c;
	
	public HTTPQuery(String adr) {this(adr,"POST");}
	public HTTPQuery(String adr, String method) {
		try {
			url = new URL(adr);
			this.method = method;
		} catch (Exception e) {e.printStackTrace();}
	}
	
	public void connect(boolean input, boolean output) {connect(false,input,output);}
	public void connect(boolean cache, boolean input, boolean output) {
		try {
			c = (HttpURLConnection)url.openConnection();
			c.setRequestMethod(method);
			c.setRequestProperty("Content-Type","application/x-www-form-urlencoded");
			
			c.setUseCaches(cache);
			c.setDoInput(input);
			c.setDoOutput(output);
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
	
	public void write(String s) {
		OutputStream os = null;
		try {
			c.setRequestProperty("Content-Length",""+s.getBytes().length);
			os = c.getOutputStream();
			os.write(s.getBytes());
		} catch (Exception e) {e.printStackTrace();}
	}
	public void write(byte[] bytes) {
		OutputStream os = null;
		try {
			c.setRequestProperty("Content-Length",""+bytes.length);
			os = c.getOutputStream();
			os.write(bytes);
		} catch (Exception e) {e.printStackTrace();}
	}
	
	public ArrayList<String> read() {
		ArrayList<String> ret = new ArrayList<String>();
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(c.getInputStream(),"UTF-8"));
			String line;
			while ((line = br.readLine()) != null) ret.add(line);
			br.close();
		} catch (Exception e) {e.printStackTrace();}
		return ret;
	}
	public String readWhole() {
		ArrayList<String> lines = read();
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < lines.size(); i++) {
			if (i != 0) sb.append("\n");
			sb.append(lines.get(i));
		}
		return sb.toString();
	}
	
	public static String parseArgs(ArrayList<Pair<String,String>> args) {
		StringBuilder sb = new StringBuilder();
		for (Pair<String,String> pair : args) {
			if (sb.length() != 0) sb.append("&");
			try {
				sb.append(URLEncoder.encode(pair.get1()+"="+pair.get2(),"UTF-8"));
			} catch (Exception e) {e.printStackTrace();}
		}
		return sb.toString();
	}
	public static String parseArgs(String... args) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.length; i += 2) {
			if (sb.length() != 0) sb.append("&");
			try {
				sb.append(URLEncoder.encode(args[i],"UTF-8")+"="+URLEncoder.encode(args[i+1],"UTF-8"));
			} catch (Exception e) {e.printStackTrace();}
		}
		return sb.toString();
	}
}