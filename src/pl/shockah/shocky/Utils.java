package pl.shockah.shocky;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.pircbotx.Channel;
import org.pircbotx.User;
import pl.shockah.*;
import pl.shockah.shocky.paste.*;

public class Utils {
	public static final Pattern
		patternURL = Pattern.compile("[a-z]+://(www\\.)?[a-z0-9]+(\\.[a-z]+)+/([^/:]+/)*([^/]*)?"),
		patternNick = Pattern.compile("[a-zA-Z0-9\\Q_-\\[]{}^`|\\E]+");
	private static final String
		mungeOriginal =	"abcdefghijklmnoprstuwxyzABCDEGHIJKLMORSTUWYZ0123456789",
		mungeReplace =	"äḃċđëƒġħíĵķĺṁñöρŗšţüωχÿźÅḂÇĎĒĠĦÍĴĶĹṀÖŖŠŢŮŴỲŻ０１２３４５６７８９";
	private static final String
		oddOriginal =	"abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789",
		oddReplace =	"αвcđєfġнίנкlмиoρqяsтυvωxуzαвcđєfġнίנкlмиoρqяsтυvωxуz０１２３４５６７８９";
	private static final String
		flipOriginal =	"!().12345679<>?ABCDEFGJKLMPQRTUVWY[]_abcdefghijklmnpqrtuvwy{},'\"┳",
		flipReplace =	"¡)(˙⇂ᄅƐㄣϛ9Ɫ6><¿∀ℇƆ◖ƎℲפſ丬˥WԀΌᴚ⊥∩ΛMλ][‾ɐqɔpǝɟɓɥıɾʞlɯudbɹʇnʌʍʎ}{',„┻";
	
	public static final List<PasteService> services = new LinkedList<PasteService>();
	
	public static ArrayList<String> getAllUrls(String text) {
		String[] spl = text.split(" ");
		ArrayList<String> urls = new ArrayList<String>();
		for (String s : spl) if (patternURL.matcher(s).find()) urls.add(s);
		return urls;
	}
	public static String shortenAllUrls(String text) {
		ArrayList<String> urls = getAllUrls(text);
		for (String url : urls) text = text.replace(url,shortenUrl(url));
		return text;
	}
	public static String shortenUrl(String url) {
		String login = Data.config.getString("main-bitlyuser");
		String key = Data.config.getString("main-bitlyapikey");
		if (login==null || key==null)
			return url;
		try {
			HTTPQuery q = HTTPQuery.create("http://api.bitly.com/v3/shorten?"+HTTPQuery.parseArgs("format","txt","login",login,"apiKey",key,"longUrl",url));
			q.connect(true,true,false);
			String line = q.readWhole().trim();
			q.close();
			
			if (line.startsWith("http://")) return line;
		} catch (Exception e) {e.printStackTrace();}
		return url;
	}
	
	public static void initPasteServices() {
		String key = null;
		services.clear();
		services.add(new ServicePasteKdeOrg());
		key = Data.config.getString("api-pastebin.com");
		if (key != null)
			services.add(new ServicePastebinCom(key));
		key = Data.config.getString("api-pastebin.ca");
		if (key != null)
			services.add(new ServicePastebinCa(key));
	}
	
	public static String paste(CharSequence data) {
		String link = null;
		for (PasteService service : services) {
			link = service.paste(data);
			if (link == null) continue;
			if (link.isEmpty() || link.startsWith("http://")) break;
		}
		return link;
	}
	
	public static String mungeAllNicks(Channel channel, int threshold, CharSequence message, String... dontMunge) {
		if (channel == null) return message.toString();
		String temp = message.toString();
		int total = 0;
		getUsers: for (User user : channel.getUsers()) {
			String nick = user.getNick();
			for (String dont : dontMunge)
				if (nick.equalsIgnoreCase(dont)) continue getUsers;
			Pattern pattern = Pattern.compile(String.format("\\b%s\\b", Pattern.quote(nick)),Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(message);
			if (matcher.find()) {
				message = matcher.replaceAll(mungeNick(nick));
				total++;
			}
		}
		if (total <= threshold)
			return temp;
		return message.toString();
	}
	public static String mungeNick(CharSequence str) {
		char[] chars = new char[str.length()];
		for (int i = 0; i < str.length(); i++) {
			char source = str.charAt(i);
			int iof = mungeOriginal.indexOf(source);
			if (iof == -1) {
				chars[i] = source;
				continue;
			}
			chars[i] = mungeReplace.charAt(iof);
		}
		return new String(chars);
	}
	
	public static String flip(CharSequence str) {
		char[] chars = new char[str.length()];
		for (int i = 0; i < str.length(); i++) {
			char source = str.charAt(i);
			int iof1 = flipOriginal.indexOf(source);
			int iof2 = flipReplace.indexOf(source);
			if (iof1 == -1 && iof2 == -1) {
				chars[i] = source;
				continue;
			}
			if (iof1 != -1)
				chars[i] = flipReplace.charAt(iof1);
			else if (iof2 != -1)
				chars[i] = flipOriginal.charAt(iof2);
		}
		return new String(chars);
	}
	
	public static String odd(CharSequence str) {
		char[] chars = new char[str.length()];
		for (int i = 0; i < str.length(); i++) {
			char source = str.charAt(i);
			int iof1 = oddOriginal.indexOf(source);
			int iof2 = oddReplace.indexOf(source);
			if (iof1 == -1 && iof2 == -1) {
				chars[i] = source;
				continue;
			}
			if (iof1 != -1)
				chars[i] = oddReplace.charAt(iof1);
			else if (iof2 != -1)
				chars[i] = oddOriginal.charAt(iof2);
		}
		return new String(chars);
	}
	
	public static String timeAgo(Date date) {return timeAgo(date,new Date());}
	public static String timeAgo(Date from, Date to) {
		long dif = (to.getTime()-from.getTime())/1000;
		String time = timeAgo(dif);
		if (!time.contentEquals("now"))
			return time+" ago";
		return time;
	}
	
	public static String timeAgo(long dif) {
		if (dif == 0L)
			return "now";
		
		StringBuilder sb = new StringBuilder();
		int s = (int) dif % 60;dif /= 60L;
		int m = (int) dif % 60;dif /= 60L;
		int h = (int) dif % 24;dif /= 24L;
		int d = (int) dif % 7; dif /= 7L;
		int w = (int) (dif % 52.175D);
		int y = (int) (dif / 52.175D);
		
		int a = 0;
		for (int i=5;i>=0;--i) {
			int v;
			char c;
			switch(i)
			{
			default:
			case 0: v = s;c = 's'; break;
			case 1: v = m;c = 'm'; break;
			case 2: v = h;c = 'h'; break;
			case 3: v = d;c = 'd'; break;
			case 4: v = w;c = 'w'; break;
			case 5: v = y;c = 'y'; break;
			}
			if (a > 0)
				sb.append(' ');
			a |= v;
			if (a > 0)
				sb.append(v).append(c);
		}

		return sb.toString();
	}
}