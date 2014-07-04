package pl.shockah;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class Delegate<I,R> {
	private final Method method;
	private final boolean isStatic;
	private Delegate(Method method) {
		this.method = method;
		this.isStatic = (method.getModifiers() & Modifier.STATIC) != 0;
	}
	
	public static <I,R> Delegate<I,R> create(Class<I> clazz, String name, Class<?>... parameterTypes) {
		try {
			return new Delegate<I,R>(clazz.getDeclaredMethod(name, parameterTypes));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public R invoke() throws IllegalArgumentException {
		if (!isStatic)
			throw new RuntimeException("Method is not static.");
		try {
			return (R) method.invoke(null);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@SuppressWarnings("unchecked")
	public R invoke(Object... args) throws IllegalArgumentException {
		if (!isStatic)
			throw new RuntimeException("Method is not static.");
		try {
			return (R) method.invoke(null, args);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public Instance instance(I instance) {
		if (isStatic)
			throw new RuntimeException("Method is static.");
		return new Instance(instance);
	}
	
	public class Instance {
		private final I instance;
		
		private Instance(I instance) {
			this.instance = instance;
		}
		
		@SuppressWarnings("unchecked")
		public R invoke() throws IllegalArgumentException {
			try {
				return (R) method.invoke(instance);
			} catch (IllegalArgumentException e) {
				throw e;
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			return null;
		}
		
		@SuppressWarnings("unchecked")
		public R invoke(Object... args) throws IllegalArgumentException {
			try {
				return (R) method.invoke(instance, args);
			} catch (IllegalArgumentException e) {
				throw e;
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			}
			return null;
		}
	}
}
