package pl.shockah.shocky;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ModuleLoader {
	static {
		Module.registerModuleLoader(new Java());
	}
	
	protected List<Module> modules = Collections.synchronizedList(new ArrayList<Module>());
	
	protected Module loadModule(ModuleSource source) {
		Module m = load(source);
		if (m != null) modules.add(m);
		return m;
	}
	protected void unloadModule(Module module) {
		if (modules.contains(module)) modules.remove(module);
	}
	
	public void unloadAllModules() {
		while (!modules.isEmpty()) Module.unload(modules.get(0));
	}
	
	protected abstract boolean accept(ModuleSource source);
	protected abstract Module load(ModuleSource source);
	
	public static class Java extends ModuleLoader {
		protected boolean accept(ModuleSource source) {
			return source instanceof ModuleSource.File || source instanceof ModuleSource.URL;
		}
		protected Module load(ModuleSource source) {
			Module module = null;
			try {
				if (source instanceof ModuleSource.File) {
					ModuleSource.File src = (ModuleSource.File)source;
					String moduleName = src.file.getName(); 
					if (moduleName.endsWith(".class")) moduleName = new StringBuilder(moduleName).reverse().delete(0,6).reverse().toString(); else return null;
					
					Class<?> c = new URLClassLoader(new URL[]{new URL(src.file.getParent())}).loadClass(moduleName);
					if (c.isAssignableFrom(Module.class)) return (Module)c.newInstance();
				} else if (source instanceof ModuleSource.URL) {
					ModuleSource.URL src = (ModuleSource.URL)source;
					String moduleName = src.url.toString();
					StringBuilder sb = new StringBuilder(moduleName).reverse();
					moduleName = new StringBuilder(sb.substring(0,sb.indexOf("/"))).reverse().toString();
					String modulePath = new StringBuilder(src.url.toString()).delete(0,src.url.toString().length()-moduleName.length()).toString();
					if (moduleName.endsWith(".class")) moduleName = new StringBuilder(moduleName).reverse().delete(0,6).reverse().toString(); else return null;
					
					Class<?> c = new URLClassLoader(new URL[]{new URL(modulePath)}).loadClass(moduleName);
					if (c.isAssignableFrom(Module.class)) return (Module)c.newInstance();
				}
			} catch (Exception e) {e.printStackTrace();}
			return module;
		}
	}
}