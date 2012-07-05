package pl.shockah;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;

import pl.shockah.shocky.Data;

public class StringTools {
	public static final Pattern unicodePattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
	public static final Pattern htmlTagPattern = Pattern.compile("<([^>]+)>");
	public static final Pattern htmlEscapePattern = Pattern.compile("&(([A-Za-z]+)|#([0-9]+));");
	public static final UnicodeUnescaper unicodeEscape = new UnicodeUnescaper();
	
	public static String getFilenameStrippedWindows(String fname) {
		fname = fname.replace('\\','-');
		fname = fname.replace('/','-');
		fname = fname.replace(": "," - ");
		fname = fname.replace(':','-');
		fname = fname.replace("*","");
		fname = fname.replace("?","");
		fname = fname.replace('"','\'');
		fname = fname.replace('<','[');
		fname = fname.replace('>',']');
		fname = fname.replace("|"," - ");
		return fname;
	}
	
	public static String escapeHTML(String s) {
		return StringEscapeUtils.escapeHtml4(s);
	}
	
	public static String unescapeHTML(String s) {
		return StringEscapeUtils.unescapeHtml4(s);
	}

	public static String stripHTMLTags(CharSequence s) {
		Matcher m = htmlTagPattern.matcher(s);
		StringBuffer sb = new StringBuffer();
		while (m.find())
			m.appendReplacement(sb, "");
		m.appendTail(sb);
		return sb.toString();
	}
	public static String unicodeParse(CharSequence s) {
		Matcher m = unicodePattern.matcher(s);
		StringBuffer sb = new StringBuffer();
		while (m.find()) {
			int hex = Integer.valueOf(m.group(1),16);
			m.appendReplacement(sb, Character.toString((char)hex));
		}
		m.appendTail(sb);
		return sb.toString();
	}
	
	public static boolean isNumber(CharSequence s) {
		boolean ret = true;
		for (int i = 0; ret && i < s.length(); i++) {
			Character c = s.charAt(i);
			if (i == 0 && c == '-')
				continue;
			if (!Character.isDigit(c))
				ret = false;
		}
		return ret;
	}
	
	public static boolean isBoolean(String s) {
		return (s.equalsIgnoreCase("true")||s.equalsIgnoreCase("false"));
	}
	
	public static String ircFormatted(CharSequence s, boolean urlDecode) {
		String output = unicodeEscape.translate(s);
		output = StringEscapeUtils.unescapeHtml4(output);
		output = output.replaceAll("</?b>", "\u0002");
		output = output.replaceAll("</?u>", "\u001f");
		if (urlDecode) {
			try {
				output = URLDecoder.decode(output, "utf8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		output = StringEscapeUtils.unescapeJava(output);
		output = stripHTMLTags(output);
		output = deleteWhitespace(output);
		output = limitLength(output);
		return output;
	}
	
    public static String deleteWhitespace(CharSequence str) {
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
        	char c = str.charAt(i);
            if (i > 0 && Character.isWhitespace(c) && Character.isWhitespace(str.charAt(i-1))) {
                continue;
            }
            if (c == '\r' || c == '\n') {
            	sb.append(' ');
            	continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
    
    public static String limitLength(CharSequence str) {
    	if (str.length() > Data.config.getInt("main-messagelength"))
    		str = str.subSequence(0, Data.config.getInt("main-messagelength"))+"...";
    	return str.toString();
    }
	
	public static <T> String implode(T[] spl, String separator) {return implode(spl,0,spl.length-1,separator);}
	public static <T> String implode(T[] spl, int a, String separator) {return implode(spl,a,spl.length-1,separator);}
	public static <T> String implode(T[] spl, int a, int b, String separator) {
		StringBuffer sb = new StringBuffer();
		while (a <= b) {
			if (sb.length() != 0) sb.append(separator);
			sb.append(spl[a++]);
		}
		return sb.toString();
	}
}