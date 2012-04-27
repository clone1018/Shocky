package pl.shockah;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class BiHashMap<K,V> extends HashMap<K,V> {
	private static final long serialVersionUID = 223379645643426032L;
	protected HashMap<V,K> second = new HashMap<V,K>();
	
	public void clear() {
		second.clear();
		super.clear();
	}
	
	public Object clone() {
		BiHashMap<K,V> ret = new BiHashMap<K,V>();
		ret.putAll(this);
		return ret;
	}
	
	public boolean containsValue(Object value) {
		return second.containsKey(value);
	}
	
	public Set<Map.Entry<V,K>> entrySetValue() {
		return second.entrySet();
	}
	
	public K getKey(Object value) {
		return second.get(value);
	}
	
	public Set<V> valueSet() {
		return second.keySet();
	}
	
	public V put(K key, V value) {
		second.put(value,key);
		return super.put(key,value);
	}
	
	public K removeKey(Object value) {
		return second.remove(value);
	}
	
	public Collection<K> keys() {
		return second.values();
	}
}