package pl.shockah.shocky.cmds;

import java.net.URL;
import java.util.ArrayList;
import org.pircbotx.Colors;

import pl.shockah.Config;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.ModuleSource;

public class CmdModule extends Command {
	public String command() {return "module";}
	public String help(Parameters params) {
		StringBuilder sb = new StringBuilder();
		sb.append("module - list modules\n");
		sb.append("module on/off - list enabled/disabled modules\n");
		sb.append("[r:controller] module loadnew - loads new modules\n");
		sb.append("[r:controller] module loadhttp {url} - loads a module from URL\n");
		sb.append("[r:controller] module on/off {module} - enables/disables module\n");
		sb.append("[r:controller] module unload {module} - unloads module\n");
		sb.append("[r:controller] module reload {module} - reloads module\n");
		sb.append("[r:controller] module reloadall - reloads all modules");
		return sb.toString();
	}
	
	public void doCommand(Parameters params, CommandCallback callback) {
		callback.type = EType.Notice;
		
		if (params.tokenCount == 0) {
			ArrayList<Module> modules = Module.getModules();
			StringBuilder sb = new StringBuilder();
			int i = 0;
			for (Module module : modules) {
				if (i != 0) sb.append(", ");
				boolean inchan = !module.isEnabled(params.channel.getName());
				boolean global = !module.isEnabled(null);
				sb.append(global ? Colors.RED : (inchan ? Colors.OLIVE : Colors.DARK_GREEN));
				sb.append(module.name());
				sb.append(Colors.NORMAL);
				if (++i == 30)
				{
					sb.append('\n');
					i = 0;
				}
			}
			callback.append(sb);
			return;
		} else if (params.tokenCount >= 1) {
			String method = params.nextParam();
			if (method.equalsIgnoreCase("on") || method.equalsIgnoreCase("off")) {
				boolean state = method.equalsIgnoreCase("on");
				if (params.tokenCount >= 2) {
					String moduleName = params.nextParam();
					Module module = Module.getModule(moduleName);
					boolean global = false;
					if (module == null) {
						callback.append("No such module");
						return;
					}
					String channelName;
					Config config;
					
					if (params.tokenCount >= 3) {
						String globalString = params.nextParam();
						if (globalString.equalsIgnoreCase("global"))
							global = true;
					}
						
					if (global) {
						params.checkController();
						channelName = null;
						config = Data.config;
					} else {
						params.checkOp();
						channelName = params.channel.getName();
						config = Data.forChannel(params.channel);
					}
					
					if (state) {
						callback.append(module.enable(channelName) ? "Enabled" : "Failed");
						config.set("module-"+module.name(),true);
						return;
					} else {
						callback.append(module.disable(channelName) ? "Disabled" : "Failed");
						config.set("module-"+module.name(),false);
						return;
					}
				} else {
					ArrayList<Module> modules = Module.getModules(state);
					StringBuilder sb = new StringBuilder();
					for (Module module : modules) {
						if (sb.length() != 0) sb.append(", ");
						sb.append(module.name());
					}
					callback.append(sb);
					return;
				}
			} else if (method.equalsIgnoreCase("loadnew")) {
				params.checkController();
				ArrayList<Module> modules = Module.loadNewModules();
				if (modules.isEmpty()) {
					callback.append("No new modules found");
				} else {
					StringBuilder sb = new StringBuilder();
					sb.append("Loaded modules: ");
					for (int i = 0; i < modules.size(); i++) {
						if (i != 0) sb.append(", ");
						sb.append(modules.get(i).name());
					}
					callback.append(sb);
				}
				return;
			} else if (method.equalsIgnoreCase("reloadall")) {
				params.checkController();
				ArrayList<Module> modules = Module.getModules(true);
				for (Module module : modules) module.reload();
				callback.append("Reloaded all modules");
				return;
			} else if (params.tokenCount >= 2 && (method.equalsIgnoreCase("reload") || method.equalsIgnoreCase("unload"))) {
				params.checkController();
				boolean state = method.equalsIgnoreCase("reload");
				String moduleName = params.nextParam();
				Module module = Module.getModule(moduleName);
				if (module == null) {
					callback.append("No such module");
					return;
				}
				if (state)
					callback.append(module.reload() ? "Reloaded" : "Failed");
				else
					callback.append(module.unload() ? "Unloaded" : "Failed");
				return;
			} else if (params.tokenCount >= 2 && method.equalsIgnoreCase("loadhttp")) {
				params.checkController();
				try {
					URL url = new URL(params.getParams(0));
					callback.append(Module.load(new ModuleSource<URL>(url)) != null ? "Loaded" : "Failed");
					Module.postLoad();
				} catch (Exception e) {
					e.printStackTrace();
				}
				return;
			}
		}
		
		callback.append(help(params));
	}
}