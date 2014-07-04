package org.luaj.vm2.lib;

import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

public class BitLib extends OneArgFunction {

	public static LuaTable instance;
	
	public BitLib() {
	}
	
	@Override
	public LuaValue call(LuaValue arg) {
		LuaTable t = new LuaTable();
		bind(t, BitLib1.class, new String[] {
			"bnot", "bswap"});
		bind(t, BitLib2.class, new String[] {
			"lshift", "rshift", "arshift", "rol", "ror", "tohex"});
		bind(t, BitLibV.class, new String[] {
			"bor", "band", "bxor"} );
		env.set("bit", t);
		instance = t;
		PackageLib.instance.LOADED.set("bit", t);
		return t;
	}
	
	static final class BitLib1 extends OneArgFunction {
		@Override
		public LuaValue call(LuaValue arg) {
			switch ( opcode ) { 
			case 0: return bnot(arg);
			case 1: return bswap(arg);
			}
			return NIL;
		}
		
		public LuaValue bnot(LuaValue arg) {
			return valueOf(~arg.checkint());
		}
		
		public LuaValue bswap(LuaValue arg) {
			int b = arg.checkint();
			return valueOf((b >> 24) | ((b >> 8) & 0xff00) | ((b & 0xff00) << 8) | (b << 24));
		}
	}
	
	static final class BitLib2 extends TwoArgFunction {
		@Override
		public LuaValue call(LuaValue arg1, LuaValue arg2) {
			switch ( opcode ) { 
			case 0: return lshift(arg1,arg2);
			case 1: return rshift(arg1,arg2);
			case 2: return arshift(arg1,arg2);
			case 3: return rol(arg1,arg2);
			case 4: return ror(arg1,arg2);
			case 5: return tohex(arg1,arg2);
			}
			return NIL;
		}
	
		public LuaValue lshift(LuaValue num1, LuaValue num2) {
			return valueOf(num1.checkint() << num2.checkint());
		}
		
		public LuaValue rshift(LuaValue num1, LuaValue num2) {
			return valueOf(num1.checkint() >>> num2.checkint());
		}
		
		public LuaValue arshift(LuaValue num1, LuaValue num2) {
			return valueOf(num1.checkint() >> num2.checkint());
		}
		
		public LuaValue rol(LuaValue num1, LuaValue num2) {
			int b = num1.checkint();
			int n = num2.checkint();
			return valueOf((b << n) | (b >> (32-n)));
		}
		
		public LuaValue ror(LuaValue num1, LuaValue num2) {
			int b = num1.checkint();
			int n = num2.checkint();
			return valueOf((b << (32-n)) | (b >> n));
		}
		
		public LuaValue tohex(LuaValue num1, LuaValue num2) {
			int b = num1.checkint();
			int n = num2.optint(8);
			boolean lower = true;
			if (n < 0) {
				n = -n;
				lower = false;
			}
			n = Math.max(1, Math.min(8, n));
			String s = String.format(lower ? "%08x" : "%08X", b);
			if (n < 8)
				s = s.substring(8-n);
			return valueOf(s);
		}
	}
	
	static final class BitLibV extends VarArgFunction {
		public Varargs invoke(Varargs args) {
			switch ( opcode ) { 
			case 0: return bor(args);
			case 1: return band(args);
			case 2: return bxor(args);
			}
			return NIL;
		}
		
		public Varargs bor(Varargs args) {
			int n = args.checkint(1);
			for (int i = 2; i <= args.narg(); ++i) {
				n |= args.checkint(i);
			}
			return valueOf(n);
		}
		
		public Varargs band(Varargs args) {
			int n = args.checkint(1);
			for (int i = 2; i <= args.narg(); ++i) {
				n &= args.checkint(i);
			}
			return valueOf(n);
		}
		
		public Varargs bxor(Varargs args) {
			int n = args.checkint(1);
			for (int i = 2; i <= args.narg(); ++i) {
				n ^= args.checkint(i);
			}
			return valueOf(n);
		}
	}
}
