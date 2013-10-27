package pl.shockah;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@SuppressWarnings("unchecked") public class Reflection {
	public static Field getPrivateField(Class<?> cls, String fieldName) {
		try {
			Field field = cls.getDeclaredField(fieldName);
			field.setAccessible(true);
			return field;
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	public static void setField(Field field, Object instance, Object value) {
		try {
			field.set(instance,value);
		} catch (Exception e) {e.printStackTrace();}
	}
	
	public static <T> void setPrivateValue(Class<? super T> cls, String fieldName, T instance, Object value) {
		try {
			getPrivateField(cls,fieldName).set(instance,value);
		} catch (Exception e) {e.printStackTrace();}
	}
	public static <T> T getPrivateValue(Class<?> cls, String fieldName, Object instance) {
		try {
			return (T)getPrivateField(cls,fieldName).get(instance);
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	public static Method getPrivateMethod(Class<?> cls, String methodName, Class<?>... argumentTypes) {
		try {
			Method method = cls.getDeclaredMethod(methodName,argumentTypes);
			method.setAccessible(true);
			return method;
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	public static <T> T invokeMethod(Method method, Object instance, Object... arguments) {
		try {
			return (T)method.invoke(instance,arguments);
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	
	public static <T> Constructor<T> getConstructor(Class<T> cls, Class<?>... argumentTypes) {
		try {
			Constructor<T> constructor = cls.getConstructor(argumentTypes);
			constructor.setAccessible(true);
			return constructor;
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	public static <T> T newInstance(Constructor<T> ctr, Object... argumentTypes) {
		try {
			return ctr.newInstance(argumentTypes);
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
	public static <T> T newInstance(Class<T> cls) {
		try {
			return cls.newInstance();
		} catch (Exception e) {e.printStackTrace();}
		return null;
	}
}