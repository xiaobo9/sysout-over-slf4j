package org.slf4j.sysoutslf4j.common;

import java.lang.reflect.Method;

public final class ReflectionUtils {
	
	public static Object invokeMethod(final String methodName, final Object target) {
		final Method method = getMethod(methodName, target.getClass());
		return invokeMethod(method, target);
	}
	
	public static Object invokeMethod(final String methodName, final Object target, final Class<?> argType, final Object arg) {
		final Method method = getMethod(methodName, target.getClass(), argType);
		if (!method.isAccessible()) {
			method.setAccessible(true);
		}
		return invokeMethod(method, target, arg);
	}
	
	public static Object invokeStaticMethod(final String methodName, final Class<?> targetClass) {
		final Method method = getMethod(methodName, targetClass);
		return invokeMethod(method, targetClass);
	}

	public static Object invokeStaticMethod(
			final String methodName, final Class<?> targetClass, final Class<?> argType, final Object arg) {
		final Method method = getMethod(methodName, targetClass, argType);
		return invokeMethod(method, targetClass, arg);
	}

	private static Method getMethod(final String methodName, final Class<?> classWithMethod, final Class<?>... argTypes) {
		try {
			return classWithMethod.getDeclaredMethod(methodName, argTypes);
		} catch (NoSuchMethodException noSuchMethodException) {
			throw new WrappedCheckedException(noSuchMethodException);
		}
	}

	public static Object invokeMethod(final Method method, final Object target, final Object... args) {
		try {
			return method.invoke(target, args);
		} catch (Exception exception) {
			throw ExceptionUtils.asRuntimeException(exception);
		}
	}
	
	private ReflectionUtils() {
		throw new UnsupportedOperationException("Not instantiable");
	}
}