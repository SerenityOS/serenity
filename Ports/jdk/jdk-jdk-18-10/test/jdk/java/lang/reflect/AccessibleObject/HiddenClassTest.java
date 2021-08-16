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

/**
 * @test
 * @build Fields HiddenClassTest
 * @run testng/othervm HiddenClassTest
 * @summary Test java.lang.reflect.AccessibleObject with modules
 */

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class HiddenClassTest {
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
     * Test Field::set that can write the value of a non-static final field
     * in a normal class
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
     * Test Field::set that fails to write the value of a non-static final field
     * in a hidden class
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

    private static void readOnlyAccessibleObject(Class<?> c, String name, Object o, boolean isFinal) throws Exception {
        Field f = c.getDeclaredField(name);
        int modifier = f.getModifiers();
        if (isFinal) {
            assertTrue(Modifier.isFinal(modifier));
        } else {
            assertFalse(Modifier.isFinal(modifier));
        }
        assertTrue(f.trySetAccessible());
        assertTrue(f.get(o) != null);
        try {
            f.set(o, null);
            assertTrue(false, "should fail to set " + name);
        } catch (IllegalAccessException e) {
        }
    }

    private static void readWriteAccessibleObject(Class<?> c, String name, Object o, boolean isFinal) throws Exception {
        Field f = c.getDeclaredField(name);
        int modifier = f.getModifiers();
        if (isFinal) {
            assertTrue(Modifier.isFinal(modifier));
        } else {
            assertFalse(Modifier.isFinal(modifier));
        }
        assertTrue(f.trySetAccessible());
        assertTrue(f.get(o) != null);
        try {
            f.set(o, null);
        } catch (IllegalAccessException e) {
            throw e;
        }
    }
}
