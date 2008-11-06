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
package org.powermock.modules.junit4;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
import org.powermock.modules.junit4.constructor.PrivateConstructorInstantiationDemoTest;
import org.powermock.modules.junit4.constructorargs.ConstructorArgsDemoTest;
import org.powermock.modules.junit4.expectnew.ExpectNewDemoTest;
import org.powermock.modules.junit4.expectvoid.ExpectVoidDemoTest;
import org.powermock.modules.junit4.finalmocking.FinalDemoTest;
import org.powermock.modules.junit4.newmocking.StupidNewTest;
import org.powermock.modules.junit4.noannotation.NoAnnotationUsageTest;
import org.powermock.modules.junit4.partialmocking.MockSelfDemoTest;
import org.powermock.modules.junit4.partialmocking.MockSelfDemoWithSubClassTest;
import org.powermock.modules.junit4.privateandfinal.PrivateFinalTest;
import org.powermock.modules.junit4.privatefield.MockSelfPrivateFieldServiceClassTest;
import org.powermock.modules.junit4.privatefield.SimplePrivateFieldServiceClassTest;
import org.powermock.modules.junit4.privatemocking.PrivateMethodDemoTest;
import org.powermock.modules.junit4.simplereturn.SimpleReturnExampleUserTest;
import org.powermock.modules.junit4.singleton.MockStaticTest;
import org.powermock.modules.junit4.staticandinstance.StaticAndInstanceDemoTest;
import org.powermock.modules.junit4.staticinitializer.StaticInitializerExampleTest;
import org.powermock.modules.junit4.suppressconstructor.SuppressConstructorDemoTest;
import org.powermock.modules.junit4.suppressconstructor.SuppressConstructorHierarchyDemoTest;
import org.powermock.modules.junit4.suppressmethod.SuppressMethodTest;

import samples.suppressconstructor.SuppressSpecificConstructorDemoTest;

@RunWith(Suite.class)
@SuiteClasses( { PrivateConstructorInstantiationDemoTest.class, ExpectNewDemoTest.class,
		ExpectVoidDemoTest.class, FinalDemoTest.class, MockSelfDemoTest.class, MockSelfDemoWithSubClassTest.class, StupidNewTest.class,
		PrivateFinalTest.class, MockSelfPrivateFieldServiceClassTest.class, SimplePrivateFieldServiceClassTest.class,
		PrivateMethodDemoTest.class, MockStaticTest.class, StaticAndInstanceDemoTest.class, SuppressMethodTest.class,
		SuppressConstructorDemoTest.class, SuppressConstructorHierarchyDemoTest.class, SuppressSpecificConstructorDemoTest.class,
		ConstructorArgsDemoTest.class, NoAnnotationUsageTest.class, SimpleReturnExampleUserTest.class, StaticInitializerExampleTest.class })
public class AllJUnit4Tests {
}
