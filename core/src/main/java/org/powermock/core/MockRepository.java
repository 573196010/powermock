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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.easymock.internal.MockInvocationHandler;
import org.powermock.core.invocationcontrol.method.MethodInvocationControl;
import org.powermock.core.invocationcontrol.method.impl.MethodInvocationControlImpl;
import org.powermock.core.invocationcontrol.newinstance.NewInvocationControl;

/**
 * Hold mock objects that should be used instead of the concrete implementation.
 * Mock transformers may use this class to gather information on which classes
 * and methods that are mocked.
 * 
 * @author Johan Haleby
 */
public class MockRepository {

	private static Map<Class<?>, NewInvocationControl<?>> newSubstitutions = new HashMap<Class<?>, NewInvocationControl<?>>();

	/**
	 * Holds info about general method invocation mocks for classes.
	 */
	private static Map<Class<?>, MethodInvocationControl> classMocks = new HashMap<Class<?>, MethodInvocationControl>();

	/**
	 * Holds info about general method invocation mocks for instances.
	 */
	private static Map<Object, MethodInvocationControl> instanceMocks = new HashMap<Object, MethodInvocationControl>();

	/**
	 * Hash map that maps between a fully qualified class name and the mock
	 * object for that class.
	 */
	private static final Map<String, Object> mocks = new HashMap<String, Object>();

	public synchronized static Object putMock(String fullyQualifiedClassName,
			Object mock) {
		return mocks.put(fullyQualifiedClassName, mock);
	}

	public synchronized static Object getMock(String fullyQualifiedClassName) {
		return mocks.get(fullyQualifiedClassName);
	}

	public synchronized static void clear() {
		mocks.clear();
		classMocks.clear();
		instanceMocks.clear();
		newSubstitutions.clear();
	}

	public static String getMockRepositoryClassKey(Class<?> type) {
		final String typeName = type.getName();
		final String mockRepositoryKey = type.isMemberClass() ? MockGateway.INNER_CLASS_PREFIX
				+ typeName
				: typeName;
		return mockRepositoryKey;
	}

	public static synchronized MethodInvocationControl getClassMethodInvocationControl(
			Class<?> type) {
		return classMocks.get(type);
	}

	public static synchronized MethodInvocationControl putClassMethodInvocationControl(
			Class<?> type, MethodInvocationControl invocationControl) {
		return classMocks.put(type, invocationControl);
	}

	public static synchronized MethodInvocationControl putClassMethodInvocationControl(
			Class<?> type, MockInvocationHandler handler, Method... methods) {
		return putClassMethodInvocationControl(type,
				new MethodInvocationControlImpl(handler, toSet(methods)));
	}

	public static synchronized MethodInvocationControl removeClassMethodInvocationControl(
			Class<?> type) {
		return classMocks.remove(type);
	}

	public static synchronized MethodInvocationControl getInstanceMethodInvocationControl(
			Object instance) {
		return instanceMocks.get(instance);
	}

	public static synchronized MethodInvocationControl putInstanceMethodInvocationControl(
			Object instance, MethodInvocationControl invocationControl) {
		return instanceMocks.put(instance, invocationControl);
	}

	public static synchronized MethodInvocationControl putInstanceMethodInvocationControl(
			Object instance, MockInvocationHandler handler, Method... methods) {
		return putInstanceMethodInvocationControl(instance,
				new MethodInvocationControlImpl(handler, toSet(methods)));
	}

	public static synchronized MethodInvocationControl removeInstanceMethodInvocationControl(
			Class<?> type) {
		return classMocks.remove(type);
	}

	private static Set<Method> toSet(Method... methods) {
		Set<Method> set = new HashSet<Method>();
		if (methods.length > 0) {
			set.addAll(Arrays.asList(methods));
		}
		return set;
	}

	public static synchronized NewInvocationControl<?> getNewInstanceSubstitute(
			Class<?> type) {
		return newSubstitutions.get(type);
	}

	public static synchronized NewInvocationControl<?> putNewInstanceSubstitute(
			Class<?> type, NewInvocationControl<?> fakeNewMock) {
		return newSubstitutions.put(type, fakeNewMock);
	}
}
