package org.luaj.vm2.lib;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import pl.shockah.shocky.Utils;

public class BotLib extends OneArgFunction {
	public BotLib() {
	}

	public LuaValue call(LuaValue arg) {
		LuaTable t = (LuaTable) env.get("string");
		bind(t, BotLib1.class, new String[] {"munge", "flip", "odd"} );
		return t;
	}
	
	static final class BotLib1 extends OneArgFunction {
		public LuaValue call(LuaValue arg) {
			switch ( opcode ) { 
			case 0: return valueOf(Utils.mungeNick(arg.checkjstring()));
			case 1: return valueOf(Utils.flip(arg.checkjstring()));
			case 2: return valueOf(Utils.odd(arg.checkjstring()));
			}
			return NIL;
		}
	}
}