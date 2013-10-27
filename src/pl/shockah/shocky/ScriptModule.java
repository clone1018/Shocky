package pl.shockah.shocky;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.StringTools;
import pl.shockah.shocky.cmds.Command;
import pl.shockah.shocky.cmds.CommandCallback;
import pl.shockah.shocky.cmds.Parameters;
import pl.shockah.shocky.sql.Factoid;

public abstract class ScriptModule extends Module {
	
	public abstract String identifier();
	
	public abstract String parse(Cache cache, PircBotX bot, Channel channel, User sender, Factoid factoid, String code, String message);
	
	public char stringCharacter() {return '"';}
	public void appendEscape(StringBuilder sb, String str) {
		char quote = stringCharacter();
		sb.append(quote);
		int x = 0;
		int y = 0;
		while ((y = str.indexOf(quote, x))>=0) {
			sb.append(str.substring(x, y)).append('\\').append(quote);
			x = y+1;
		}
		sb.append(str.substring(x));
		sb.append(quote);
	}
	
	public abstract class ScriptCommand extends Command {
		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.tokenCount < 1) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			
			String output = parse(new Cache(), params.bot, params.channel, params.sender, null, params.input, null);
			if (output != null && !output.isEmpty())
				callback.append(StringTools.limitLength(StringTools.formatLines(output)));
		}
	}
}
