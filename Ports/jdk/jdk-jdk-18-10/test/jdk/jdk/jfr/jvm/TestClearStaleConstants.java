/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.jvm;

import java.time.Duration;
import java.util.List;

import jdk.jfr.consumer.RecordedClass;
import jdk.jfr.consumer.RecordedClassLoader;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.internal.JVM;
import jdk.jfr.Recording;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;
import jdk.test.lib.jfr.TestClassLoader;

/**
 * @test
 * @bug 8231081
 * @key jfr
 * @requires vm.hasJFR
 * @modules jdk.jfr/jdk.jfr.internal
 * @library /test/lib /test/jdk
 * @run main/othervm -Xlog:class+unload -Xlog:gc -Xmx16m jdk.jfr.jvm.TestClearStaleConstants
 */

/**
 * System.gc() will trigger class unloading if -XX:+ExplicitGCInvokesConcurrent is NOT set.
 * If this flag is set G1 will never unload classes on System.gc() and
 * As far as the "jfr" key guarantees no VM flags are set from the outside
 * it should be enough with System.gc().
 */
public final class TestClearStaleConstants {
    static class MyClass {
    }
    private final static String TEST_CLASS_NAME = "jdk.jfr.jvm.TestClearStaleConstants$MyClass";
    private final static String EVENT_NAME = EventNames.ClassDefine;

    // to prevent the compiler to optimize away all unread writes
    public static TestClassLoader firstClassLoader;
    public static TestClassLoader secondClassLoader;

    public static void main(String... args) throws Exception {
        firstClassLoader = new TestClassLoader();
        // define a  class using a class loader under a recording
        Class<?> clz = recordClassDefinition(firstClassLoader);
        JVM jvm = JVM.getJVM();
        // we will now tag the defined and loaded clz as being in use (no recordings are running here)
        jvm.getClassId(clz);
        // null out for unload to occur
        firstClassLoader = null;
        clz = null;
        // provoke unload
        System.gc();
        // try to define another class _with the same name_ using a different class loader
        secondClassLoader = new TestClassLoader();
        // this will throw a NPE for 8231081 because it will reuse the same class name
        // that symbol was  marked as already serialized by the unload, but since no recordings were running
        // it was not written to any chunk. This creates a reference to a non-existing symbol, leading to an NPE (no symbol at the expected location).
        recordClassDefinition(secondClassLoader);
    }

    private static Class<?> recordClassDefinition(TestClassLoader classLoader) throws Exception  {
        try (Recording recording = new Recording())  {
            recording.enable(EVENT_NAME);
            recording.start();
            Class<?> clz = classLoader.loadClass(TEST_CLASS_NAME);
            recording.stop();
            assertClassDefineEvent(recording);
            return clz;
        }
    }

    private static void assertClassDefineEvent(Recording recording) throws Exception {
        boolean isAnyFound = false;
        for (RecordedEvent event : Events.fromRecording(recording)) {
            System.out.println(event);
            RecordedClass definedClass = event.getValue("definedClass");
            if (TEST_CLASS_NAME.equals(definedClass.getName())) {
                RecordedClassLoader definingClassLoader = definedClass.getClassLoader();
                String definingName = definingClassLoader.getType().getName();
                String testName = TestClassLoader.class.getName();
                String errorMsg = "Expected " + testName + ", got " + definingName;
                Asserts.assertEquals(testName, definingName, errorMsg);
                Asserts.assertFalse(isAnyFound, "Found more than 1 event");
                isAnyFound = true;
            }
        }
        Asserts.assertTrue(isAnyFound, "No events found");
    }
}
