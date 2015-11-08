package pl.shockah.shocky.paste;

import java.io.IOException;
import java.net.MalformedURLException;

import org.json.JSONException;
import org.json.JSONObject;

import pl.shockah.HTTPQuery;

public class ServicePasteKdeOrg implements PasteService {
	
	public String paste(CharSequence data) {
		HTTPQuery q;
		try {
			q = HTTPQuery.create("http://pastebin.kde.org/api/json/create",HTTPQuery.Method.POST);
		} catch (MalformedURLException e1) {return null;}
		
		StringBuilder sb = new StringBuilder(data.length()+32);
		sb.append("language=text&private=true&data=").append(data);
		
		q.connect(true,true);
		q.write(sb.toString());
		JSONObject json;
		try {
			json = new JSONObject(q.readWhole()).getJSONObject("result");
			if (json.has("error")) {
				System.out.println(json.getString("error"));
				return null;
			}
			
			String pasteId = json.getString("id");
			String pasteHash = json.getString("hash");
			
			return "http://pastebin.kde.org/"+pasteId+"/"+pasteHash;
		} catch (JSONException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			q.close();
		}
		return null;
	}
}
