/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @build ModuleSetAccessibleTest
 * @modules java.base/java.lang:open
 *          java.base/jdk.internal.misc:+open
 * @run testng/othervm ModuleSetAccessibleTest
 * @summary Test java.lang.reflect.AccessibleObject with modules
 */

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InaccessibleObjectException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import jdk.internal.misc.Unsafe;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class ModuleSetAccessibleTest {

    /**
     * Invoke a private constructor on a public class in an exported package
     */
    public void testPrivateConstructorInExportedPackage() throws Exception {
        Constructor<?> ctor = Unsafe.class.getDeclaredConstructor();

        try {
            ctor.newInstance();
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        ctor.setAccessible(true);
        Unsafe unsafe = (Unsafe) ctor.newInstance();
    }


    /**
     * Invoke a private method on a public class in an exported package
     */
    public void testPrivateMethodInExportedPackage() throws Exception {
        Method m = Unsafe.class.getDeclaredMethod("throwIllegalAccessError");
        try {
            m.invoke(null);
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        m.setAccessible(true);
        try {
            m.invoke(null);
            assertTrue(false);
        } catch (InvocationTargetException e) {
            // thrown by throwIllegalAccessError
            assertTrue(e.getCause() instanceof IllegalAccessError);
        }
    }


    /**
     * Access a private field in a public class that is an exported package
     */
    public void testPrivateFieldInExportedPackage() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");

        try {
            f.get(null);
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);
    }


    /**
     * Invoke a public constructor on a public class in a non-exported package
     */
    public void testPublicConstructorInNonExportedPackage() throws Exception {
        Class<?> clazz = Class.forName("sun.security.x509.X500Name");
        Constructor<?> ctor = clazz.getConstructor(String.class);

        try {
            ctor.newInstance("cn=duke");
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        try {
            ctor.setAccessible(true);
            assertTrue(false);
        } catch (InaccessibleObjectException expected) { }

        ctor.setAccessible(false); // should succeed
    }


    /**
     * Access a public field in a public class that in a non-exported package
     */
    public void testPublicFieldInNonExportedPackage() throws Exception {
        Class<?> clazz = Class.forName("sun.security.x509.X500Name");
        Field f = clazz.getField("SERIALNUMBER_OID");

        try {
            f.get(null);
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        try {
            f.setAccessible(true);
            assertTrue(false);
        } catch (InaccessibleObjectException expected) { }

        f.setAccessible(false); // should succeed
    }


    /**
     * Test that the Class constructor cannot be make accessible.
     */
    public void testJavaLangClass() throws Exception {

        // non-public constructor
        Constructor<?> ctor
            = Class.class.getDeclaredConstructor(ClassLoader.class, Class.class);
        AccessibleObject[] ctors = { ctor };

        try {
            ctor.setAccessible(true);
            assertTrue(false);
        } catch (SecurityException expected) { }

        try {
            AccessibleObject.setAccessible(ctors, true);
            assertTrue(false);
        } catch (SecurityException expected) { }

        // should succeed
        ctor.setAccessible(false);
        AccessibleObject.setAccessible(ctors, false);

    }

}
