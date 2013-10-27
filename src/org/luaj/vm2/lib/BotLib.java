package org.luaj.vm2.lib;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import pl.shockah.shocky.Utils;

public class BotLib extends OneArgFunction {

	public LuaValue init() {
		LuaTable t = (LuaTable) env.get("string");
		bind(t, BotLib.class, new String[] {"munge", "flip", "odd", "paste", "shorten"}, 1);
		return t;
	}
	
	public LuaValue call(LuaValue arg) {
		if (opcode == 0)
			return init();
		String a = arg.checkjstring();
		String s = null;
		switch (opcode) { 
		case 1: s = Utils.mungeNick(a); break;
		case 2: s = Utils.flip(a); break;
		case 3: s = Utils.odd(a); break;
		case 4: s = Utils.paste(a); break;
		case 5: s = Utils.shortenUrl(a); break;
		}
		return s == null ? NIL : valueOf(s);
	}
}