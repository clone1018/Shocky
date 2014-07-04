package pl.shockah.shocky.cmds;

import java.io.IOException;

import pl.shockah.shocky.WebServer;

public class CmdWeb extends Command {
	public String command() {return "web";}
	public String help(Parameters params) {
		return "[r:controller] web {cmd} [args] - configures the web server";
	}
	
	public void doCommand(Parameters params, CommandCallback callback) {
		params.checkController();
		callback.type = EType.Notice;
		if (params.tokenCount == 0) {
			callback.append(help(params));
			return;
		}
		
		String command = params.nextParam();
		if (command.equalsIgnoreCase("stop")) {
			WebServer.stop();
			callback.append("Done.");
			return;
		}
		
		if (command.equalsIgnoreCase("start")) {
			String host;
			int port;
			
			if (params.hasMoreParams())
				host = params.nextParam();
			else
				host = params.bot.getUserBot().getHostmask();
			
			try {
				if (params.hasMoreParams())
					port = Integer.valueOf(params.nextParam());
				else
					port = 8000;
			
				WebServer.start(host, port);
				callback.append("Done.");
			} catch (IOException e) {
				callback.append(e.getLocalizedMessage());
			} catch (NumberFormatException e) {
				callback.append(e.getLocalizedMessage());
			}
			return;
		}
		
		callback.append(help(params));
	}
}