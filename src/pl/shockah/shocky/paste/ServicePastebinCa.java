package pl.shockah.shocky.paste;

import java.net.URLEncoder;
import java.util.ArrayList;
import pl.shockah.HTTPQuery;

public class ServicePastebinCa implements PasteService {
	
	private final String apiKey;
	
	public ServicePastebinCa(String apiKey) {
		this.apiKey = apiKey;
	}
	
	public String paste(CharSequence data) {
		HTTPQuery q = new HTTPQuery("http://pastebin.ca/quiet-paste.php",HTTPQuery.Method.POST);
		
		StringBuilder sb = new StringBuilder(data.length()+50);
		try {
			sb.append("api="+URLEncoder.encode(apiKey,"UTF8"));
			sb.append("&content=");
			sb.append(data);
		} catch (Exception e) {e.printStackTrace();}
		
		q.connect(true,true);
		q.write(sb.toString());
		ArrayList<String> list = q.read();
		q.close();
		
		String s = list.get(0);
		if (s.startsWith("SUCCESS")) return "http://pastebin.ca/"+s.substring(7);
		return null;
	}
}