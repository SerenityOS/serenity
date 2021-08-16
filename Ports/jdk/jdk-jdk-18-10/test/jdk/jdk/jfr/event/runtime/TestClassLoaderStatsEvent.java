/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jfr.event.runtime;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedClassLoader;
import jdk.jfr.consumer.RecordedEvent;
import jdk.test.lib.Asserts;
import jdk.test.lib.jfr.EventNames;
import jdk.test.lib.jfr.Events;

import jdk.test.lib.compiler.InMemoryJavaCompiler;

/**
 * @test
 * @key jfr
 * @requires vm.hasJFR
 * @library /test/lib /test/jdk
 * @modules java.base/jdk.internal.misc
 *          jdk.compiler
 *          jdk.jfr
 * @build jdk.jfr.event.runtime.TestClasses
 * @run main/othervm jdk.jfr.event.runtime.TestClassLoaderStatsEvent
 */
public class TestClassLoaderStatsEvent {
    private final static String EVENT_NAME = EventNames.ClassLoaderStatistics;
    private final static String CLASS_LOADER_NAME = "MyDummyClassLoader";
    private final static String CLASSLOADER_TYPE_NAME = "jdk.jfr.event.runtime.TestClassLoaderStatsEvent$DummyClassLoader";
    public static DummyClassLoader dummyloader;

    public static void main(String[] args) throws Throwable {
        createDummyClassLoader(CLASS_LOADER_NAME);

        Recording recording = new Recording();
        recording.enable(EVENT_NAME);
        recording.start();
        recording.stop();
        List<RecordedEvent> consumer = Events.fromRecording(recording);
        Events.hasEvents(consumer);

        boolean isAnyFound = false;
        for (RecordedEvent event : consumer) {
            System.out.println("Event:" + event);
            if (Events.assertField(event, "classLoader").getValue() == null) {
                continue;
            }
            RecordedClassLoader recordedClassLoader = event.getValue("classLoader");
            if (CLASSLOADER_TYPE_NAME.equals(recordedClassLoader.getType().getName())) {
                Asserts.assertEquals(CLASS_LOADER_NAME, recordedClassLoader.getName(),
                    "Expected class loader name " + CLASS_LOADER_NAME + ", got name " + recordedClassLoader.getName());
                Events.assertField(event, "classCount").equal(2L);
                Events.assertField(event, "chunkSize").above(1L);
                Events.assertField(event, "blockSize").above(1L);
                Events.assertField(event, "hiddenClassCount").equal(2L);
                Events.assertField(event, "hiddenChunkSize").above(0L);
                Events.assertField(event, "hiddenBlockSize").above(0L);
                isAnyFound = true;
            }
        }
        Asserts.assertTrue(isAnyFound, "No events found");
    }

    private static void createDummyClassLoader(String name) throws Throwable {
        dummyloader = new DummyClassLoader(name);
        Class<?> c = Class.forName(TestClass.class.getName(), true, dummyloader);
        if (c.getClassLoader() != dummyloader) {
            throw new RuntimeException("TestClass defined by wrong classloader: " + c.getClassLoader());
        }

        // Compile a class for method createNonFindableClasses() to use to create a
        // non-strong hidden class.
        byte klassbuf[] = InMemoryJavaCompiler.compile("jdk.jfr.event.runtime.TestClass",
            "package jdk.jfr.event.runtime; " +
            "public class TestClass { " +
            "    public static void concat(String one, String two) throws Throwable { " +
            " } } ");

        Method m = c.getDeclaredMethod("createNonFindableClasses", byte[].class);
        m.setAccessible(true);
        m.invoke(null, klassbuf);
    }

    public static class DummyClassLoader extends ClassLoader {

        static ByteBuffer readClassFile(String name) {
            String testClasses = System.getProperty("test.classes");
            File f = new File(testClasses, name);
            try (FileInputStream fin = new FileInputStream(f)) {
                FileChannel fc = fin.getChannel();
                return fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
            } catch (IOException e) {
                throw new RuntimeException("Can't open file: " + f, e);
            }
        }

        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            Class<?> c;
            if (TestClass.class.getName().equals(name)) {
                c = findClass(name);
                if (resolve) {
                    resolveClass(c);
                }
            } else {
                c = super.loadClass(name, resolve);
            }
            return c;
        }

        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (!TestClass.class.getName().equals(name)) {
                throw new ClassNotFoundException("Unexpected class: " + name);
            }
            return defineClass(name, readClassFile(TestClass.class.getName().replace(".", File.separator) + ".class"), null);
        }

        public DummyClassLoader(String name) {
            super(name, ClassLoader.getSystemClassLoader());
        }
    }
}
