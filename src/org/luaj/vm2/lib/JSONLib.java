package org.luaj.vm2.lib;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import pl.shockah.HTTPQuery;
import pl.shockah.HTTPQuery.Method;
import pl.shockah.StringTools;

public class JSONLib extends VarArgFunction {
	
	static {
		try {
			Class.forName("org.json.JSONArray");
			Class.forName("org.json.JSONObject");
			Class.forName("org.json.JSONTokener");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	public LuaValue init() {
		LuaTable t = new LuaTable();
		bind(t, JSONLib.class, new String[] {"json","get","url","irc"}, 1);
		env.set("net", t);
		PackageLib.instance.LOADED.set("net", t);
		return t;
	}

	@Override
	public Varargs invoke(Varargs args) {
		switch (opcode) {
		case 0:
			return init();
		case 1:
			return JSONLib.json(args);
		case 2:
			return JSONLib.get(args);
		case 3:
			try {
				return valueOf(URLEncoder.encode(args.checkjstring(1), "UTF8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		case 4:
			return valueOf(StringTools.ircFormatted(args.checkjstring(1), args.optboolean(2, true)));
		}
		return NONE;
	}
	
	
	public static Varargs get(Varargs args) {
		String url = args.checkjstring(1);
		String post = args.optjstring(2, null);
		HTTPQuery q = null;
		
		LuaValue[] ret = new LuaValue[3];
		ret[0] = NIL;
		ret[1] = MINUSONE;
		
		try {
			Method method = Method.GET;
			if (post != null) {
				if (post.contentEquals("HEAD"))
					method = Method.HEAD;
				else if (!post.isEmpty())
					method = Method.POST;
			}
			q = HTTPQuery.create(url, method);
			q.connect(true, method == Method.POST);
			if (method == Method.POST)
				q.write(post);
			ret[0] = valueOf(q.readWhole());
		} catch (Exception e) {
			//throw new LuaError(e);
		} finally {
			if (q == null) 
				ret[2] = valueOf(url);
			else {
				HttpURLConnection connection = q.getConnection();
				try {
					ret[1] = valueOf(connection.getResponseCode());
				} catch (IOException e) {}
				ret[2] = valueOf(connection.getURL().toExternalForm());
				q.close();
			}
		}
		return varargsOf(ret);
	}

	public static Varargs json(Varargs args) {
		try {
			String content = args.checkjstring(1);
			if (content.isEmpty())
				return varargsOf(NIL, valueOf("empty input"));
			if (content.charAt(0)=='[')
				return getJSONArray(new JSONArray(content));
			return getJSONTable(new JSONObject(content));
		} catch (JSONException e) {
			return varargsOf(NIL, valueOf(e.getMessage()));
		}
	}

	private static LuaTable getJSONTable(JSONObject json) throws JSONException {
		LuaTable table = new LuaTable();
		@SuppressWarnings("unchecked")
		Iterator<String> iter = json.keys();
		while (iter.hasNext()) {
			String key = iter.next();
			Object obj = json.get(key);
			table.set(key,getValue(obj));
		}
		return table;
	}
	
	private static LuaTable getJSONArray(JSONArray json) throws JSONException {
		LuaTable table = new LuaTable();
		for (int i = 0; i < json.length(); i++) {
			Object obj = json.get(i);
			table.set(i+1,getValue(obj));
		}
		return table;
	}
	
	private static LuaValue getValue(Object obj) throws JSONException {
		if (obj instanceof Boolean)
			return valueOf((Boolean)obj);
		else if (obj instanceof Integer)
			return valueOf((Integer)obj);
		else if (obj instanceof Double)
			return valueOf((Double)obj);
		else if (obj instanceof String)
			return valueOf((String)obj);
		else if (obj instanceof JSONArray) {
			return getJSONArray((JSONArray)obj);
		} else if (obj instanceof JSONObject) {
			return getJSONTable((JSONObject)obj);
		}
		return NIL;
	}
}
