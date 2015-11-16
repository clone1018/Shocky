package pl.shockah.shocky.js;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;

import jdk.nashorn.internal.lookup.Lookup;
import jdk.nashorn.internal.objects.Global;
import jdk.nashorn.internal.runtime.AccessorProperty;
import jdk.nashorn.internal.runtime.Property;
import jdk.nashorn.internal.runtime.PropertyMap;
import jdk.nashorn.internal.runtime.ScriptFunction;
import jdk.nashorn.internal.runtime.ScriptFunctionData;
import jdk.nashorn.internal.runtime.ScriptObject;
import jdk.nashorn.internal.runtime.ScriptRuntime;
import jdk.nashorn.internal.runtime.Specialization;

public class ShockyScriptFunction extends ScriptFunction {
	
	private Object prototype;
	
	private static final PropertyMap map;
	static {
	    ArrayList<Property> properties = new ArrayList<Property>(3);
	    properties.add(AccessorProperty.create("prototype", 6, G$PROTOTYPE, S$PROTOTYPE));
	    properties.add(AccessorProperty.create("length", 7, G$LENGTH, null));
	    properties.add(AccessorProperty.create("name", 7, G$NAME, null));
	    map = PropertyMap.newMap(properties);
	}
	
	public ShockyScriptFunction(String name, MethodHandle methodHandle, Global global, Specialization[] specs, int flags) {
		super(name, methodHandle, map, global, specs, flags);
		this.setProto(ScriptFunction.getPrototype((ScriptFunction)global.function));
	    this.setPrototype(ScriptRuntime.UNDEFINED);
	}

	@Override
	protected ScriptObject getObjectPrototype() {
		return Global.objectPrototype();
	}

	@Override
	public Object getPrototype() {
	    return this.prototype;
	}

	@Override
	protected ScriptFunction makeBoundFunction(ScriptFunctionData arg0) {
		return null;
	}

	@Override
	public ScriptFunction makeSynchronizedFunction(Object arg0) {
		return null;
	}

	@Override
	public void setPrototype(Object arg0) {
		this.prototype = arg0;
	}

	public static ShockyScriptFunction makeFunction(Global global, int flags, Class<?> c, String name, Class<?> r, Class<?>... a) {
		MethodHandle mh = Lookup.MH.findStatic(MethodHandles.lookup(), c, name, Lookup.MH.type(r, a));
		return (mh == null) ? null : new ShockyScriptFunction(name, mh, global, null, flags);
	}
	
	public ShockyProperty makeProperty(int flags, boolean writable) {
		return new ShockyProperty(this.getName(), flags, writable, this);
	}
	
	public static class ShockyProperty {
		private static final MethodHandle GET = Lookup.MH.findVirtual(MethodHandles.lookup(), ShockyProperty.class, "get", Lookup.MH.type(Object.class, Object.class));
		private static final MethodHandle SET = Lookup.MH.findVirtual(MethodHandles.lookup(), ShockyProperty.class, "set", Lookup.MH.type(Void.TYPE, Object.class, Object.class));
		
		private final String name;
		private final int flags;
		private final boolean writable;
		private Object reference;
		
		public ShockyProperty(String name, int flags, boolean writable, Object reference) {
			this.name = name;
			this.flags = flags;
			this.writable = writable;
			this.reference = reference;
		}
		
		public Property getProperty() {
			return AccessorProperty.create(name, flags, GET.bindTo(this), this.writable ? SET.bindTo(this) : null);
		}
		
		public Object get(Object self) {
			return this.reference == null ? ScriptRuntime.UNDEFINED : this.reference;
		}
		
		public void set(Object self, Object reference) {
			this.reference = reference;
		}
	}
}
