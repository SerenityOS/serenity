/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package jdk.jfr.api.consumer;

import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedClassLoader;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.TestClassLoader;

/**
 * @test
 * @summary Verifies the methods of the RecordedClassLoader
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib
 * @run main/othervm jdk.jfr.api.consumer.TestRecordedClassLoader
 */
public class TestRecordedClassLoader {

    private final static String TEST_CLASS_NAME = "jdk.jfr.api.consumer.TestRecordedClassLoader$MyTestClass";
    private final static String EVENT_NAME = EventNames.ClassDefine;

    static class MyTestClass {
    }

    public static void main(String[] args) throws Exception {
        try (Recording recording = new Recording()) {
            recording.enable(EVENT_NAME).withoutStackTrace();
            TestClassLoader cl = new TestClassLoader();
            recording.start();
            cl.loadClass(TEST_CLASS_NAME);
            recording.stop();

            List<RecordedEvent> events = Events.fromRecording(recording);
            boolean isDefined = false;
            for (RecordedEvent event : events) {
                RecordedClass definedClass = event.getValue("definedClass");
                if (TEST_CLASS_NAME.equals(definedClass.getName())) {
                    System.out.println(event);

                    // get the RecordedClassLoader from the RecordedClass, the "definedClass"
                    RecordedClassLoader definingClassLoader = definedClass.getClassLoader();
                    Asserts.assertNotNull(definingClassLoader, "Defining Class Loader should not be null");

                    // invoke RecordedClassLoader.getType() in order to validate the type of the RecordedClassLoader
                    RecordedClass definingClassLoaderType = definingClassLoader.getType();
                    Asserts.assertNotNull(definingClassLoaderType, "The defining Class Loader type should not be null");

                    // verify matching types
                    Asserts.assertEquals(cl.getClass().getName(), definingClassLoaderType.getName(),
                        "Expected type " + cl.getClass().getName() + ", got type " + definingClassLoaderType.getName());

                    // get a RecordedClassLoader directly from the "definingClassLoader" field as well
                    RecordedClassLoader definingClassLoaderFromField = event.getValue("definingClassLoader");
                    Asserts.assertNotNull(definingClassLoaderFromField,
                        "Defining Class Loader instantatiated from field should not be null");

                    // ensure that the class loader instance used in the test actually has a name
                    Asserts.assertNotNull(cl.getName(),
                        "Expected a valid name for the TestClassLoader");

                    // invoke RecordedClassLoader.getName() to get the name of the class loader instance
                    Asserts.assertEquals(cl.getName(), definingClassLoader.getName(),
                        "Defining Class Loader should have the same name as the original class loader");
                    Asserts.assertEquals(definingClassLoaderFromField.getName(), definingClassLoader.getName(),
                        "Defining Class Loader representations should have the same class loader name");

                    // invoke uniqueID()
                    Asserts.assertGreaterThan(definingClassLoader.getId(), 0L, "Invalid id assignment");

                    // second order class loader information ("check class loader of the class loader")
                    RecordedClassLoader classLoaderOfDefClassLoader = definingClassLoaderType.getClassLoader();
                    Asserts.assertNotNull(classLoaderOfDefClassLoader,
                        "The class loader for the definining class loader should not be null");
                    Asserts.assertEquals(cl.getClass().getClassLoader().getName(), classLoaderOfDefClassLoader.getName(),
                        "Expected class loader name " + cl.getClass().getClassLoader().getName() + ", got name " + classLoaderOfDefClassLoader.getName());

                    RecordedClass classLoaderOfDefClassLoaderType = classLoaderOfDefClassLoader.getType();
                    Asserts.assertNotNull(classLoaderOfDefClassLoaderType,
                        "The class loader type for the defining class loader should not be null");
                    Asserts.assertEquals(cl.getClass().getClassLoader().getClass().getName(), classLoaderOfDefClassLoaderType.getName(),
                        "Expected type " + cl.getClass().getClassLoader().getClass().getName() + ", got type " + classLoaderOfDefClassLoaderType.getName());

                    Asserts.assertGreaterThan(definingClassLoader.getId(), classLoaderOfDefClassLoader.getId(),
                        "expected id assignment invariant broken for Class Loaders");

                    isDefined = true;
                }
            }
            Asserts.assertTrue(isDefined, "No class define event found to verify RecordedClassLoader");
        }
    }
}
