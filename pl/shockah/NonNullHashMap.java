package pl.shockah;

import java.util.HashMap;

public class NonNullHashMap<K,V> extends HashMap<K,V> {
	private static final long serialVersionUID = -7976325226899180370L;
	
	public V put(K key, V value) {
		if (value == null) {
			if (containsKey(key)) remove(key);
			return null;
		}
		return super.put(key,value);
	}
}