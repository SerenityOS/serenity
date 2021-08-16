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

/*
 * @test
 * @bug 8238358 8247444
 * @run testng/othervm UnreflectTest
 * @summary Test Lookup::unreflectSetter and Lookup::unreflectVarHandle on
 *          trusted final fields (declared in hidden classes and records)
 */

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class UnreflectTest {
    static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();
    static final Class<?> hiddenClass = defineHiddenClass();
    private static Class<?> defineHiddenClass() {
        String classes = System.getProperty("test.classes");
        Path cf = Paths.get(classes, "Fields.class");
        try {
            byte[] bytes = Files.readAllBytes(cf);
            return MethodHandles.lookup().defineHiddenClass(bytes, true).lookupClass();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /*
     * Test Lookup::unreflectSetter and Lookup::unreflectVarHandle that
     * can write the value of a non-static final field in a normal class
     */
    @Test
    public void testFieldsInNormalClass() throws Throwable {
        // despite the name "HiddenClass", this class is loaded by the
        // class loader as non-hidden class
        Class<?> c = Fields.class;
        Fields o = new Fields();
        assertFalse(c.isHidden());
        readOnlyAccessibleObject(c, "STATIC_FINAL", null, true);
        readWriteAccessibleObject(c, "STATIC_NON_FINAL", null, false);
        readWriteAccessibleObject(c, "FINAL", o, true);
        readWriteAccessibleObject(c, "NON_FINAL", o, false);
    }

    /*
     * Test Lookup::unreflectSetter and Lookup::unreflectVarHandle that
     * has NO write the value of a non-static final field in a hidden class
     */
    @Test
    public void testFieldsInHiddenClass() throws Throwable {
        assertTrue(hiddenClass.isHidden());
        Object o = hiddenClass.newInstance();
        readOnlyAccessibleObject(hiddenClass, "STATIC_FINAL", null, true);
        readWriteAccessibleObject(hiddenClass, "STATIC_NON_FINAL", null, false);
        readOnlyAccessibleObject(hiddenClass, "FINAL", o, true);
        readWriteAccessibleObject(hiddenClass, "NON_FINAL", o, false);
    }

    static record TestRecord(int i) {
        static final Object STATIC_FINAL = new Object();
        static Object STATIC_NON_FINAL = new Object();
    }

    /*
     * Test Lookup::unreflectSetter and Lookup::unreflectVarHandle that
     * cannot write the value of a non-static final field in a record class
     */
    public void testFieldsInRecordClass() throws Throwable {
        assertTrue(TestRecord.class.isRecord());
        Object o = new TestRecord(1);
        readOnlyAccessibleObject(TestRecord.class, "STATIC_FINAL", null, true);
        readWriteAccessibleObject(TestRecord.class, "STATIC_NON_FINAL", null, false);
        readOnlyAccessibleObject(TestRecord.class, "i", o, true);
    }

    /*
     * Verify read-only access via unreflectSetter and unreflectVarHandle
     */
    private static void readOnlyAccessibleObject(Class<?> c, String name, Object o, boolean isFinal) throws Throwable {
        Field f = c.getDeclaredField(name);
        int modifier = f.getModifiers();
        if (isFinal) {
            assertTrue(Modifier.isFinal(modifier));
        } else {
            assertFalse(Modifier.isFinal(modifier));
        }
        assertTrue(f.trySetAccessible());

        // Field object with read-only access
        MethodHandle mh = LOOKUP.unreflectGetter(f);
        Object value = Modifier.isStatic(modifier) ? mh.invoke() : mh.invoke(o);
        assertTrue(value == f.get(o));
        try {
            LOOKUP.unreflectSetter(f);
            assertTrue(false, "should fail to unreflect a setter for " + name);
        } catch (IllegalAccessException e) {
        }

        VarHandle vh = LOOKUP.unreflectVarHandle(f);
        if (isFinal) {
            assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.SET));
        } else {
            assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.SET));
        }
    }

    private static void readWriteAccessibleObject(Class<?> c, String name, Object o, boolean isFinal) throws Throwable {
        Field f = c.getDeclaredField(name);
        int modifier = f.getModifiers();
        if (isFinal) {
            assertTrue(Modifier.isFinal(modifier));
        } else {
            assertFalse(Modifier.isFinal(modifier));
        }
        assertTrue(f.trySetAccessible());

        // Field object with read-write access
        MethodHandle mh = MethodHandles.lookup().unreflectGetter(f);
        Object value = Modifier.isStatic(modifier) ? mh.invoke() : mh.invoke(o);
        assertTrue(value == f.get(o));
        try {
            MethodHandle setter = MethodHandles.lookup().unreflectSetter(f);
            if (Modifier.isStatic(modifier)) {
                setter.invokeExact(value);
            } else {
                setter.invoke(o, value);
            }
        } catch (IllegalAccessException e) {
            throw e;
        }

        VarHandle vh = LOOKUP.unreflectVarHandle(f);
        if (isFinal) {
            assertFalse(vh.isAccessModeSupported(VarHandle.AccessMode.SET));
        } else {
            assertTrue(vh.isAccessModeSupported(VarHandle.AccessMode.SET));
        }
    }
}
