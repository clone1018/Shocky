package org.luaj.vm2;

public class LuaImmutableTable extends LuaTable {
	
	private LuaImmutableTable(LuaTable table) {
		this.array = table.array;
		this.hashEntries = table.hashEntries;
		this.hashKeys = table.hashKeys;
		this.hashValues = table.hashValues;
		this.m_metatable = table.m_metatable;
	}
	
	public static LuaValue immutableOf(LuaValue value) {
		if (value == null)
			return null;
		if (value.istable())
			return new LuaImmutableTable(value.checktable());
		return NIL;
	}
	
	private void error() {
		error("attempted modification of immutable");
	}

	@Override
	public void presize(int narray) {
		error();
	}

	@Override
	public void presize(int narray, int nhash) {
		error();
	}

	@Override
	public LuaValue setmetatable(LuaValue metatable) {
		error();
		return this;
	}

	@Override
	public void set(int key, LuaValue value) {
		error();
	}

	@Override
	public void set(LuaValue key, LuaValue value) {
		error();
	}

	@Override
	public void rawset(int key, LuaValue value) {
		error();
	}

	@Override
	public void rawset(LuaValue key, LuaValue value) {
		error();
	}

	@Override
	public LuaValue remove(int pos) {
		error();
		return NONE;
	}

	@Override
	public void insert(int pos, LuaValue value) {
		error();
	}

	@Override
	public void hashset(LuaValue key, LuaValue value) {
		error();
	}

	@Override
	protected void hashClearSlot(int i) {
		error();
	}

	@Override
	public void sort(LuaValue comparator) {
		error();
	}
}
