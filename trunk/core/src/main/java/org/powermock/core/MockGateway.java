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
package org.powermock.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import net.sf.cglib.proxy.Enhancer;

import org.powermock.core.invocationcontrol.method.MethodInvocationControl;
import org.powermock.core.invocationcontrol.newinstance.NewInvocationControl;

/**
 * All mock invocations are routed through this gateway. This includes method
 * calls, construction of new instances and more. Do not use this class
 * directly, but always go through the PowerMock facade.
 */
public class MockGateway {

	public static final Object PROCEED = new Object();

	public static final String INNER_CLASS_PREFIX = "___";

	public static final String POWER_MOCK_PACKAGE_PREFIX = "powerMockPackagePrefix";

	public static final String POWER_MOCK_SUPERCLASS_SUFFIX = "$$AlteredByPowerMock";

	private static final Set<Constructor<?>> suppressConstructor = new HashSet<Constructor<?>>();

	private static final Set<Method> suppressMethod = new HashSet<Method>();

	// used for static methods
	public static synchronized Object methodCall(Class<?> type, String methodName, Object[] args, Class<?>[] sig, String returnTypeAsString)
			throws Throwable {
		return doMethodCall(type, methodName, args, sig, returnTypeAsString);
	}

	private static Object doMethodCall(Object object, String methodName, Object[] args, Class<?>[] sig, String returnTypeAsString) throws Throwable,
			NoSuchMethodException {
		if ((methodName.equals("hashCode") && sig.length == 0) || (methodName.equals("equals") && sig.length == 1)) {
			return PROCEED;
		}
		Object returnValue = null;

		MethodInvocationControl methodInvocationControl = null;
		Class<?> objectType = null;

		if (object instanceof Class<?>) {
			objectType = (Class<?>) object;
			methodInvocationControl = MockRepository.getClassMethodInvocationControl(objectType);
		} else {
			final Class<? extends Object> type = object.getClass();
			objectType = Enhancer.isEnhanced(type) ? type.getSuperclass() : type;
			methodInvocationControl = MockRepository.getInstanceMethodInvocationControl(object);
		}

		/*
		 * if invocationControl is null or the method is not mocked, invoke
		 * original method or suppress the method code otherwise invoke the
		 * invocation handler.
		 */
		if (methodInvocationControl != null && methodInvocationControl.isMocked(WhiteboxImpl.getMethod(objectType, methodName, sig))) {

			final InvocationHandler handler = methodInvocationControl.getInvocationHandler();
			returnValue = handler.invoke(objectType, WhiteboxImpl.getMethod(objectType, methodName, sig), args);
		} else {
			final boolean shouldSuppressMethodCode = suppressMethod.contains(WhiteboxImpl.getMethod(objectType, methodName, sig));
			if (shouldSuppressMethodCode) {
				returnValue = suppressMethodCode(returnTypeAsString);
			} else {
				returnValue = PROCEED;
			}
		}
		return returnValue;
	}

	// used for instance methods
	public static synchronized Object methodCall(Object instance, String methodName, Object[] args, Class<?>[] sig, String returnTypeAsString)
			throws Throwable {
		return doMethodCall(instance, methodName, args, sig, returnTypeAsString);
	}

	public static synchronized Object newInstanceCall(Class<?> type, Object[] args, Class<?>[] sig) throws Throwable {
		final NewInvocationControl<?> newInvocationControl = MockRepository.getNewInstanceSubstitute(type);
		if (newInvocationControl != null) {
			Constructor<?> constructor = WhiteboxImpl.findConstructorOrThrowException(type, args);
			if (constructor.isVarArgs()) {
				/*
				 * Get the first argument because this contains the actual
				 * varargs arguments.
				 */
				args = (Object[]) args[0];
			}
			try {
				final Object result = newInvocationControl.createInstance(args);
				if (result == null) {
					throw new IllegalStateException("Must replay class " + type.getName() + " to get configured expectation.");
				}
				return result;
			} catch (AssertionError e) {
				PowerMockUtils.throwAssertionErrorForNewSubstitutionFailure(e, type);
			}
		}
		// Check if we should suppress the constructor code
		if (suppressConstructor.contains(WhiteboxImpl.getConstructor(type, sig))) {
			return WhiteboxImpl.getFirstParentConstructor(type);
		}
		return PROCEED;
	}

	public static void clear() {
		suppressMethod.clear();
		suppressConstructor.clear();
	}

	public static synchronized Object staticConstructorCall(String className) {
		@SuppressWarnings("unused")
		Class<?> ikk = MockRepository.class;
		if (MockRepository.shouldSuppressStaticInitializerFor(className)) {
			return "suppress";
		}
		return PROCEED;
	}

	private static Object suppressMethodCode(String returnTypeAsString) {
		if (returnTypeAsString == null) { // Void
			return "";
		} else if (returnTypeAsString.equals(byte.class.getName())) {
			return (byte) 0;
		} else if (returnTypeAsString.equals(int.class.getName())) {
			return 0;
		} else if (returnTypeAsString.equals(short.class.getName())) {
			return (short) 0;
		} else if (returnTypeAsString.equals(long.class.getName())) {
			return 0L;
		} else if (returnTypeAsString.equals(float.class.getName())) {
			return 0.0F;
		} else if (returnTypeAsString.equals(double.class.getName())) {
			return 0.0D;
		} else if (returnTypeAsString.equals(boolean.class.getName())) {
			return false;
		} else if (returnTypeAsString.equals(char.class.getName())) {
			return ' ';
		} else {
			return null;
		}
	}

	public static synchronized Object constructorCall(Class<?> type, Object[] args, Class<?>[] sig) throws Throwable {
		final Constructor<?> constructor = WhiteboxImpl.getConstructor(type, sig);
		if (suppressConstructor.contains(constructor)) {
			return null;
		}
		return PROCEED;
	}

	public static synchronized void addMethodToSuppress(Method method) {
		suppressMethod.add(method);
	}

	public static synchronized void addConstructorToSuppress(Constructor<?> constructor) {
		suppressConstructor.add(constructor);
	}
}
