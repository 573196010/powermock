/*
 * Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.powermock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.sf.cglib.proxy.Enhancer;

import org.easymock.classextension.internal.objenesis.Objenesis;
import org.easymock.classextension.internal.objenesis.ObjenesisStd;
import org.easymock.classextension.internal.objenesis.instantiator.ObjectInstantiator;
import org.powermock.core.PrimitiveWrapper;

/**
 * Various utilities for accessing internals of a class. Basically a simplified
 * reflection utility intended for tests.
 */
public class Whitebox {

	/**
	 * Convenience method to get a method from a class type without having to
	 * catch the checked exceptions otherwise required. These exceptions are
	 * wrapped as runtime exceptions.
	 * <p>
	 * The method will first try to look for a declared method in the same
	 * class. If the method is not declared in this class this method will look
	 * for it in the super classes (although this time only non-private methods
	 * are looked for).
	 * 
	 * @param type
	 *            The type of the class where the method is located.
	 * @param methodName
	 *            The method names.
	 * @param parameterTypes
	 *            All parameter types of the method (may be <code>null</code>).
	 * @return A <code>java.lang.reflect.Method</code>.
	 */
	public static Method getMethod(Class<?> type, String methodName, Class<?>... parameterTypes) {
		final Class<?> unmockedType = getUnmockedType(type);
		try {
			return unmockedType.getDeclaredMethod(methodName, parameterTypes);
		} catch (NoSuchMethodException e) {
			try {
				return unmockedType.getMethod(methodName, parameterTypes);
			} catch (Exception e1) {
				throw new IllegalArgumentException("Failed to lookup method.", e1);
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to lookup method.", e);
		}
	}

	/**
	 * Create a new instance of a class without invoking its constructor.
	 * <p>
	 * No byte-code manipulation is needed to perform this operation and thus
	 * it's not necessary use the <code>PowerMockRunner</code> or
	 * <code>PrepareForTest</code> annotation to use this functionality.
	 * 
	 * @param <T>
	 *            The type of the instance to create.
	 * @param classToInstantiate
	 *            The type of the instance to create.
	 * @return A new instance of type T, created without invoking the
	 *         constructor.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T newInstance(Class<T> classToInstantiate) {
		Objenesis objenesis = new ObjenesisStd();
		ObjectInstantiator thingyInstantiator = objenesis.getInstantiatorOf(classToInstantiate);
		return (T) thingyInstantiator.newInstance();
	}

	/**
	 * Convenience method to get a (declared) constructor from a class type
	 * without having to catch the checked exceptions otherwise required. These
	 * exceptions are wrapped as runtime exceptions.
	 * 
	 * @param type
	 *            The type of the class where the constructor is located.
	 * @param parameterTypes
	 *            All parameter types of the constructor (may be
	 *            <code>null</code>).
	 * @return A <code>java.lang.reflect.Constructor</code>.
	 */
	public static Constructor<?> getConstructor(Class<?> type, Class<?>... parameterTypes) {

		try {
			return getUnmockedType(type).getDeclaredConstructor(parameterTypes);
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to lookup constructor.", e);
		}
	}

	/**
	 * Set the value of a field using reflection.
	 * 
	 * @param object
	 *            the object to modify
	 * @param fieldName
	 *            the name of the field
	 * @param value
	 *            the new value of the field
	 */
	public static void setInternalState(Object object, String fieldName, Object value) {

		if (object == null) {
			throw new IllegalArgumentException("The object parameter cannot be null in method invocation Whitebox.setInternalState(..).");
		}

		setInternalState(object, fieldName, value, object.getClass());
	}

	/**
	 * Set the value of a field using reflection. Use this method when you need
	 * to specify in which class the field is declared. This might be useful
	 * when you have mocked the instance you are trying to modify.
	 * 
	 * @param object
	 *            the object to modify
	 * @param fieldName
	 *            the name of the field
	 * @param value
	 *            the new value of the field
	 * @param where
	 *            which class the field is defined
	 */
	public static void setInternalState(Object object, String fieldName, Object value, Class<?> where) {

		Class<?> tempClass = findField(object, fieldName, where);

		Field field = null;
		try {
			field = tempClass.getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(object, value);
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Field '" + fieldName + "' was not found in class " + object.getClass());
		} catch (Exception e) {
			throw new RuntimeException("Internal Error: Failed to set field in method setInternalState.", e);
		}
	}

	private static Class<?> findField(Object object, String fieldName, Class<?> where) {
		if (object == null || fieldName == null || fieldName.equals("") || fieldName.startsWith(" ")) {
			throw new IllegalArgumentException("object, field name, and \"where\" must not be empty or null.");
		}

		Class<?> tempClass;
		if (object instanceof Class) {
			tempClass = (Class<?>) object;
		} else {
			tempClass = object.getClass();
			while (!tempClass.equals(where)) {
				tempClass = tempClass.getSuperclass();
				if (tempClass.equals(Object.class)) {
					throw new IllegalArgumentException("The field " + fieldName + " was not found in the class heirachy for " + object.getClass());
				}
			}
		}
		return tempClass;
	}

	/**
	 * Get the value of a field using reflection.
	 * 
	 * @param object
	 *            the object to modify
	 * @param fieldName
	 *            the name of the field
	 */
	public static Object getInternalState(Object object, String fieldName) {
		if (object == null) {
			throw new IllegalArgumentException("The object parameter cannot be null in method invocation Whitebox.getInternalState(..).");
		}
		return getInternalState(object, fieldName, object.getClass());
	}

	/**
	 * Get the value of a field using reflection. Use this method when you need
	 * to specify in which class the field is declared. This might be useful
	 * when you have mocked the instance you are trying to access.
	 * 
	 * @param object
	 *            the object to modify
	 * @param fieldName
	 *            the name of the field
	 * @param where
	 *            which class the field is defined
	 */
	public static Object getInternalState(Object object, String fieldName, Class<?> where) {
		return getInternalState(object, fieldName, where, Object.class);
	}

	/**
	 * Get the value of a field using reflection. Use this method when you need
	 * to specify in which class the field is declared. This might be useful
	 * when you have mocked the instance you are trying to access. Use this
	 * method to avoid casting.
	 * 
	 * @param <T>
	 *            the expected type of the field
	 * @param object
	 *            the object to modify
	 * @param fieldName
	 *            the name of the field
	 * @param where
	 *            which class the field is defined
	 * @param type
	 *            the expected type of the field
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getInternalState(Object object, String fieldName, Class<?> where, T type) {

		if (type == null) {
			throw new IllegalArgumentException("type cannot be null.");
		}

		Class<?> tempClass = findField(object, fieldName, where);

		Field field = null;
		try {
			field = tempClass.getDeclaredField(fieldName);
			field.setAccessible(true);
			// ParameterizedType t = new
			// TODO Ta reda p� typen och kolla s� att returnv�rdet �r av
			// samma typ. Annars kasta exception.
			final Object fieldValue = field.get(object);
			// if (!fieldValue.getClass().isAssignableFrom(
			// ((ParameterizedType) type.getClass()).getOwnerType()
			// .getClass())) {
			// StringBuilder sb = new StringBuilder();
			// sb.append("The type of the field with name ").append(fieldName);
			// sb.append(" (").append(fieldValue.getClass()).append(") ");
			// sb.append("is not super class of ").append(type);
			// throw new RuntimeException(sb.toString());
			// }
			return (T) fieldValue;
		} catch (NoSuchFieldException e) {
			throw new RuntimeException("Field '" + fieldName + "' was not found in class " + object.getClass());
		} catch (Exception e) {
			throw new RuntimeException("Internal error: Failed to get field in method getInternalState.", e);
		}
	}

	/**
	 * Invoke a private or inner class method. This might be useful to test
	 * private methods.
	 * 
	 * @throws Throwable
	 */
	public static synchronized Object invokeMethod(Object tested, String methodToExecute, Object... arguments) throws Exception {
		return doInvokeMethod(tested, null, methodToExecute, arguments);
	}

	/**
	 * Invoke a private or inner class method in cases where power mock cannot
	 * automatically determine the type of the parameters, for example when
	 * mixing primitive types and wrapper types in the same method. For most
	 * situations use {@link #invokeMethod(Class, String, Object...)} instead.
	 * 
	 * @throws Exception
	 *             Exception that may occur when invoking this method.
	 */
	public static synchronized Object invokeMethod(Object tested, String methodToExecute, Class<?>[] argumentTypes, Object... arguments)
			throws Exception {
		final Class<?> unmockedType = getUnmockedType(tested.getClass());
		Method method = getMethod(unmockedType, methodToExecute, argumentTypes);
		if (method == null) {
			throwExceptionIfMethodWasNotFound(unmockedType, methodToExecute, null, arguments);
		}
		return performMethodInvocation(tested, method, arguments);
	}

	/**
	 * Invoke a private or inner class method in a subclass (defined by
	 * <code>definedIn</code>) in cases where power mock cannot automatically
	 * determine the type of the parameters, for example when mixing primitive
	 * types and wrapper types in the same method. For most situations use
	 * {@link #invokeMethod(Class, String, Object...)} instead.
	 * 
	 * @throws Exception
	 *             Exception that may occur when invoking this method.
	 */
	public static synchronized Object invokeMethod(Object tested, String methodToExecute, Class<?> definedIn, Class<?>[] argumentTypes,
			Object... arguments) throws Exception {
		Method method = getMethod(definedIn, methodToExecute, argumentTypes);
		if (method == null) {
			throwExceptionIfMethodWasNotFound(definedIn, methodToExecute, null, arguments);
		}
		return performMethodInvocation(tested, method, arguments);
	}

	/**
	 * Invoke a private or inner class method in that is located in a subclass
	 * of tested. This might be useful to test private methods.
	 * 
	 * @throws Exception
	 *             Exception that may occur when invoking this method.
	 */
	public static synchronized Object invokeMethod(Object tested, Class<?> declaringClass, String methodToExecute, Object... arguments)
			throws Exception {
		return doInvokeMethod(tested, declaringClass, methodToExecute, arguments);
	}

	/**
	 * Invoke a private or inner class method. This might be useful to test
	 * private methods.
	 * 
	 * @throws Throwable
	 */
	public static synchronized Object invokeMethod(Class<?> clazz, String methodToExecute, Object... arguments) throws Exception {
		return doInvokeMethod(clazz, null, methodToExecute, arguments);
	}

	private static Object doInvokeMethod(Object tested, Class<?> declaringClass, String methodToExecute, Object... arguments) throws Exception {
		Method methodToInvoke = findMethodOrThrowException(tested, declaringClass, methodToExecute, arguments);

		// Invoke test
		return performMethodInvocation(tested, methodToInvoke, arguments);
	}

	/**
	 * Finds and returns a certain method. If the method couldn't be found this
	 * method delegates to
	 * {@link Whitebox#throwExceptionIfMethodWasNotFound(Object, String, Method, Object...)}.
	 * 
	 * @param tested
	 * @param declaringClass
	 *            The class where the method is supposed to be declared (may be
	 *            <code>null</code>).
	 * @param methodToExecute
	 * @param arguments
	 * @return
	 */
	static Method findMethodOrThrowException(Object tested, Class<?> declaringClass, String methodToExecute, Object... arguments) {

		if (tested == null) {
			throw new IllegalArgumentException("The to perform the operation on cannot be null.");
		}

		/*
		 * Get methods from the type if it's not mocked or from the super type
		 * if the tested object is mocked.
		 */
		Class<?> testedType = null;
		if (tested instanceof Class<?>) {
			testedType = (Class<?>) tested;
		} else {
			testedType = tested.getClass();
		}

		Method[] methods = null;
		if (declaringClass == null) {
			methods = Enhancer.isEnhanced(testedType) ? testedType.getSuperclass().getDeclaredMethods() : testedType.getDeclaredMethods();
		} else {
			methods = declaringClass.getDeclaredMethods();
		}
		Method potentialMethodToInvoke = null;
		for (Method method : methods) {
			if (method.getName().equals(methodToExecute)) {
				Class<?>[] paramTypes = method.getParameterTypes();
				if ((arguments != null && (paramTypes.length == arguments.length))) {
					if (paramTypes.length == 0) {
						potentialMethodToInvoke = method;
						break;
					}
					boolean wrappedMethodFound = true;
					boolean primitiveMethodFound = true;
					for (int i = 0; i < paramTypes.length; i++) {
						if (!paramTypes[i].equals(arguments[i].getClass())) {
							wrappedMethodFound = false;
							break;
						}
					}

					primitiveMethodFound = findPrimitiveMethodOrConstructor(paramTypes, arguments);

					if (wrappedMethodFound || primitiveMethodFound) {
						if (potentialMethodToInvoke == null) {
							potentialMethodToInvoke = method;
						} else {
							/*
							 * We've already found a method match before, this
							 * means that PowerMock cannot determine which
							 * method to expect since there are two methods with
							 * the same name and the same number of arguments
							 * but one is using wrapper types.
							 */
							throwExceptionWhenMultipleMethodMatchesFound(new Method[] { potentialMethodToInvoke, method });
						}
					}
				} else if (arguments != null && (paramTypes.length != arguments.length)) {
					continue;
				}
			}
		}

		Whitebox.throwExceptionIfMethodWasNotFound(tested.getClass(), methodToExecute, potentialMethodToInvoke, arguments);
		return potentialMethodToInvoke;
	}

	private static boolean findPrimitiveMethodOrConstructor(Class<?>[] paramTypes, Object... arguments) {
		boolean primitiveMethodFound = true;
		for (int i = 0; i < paramTypes.length; i++) {
			Class<?> primitiveWrapperType = PrimitiveWrapper.getPrimitiveFromWrapperType(arguments[i].getClass());
			if (primitiveWrapperType == null) {
				continue;
			} else if (!paramTypes[i].equals(primitiveWrapperType)) {
				primitiveMethodFound = false;
				break;
			}
		}
		return primitiveMethodFound;
	}

	/**
	 * Finds and returns a certain constructor. If the constructor couldn't be
	 * found this method delegates to
	 * {@link Whitebox#throwExceptionIfConstructorWasNotFound(Class, Object...).
	 * 
	 * @param type
	 * @param arguments
	 * @return
	 */
	static Constructor<?> findConstructorOrThrowException(Class<?> type, Object... arguments) {
		if (type == null) {
			throw new IllegalArgumentException("Class type cannot be null.");
		}

		Class<?> unmockedType = getUnmockedType(type);

		Constructor<?>[] constructors = unmockedType.getDeclaredConstructors();
		Constructor<?> potentialConstructor = null;
		for (Constructor<?> constructor : constructors) {
			Class<?>[] paramTypes = constructor.getParameterTypes();
			if ((arguments != null && (paramTypes.length == arguments.length))) {
				if (paramTypes.length == 0) {
					potentialConstructor = constructor;
					break;
				}
				boolean wrappedConstructorFound = true;
				boolean primitiveConstructorFound = true;
				for (int i = 0; i < paramTypes.length; i++) {
					if (!paramTypes[i].equals(arguments[i].getClass())) {
						wrappedConstructorFound = false;
						break;
					}
				}

				primitiveConstructorFound = findPrimitiveMethodOrConstructor(paramTypes, arguments);

				if (wrappedConstructorFound || primitiveConstructorFound) {
					if (potentialConstructor == null) {
						potentialConstructor = constructor;
					} else {
						/*
						 * We've already found a constructor match before, this
						 * means that PowerMock cannot determine which method to
						 * expect since there are two methods with the same name
						 * and the same number of arguments but one is using
						 * wrapper types.
						 */
						throwExceptionWhenMultipleConstructorMatchesFound(new Constructor<?>[] { potentialConstructor, constructor });
					}
				}
			} else if (arguments != null && (paramTypes.length != arguments.length)) {
				continue;
			}
		}

		Whitebox.throwExceptionIfConstructorWasNotFound(type, potentialConstructor, arguments);
		return potentialConstructor;
	}

	static void throwExceptionIfMethodWasNotFound(Class<?> type, String methodName, Method methodToMock, Object... arguments) {
		if (methodToMock == null) {
			throw new IllegalArgumentException("No method found with name '" + methodName + "' with argument types: [ "
					+ getArgumentsAsString(arguments) + "] in class " + getUnmockedType(type).getName());
		}
	}

	static void throwExceptionIfConstructorWasNotFound(Class<?> type, Constructor<?> potentialConstructor, Object... arguments) {
		if (potentialConstructor == null) {
			throw new IllegalArgumentException("No constructor found in class '" + getUnmockedType(type).getName() + "' with argument types: [ "
					+ getArgumentsAsString(arguments) + " ]");
		}
	}

	private static String getArgumentsAsString(Object... arguments) {
		StringBuilder argumentsAsString = new StringBuilder();
		if (arguments != null && arguments.length != 0) {
			for (int i = 0; i < arguments.length; i++) {
				String argumentName = null;
				Object argument = arguments[i];
				if (argument instanceof Class) {
					argumentName = ((Class<?>) argument).getName();
				} else {
					argumentName = getUnmockedType(argument.getClass()).getName();
				}

				argumentsAsString.append(argumentName);
				if (i != arguments.length - 1) {
					argumentsAsString.append(", ");
				}
			}
		}
		return argumentsAsString.toString();
	}

	/**
	 * Invoke a constructor. Useful for testing classes with a private
	 * constructor when PowerMock cannot determine which constructor to invoke.
	 * This only happens if you have two constructors with the same number of
	 * arguments where one is using primitive data types and the other is using
	 * the wrapped counter part. For example:
	 * 
	 * <pre>
	 * public class MyClass {
	 *     private MyClass(Integer i) {
	 *         ...
	 *     } 
	 * 
	 *     private MyClass(int i) {
	 *         ...
	 *     }
	 * </pre>
	 * 
	 * This ought to be a really rare case. So for most situation, use
	 * {@link #invokeConstructor(Class, Object...)} instead.
	 * 
	 * 
	 * @return The object created after the constructor has been invoked.
	 */
	public static <T> T invokeConstructor(Class<T> classThatContainsTheConstructorToTest, Class<?>[] parameterTypes, Object[] arguments) {
		if (parameterTypes != null || arguments != null) {
			if (parameterTypes.length != arguments.length) {
				throw new IllegalArgumentException("parameterTypes and arguments must have the same length");
			}
		}

		Constructor<T> constructor = null;
		try {
			constructor = classThatContainsTheConstructorToTest.getDeclaredConstructor(parameterTypes);
		} catch (Exception e) {
			throw new RuntimeException("Could not lookup the constructor", e);
		}

		return createInstance(constructor, arguments);
	}

	/**
	 * Invoke a constructor. Useful for testing classes with a private
	 * constructor.
	 * 
	 * 
	 * @return The object created after the constructor has been invoked.
	 */
	public static <T> T invokeConstructor(Class<T> classThatContainsTheConstructorToTest, Object... arguments) {

		if (classThatContainsTheConstructorToTest == null) {
			throw new IllegalArgumentException("The class should contain the constructor cannot be null.");
		}

		Class<?>[] argumentTypes = new Class<?>[arguments.length];
		for (int i = 0; i < arguments.length; i++) {
			argumentTypes[i] = arguments[i].getClass();
		}

		Constructor<T> constructor = null;

		Constructor<T> potentialContstructorWrapped = null;
		Constructor<T> potentialContstructorPrimitive = null;

		try {
			potentialContstructorWrapped = classThatContainsTheConstructorToTest.getDeclaredConstructor(argumentTypes);
		} catch (Exception e) {
			// Do nothing, we'll try with primitive type next.
		}

		try {
			potentialContstructorPrimitive = classThatContainsTheConstructorToTest.getDeclaredConstructor(PrimitiveWrapper
					.toPrimitiveType(argumentTypes));
		} catch (Exception e) {
			// Do nothing
		}

		if (potentialContstructorPrimitive == null || potentialContstructorWrapped != null) {
			constructor = potentialContstructorWrapped;
		} else if (potentialContstructorPrimitive != null && potentialContstructorWrapped == null) {
			constructor = potentialContstructorPrimitive;
		} else if (potentialContstructorPrimitive == null && potentialContstructorWrapped == null) {
			throw new RuntimeException("Could not lookup the constructor");
		} else {
			throw new RuntimeException("Could not determine which constructor to execute. Please specify the parameter types by hand.");
		}

		return createInstance(constructor, arguments);
	}

	private static <T> T createInstance(Constructor<T> constructor, Object... arguments) {
		if (constructor == null) {
			throw new IllegalArgumentException("Constructor cannot be null");
		}
		constructor.setAccessible(true);

		T createdObject = null;
		try {
			createdObject = constructor.newInstance(arguments);
		} catch (InvocationTargetException e) {
			throw new RuntimeException("An exception was caught when executing the constructor", e);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return createdObject;
	}

	/**
	 * Get all methods in a class! Both declared an non-declared (no
	 * duplicates).
	 * 
	 * @param clazz
	 *            The class whose methods to get.
	 * @return All methods declared in this class and all non-private members
	 *         visible in its subclass.
	 */
	private static Method[] getAllMethods(Class<?> clazz) {
		final Class<?> unmockedType = getUnmockedType(clazz);
		Method[] declaredMethods = unmockedType.getDeclaredMethods();
		Method[] nonDeclaredMethods = unmockedType.getMethods();

		Set<Method> methods = new HashSet<Method>();
		// Copy all declared and non-declared methods to the methods set.
		for (Method method : declaredMethods) {
			methods.add(method);
		}
		for (Method method : nonDeclaredMethods) {
			if (!methods.contains(method)) {
				methods.add(method);
			}
		}

		return methods.toArray(new Method[0]);
	}

	/**
	 * Get the first parent constructor defined in a super class of
	 * <code>klass</code>.
	 * 
	 * @param klass
	 *            The class where the constructor is located. <code>null</code>).
	 * @return A <code>java.lang.reflect.Constructor</code>.
	 */
	public static Constructor<?> getFirstParentConstructor(Class<?> klass) {

		try {
			return getUnmockedType(klass).getSuperclass().getDeclaredConstructors()[0];
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to lookup constructor.", e);
		}
	}

	/**
	 * Finds and returns a method based on the input parameters. If no
	 * <code>parameterTypes</code> are present the method will return the
	 * first method with name <code>methodNameToMock</code>. If no method was
	 * found, <code>null</code> will be returned.
	 * 
	 * @param <T>
	 * @param type
	 * @param methodNameToMock
	 * @param parameterTypes
	 * @return
	 */
	static <T> Method findMethod(Class<T> type, String methodNameToMock, Class<?>... parameterTypes) {
		List<Method> matchingMethodsList = new LinkedList<Method>();
		outer: for (Method method : getAllMethods(type)) {
			if (method.getName().equals(methodNameToMock)) {
				if (parameterTypes != null && parameterTypes.length > 0) {
					// If argument types was supplied, make sure that they
					// match.
					Class<?>[] paramTypes = method.getParameterTypes();
					if (parameterTypes.length == paramTypes.length) {
						for (int i = 0; i < paramTypes.length; i++) {
							if (!parameterTypes[i].equals(paramTypes[i])) {
								continue outer;
							}
						}
					} else {
						continue;
					}
				}
				// Add the method to the matching methods list.
				matchingMethodsList.add(method);
			}
		}

		Method methodToMock = null;
		if (matchingMethodsList.size() > 0) {
			if (matchingMethodsList.size() == 1) {
				// We've found a unique method match.
				methodToMock = matchingMethodsList.get(0);
			} else if (parameterTypes.length == 0) {
				/*
				 * If we've found several matches and we've supplied no
				 * parameter types, go through the list of found methods and see
				 * if we have a method with no parameters. In that case return
				 * that method.
				 */
				for (Method method : matchingMethodsList) {
					if (method.getParameterTypes().length == 0) {
						methodToMock = method;
						break;
					}
				}

				if (methodToMock == null) {
					Whitebox.throwExceptionWhenMultipleMethodMatchesFound(matchingMethodsList.toArray(new Method[0]));
				}
			} else {
				// We've found several matching methods.
				Whitebox.throwExceptionWhenMultipleMethodMatchesFound(matchingMethodsList.toArray(new Method[0]));
			}
		}

		return methodToMock;
	}

	static <T> Class<?> getUnmockedType(Class<T> type) {
		if (type == null) {
			throw new IllegalArgumentException("type cannot be null");
		}

		Class<?> typeContainingMethod;
		if (Enhancer.isEnhanced(type)) {
			typeContainingMethod = type.getSuperclass();
		} else if (Proxy.isProxyClass(type)) {
			typeContainingMethod = type.getInterfaces()[0];
		} else {
			typeContainingMethod = type;
		}
		return typeContainingMethod;
	}

	static void throwExceptionWhenMultipleMethodMatchesFound(Method[] methods) {
		if (methods == null || methods.length < 2) {
			throw new IllegalArgumentException("Internal error: throwExceptionWhenMultipleMethodMatchesFound needs at least two methods.");
		}
		StringBuilder sb = new StringBuilder();
		sb
				.append("Several matching methods found, please specify the argument parameter types so that PowerMock can determine which method you're refering to.\n");
		sb.append("Matching methods in class ").append(methods[0].getDeclaringClass().getName()).append(" were:\n");

		for (Method method : methods) {
			sb.append(method.getReturnType().getName()).append(" ");
			sb.append(method.getName()).append("( ");
			final Class<?>[] parameterTypes = method.getParameterTypes();
			for (Class<?> paramType : parameterTypes) {
				sb.append(paramType.getName()).append(".class ");
			}
			sb.append(")\n");
		}
		throw new RuntimeException(sb.toString());
	}

	static void throwExceptionWhenMultipleConstructorMatchesFound(Constructor<?>[] constructors) {
		if (constructors == null || constructors.length < 2) {
			throw new IllegalArgumentException("Internal error: throwExceptionWhenMultipleMethodMatchesFound needs at least two methods.");
		}
		StringBuilder sb = new StringBuilder();
		sb
				.append("Several matching constructors found, please specify the argument parameter types so that PowerMock can determine which method you're refering to.\n");
		sb.append("Matching constructors in class ").append(constructors[0].getDeclaringClass().getName()).append(" were:\n");

		for (Constructor<?> constructor : constructors) {
			sb.append(constructor.getName()).append("( ");
			final Class<?>[] parameterTypes = constructor.getParameterTypes();
			for (Class<?> paramType : parameterTypes) {
				sb.append(paramType.getName()).append(".class ");
			}
			sb.append(")\n");
		}
		throw new RuntimeException(sb.toString());
	}

	@SuppressWarnings("all")
	static Method findMethodOrThrowException(Class<?> type, String methodName, Class<?>... parameterTypes) {
		Method methodToMock = findMethod(type, methodName, parameterTypes);
		throwExceptionIfMethodWasNotFound(type, methodName, methodToMock, parameterTypes);
		return methodToMock;
	}

	/**
	 * Get an array of {@link Method}'s that matches the supplied list of
	 * method names.
	 * 
	 * @param clazz
	 *            The class that should contain the methods.
	 * @param methodNames
	 *            An array names of the methods that will be returned.
	 * @return An array of Method's. May be of length 0 but not
	 *         <code>null</code>.
	 */
	public static Method[] getMethods(Class<?> clazz, String... methodNames) {
		final List<Method> methodsToMock = new LinkedList<Method>();

		for (Method method : getAllMethods(clazz)) {
			for (String methodName : methodNames) {
				if (method.getName().equals(methodName)) {
					methodsToMock.add(method);
				}
			}
		}

		final Method[] methodArray = methodsToMock.toArray(new Method[0]);
		return methodArray;
	}

	private static Object performMethodInvocation(Object tested, Method methodToInvoke, Object... arguments) throws Exception {
		methodToInvoke.setAccessible(true);
		try {
			return methodToInvoke.invoke(tested, arguments);
		} catch (InvocationTargetException e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception) {
				throw (Exception) cause;
			} else {
				throw new RuntimeException(cause);
			}
		} catch (Exception e) {
			throw new RuntimeException("Failed to invoke method " + methodToInvoke.getName() + " on object " + tested + ". Reason was \""
					+ e.getMessage() + "\".", e);
		}
	}

	static <T> Method[] getAllMethodExcept(Class<T> type, String... methodNames) {
		List<Method> methodsToMock = new LinkedList<Method>();
		Method[] methods = type.getDeclaredMethods();
		iterateMethods: for (Method method : methods) {
			for (String methodName : methodNames) {
				if (method.getName().equals(methodName)) {
					continue iterateMethods;
				}
			}
			methodsToMock.add(method);
		}
		return methodsToMock.toArray(new Method[0]);
	}

	static <T> Method[] getAllMetodsExcept(Class<T> type, String methodNameToExclude, Class<?>[] argumentTypes) {
		Method[] methods = type.getDeclaredMethods();
		List<Method> methodList = new ArrayList<Method>();
		outer: for (Method method : methods) {
			if (method.getName().equals(methodNameToExclude)) {
				if (argumentTypes != null && argumentTypes.length > 0) {
					final Class<?>[] args = method.getParameterTypes();
					if (args != null && args.length == argumentTypes.length) {
						for (int i = 0; i < args.length; i++) {
							if (args[i].equals(argumentTypes[i])) {
								/*
								 * Method was not found thus it should not be
								 * mocked. Continue to investigate the next
								 * method.
								 */
								continue outer;
							}
						}
					}
				} else {
					continue;
				}
			}
			methodList.add(method);
		}
		return methodList.toArray(new Method[0]);
	}

	static boolean areAllMethodsStatic(Method... methods) {
		for (Method method : methods) {
			if (!Modifier.isStatic(method.getModifiers())) {
				return false;
			}
		}
		return true;
	}
}
