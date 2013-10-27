package pl.shockah.shocky.cmds;

import java.util.StringTokenizer;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.shocky.Data;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.cmds.Command.EType;


public class Parameters {

	public final PircBotX bot;
	public final Channel channel;
	public final User sender;
	public final EType type;
	
	public final String input;
	private StringTokenizer tokens;
	public final int tokenCount;
	
	public Parameters(PircBotX bot, EType type, Channel channel, User sender, String input) {
		this.bot = bot;
		this.type = type;
		this.channel = channel;
		this.sender = sender;
		
		this.input = input;
		this.tokens = new StringTokenizer(input);
		this.tokenCount = this.tokens.countTokens();
	}
	
	public void resetParams() {
		this.tokens = new StringTokenizer(this.input);
	}
	
	public String nextParam() {
		return this.tokens.nextToken();
	}
	
	public boolean hasMoreParams() {
		return this.tokens.hasMoreTokens();
	}
	
	public int countParams() {
		return this.tokens.countTokens();
	}
	
	public String getParams(int start) {
		if (start < 0)
			throw new IndexOutOfBoundsException("start is outside token range");
		StringBuilder sb = new StringBuilder();
		int i = 0;
		int c = 0;
		while (this.tokens.hasMoreTokens()) {
			String token = this.tokens.nextToken();
			if (i++ < start)
				continue;
			if (c++ > 0)
				sb.append(' ');
			sb.append(token);
		}
		return sb.toString();
	}
	
	public String getParams(int start, int end) {
		if (start < 0 || start >= tokenCount)
			throw new IndexOutOfBoundsException("start is outside token range");
		if (end < 0 || end >= tokenCount)
			throw new IndexOutOfBoundsException("end is outside token range");
		if (end < start)
			throw new IllegalArgumentException("end is less than start");
		this.tokens = new StringTokenizer(this.input);
		StringBuilder sb = new StringBuilder();
		int i = 0;
		int c = 0;
		while (this.tokens.hasMoreTokens()) {
			String token = this.tokens.nextToken();
			if (i++ < start)
				continue;
			sb.append(token);
			if (c++ > 0)
				sb.append(' ');
			if (i > end)
				break;
		}
		return sb.toString();
	}
	
	public boolean isController() {
		if (bot == null) return true;
		if (bot.getInetAddress().isLoopbackAddress()) return true;
		if (type == EType.Console) return true;
		if (Shocky.getLogin(sender) == null) return false;
		return Data.controllers.contains(Shocky.getLogin(sender));
	}
	public void checkController() {
		if (isController())
			return;
		throw new AuthorizationException("Must be a controller to use this command.");
	}
	
	public boolean isOp() {
		if (type == EType.Console) return false;
		if (channel == null) return false;
		return channel.isOp(sender);
	}
	public void checkOp() {
		if (isOp())
			return;
		throw new AuthorizationException("Must have +o in channel to use this command.");
	}
	
	public void checkAny() {
		if (isController() || isOp()) return;
		throw new AuthorizationException("Restricted command");
	}
}
