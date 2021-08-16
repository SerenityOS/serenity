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

package jdk.jfr.jvm;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import jdk.jfr.Event;
import jdk.jfr.FlightRecorder;
import jdk.jfr.internal.JVM;

/**
 * @test
 * @key jfr
 * @summary Unit test for JVM#getUnloadedEventClassCount
 * @requires vm.hasJFR
 *
 * @library /test/lib
 * @modules jdk.jfr/jdk.jfr.internal
 *          java.base/jdk.internal.misc
 *
 * @run main/othervm -Xlog:class+unload -Xlog:gc -Xmx16m jdk.jfr.jvm.TestUnloadEventClassCount
 */
public class TestUnloadEventClassCount {

    private static final String EVENT_NAME = "jdk.jfr.jvm.TestUnloadEventClassCount$ToBeUnloaded";

    public static class ToBeUnloaded extends Event {
    }

    static public class MyClassLoader extends ClassLoader {
        public MyClassLoader() {
            super("MyClassLoader", null);
        }

        public final Class<?> defineClass(String name, byte[] b) {
            return super.defineClass(name, b, 0, b.length);
        }
    }

    public static MyClassLoader myClassLoader;

    public static void main(String[] args) throws Throwable {
        FlightRecorder.getFlightRecorder();
        myClassLoader = createClassLoaderWithEventClass();
        System.out.println("MyClassLoader instance created");
        long initialCount = JVM.getJVM().getUnloadedEventClassCount();
        System.out.println("Initiali unloaded count is " + initialCount);
        myClassLoader = null;
        System.out.println("Reference to class loader cleared");
        long count = 0;
        do {
            System.gc();
            System.out.println("GC triggered");
            count = JVM.getJVM().getUnloadedEventClassCount();
            System.out.println("Unloaded count was " + count);
            Thread.sleep(1000); // sleep to reduce log
        } while (count != initialCount + 1);
    }

    private static MyClassLoader createClassLoaderWithEventClass() throws Exception {
        String resourceName = EVENT_NAME.replace('.', '/') + ".class";
        try (InputStream is = TestUnloadEventClassCount.class.getClassLoader().getResourceAsStream(resourceName)) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int byteValue = 0;
            while ((byteValue = is.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, byteValue);
            }
            baos.flush();
            MyClassLoader myClassLoader = new MyClassLoader();
            Class<?> eventClass = myClassLoader.defineClass(EVENT_NAME, baos.toByteArray());
            if (eventClass == null) {
                throw new Exception("Could not define test class");
            }
            if (eventClass.getSuperclass() != Event.class) {
                throw new Exception("Superclass should be jdk.jfr.Event");
            }
            if (eventClass.getSuperclass().getClassLoader() != null) {
                throw new Exception("Class loader of jdk.jfr.Event should be null");
            }
            if (eventClass.getClassLoader() != myClassLoader) {
                throw new Exception("Incorrect class loader for event class");
            }
            eventClass.newInstance(); // force <clinit>
            return myClassLoader;
        }
    }
}
