package pl.shockah.shocky;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class Cache {
	private final Map<String,Map<Object, Object>> cache = new TreeMap<String,Map<Object, Object>>();
	
	public boolean containsKey(String type, Object key) {
		synchronized (this) {
			if (cache.containsKey(type))
				return cache.get(type).containsKey(key);
		}
		return false;
	}
	
	public Object get(String type, Object key) {
		synchronized (this) {
			if (cache.containsKey(type))
				return cache.get(type).get(key);
		}
		return null;
	}
	
	public void put(String type, Object key, Object value) {
		synchronized (this) {
			Map<Object, Object> map;
			if (cache.containsKey(type)) {
				map = cache.get(type);
			} else {
				map = new HashMap<Object, Object>(4);
				cache.put(type, map);
			}
			map.put(key, value);
		}
	}
}
