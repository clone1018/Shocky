package pl.shockah.shocky;

import java.io.Console;

import pl.shockah.shocky.cmds.AuthorizationException;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;

public class ThreadConsoleInput extends Thread {
	public void run() {
		Console c = System.console();
		if (c == null) return;
		
		String line;
		while (true) {
			line = c.readLine();
			if (line != null) {
				CommandCallback callback = new CommandCallback();
				String[] args = line.split("\\s+", 2);
				Command cmd = Command.getCommand(null,null,null,Command.EType.Console,callback,args[0]);
				if (cmd != null) {
					String s = (args.length == 1) ? "" : args[1];
					Parameters params = new Parameters(null,Command.EType.Console,null,null,s);
					try {
						cmd.doCommand(params,callback);
					} catch (AuthorizationException e) {
						Shocky.sendConsole(e.toString());
						continue;
					}
				}
				if (callback.length()>0)
					Shocky.sendConsole(callback.toString());
			}
		}
	}
}