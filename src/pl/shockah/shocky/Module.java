package pl.shockah.shocky;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pircbotx.PircBotX;

public abstract class Module extends ListenerAdapter implements Comparable<Module> {
	private static final List<Module> modules = Collections.synchronizedList(new ArrayList<Module>());
	private static final List<Module> modulesOn = Collections.synchronizedList(new ArrayList<Module>());
	private static final List<ModuleLoader> loaders = Collections.synchronizedList(new ArrayList<ModuleLoader>());
	private static final Map<String, ScriptModule> scriptingModules = Collections.synchronizedMap(new HashMap<String,ScriptModule>());
	private static final Map<String, List<Module>> disabledModules = Collections.synchronizedMap(new HashMap<String,List<Module>>());
	
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
			if (module instanceof ScriptModule) {
				ScriptModule sModule = (ScriptModule)module;
				scriptingModules.put(sModule.identifier(), sModule);
			}
			Data.config.setNotExists("module-"+module.name(),true);
			if (Data.config.getBoolean("module-"+module.name())) enable(module,null);
			for (String key : Data.config.getKeysSubconfigs())
				if (key.startsWith("#")&&!Data.forChannel(key).getBoolean("module-"+module.name())) disable(module,key);
		}
		return module;
	}
	public static boolean unload(Module module) {
		if (module == null) return false;
		if (!modules.contains(module)) return false;
		if (modulesOn.contains(module)) {
			disable(module,null);
		}
		modules.remove(module);
		if (module instanceof ScriptModule) {
			ScriptModule sModule = (ScriptModule)module;
			scriptingModules.remove(sModule.identifier());
		}
		module.loader.unloadModule(module);
		return true;
	}
	public static boolean reload(Module module) {
		if (module == null) return false;
		ModuleSource<?> src = module.source;
		unload(module);
		return load(src) != null;
	}
	
	public static boolean enable(Module module, String channel) {
		if (module == null) return false;
		if (channel != null) {
			List<Module> disabled;
			if (!disabledModules.containsKey(channel))
			{
				disabled = new ArrayList<Module>();
				disabledModules.put(channel, disabled);
			} else {
				disabled = disabledModules.get(channel);
			}
			return disabled.remove(module);
		} else {
			if (modulesOn.contains(module)) return false;
			module.onEnable();
			if (module.isListener()) Shocky.getBotManager().getListenerManager().addListener(module);
			modulesOn.add(module);
			return true;
		}
	}
	public static boolean disable(Module module, String channel) {
		if (module == null) return false;
		if (channel != null) {
			List<Module> disabled;
			if (!disabledModules.containsKey(channel))
			{
				disabled = new ArrayList<Module>();
				disabledModules.put(channel, disabled);
			} else {
				disabled = disabledModules.get(channel);
			}
			if (disabled.contains(module))
				return false;
			return disabled.add(module);
		} else {
			if (!modulesOn.contains(module)) return false;
			if (module.isListener()) Shocky.getBotManager().getListenerManager().removeListener(module);
			module.onDataSave();
			module.onDisable();
			modulesOn.remove(module);
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
		Collections.sort(ret);
		return ret;
	}
	
	public static Module getModule(String name) {
		for (int i = 0; i < modules.size(); i++) if (modules.get(i).name().equals(name)) return modules.get(i);
		return null;
	}
	public static ArrayList<Module> getModules() {
		ArrayList<Module> ret = new ArrayList<Module>(modules);
		Collections.sort(ret);
		return ret;
	}
	public static ArrayList<Module> getModules(boolean enabled) {
		ArrayList<Module> ret = getModules();
		for (int i = 0; i < ret.size(); i++) if (modulesOn.contains(ret.get(i)) != enabled) ret.remove(i--);
		return ret;
	}
	
	public static ScriptModule getScriptingModule(String id) {
		if (scriptingModules.containsKey(id)) return scriptingModules.get(id);
		return null;
	}
	
	private ModuleLoader loader;
	private ModuleSource<?> source;
	
	public abstract String name();
	
	public void onEnable() {}
	public void onDisable() {}
	public void onDie(PircBotX bot) {}
	public void onDataSave() {}
	public boolean isListener() {return false;}
	
	public final boolean isEnabled(String channel) {
		if (disabledModules.containsKey(channel) && disabledModules.get(channel).contains(this))
			return false;
		return modulesOn.contains(this);
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