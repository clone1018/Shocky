package pl.shockah.shocky;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.shocky.cmds.CommandCallback;

public abstract class Module extends ListenerAdapter implements Comparable<Module> {
	private static final List<Module> modules = Collections.synchronizedList(new ArrayList<Module>());
	private static final List<ModuleLoader> loaders = Collections.synchronizedList(new ArrayList<ModuleLoader>());
	private static final Map<String, ScriptModule> scriptingModules = Collections.synchronizedMap(new HashMap<String,ScriptModule>());
	private static final LinkedList<Module> needsInit =  new LinkedList<Module>();
	
	private boolean enabled = false;
	private final List<String> disabledChannnels = Collections.synchronizedList(new LinkedList<String>());
	
	static {
		Module.registerModuleLoader(new ModuleLoader.Java());
	}
	
	public static void registerModuleLoader(ModuleLoader loader) {
		if (loaders.contains(loader)) loaders.remove(loader);
		loaders.add(loader);
	}
	public static void unregisterModuleLoader(ModuleLoader loader) {
		if (loaders.contains(loader)) {
			loader.unloadAllModules();
			loaders.remove(loader);
		}
	}
	
	private static void setup(Module module, ModuleLoader loader, ModuleSource<?> source) {
		module.loader = loader;
		module.source = source;
	}
	public static Module load(ModuleSource<?> source) {
		Module module = null;
		for (int i = 0; i < loaders.size(); i++) {
			if (loaders.get(i).accept(source)) module = loaders.get(i).loadModule(source);
			if (module != null) {
				setup(module,loaders.get(i),source);
				break;
			}
		}
		
		if (module != null) {
			for (int i = 0; i < modules.size(); i++) if (modules.get(i).name().equals(module.name())) {
				module.loader.unloadModule(module);
				return null;
			}
			
			modules.add(module);
			needsInit.push(module);
			if (module instanceof ScriptModule) {
				ScriptModule sModule = (ScriptModule)module;
				scriptingModules.put(sModule.identifier(), sModule);
			}
		}
		return module;
	}
	
	public static void postLoad() {
		synchronized (needsInit) {
			while (!needsInit.isEmpty()) {
				Module module = needsInit.pop();
				String name = "module-"+module.name();
				Data.config.setNotExists(name,true);
				if (Data.config.getBoolean(name))
					module.enable(null);
				for (String key : Data.config.getKeysSubconfigs())
					if (key.startsWith("#")&&!Data.forChannel(key).getBoolean(name))
						module.disable(key);
			}
		}
	}
	
	public final boolean unload() {
		if (!modules.contains(this))
			return false;
		if (this.enabled)
			this.disable(null);
		modules.remove(this);
		if (this instanceof ScriptModule)
			scriptingModules.remove(((ScriptModule)this).identifier());
		this.loader.unloadModule(this);
		return true;
	}
	public final boolean reload() {
		ModuleSource<?> src = this.source;
		this.unload();
		boolean ret = load(src) != null;
		postLoad();
		return ret;
	}
	
	public final boolean enable(String channel) {
		if (channel != null) {
			return disabledChannnels.remove(channel);
		} else {
			if (this.enabled)
				return false;
			this.onEnable(Data.lastSave);
			if (this.isListener())
				Shocky.getBotManager().getListenerManager().addListener(this);
			this.enabled = true;
			return true;
		}
	}
	
	public final boolean disable(String channel) {
		if (channel != null) {
			if (disabledChannnels.contains(channel))
				return false;
			return disabledChannnels.add(channel);
		} else {
			if (!this.enabled)
				return false;
			if (this.isListener())
				Shocky.getBotManager().getListenerManager().removeListener(this);
			this.onDataSave(Data.lastSave);
			this.onDisable();
			this.enabled = false;
			return true;
		}
	}
	
	public static ArrayList<Module> loadNewModules() {
		ArrayList<Module> ret = new ArrayList<Module>();
		File dir = new File("modules"); dir.mkdir();
		for (File f : dir.listFiles()) {
			if (f.isDirectory()) continue;
			Module m = load(new ModuleSource<File>(f));
			if (m != null) ret.add(m);
		}
		Module.postLoad();
		Collections.sort(ret);
		return ret;
	}
	
	public static Module getModule(String name) {
		for (int i = 0; i < modules.size(); i++)
			if (modules.get(i).name().equals(name)) return modules.get(i);
		return null;
	}
	public static ArrayList<Module> getModules() {
		ArrayList<Module> ret = new ArrayList<Module>(modules);
		Collections.sort(ret);
		return ret;
	}
	public static ArrayList<Module> getModules(boolean enabled) {
		ArrayList<Module> ret = new ArrayList<Module>(modules.size());
		for (int i = modules.size()-1; i >= 0; --i) {
			Module m = modules.get(i);
			if (m.enabled == enabled)
				ret.add(m);
		}
		Collections.sort(ret);
		return ret;
	}
	
	public static ScriptModule getScriptingModule(String id) {
		if (scriptingModules.containsKey(id))
			return scriptingModules.get(id);
		return null;
	}
	
	public File[] getReadableFiles() {
		return new File[0];
	}
	
	private ModuleLoader loader;
	private ModuleSource<?> source;
	
	public abstract String name();
	
	public void onEnable(File dir) {}
	public void onDisable() {}
	public void onDie(PircBotX bot) {}
	public void onDataSave(File dir) {}
	public void onCleanup(PircBotX bot, CommandCallback callback, User sender) {}
	public boolean isListener() {return false;}
	
	public final boolean isEnabled(String channel) {
		if (this.disabledChannnels.contains(channel))
			return false;
		return this.enabled;
	}
	
	public final int compareTo(Module module) {
		return name().compareTo(module.name());
	}
	
	@SuppressWarnings("unchecked")
	public <R> R invokeMethod(String name, Object... args) {
		Class<?>[] argTypes = new Class[args.length];
		for (int i = 0; i < args.length; i++)
			argTypes[i] = args[i].getClass();
		try {
			Method method = this.getClass().getDeclaredMethod(name, argTypes);
			if ((method.getModifiers() & Modifier.PUBLIC) == 0)
				method.setAccessible(true);
			return (R)method.invoke(this, args);
		} catch (Exception e) {
			return null;
		}
	}
}