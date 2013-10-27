package org.luaj.vm2.lib;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;

import pl.shockah.shocky.Cache;
import pl.shockah.shocky.Data;
import pl.shockah.shocky.Module;
import pl.shockah.shocky.Shocky;
import pl.shockah.shocky.interfaces.IFactoid;
import pl.shockah.shocky.interfaces.IRollback;

public class LuaState {
	public final PircBotX bot;
	public final Channel chan;
	public final User user;
	
	private IFactoid factoidmod;
	private IRollback rollbackmod;
	
	public final Cache cache;
	
	private static final Map<Thread,LuaState> stateMap = Collections.synchronizedMap(new HashMap<Thread,LuaState>());
	
	public static LuaState getState() {
		return stateMap.get(Thread.currentThread());
	}
	
	public static void clearState() {
		stateMap.remove(Thread.currentThread());
	}
	
	public static void clearState(LuaState state) {
		stateMap.values().remove(state);
	}
	
	public static void setState(LuaState state) {
		stateMap.put(Thread.currentThread(),state);
	}
	
	public boolean containsKey(String type, Object key) {
		if (cache != null)
			return cache.containsKey(type, key);
		return false;
	}
	
	public Object get(String type, Object key) {
		if (cache != null)
			return cache.get(type, key);
		return null;
	}
	
	public void put(String type, Object key, Object value) {
		if (cache != null)
			cache.put(type, key, value);
	}
	
	public LuaState(PircBotX bot, Channel chan, User user) {
		this(bot, chan, user, null);
	}

	public LuaState(PircBotX bot, Channel chan, User user, Cache cache) {
		this.bot = bot;
		this.chan = chan;
		this.user = user;
		this.cache = cache;
	}

	public boolean isController() {
		if (bot == null)
			return true;
		if (bot.getInetAddress().isLoopbackAddress())
			return true;
		String login = Shocky.getLogin(user);
		return login != null && Data.controllers.contains(login);
	}
	
	public IFactoid getFactoidModule() {
		if (factoidmod == null) {
			Module module = Module.getModule("factoid");
			if (module instanceof IFactoid)
				factoidmod = (IFactoid) module;
		}
		if (factoidmod != null && chan != null && !factoidmod.isEnabled(chan.getName()))
			return null;
		return factoidmod;
	}
	
	public IRollback getRollbackModule() {
		if (rollbackmod == null) {
			Module module = Module.getModule("rollback");
			if (module instanceof IRollback)
				rollbackmod = (IRollback) module;
		}
		if (rollbackmod != null && chan != null && !rollbackmod.isEnabled(chan.getName()))
			return null;
		return rollbackmod;
	}
}