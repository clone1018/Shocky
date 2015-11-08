package pl.shockah.shocky;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pircbotx.Channel;
import org.pircbotx.User;

import com.sun.net.httpserver.HttpContext;

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
		flipReplace =	"¡)(˙⇂ᄅƐㄣϛ9Ɫ6><¿∀ᗺƆᗡƎℲפᒋ丬˥WԀΌᴚ⊥∩ΛMλ][‾ɐqɔpǝɟɓɥıɾʞlɯudbɹʇnʌʍʎ}{',„┻";
	
	public static final List<PasteService> services = new LinkedList<PasteService>();
	public static final Map<String,HttpContext> urls = new HashMap<String,HttpContext>();
	
	public static List<String> getAllUrls(String text) {
		String[] spl = text.split(" ");
		List<String> urls = new ArrayList<String>();
		for (String s : spl) if (patternURL.matcher(s).find()) urls.add(s);
		return urls;
	}
	public static String shortenAllUrls(String text) {
		List<String> urls = getAllUrls(text);
		for (String url : urls) text = text.replace(url,shortenUrl(url));
		return text;
	}
	public static String shortenUrl(String url) {
		if (WebServer.exists())
		{
			StringBuilder sb = new StringBuilder(WebServer.getURL());
			if (url.startsWith(sb.toString()))
				return url;
			HttpContext context;
			synchronized (urls) {
				if (urls.containsKey(url))
					context = urls.get(url);
				else {
					context = WebServer.addRedirect(url);
					urls.put(url, context);
				}
			}
			sb.append(context.getPath());
			return sb.toString();
		}
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
		if (WebServer.exists() && data.length() < 5242880)
		{
			File file;
			try {
				file = File.createTempFile("shocky_paste", ".txt");
				file.deleteOnExit();
				FileOutputStream os = new FileOutputStream(file);
				BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os,Helper.utf8));
				bw.append(data);
				bw.close();
				os.close();
			} catch (IOException e) {
				e.printStackTrace();
				file = null;
			}
			
			if (file != null) {
				StringBuilder sb = new StringBuilder(WebServer.getURL());
				HttpContext context = WebServer.addPaste(file);
				sb.append(context.getPath());
				return sb.toString();
			}
		}
		String link = null;
		for (PasteService service : services) {
			link = service.paste(data);
			if (link == null) continue;
			if (link.isEmpty() || link.startsWith("http://")) break;
		}
		return link;
	}
	
	public static String mungeAllNicks(Channel channel, int threshold, CharSequence message, User... dontMunge) {
		String temp = message.toString();
		if (channel == null)
			return temp;
		int total = 0;
		User bot = channel.getBot().getUserBot();
		getUsers: for (User user : channel.getUsers()) {
			for (User dont : dontMunge)
				if (user.equals(dont)) continue getUsers;
			if (user.equals(bot))
				continue;
			String nick = user.getNick();
			Pattern pattern = Pattern.compile(String.format("\\b%s\\b", Pattern.quote(nick)),Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(message);
			if (matcher.find()) {
				message = matcher.replaceAll(Matcher.quoteReplacement(mungeNick(nick)));
				total++;
			}
		}
		if (total <= threshold)
			return temp;
		return message.toString();
	}
	public static String mungeNick(CharSequence str) {
		char[] chars = new char[str.length()];
		for (int i = 0; i < chars.length; ++i) {
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
	
	private static String mutate(String original, String replacement, CharSequence str) {
		char[] chars = new char[str.length()];
		for (int i = 0; i < chars.length; ++i) {
			char source = str.charAt(i);
			int iof1 = original.indexOf(source);
			int iof2 = replacement.indexOf(source);
			if (iof1 == -1 && iof2 == -1) {
				chars[i] = source;
				continue;
			}
			if (iof1 != -1)
				chars[i] = replacement.charAt(iof1);
			else if (iof2 != -1)
				chars[i] = original.charAt(iof2);
		}
		return new String(chars);
	}
	
	public static String flip(CharSequence str) {
		return mutate(flipOriginal, flipReplace, str);
	}
	
	public static String odd(CharSequence str) {
		return mutate(oddOriginal, oddReplace, str);
	}
	
	public static String timeAgo(Date date) {return timeAgo(date,new Date());}
	public static String timeAgo(Date from, Date to) {
		long dif = (to.getTime()-from.getTime())/1000;
		String time = timeAgo(dif);
		if (time!="now")
			return time+" ago";
		return time;
	}
	
	public static long parseInterval(CharSequence str) {
		long result = 0L;
		int start = 0;
		for (int i = 0; i < str.length(); ++i) {
			char c = str.charAt(i);
			if (Character.isDigit(c))
				continue;
			if (i > start) {
				long num = Long.parseLong(str.subSequence(start, i).toString());
				switch (c) {
					case 's':result+=num; break;
					case 'm':result+=num*(60L); break;
					case 'h':result+=num*(60L*60L); break;
					case 'd':result+=num*(24L*60L*60L); break;
					case 'w':result+=num*(7L*24L*60L*60L); break;
				}
			}
			start = i + 1;
		}
		return result;
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
	
	public static String timeAgo(String pt) {
		if (!pt.startsWith("PT"))
			return null;
		int i = 2;
		int num = 0;
		long time = 0;
		while (i < pt.length()) {
			char c = pt.charAt(i++);
			if (Character.isDigit(c)) {
				num *= 10;
				num += Character.digit(c, 10);
			} else {
				switch (c) {
				case 'D': time += num * (60 * 60 * 24); break;
				case 'H': time += num * (60 * 60); break;
				case 'M': time += num * 60; break;
				case 'S': time += num; break;
				}
				num = 0;
			}
		}
		return timeAgo(time);
	}
	
	public static <T> T rndCollection(Collection<T> c, Random rnd) {
		if (c == null || rnd == null || c.size() == 0)
			return null;
		int i = rnd.nextInt(c.size());
		if (c instanceof RandomAccess && c instanceof List<?>)
			return ((List<T>)c).get(i);
		else {
			for (T val : c)
				if (i-- == 0)
					return val;
			return null;
		}
	}
}