package pl.shockah;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Delegate<I,R> {
	private final Method method;
	private Delegate(Method method) {
		this.method = method;
	}
	
	public static <I,R> Delegate<I,R> create(Class<I> clazz, String name, Class<?>... parameterTypes) {
		try {
			return new Delegate<I,R>(clazz.getDeclaredMethod(name, parameterTypes));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	@SuppressWarnings("unchecked")
	public R invokeStatic() {
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
	public R invoke(I instance) {
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
	public R invokeStaticArgs(Object... args) {
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
	
	@SuppressWarnings("unchecked")
	public R invokeArgs(I instance, Object... args) {
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
