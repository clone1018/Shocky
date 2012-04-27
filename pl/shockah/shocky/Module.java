package pl.shockah.shocky;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import org.pircbotx.PircBotX;

public abstract class Module extends ListenerAdapter {
	private static final ArrayList<Module> modules = new ArrayList<Module>();
	private static final ArrayList<URL> urls = new ArrayList<URL>();
	private static final ArrayList<String> loadedClasses = new ArrayList<String>();
	private static final HashMap<Module,Object> classSource = new HashMap<Module,Object>();
	
	private static synchronized Module load(String cls) {
		try {
			Module module = (Module)new URLClassLoader(urls.toArray(new URL[urls.size()])).loadClass(cls).newInstance();
			classSource.put(module,cls);
			afterLoad(module);
			return module;
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	public static synchronized Module load(URL url) {
		try {
			String u = new StringBuilder(url.toExternalForm()).reverse().toString(), cname = u;
			u = new StringBuilder(u.substring(u.indexOf('/'))).reverse().toString();
			cname = new StringBuilder(cname.substring(0,cname.indexOf('/'))).reverse().toString().replace(".class","");
			
			Module module = (Module)new URLClassLoader(new URL[]{new URL(u)}).loadClass(cname).newInstance();
			classSource.put(module,url);
			afterLoad(module);
			return module;
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	private static synchronized void afterLoad(Module module) {
		loadedClasses.add(module.getClass().getName());
		modules.add(module);
		Data.config.setNotExists("module-"+module.name(),true);
		if (Data.config.getBoolean("module-"+module.name())) on(module);
	}
	
	private static Module load(Module module, Object source) {
		if (source instanceof URL) return load((URL)source);
		return load(module.getClass().getName());
	}
	public static synchronized boolean unload(Module module) {
		if (module == null) return false;
		off(module);
		loadedClasses.remove(module.getClass().getName());
		classSource.remove(module);
		modules.remove(module);
		return true;
	}
	public static boolean reload(Module module) {
		Object source = classSource.get(module);
		if (!unload(module)) return false;
		return load(module,source) != null;
	}
	public static boolean reload(String module) {
		return reload(getModule(module.toLowerCase()));
	}
	
	public static synchronized void updateURLs() {
		urls.clear();
		try {
			File dir = new File("modules"); dir.mkdir();
			urls.add(dir.toURI().toURL());
			for (File f : dir.listFiles()) {
				if (f.isDirectory()) continue;
				if (!f.getName().endsWith(".jar")) continue;
				urls.add(f.toURI().toURL());
			}
			
		} catch (Exception e) {e.printStackTrace();}
	}
	public static synchronized ArrayList<Module> loadNewModules() {
		updateURLs();
		ArrayList<Module> ret = new ArrayList<Module>();
		File dir = new File("modules"); dir.mkdir();
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) continue;
			if (f.getName().contains("$")) continue;
			if (!f.getName().endsWith(".class")) continue;
			if (!f.getName().startsWith("Module")) continue;
			if (loadedClasses.contains(f.getName().replace(".class",""))) continue;
			ret.add(load(f.getName().replace(".class","")));
		}
		return ret;
	}
	
	public static synchronized ArrayList<Module> getModules(boolean withTurnedOff) {
		ArrayList<Module> ret = new ArrayList<Module>(modules);
		if (!withTurnedOff) for (int i = 0; i < ret.size(); i++) if (!ret.get(i).isOn()) ret.remove(i--);
		return ret;
	}
	public static synchronized Module getModule(String name) {
		for (Module module : modules) if (module.name().equalsIgnoreCase(name)) return module;
		return null;
	}
	public static synchronized boolean on(Module module) {
		if (module == null) return false;
		if (module.isOn()) return false;
		Shocky.getBotManager().getListenerManager().addListener(module);
		module.load();
		module.isOn = true;
		return true;
	}
	public static synchronized boolean off(Module module) {
		if (module == null) return false;
		if (!module.isOn()) return false;
		Shocky.getBotManager().getListenerManager().removeListener(module);
		module.onDataSave();
		module.unload();
		module.isOn = false;
		return true;
	}
	
	protected boolean isOn = false;
	
	public Module() {}
	public final boolean isOn() {return isOn;}
	public abstract String name();
	public abstract void load();
	public abstract void unload();
	public void onDataSave() {}
	public void onDie(PircBotX bot) {}
}