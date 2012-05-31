package org.luaj.vm2.lib;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import pl.shockah.shocky.Utils;

public class BotLib extends OneArgFunction {

	public LuaValue init() {
		LuaTable t = (LuaTable) env.get("string");
		bind(t, BotLib.class, new String[] {"munge", "flip", "odd"}, 1);
		return t;
	}
	
	public LuaValue call(LuaValue arg) {
		switch ( opcode ) { 
		case 0: return init();
		case 1: return valueOf(Utils.mungeNick(arg.checkjstring()));
		case 2: return valueOf(Utils.flip(arg.checkjstring()));
		case 3: return valueOf(Utils.odd(arg.checkjstring()));
		}
		return NIL;
	}
}