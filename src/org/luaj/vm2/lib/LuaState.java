package org.luaj.vm2.lib;

import java.util.Map;

import org.luaj.vm2.LuaValue;
import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.interfaces.IFactoid;
import pl.shockah.shocky.interfaces.IRollback;

public class LuaState extends LuaValue {
	public final PircBotX bot;
	public final Channel chan;
	public final User user;
	
	private IFactoid factoidmod;
	private IRollback rollbackmod;
	
	public final Map<Integer, Object> cache;

	public LuaState(PircBotX bot, Channel chan, User user, Map<Integer, Object> cache) {
		super();
		this.bot = bot;
		this.chan = chan;
		this.user = user;
		
		this.cache = cache;
	}

	@Override
	public int type() {
		return LuaValue.TUSERDATA;
	}

	@Override
	public String typename() {
		return "userdata";
	}

	public boolean isController() {
		if (bot == null)
			return true;
		if (bot.getInetAddress().isLoopbackAddress())
			return true;
		if (Shocky.getLogin(user) == null)
			return false;
		return Data.controllers.contains(Shocky.getLogin(user));
	}
	
	public IFactoid getFactoidModule() {
		if (factoidmod == null)
			factoidmod = (IFactoid) Module.getModule("factoid");
		return factoidmod;
	}
	
	public IRollback getRollbackModule() {
		if (rollbackmod == null)
			rollbackmod = (IRollback) Module.getModule("rollback");
		return rollbackmod;
	}
}