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
		if (text.charAt(i) == (j.isArray() ? ']' : '}')) return i+1;
		
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
					value = new StringBuilder(value.toString().replaceAll("\\\\(.)","$1"));
					j.elements.put(key.toString(),new JSON<CharSequence>(value));
				} else {
					i--;
					while ((c = text.charAt(i++)) != ',' && c != '}' && c != ']' && c != ' ') value.append(c);
					i--;
					String s = value.toString();
					if (s.length()>0) {
						Object valueObj;
						if (s.equals("null"))
							valueObj = null;
						else if (StringTools.isBoolean(s))
							valueObj = new JSON<Boolean>(Boolean.parseBoolean(s));
						else if (StringTools.isNumber(s))
							valueObj = new JSON<Long>(Long.parseLong(s));
						else
							valueObj = new JSON<Double>(Double.parseDouble(s));
						j.elements.put(key.toString(),valueObj);
					}
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
	public boolean isEmpty() {return size() == 0;}
	
	public JSON<?> getElement(String key) {Object o = elements.get(key); return (o instanceof JSON ? (JSON<?>)o : null);}
	public JSONObject getJSONObject(String key) {Object o = elements.get(key); if (o instanceof JSONObject) return (JSONObject)o; return null;}
	
	public String getString(String key) {JSON<?> o = getElement(key); if (o.value instanceof CharSequence) return o.value.toString(); return null;}
	public long getLong(String key) {JSON<?> o = getElement(key); if (o.value instanceof Long) return (Long) o.value; throw new RuntimeException("Wrong type (long) for "+key);}
	public double getDouble(String key) {JSON<?> o = getElement(key); if (o.value instanceof Double) return (Double) o.value; throw new RuntimeException("Wrong type (double) for "+key);}
	public boolean getBoolean(String key) {JSON<?> o = getElement(key); if (o.value instanceof Boolean) return (Boolean) o.value; throw new RuntimeException("Wrong type (boolean) for "+key);}
	public int getInt(String key) {return (int)getLong(key);}
	
	public Object[] getArray(String key) {
		JSONObject o = getJSONObject(key);
		if (o == null) return null;
		Object[] a = new Object[o.elements.size()];
		int i = 0;
		for (String k : o.elements.keySet()) if ((a[i++] = o.getElement(k)) == null) return null;
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
	public long[] getLongArray(String key) {
		JSONObject o = getJSONObject(key);
		if (o == null) return null;
		long[] a = new long[o.elements.size()];
		int i = 0;
		for (String k : o.elements.keySet()) a[i++] = o.getLong(k);
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
			else if (e instanceof JSON) sb.append(((JSON<?>)e).serialize());
		}
		sb.append(isArray() ? "]" : "}");
		return sb;
	}
	
	private static class JSON<T> {
		private final T value;
		private JSON(T value) {this.value = value;}
		public String toString() {return value.toString();}
		public String serialize() {
			if (value instanceof CharSequence)
			{
				CharSequence seq = (CharSequence)value;
				StringBuilder sb = new StringBuilder();
				sb.append('"');
				for (int i = 0; i < seq.length(); i++) {
					char c = seq.charAt(i);
					if (c == '"' && seq.charAt(i-1) != '\\')
						sb.append('\\');
					sb.append(c);
				}
				sb.append('"');
				return sb.toString();
			} else {
				return this.toString();
			}
		}
	}
}