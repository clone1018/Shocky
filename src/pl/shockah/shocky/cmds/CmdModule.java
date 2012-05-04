package pl.shockah.shocky.cmds;

import java.net.URL;
import java.util.ArrayList;
import org.pircbotx.Channel;
import org.pircbotx.Colors;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import pl.shockah.StringTools;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;

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
	public boolean matches(PircBotX bot, EType type, String cmd) {return cmd.equals(command());}
	
	public void doCommand(PircBotX bot, EType type, Channel channel, User sender, String message) {
		String[] args = message.split(" ");
		
		if (args.length == 1) {
			ArrayList<Module> modules = Module.getModules(true);
			StringBuilder sb = new StringBuilder();
			for (Module module : modules) {
				if (sb.length() != 0) sb.append(", ");
				sb.append(module.isOn() ? Colors.DARK_GREEN : Colors.RED);
				sb.append(module.name());
				sb.append(Colors.BLACK);
			}
			Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,sb.toString());
			return;
		}
		
		if (args.length == 2) {
			if (args[1].toLowerCase().equals("on") || args[1].toLowerCase().equals("off")) {
				boolean state = args[1].toLowerCase().equals("on");
				ArrayList<Module> modules = Module.getModules(true);
				StringBuilder sb = new StringBuilder();
				for (Module module : modules) if (module.isOn() == state) {
					if (sb.length() != 0) sb.append(", ");
					sb.append(module.name());
				}
				Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,sb.toString());
				return;
			} else if (args[1].toLowerCase().equals("loadnew")) {
				if (!canUseController(bot,type,sender)) return;
				ArrayList<Module> modules = Module.loadNewModules();
				if (modules.isEmpty()) {
					Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,"No new modules found");
				} else {
					StringBuilder sb = new StringBuilder();
					sb.append("Loaded modules: ");
					for (int i = 0; i < modules.size(); i++) {
						if (i != 0) sb.append(", ");
						sb.append(modules.get(i).name());
					}
					Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,sb.toString());
				}
				return;
			} else if (args[1].toLowerCase().equals("reloadall")) {
				if (!canUseController(bot,type,sender)) return;
				ArrayList<Module> modules = Module.getModules(true);
				for (Module module : modules) Module.reload(module);
				Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,"Reloaded all modules");
				return;
			}
		}
		
		if (!canUseController(bot,type,sender)) return;
		
		if (args.length == 3) {
			Module module = Module.getModule(args[2]);
			if (args[1].toLowerCase().equals("on")) {
				Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,Module.on(module) ? "Enabled" : "Failed");
				Data.config.set("module-"+module.name(),true);
				return;
			} else if (args[1].toLowerCase().equals("off")) {
				Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,Module.off(module) ? "Disabled" : "Failed");
				Data.config.set("module-"+module.name(),false);
				return;
			} else if (args[1].toLowerCase().equals("reload")) {
				Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,Module.reload(module) ? "Reloaded" : "Failed");
				return;
			} else if (args[1].toLowerCase().equals("unload")) {
				Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,Module.unload(module) ? "Unloaded" : "Failed");
				return;
			}
		}
		
		if (args.length >= 3) {
			if (args[1].toLowerCase().equals("loadhttp")) {
				try {
					URL url = new URL(StringTools.implode(args,2," "));
					Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,Module.load(url) != null ? "Loaded" : "Failed");
				} catch (Exception e) {e.printStackTrace();}
				return;
			}
		}
		
		Shocky.send(bot,type,EType.Notice,EType.Notice,EType.Notice,EType.Console,channel,sender,help(bot,type,channel,sender));
	}
}