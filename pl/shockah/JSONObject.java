package pl.shockah;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

public class JSONObject {
	public static JSONObject deserialize(String text) {
		text = text.replace("\t","").replace("\r","").replace("\n","").trim();
		JSONObject j = new JSONObject(false);
		return deserialize(j,text,0) == -1 ? null : j;
	}
	private static int deserialize(JSONObject j, String text, int i) {
		if (text.charAt(i++) != (j.isArray() ? '[' : '{')) return -1;
		
		char c; int count = 0;
		while (true) {
			StringBuilder key = new StringBuilder();
			if (j.isArray()) key.append(count++); else {
				i++;
				while ((c = text.charAt(i++)) != '"' || (key.length() != 0 && key.charAt(key.length()-1) == '\\')) key.append(c);
				if (key.charAt(key.length()-1) == '\\') key.deleteCharAt(key.length()-1);
				if (text.charAt(i++) != ':') return -1; else {
					while ((c = text.charAt(i++)) == ' ') {}
					i--;
				}
			}
			
			c = text.charAt(i++);
			if (c == '{' || c == '[') {
				JSONObject e = new JSONObject(c == '[');
				i = deserialize(e,text,--i);
				if (i == -1) return -1;
				j.elements.put(key.toString(),e);
			} else {
				StringBuilder value = new StringBuilder();
				if (c == '"') {
					while ((c = text.charAt(i++)) != '"' || (value.length() != 0 && value.charAt(value.length()-1) == '\\')) value.append(c);
					if (value.length()>0&&value.charAt(value.length()-1) == '\\') value.deleteCharAt(value.length()-1);
					j.elements.put(key.toString(),value.toString());
				} else {
					i--;
					while ((c = text.charAt(i++)) != ',' && c != '}' && c != ']' && c != ' ') value.append(c);
					i--;
					String s = value.toString();
					if (s.length()>0)
						j.elements.put(key.toString(),StringTools.isBoolean(s) ? new JSONBoolean(Boolean.parseBoolean(s)) : (StringTools.isNumber(s) ? new JSONInt(Integer.parseInt(s)) : new JSONDouble(Double.parseDouble(s))));
				}
			}
			
			if ((c = text.charAt(i++)) != ',') {
				i--;
				break;
			} else {
				while ((c = text.charAt(i++)) == ' ') {}
				i--;
			}
		}
		
		if (text.charAt(i++) != (j.isArray() ? ']' : '}')) return -1;
		return i;
	}
	
	private final boolean isArray;
	private Map<String,Object> elements = Collections.synchronizedMap(new TreeMap<String,Object>());
	
	private JSONObject(boolean isArray) {
		this.isArray = isArray;
	}
	
	public boolean isArray() {return isArray;}
	public boolean exists(String key) {return elements.containsKey(key);}
	public int size() {return elements.size();}
	
	public Object get(String key) {return elements.get(key);}
	public JSONObject getJSONObject(String key) {Object o = get(key); if (o instanceof JSONObject) return (JSONObject)o; return null;}
	public String getString(String key) {Object o = get(key); if (o instanceof String) return (String)o; return null;}
	public int getInt(String key) {Object o = get(key); if (o instanceof JSONInt) return ((JSONInt)o).value; throw new RuntimeException("Wrong type (int) for "+key);}
	public double getDouble(String key) {Object o = get(key); if (o instanceof JSONDouble) return ((JSONDouble)o).value; throw new RuntimeException("Wrong type (double) for "+key);}
	public boolean getBoolean(String key) {Object o = get(key); if (o instanceof JSONBoolean) return ((JSONBoolean)o).value; throw new RuntimeException("Wrong type (boolean) for "+key);}
	public Object[] getArray(String key) {
		JSONObject o = getJSONObject(key);
		if (o == null) return null;
		Object[] a = new Object[o.elements.size()];
		int i = 0;
		for (String k : o.elements.keySet()) if ((a[i++] = o.get(k)) == null) return null;
		return a;
	}
	public JSONObject[] getJSONObjectArray(String key) {
		JSONObject o = getJSONObject(key);
		if (o == null) return null;
		JSONObject[] a = new JSONObject[o.elements.size()];
		int i = 0;
		for (String k : o.elements.keySet()) if ((a[i++] = o.getJSONObject(k)) == null) return null;
		return a;
	}
	public String[] getStringArray(String key) {
		JSONObject o = getJSONObject(key);
		if (o == null) return null;
		String[] a = new String[o.elements.size()];
		int i = 0;
		for (String k : o.elements.keySet()) if ((a[i++] = o.getString(k)) == null) return null;
		return a;
	}
	public int[] getIntArray(String key) {
		JSONObject o = getJSONObject(key);
		if (o == null) return null;
		int[] a = new int[o.elements.size()];
		int i = 0;
		for (String k : o.elements.keySet()) a[i++] = o.getInt(k);
		return a;
	}
	public double[] getDoubleArray(String key) {
		JSONObject o = getJSONObject(key);
		if (o == null) return null;
		double[] a = new double[o.elements.size()];
		int i = 0;
		for (String k : o.elements.keySet()) a[i++] = o.getDouble(k);
		return a;
	}
	public boolean[] getBooleanArray(String key) {
		JSONObject o = getJSONObject(key);
		if (o == null) return null;
		boolean[] a = new boolean[o.elements.size()];
		int i = 0;
		for (String k : o.elements.keySet()) a[i++] = o.getBoolean(k);
		return a;
	}
	
	public void print() {
		print(0);
	}
	private void print(int spaces) {
		String s = ""; for (int i = 0; i < spaces; i++) s += " ";
		
		for (String key : elements.keySet()) {
			System.out.print(s+key+" -> ");
			Object e = elements.get(key);
			if (e instanceof JSONObject) {
				System.out.println();
				((JSONObject)e).print(spaces+2);
			} else System.out.println(e);
		}
	}
	
	public String serialize() {
		return serialize(new StringBuilder()).toString();
	}
	private StringBuilder serialize(StringBuilder sb) {
		sb.append(isArray() ? "[" : "{");
		int i = 0;
		for (String key : elements.keySet()) {
			if (i++ != 0) sb.append(",");
			if (!isArray()) sb.append("\""+key.replace("\"","\\\"")+"\":");
			Object e = elements.get(key);
			if (e instanceof JSONObject) ((JSONObject)e).serialize(sb);
			else if (e instanceof String) sb.append("\""+((String)e).replace("\"","\\\"")+"\"");
			else if (e instanceof JSONInt) sb.append(((JSONInt)e).toString());
			else if (e instanceof JSONDouble) sb.append(((JSONDouble)e).toString());
			else if (e instanceof JSONBoolean) sb.append(((JSONBoolean)e).toString());
		}
		sb.append(isArray() ? "]" : "}");
		return sb;
	}
	
	private static class JSONInt {
		private final int value;
		private JSONInt(int value) {this.value = value;}
		public String toString() {return Integer.toString(value);}
	}
	private static class JSONDouble {
		private final double value;
		private JSONDouble(double value) {this.value = value;}
		public String toString() {return Double.toString(value);}
	}
	private static class JSONBoolean {
		private final boolean value;
		private JSONBoolean(boolean value) {this.value = value;}
		public String toString() {return Boolean.toString(value);}
	}
}