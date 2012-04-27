package pl.shockah.shocky;

import java.io.Console;
import pl.shockah.shocky.cmds.Command;

public class ThreadConsoleInput extends Thread {
	public void run() {
		Console c = System.console();
		if (c == null) return;
		
		String line;
		while (true) {
			line = c.readLine();
			if (line != null) {
				Command cmd = Command.getCommand(null,Command.EType.Console,line);
				if (cmd != null) cmd.doCommand(null,Command.EType.Console,null,null,line);
			}
		}
	}
}