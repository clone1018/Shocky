package pl.shockah.shocky;

import java.io.Console;

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
				if (cmd != null)
					cmd.doCommand(null,Command.EType.Console,callback,null,null,line);
				if (callback.length()>0)
					Shocky.sendConsole(callback.toString());
			}
		}
	}
}