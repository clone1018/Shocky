package pl.shockah.shocky;

import java.util.Map;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.shocky.cmds.Command.EType;
import pl.shockah.shocky.sql.Factoid;

public abstract class ScriptModule extends Module {
	
	public abstract String identifier();
	
	public abstract String parse(Map<Integer,Object> cache, PircBotX bot, EType type, Channel channel, User sender, Factoid factoid, String code, String message);
	
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
}
