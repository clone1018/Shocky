package org.luaj.vm2.lib;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

import pl.shockah.HTTPQuery;
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
		bind(t, JSONLib.class, new String[] {"json","url","irc"}, 1);
		env.set("net", t);
		if (LuaString.s_metatable == null)
			LuaString.s_metatable = tableOf(new LuaValue[] { INDEX, t });
		PackageLib.instance.LOADED.set("net", t);
		return t;
	}

	@Override
	public Varargs invoke(Varargs args) {
		switch (opcode) {
		case 0:
			return init();
		case 1:
			return JSONLib.get(args);
		case 2:
			try {
				return LuaValue.valueOf(URLEncoder.encode(args.checkjstring(1), "UTF8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		case 3:
			boolean urlEncode = true;
			if (args.narg() == 2)
				urlEncode = args.checkboolean(2);
			return LuaValue.valueOf(StringTools.ircFormatted(args.checkjstring(1), urlEncode));
		}
		return NONE;
	}

	public static Varargs get(Varargs args) {
		try {
			LuaString url = args.checkstring(1);
			String post = null;
			if (args.narg() == 2) {
				post = args.checkstring(2).checkjstring();
				if (post.isEmpty())
					post = null;
			}
			HTTPQuery q = new HTTPQuery(url.checkjstring(), post != null ? "POST" : "GET");
			q.connect(true, post != null);
			if (post != null)
				q.write(post);
			String line = q.readWhole();
			JSONObject json = new JSONObject(line);
			return getJSONTable(json);
		} catch (Exception e) {
			throw new LuaError(e);
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
			return LuaValue.valueOf((Boolean)obj);
		else if (obj instanceof Integer)
			return LuaValue.valueOf((Integer)obj);
		else if (obj instanceof Double)
			return LuaValue.valueOf((Double)obj);
		else if (obj instanceof String)
			return LuaValue.valueOf((String)obj);
		else if (obj instanceof JSONArray) {
			return getJSONArray((JSONArray)obj);
		} else if (obj instanceof JSONObject) {
			return getJSONTable((JSONObject)obj);
		}
		return NIL;
	}
}
