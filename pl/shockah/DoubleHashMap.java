package pl.shockah;

import java.util.HashMap;

public class DoubleHashMap<K> extends HashMap<K,K> {
	private static final long serialVersionUID = -4675652729925899134L;
	
	public K put(K key, K value) {
		super.put(value,key);
		return super.put(key,value);
	}
}
