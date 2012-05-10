package pl.shockah;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XMLObject {
	public static XMLObject deserialize(String text) {
		text = text.replace("\t","").replace("\r","");
		String[] lines = text.split("\\r?\\n");
		for (int i = 0; i < lines.length; i++) lines[i] = lines[i].trim();
		text = StringTools.implode(lines,"");
		
		Pattern p = Pattern.compile("\\Q<![CDATA[\\E(.*?)\\Q]]>\\E");
		Matcher m; while ((m = p.matcher(text)).find()) text = new StringBuilder(text).replace(m.start(),m.end(),StringTools.unescapeHTML(m.group(1))).toString();
		
		XMLObject x = new XMLObject();
		return deserialize(x,text) ? x : null;
	}
	private static boolean deserialize(XMLObject x, String text) {
		int i = 0;
		
		char c;
		while (true) {
			if (i > text.length()-1) break;
			while (text.charAt(i) == ' ' && i > text.length()-1) i++;
			if (i > text.length()-1) break;
			int openCount = 0;
			
			StringBuilder tagOpen = new StringBuilder();
			while (true) {
				tagOpen.append(text.charAt(i++));
				if (tagOpen.length() == 1 && tagOpen.charAt(0) != '<') {
					i--;
					tagOpen = null;
					break;
				}
				if (tagOpen.charAt(tagOpen.length()-1) == '<') openCount++;
				if (tagOpen.charAt(tagOpen.length()-1) == '>') {
					openCount--;
					if (openCount == 0) {
						if ((tagOpen.length() >= 3 && tagOpen.charAt(1) == '!') || (tagOpen.length() >= 4 && tagOpen.charAt(1) == '?' && tagOpen.charAt(tagOpen.length()-2) == '?')) {
							tagOpen = new StringBuilder();
							break;
						}
						break;
					}
				}
			}
			
			if (tagOpen == null) return false;
			if (tagOpen.length() == 0) continue;
			
			XMLObject newx = new XMLObject();
			x.elements.add(newx);
			
			StringBuilder tagName = new StringBuilder();
			for (int _i = 1; _i < tagOpen.length(); _i++) if ((c = tagOpen.charAt(_i)) != '>') tagName.append(c);
			
			boolean noClose = false;
			if (tagName.charAt(tagName.length()-1) == '/') {
				tagName.deleteCharAt(tagName.length()-1);
				noClose = true;
			}
			
			String[] args = tagName.toString().trim().split(" ");
			String arg = args.length == 1 ? "" : StringTools.implode(args,1," ");
			int i2 = 0;
			while (i2 <= arg.length()-1) {
				while (arg.charAt(i2) == ' ') i2++;
				
				StringBuilder key = new StringBuilder();
				while ((c = arg.charAt(i2++)) != '=' && c != ' ') key.append(c);
				
				if (c == ' ') {
					newx.attributes.put(key.toString(),null);
					break;
				}
				
				StringBuilder value = new StringBuilder(); char _start = arg.charAt(i2++);
				while ((c = arg.charAt(i2++)) != _start) value.append(c);
				newx.attributes.put(StringTools.unescapeHTML(key.toString()),StringTools.unescapeHTML(value.toString()));
			}
			
			String _end = "</"+(newx.name = StringTools.unescapeHTML(args[0]))+">";
			if (!noClose) {
				StringBuilder tagInside = new StringBuilder();
				if (text.charAt(i) != '<' || text.charAt(i+1) != '/') {
					while (true) {
						tagInside.append(text.charAt(i++));
						if (tagInside.length() >= _end.length() && tagInside.toString().endsWith(_end)) {
							tagInside.delete(tagInside.length()-_end.length(),tagInside.length());
							break;
						}
					}
				} else i += _end.length();
				
				StringBuilder _tag = new StringBuilder(tagInside.toString().trim());
				if (_tag.length() != 0 && _tag.charAt(0) == '<') tagInside = _tag;
				
				if (tagInside.length() != 0 && tagInside.charAt(0) == '<') {
					deserialize(newx,tagInside.toString().trim());
				} else {
					newx.value = StringTools.unescapeHTML(tagInside.toString());
					if (newx.value.isEmpty()) newx.value = null;
				}
			}
		}
		
		return true;
	}
	
	private String name = null, value = null;
	private Map<String,String> attributes = Collections.synchronizedMap(new TreeMap<String,String>());
	private List<XMLObject> elements = Collections.synchronizedList(new ArrayList<XMLObject>());
	
	public String getName() {return name;}
	
	public int countAttributes() {return attributes.size();}
	public boolean existsAttribute(String key) {return attributes.containsKey(key);}
	public String getAttribute(String key) {return attributes.get(key);}
	public Map<String,String> getAllAttributes() {return new TreeMap<String,String>(attributes);}
	
	public int countElements() {return elements.size();}
	public XMLObject getBaseElement() {return getAllElements().get(0);}
	public ArrayList<XMLObject> getElement(String name) {
		ArrayList<XMLObject> list = new ArrayList<XMLObject>();
		for (XMLObject e : elements) if (e.name.equals(name)) list.add(e);
		return list;
	}
	public ArrayList<XMLObject> getAllElements() {return new ArrayList<XMLObject>(elements);}
	
	public String getValue() {return value;}
	
	public void print() {
		print(0);
	}
	private void print(int spaces) {
		String s = ""; for (int i = 0; i < spaces; i++) s += " ";
		
		if (name == null) {
			for (XMLObject e : elements) e.print(name == null ? 0 : spaces+2);
		} else {
			System.out.print(s+"<"+StringTools.escapeHTML(name));
			for (String key : attributes.keySet()) {
				String v = attributes.get(key); char c = v.contains("\"") ? '\'' : '"';
				System.out.print(" "+key+"="+c+StringTools.escapeHTML(attributes.get(key))+c);
			}
			if (countElements() != 0 || value != null) System.out.print(">");
			
			if (value != null) {
				System.out.print(StringTools.escapeHTML(value));
				System.out.println("</"+StringTools.escapeHTML(name)+">");
			} else {
				if (countElements() == 0) {
					System.out.println(" />");
				} else {
					System.out.println();
					for (XMLObject e : elements) e.print(name == null ? 0 : spaces+2);
					System.out.println(s+"</"+StringTools.escapeHTML(name)+">");
				}
			}
		}
	}
	
	/*public String serialize() {
		return serialize(new StringBuilder()).toString();
	}
	private StringBuilder serialize(StringBuilder sb) {
		sb.append(isArray() ? "[" : "{");
		int i = 0;
		for (String key : elements.keySet()) {
			if (i++ != 0) sb.append(",");
			if (!isArray()) sb.append("\""+key.replace("\"","\\\"")+"\":");
			Object e = elements.get(key);
			if (e instanceof XMLObject) ((XMLObject)e).serialize(sb);
			else if (e instanceof String) sb.append("\""+((String)e).replace("\"","\\\"")+"\"");
		}
		sb.append(isArray() ? "]" : "}");
		return sb;
	}*/
}