/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8238358 8247444
 * @summary Ensure that sun.misc.Unsafe::objectFieldOffset and staticFieldOffset
 *          throw UnsupportedOperationException on Field of a hidden or record class
 * @modules jdk.unsupported
 * @compile --enable-preview -source ${jdk.version} UnsafeFieldOffsets.java
 * @run testng/othervm --enable-preview UnsafeFieldOffsets
 */

import sun.misc.Unsafe;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class UnsafeFieldOffsets {
    static class Fields {
        static final Object STATIC_FINAL = new Object();
        static Object STATIC_NON_FINAL = new Object();
        final Object FINAL = new Object();
        Object NON_FINAL = new Object();
    }
    record TestRecord(int i) {
        static final Object STATIC_FINAL = new Object();
        static Object STATIC_NON_FINAL = new Object();
    }

    private static Unsafe UNSAFE = getUnsafe();
    private static final Class<?> HIDDEN_CLASS = defineHiddenClass();
    private static final Class<?> RECORD_CLASS = TestRecord.class;

    private static Unsafe getUnsafe() {
        try {
            Field f = Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (Unsafe) f.get(null);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static Class<?> defineHiddenClass() {
        String classes = System.getProperty("test.classes");
        Path cf = Paths.get(classes, "UnsafeFieldOffsets$Fields.class");
        try {
            byte[] bytes = Files.readAllBytes(cf);
            Class<?> c = MethodHandles.lookup().defineHiddenClass(bytes, true).lookupClass();
            assertTrue(c.isHidden());
            return c;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testNormalClass() throws Throwable {
        // hidden class
        testStaticField(HIDDEN_CLASS, "STATIC_FINAL");
        testStaticField(HIDDEN_CLASS, "STATIC_NON_FINAL");
        testInstanceField(HIDDEN_CLASS, "FINAL");
        testInstanceField(HIDDEN_CLASS, "NON_FINAL");
    }

    @Test
    public void testHiddenClass() throws Throwable {
        // hidden class
        testStaticField(HIDDEN_CLASS, "STATIC_FINAL");
        testStaticField(HIDDEN_CLASS, "STATIC_NON_FINAL");
        testInstanceField(HIDDEN_CLASS, "FINAL");
        testInstanceField(HIDDEN_CLASS, "NON_FINAL");
    }

    @Test
    public void testRecordClass() throws Throwable {
        // record class
        testRecordStaticField(RECORD_CLASS, "STATIC_FINAL");
        testRecordStaticField(RECORD_CLASS, "STATIC_NON_FINAL");
        testRecordInstanceField(RECORD_CLASS, "i");
    }

    private static void testStaticField(Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        try {
            UNSAFE.staticFieldOffset(f);
            assertFalse(c.isHidden(), "Expected UOE thrown: " + c);
        } catch (UnsupportedOperationException e) {
            assertTrue(c.isHidden(), "Expected hidden class: " + c);
        }
    }

    private static void testInstanceField(Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        try {
            UNSAFE.objectFieldOffset(f);
            assertFalse(c.isHidden(), "Expected UOE thrown: " + c);
        } catch (UnsupportedOperationException e) {
            assertTrue(c.isHidden(), "Expected hidden class: " + c);
        }
    }

    @SuppressWarnings("preview")
    private static void testRecordStaticField(Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        try {
            UNSAFE.staticFieldOffset(f);
            assertFalse(c.isRecord(), "Expected UOE thrown: " + c);
        } catch (UnsupportedOperationException e) {
            assertTrue(c.isRecord(), "Expected record class: " + c);
        }
    }

    @SuppressWarnings("preview")
    private static void testRecordInstanceField(Class<?> c, String name) throws Exception {
        Field f = c.getDeclaredField(name);
        try {
            UNSAFE.objectFieldOffset(f);
            assertFalse(c.isRecord(), "Expected UOE thrown: " + c);
        } catch (UnsupportedOperationException e) {
            assertTrue(c.isRecord(), "Expected record class: " + c);
        }
    }
}
