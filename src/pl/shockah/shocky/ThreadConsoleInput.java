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
				Command cmd = Command.getCommand(null,null,null,Command.EType.Console,callback,line);
				if (cmd != null) {
					Parameters params = new Parameters(null,Command.EType.Console,null,null,line);
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