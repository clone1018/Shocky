package pl.shockah.shocky.cmds;

import java.net.URL;
import java.util.ArrayList;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.Config;
import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.ModuleSource;

public class CmdModule extends Command {
	public String command() {return "module";}
	public String help(PircBotX bot, EType type, Channel channel, User sender) {
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
	
	public void doCommand(PircBotX bot, EType type, CommandCallback callback, Channel channel, User sender, String message) {
		String[] args = message.split(" ");
		callback.type = EType.Notice;
		
		if (args.length == 1) {
			ArrayList<Module> modules = Module.getModules();
			StringBuilder sb = new StringBuilder();
			for (Module module : modules) {
				if (sb.length() != 0) sb.append(", ");
				sb.append(module.isEnabled(channel.getName()) ? Colors.DARK_GREEN : Colors.RED);
				sb.append(module.name());
				sb.append(Colors.BLACK);
			}
			callback.append(sb);
			return;
		}
		
		if (args.length == 2) {
			if (args[1].equalsIgnoreCase("on") || args[1].equalsIgnoreCase("off")) {
				boolean state = args[1].equalsIgnoreCase("on");
				ArrayList<Module> modules = Module.getModules(state);
				StringBuilder sb = new StringBuilder();
				for (Module module : modules) {
					if (sb.length() != 0) sb.append(", ");
					sb.append(module.name());
				}
				callback.append(sb);
				return;
			} else if (args[1].equalsIgnoreCase("loadnew")) {
				if (!canUseController(bot,type,sender)) return;
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
			} else if (args[1].equalsIgnoreCase("reloadall")) {
				if (!canUseController(bot,type,sender)) return;
				ArrayList<Module> modules = Module.getModules(true);
				for (Module module : modules) Module.reload(module);
				callback.append("Reloaded all modules");
				return;
			}
		}
		
		if (args.length == 3 || args.length == 4) {
			Module module = Module.getModule(args[2]);
			if (module == null) {
				callback.append("No such module");
				return;
			}
			
			String channelName;
			Config config;
			
			if (args.length == 4 && args[3].equalsIgnoreCase("global")) {
				if (!canUseController(bot,type,sender)) return;
				channelName = null;
				config = Data.config;
			} else {
				if (!canUseOp(bot,type,channel,sender)) return;
				channelName = channel.getName();
				config = Data.forChannel(channel);
			}
			
			if (args[1].equalsIgnoreCase("on")) {
				callback.append(Module.enable(module,channelName) ? "Enabled" : "Failed");
				config.set("module-"+module.name(),true);
				return;
			} else if (args[1].equalsIgnoreCase("off")) {
				callback.append(Module.disable(module,channelName) ? "Disabled" : "Failed");
				config.set("module-"+module.name(),false);
				return;
			} else if (args[1].equalsIgnoreCase("reload")) {
				callback.append(Module.reload(module) ? "Reloaded" : "Failed");
				return;
			} else if (args[1].equalsIgnoreCase("unload")) {
				callback.append(Module.unload(module) ? "Unloaded" : "Failed");
				return;
			}
		}
		
		if (args.length >= 3) {
			if (args[1].equalsIgnoreCase("loadhttp")) {
				if (!canUseController(bot,type,sender)) return;
				try {
					URL url = new URL(StringTools.implode(args,2," "));
					callback.append(Module.load(new ModuleSource<URL>(url)) != null ? "Loaded" : "Failed");
				} catch (Exception e) {e.printStackTrace();}
				return;
			}
		}
		
		callback.append(help(bot,type,channel,sender));
	}
}