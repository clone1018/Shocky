package pl.shockah.shocky;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.StringTools;
import pl.shockah.shocky.cmds.AuthorizationException;
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
		char c = 0;
		int x = 0;
		int y = 0;
		while (true) {
			int a = str.indexOf(quote, x);
			int b = str.indexOf('\\', x);
			if (a == -1 && b == -1)
				break;
			if (b != -1 && (a == -1 || b < a)) {
				c = '\\';
				y = b;
			}
			else if (a != -1 && (b == -1 || a < b)) {
				c = quote;
				y = a;
			}
			sb.append(str.substring(x, y)).append('\\').append(c);
			x = y+1;
		}
		sb.append(str.substring(x)).append(quote);
	}
	
	public Map<String,Object> getParams(PircBotX bot, Channel channel, User sender, String message, Factoid factoid) {
		User[] users;
		Map<String,Object> map = new LinkedHashMap<String,Object>();
		if (channel == null)
			users = new User[]{bot.getUserBot(),sender};
		else {
			users = channel.getUsers().toArray(new User[0]);
			map.put("channel", channel.getName());
		}
		map.put("bot", bot.getNick());
		map.put("sender", sender.getNick());
		map.put("host", sender.getHostmask());
		map.put("login", Whois.getWhoisLogin(sender));
		map.put("randnick", users[new Random().nextInt(users.length)].getNick());
		map.put("time", System.currentTimeMillis());

		if (message == null)
			message = "";
		StringTokenizer strtok = new StringTokenizer(message," ");
		String[] args = new String[strtok.countTokens()];
		int i = 0;
		while (strtok.hasMoreTokens())
			args[i++] = strtok.nextToken();
		map.put("argc", args.length);
		map.put("args", message);
		map.put("arg", args);
		map.put("ioru", (args.length == 0) ? sender.getNick() : message);
		if (factoid != null && factoid.registry != null)
			map.put("map", factoid.registry.getMap());
		return map;
	}
	
	public abstract class ScriptCommand extends Command {
		public void doCommand(Parameters params, CommandCallback callback) {
			if (params.tokenCount < 1) {
				callback.type = EType.Notice;
				callback.append(help(params));
				return;
			}
			
			String output;
			
			try {
				output = parse(new Cache(), params.bot, params.channel, params.sender, null, params.input, null);
			} catch (Throwable e) {
				while (e.getCause() != null)
					e = e.getCause();
				if (e instanceof AuthorizationException && params.sender != null)
					callback.type = EType.Notice;
				output = e.getMessage();
			}
			
			if (output != null && !output.isEmpty())
				callback.append(StringTools.limitLength(StringTools.formatLines(output)));
		}
	}
}
