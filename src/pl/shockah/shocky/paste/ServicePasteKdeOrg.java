package pl.shockah.shocky.paste;

import java.util.ArrayList;
import pl.shockah.HTTPQuery;

public class ServicePasteKdeOrg implements PasteService {
	
	public String paste(CharSequence data) {
		HTTPQuery q = HTTPQuery.create("http://paste.kde.org/",HTTPQuery.Method.POST);
		
		StringBuilder sb = new StringBuilder(data.length()+100);
		sb.append("paste_lang=Text");
		sb.append("&api_submit=1");
		sb.append("&mode=xml");
		sb.append("&paste_private=yes");
		sb.append("&paste_data=");
		sb.append(data);
		
		q.connect(true,true);
		q.write(sb.toString());
		ArrayList<String> list = q.readLines();
		q.close();
		
		String pasteId = null, pasteHash = null;
		for (String s : list) {
			s = s.trim();
			if (s.startsWith("<id>")) pasteId = s.substring(4,s.length()-5);
			if (s.startsWith("<hash>")) pasteHash = s.substring(6,s.length()-7);
		}
		
		if (pasteId != null) {
			if (pasteHash != null) return "http://paste.kde.org/"+pasteId+"/"+pasteHash+"/";
			return "http://paste.kde.org/"+pasteId+"/";
		}
		return null;
	}
}
