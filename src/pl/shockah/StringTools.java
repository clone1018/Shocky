package pl.shockah;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.text.translate.UnicodeUnescaper;

import pl.shockah.shocky.Data;

public class StringTools {
	public static final Pattern unicodePattern = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
	public static final Pattern htmlTagPattern = Pattern.compile("<([^>]+)>");
	public static final Pattern htmlEscapePattern = Pattern.compile("&(([A-Za-z]+)|#([0-9]+));");
	public static final Pattern controlCharPattern = Pattern.compile("(([\r\n]+)|(\t))");
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
	
    public static CharSequence trimWhitespace(CharSequence str) {
    	int len = str.length();
    	int st = 0;

    	while ((st < len) && Character.isWhitespace(str.charAt(st))) {
    	    st++;
    	}
    	while ((st < len) && Character.isWhitespace(str.charAt(len - 1))) {
    	    len--;
    	}
    	return (((st > 0) || (len < str.length())) ? str.subSequence(st, len) : str);
    }
	
    public static String deleteWhitespace(CharSequence str) {
        StringBuilder sb = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
        	char c = str.charAt(i);
            if (i > 0 && Character.isWhitespace(c) && Character.isWhitespace(str.charAt(i-1)))
                continue;
            if (c == '\r' || c == '\n') {
            	sb.append(' ');
            	continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }
    
    public static String formatLines(CharSequence str) {
    	if (str == null)
    		return "";
    	boolean ctcp = str.charAt(0) == '\1' && str.charAt(str.length()-1) == '\1';
    	if (ctcp)
    		str = str.subSequence(1, str.length()-1);
    	str = StringTools.trimWhitespace(str);
    	if (str.length() == 0)
    		return "";
    	StringBuffer sb = new StringBuffer(str.length()+16);
    	Matcher m = controlCharPattern.matcher(str);
    	while(m.find()) {
    		String newline = m.group(2);
    		String tab = m.group(3);
    		if (newline != null)
    			m.appendReplacement(sb, " | ");
    		else if (tab != null)
    			m.appendReplacement(sb, ",");
    	}
    	m.appendTail(sb);
    	if (ctcp)
    		sb.insert(0, '\1').append('\1');
		return sb.toString();
    }
    
    public static String limitLength(CharSequence str) {
    	if (str.length() > Data.config.getInt("main-messagelength"))
    		str = str.subSequence(0, Data.config.getInt("main-messagelength"))+"...";
    	return str.toString();
    }
    
    public static String build_translate(CharSequence s) {
    	StringBuilder sb = new StringBuilder();
    	for (int i = 0; i < s.length(); ++i)
    	{
    		if (sb.length() >= 512)
    			throw new RuntimeException("sequence is longer than allowed");
    		char c1 = s.charAt(i);
    		if (i+2 < s.length() && s.charAt(i+1) == '-') {
    			i+=2;
    			char c2 = s.charAt(i);
    			if (c2 < c1)
    				throw new RuntimeException("range-endpoints of '"+c1+'-'+c2+"' are in reverse collating sequence order");
    			if (c1 == c2)
    			{
    				sb.append(c1);
    				continue;
    			}
    			if (((int)c2 - (int)c1) >= 512)
        			throw new RuntimeException("sequence is longer than allowed");
    			for (char c = c1; c <= c2; ++c)
    				sb.append(c);
    		} else {
    			sb.append(c1);
    		}
    	}
    	return sb.toString();
    }
    
    public static String translate(String search, String replace, CharSequence source) {
    	if (source == null)
    		return null;
    	if (source.length() == 0)
    		return "";
    	if (search == null || search.isEmpty())
    		return source.toString();
    	StringBuilder sb = new StringBuilder(source.length());
    	for (int i = 0; i < source.length(); ++i)
    	{
    		char c = source.charAt(i);
    		int index;
    		if ((index = search.indexOf(c)) == -1)
    			sb.append(c);
    		else if (replace == null || replace.isEmpty())
    			continue;
    		else if (index >= replace.length())
    			sb.append(replace.charAt(replace.length()-1));
    		else
    			sb.append(replace.charAt(index));
    	}
    	return sb.toString();
    }
	
	public static String implode(Object[] spl, CharSequence separator) {return implode(spl,0,spl.length-1,separator);}
	public static String implode(Object[] spl, int a, CharSequence separator) {return implode(spl,a,spl.length-1,separator);}
	public static String implode(Object[] spl, int a, int b, CharSequence separator) {
		StringBuffer sb = new StringBuffer();
		while (a <= b) {
			if (a >= spl.length)
				break;
			Object s = spl[a++];
			if (s == null) continue;
			if (sb.length() != 0) sb.append(separator);
			sb.append(s);
		}
		return sb.toString();
	}
	
	public static String implode(Collection<?> spl, CharSequence separator) {return implode(spl,0,spl.size()-1,separator);}
	public static String implode(Collection<?> spl, int a, CharSequence separator) {return implode(spl,a,spl.size()-1,separator);}
	public static String implode(Collection<?> spl, int a, int b, CharSequence separator) {
		return implode(spl.toArray(), a, b, separator);
	}
	
	public static String implode(String spl, String separator) {return implode(spl,0,separator);}
	public static String implode(String spl, int a, String separator) {
		if (separator == null)
			throw new IllegalArgumentException("separator cannot be null");
		if (a < 0)
			throw new IndexOutOfBoundsException("a must be positive");
		if (a == 0)
			return spl;
		int ap = 0,i = 0;
		while (i < a) {
			ap = spl.indexOf(separator, ap);
			if (ap == -1)
				return null;
			ap += separator.length();
			if (++i != a)
				continue;
			return spl.substring(ap);
		}
		return null;
	}
	
	public static String[] split( CharSequence spl )
	{
		List<String> list = new LinkedList<String>();
		int start = 0;
		int end = 0;
		while (start < spl.length())
		{
			char c = spl.charAt(start);
			if (c == '"')
			{
				++start;
				end = start;
				while (end < spl.length())
				{
					if (spl.charAt(end) != '"' || spl.charAt(end - 1) == '\\')
						++end;
					else
						break;
				}

				list.add(spl.subSequence( start, end - start ).toString());
				start = end + 2;
			}
			else if (c != ' ')
			{
				end = start;
				while (end < spl.length())
				{
					if (spl.charAt(end) != ' ')
						++end;
					else
						break;
				}

				list.add(spl.subSequence( start, end - start ).toString());
				start = end + 1;
			}
			else
			{
				++start;
			}
		}

		return list.toArray(new String[0]);
	}
}
