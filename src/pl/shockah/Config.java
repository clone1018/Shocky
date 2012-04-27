package pl.shockah;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import pl.shockah.FileLine;

public class Config {
	private HashMap<String,String> mapValues = new HashMap<String,String>();
	private HashMap<String,Config> mapSubconfigs = new HashMap<String,Config>();
	
	public synchronized void set(String key, Object value) {
		String[] sub = key.split(Pattern.quote("->"));
		
		Config cfg = this;
		for (int i = 0; i < sub.length-1; i++) cfg = cfg.getConfig(sub[i]);
		cfg.set(sub[sub.length-1],value,true);
	}
	protected synchronized void set(String key, Object value, boolean direct) {
		if (direct) mapValues.put(key,value.toString());
		else set(key,value);
	}
	public void set(String key, boolean value) {set(key,new Boolean(value));}
	public void set(String key, int value) {set(key,new Integer(value));}
	public void set(String key, long value) {set(key,new Long(value));}
	public void set(String key, float value) {set(key,new Float(value));}
	public void set(String key, double value) {set(key,new Double(value));}
	
	public void add(String key, int value, int def) {setNotExists(key,def); set(key,getInt(key)+value);}
	public void add(String key, long value, long def) {setNotExists(key,def); set(key,getLong(key)+value);}
	public void add(String key, float value, float def) {setNotExists(key,def); set(key,getFloat(key)+value);}
	public void add(String key, double value, double def) {setNotExists(key,def); set(key,getDouble(key)+value);}
	
	public synchronized boolean remove(String key) {
		String[] sub = key.split(Pattern.quote("->"));
		
		Config cfg = this;
		for (int i = 0; i < sub.length-1; i++) cfg = cfg.getConfig(sub[i].trim());
		return cfg.remove(sub[sub.length-1].trim(),true);
	}
	protected synchronized boolean remove(String key, boolean direct) {
		if (direct) {
			if (mapValues.containsKey(key)) {
				mapValues.remove(key);
				return true;
			} else return false;
		} else return remove(key);
	}
	
	public synchronized void makeConfig(String key) {
		if (mapValues.containsKey(key)) mapValues.remove(key);
		if (mapSubconfigs.containsKey(key)) return;
		mapSubconfigs.put(key,new Config());
	}
	public synchronized Config getConfig(String key) {
		if (!mapSubconfigs.containsKey(key)) makeConfig(key);
		return mapSubconfigs.get(key);
	}
	public synchronized void removeConfig(String key) {
		mapSubconfigs.remove(key);
	}
	public synchronized boolean existsConfig(String key) {
		return mapSubconfigs.containsKey(key);
	}
	
	public synchronized boolean exists(String key) {
		return mapValues.containsKey(key);
	}
	public void setNotExists(String key, Object value) {
		if (!exists(key)) set(key,value);
	}
	public void setNotExists(String key, boolean value) {setNotExists(key,new Boolean(value));}
	public void setNotExists(String key, int value) {setNotExists(key,new Integer(value));}
	public void setNotExists(String key, long value) {setNotExists(key,new Long(value));}
	public void setNotExists(String key, float value) {setNotExists(key,new Float(value));}
	public void setNotExists(String key, double value) {setNotExists(key,new Double(value));}
	
	public synchronized String getString(String key) {
		return mapValues.get(key);
	}
	public boolean getBoolean(String key) {
		String v = getString(key);
		if (v.equals("1")) return true;
		if (v.equals("0")) return false;
		return Boolean.parseBoolean(v);
	}
	public int getInt(String key) {return Integer.parseInt(getString(key));}
	public long getLong(String key) {return Long.parseLong(getString(key));}
	public float getFloat(String key) {return Float.parseFloat(getString(key));}
	public double getDouble(String key) {return Double.parseDouble(getString(key));}
	
	public void load(File file) {
		load(FileLine.read(file));
	}
	public void load(ArrayList<String> lines) {
		for (String line : lines) {
			line = line.trim();
			if (line.isEmpty()) continue;
			
			if (line.startsWith("//")) continue;
			if (line.indexOf('=') == -1) continue;
			String key = line.substring(0,line.indexOf('='));
			String value = line.substring(key.length()+1);
			set(key.trim(),value.trim());
		}
	}
	public void save(File file) {
		ArrayList<String> lines = new ArrayList<String>();
		saveSubconfig(lines,"");
		
		Collections.sort(lines);
		FileLine.write(file,lines);
	}
	protected void saveSubconfig(ArrayList<String> lines, String path) {
		Iterator<Entry<String,Config>> it1 = mapSubconfigs.entrySet().iterator();
		while (it1.hasNext()) {
			Entry<String,Config> pair = it1.next();
			pair.getValue().saveSubconfig(lines,(path.isEmpty()? "" : path+"->")+pair.getKey());
		}
		
		Iterator<Entry<String,String>> it2 = mapValues.entrySet().iterator();
		while (it2.hasNext()) {
			Entry<String,String> pair = it2.next();
			lines.add(path+(path.isEmpty() ? "" : "->")+pair.getKey()+" = "+pair.getValue());
		}
	}
}