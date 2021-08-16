/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

package test;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Test access to public/non-public members of public/non-public classes in
 * exported and non-exported packages.
 */

public class Main {

    public static void main(String[] args) throws Exception {
        testPublicClassInExportedPackage();
        testNonPublicClassInExportedPackage();
        testPublicClassInNonExportedPackage();
        testNonPublicClassInNonExportedPackage();
    }

    static void testPublicClassInExportedPackage() throws Exception {
        Module thisModule = Main.class.getModule();
        Module targetModule = getTargetModule();

        assertTrue(targetModule.isExported("p1"));
        assertTrue(targetModule.isExported("p1", thisModule));
        assertTrue(targetModule.isExported("p1", targetModule));

        assertFalse(targetModule.isOpen("p1"));
        assertFalse(targetModule.isOpen("p1", thisModule));
        assertTrue(targetModule.isOpen("p1", targetModule));

        Class<?> clazz = Class.forName("p1.Public");
        Constructor<?> ctor1 = clazz.getConstructor();   // public
        Constructor<?> ctor2 = clazz.getDeclaredConstructor(Void.class); // non-public

        Field f1 = clazz.getField("f1");    // public
        Field f2 = clazz.getDeclaredField("f2");    // non-public

        Method m1 = clazz.getMethod("foo");  // public
        Method m2 = clazz.getDeclaredMethod("bar");  // non-public

        tryAccessConstructor(ctor1, true);
        tryAccessConstructor(ctor2, false);
        tryAccessMethod(m1, true);
        tryAccessMethod(m2, false);
        tryAccessObjectField(f1, true);
        tryAccessObjectField(f2, false);

        trySetAccessible(ctor1, true);
        trySetAccessible(ctor2, false);
        trySetAccessible(m1, true);
        trySetAccessible(m2, false);
        trySetAccessible(f1, true);
        trySetAccessible(f2, false);

        targetAddOpens("p1", thisModule);

        tryAccessConstructor(ctor1, true);
        tryAccessConstructor(ctor2, false);
        tryAccessMethod(m1, true);
        tryAccessMethod(m2, false);
        tryAccessObjectField(f1, true);
        tryAccessObjectField(f2, false);

        trySetAccessible(ctor1, true);
        trySetAccessible(ctor2, true);
        trySetAccessible(m1, true);
        trySetAccessible(m2, true);
        trySetAccessible(f1, true);
        trySetAccessible(f2, true);
    }

    static void testNonPublicClassInExportedPackage() throws Exception {
        Module thisModule = Main.class.getModule();
        Module targetModule = getTargetModule();

        assertTrue(targetModule.isExported("p2"));
        assertTrue(targetModule.isExported("p2", thisModule));
        assertTrue(targetModule.isExported("p2", targetModule));

        assertFalse(targetModule.isOpen("p2"));
        assertFalse(targetModule.isOpen("p2", thisModule));
        assertTrue(targetModule.isOpen("p1", targetModule));

        Class<?> clazz = Class.forName("p2.NonPublic");
        Constructor<?> ctor1 = clazz.getConstructor();
        Constructor<?> ctor2 = clazz.getDeclaredConstructor(Void.class);

        Field f1 = clazz.getField("f1");    // public
        Field f2 = clazz.getDeclaredField("f2");    // non-public

        Method m1 = clazz.getMethod("foo");  // public
        Method m2 = clazz.getDeclaredMethod("bar");  // non-public

        tryAccessConstructor(ctor1, false);
        tryAccessConstructor(ctor2, false);
        tryAccessMethod(m1, false);
        tryAccessMethod(m2, false);
        tryAccessObjectField(f1, false);
        tryAccessObjectField(f2, false);

        trySetAccessible(ctor1, false);
        trySetAccessible(ctor2, false);
        trySetAccessible(m1, false);
        trySetAccessible(m2, false);
        trySetAccessible(f1, false);
        trySetAccessible(f2, false);

        targetAddExports("p2", thisModule);

        tryAccessConstructor(ctor1, false);
        tryAccessConstructor(ctor2, false);
        tryAccessMethod(m1, false);
        tryAccessMethod(m2, false);
        tryAccessObjectField(f1, false);
        tryAccessObjectField(f2, false);

        trySetAccessible(ctor1, false);
        trySetAccessible(ctor2, false);
        trySetAccessible(m1, false);
        trySetAccessible(m2, false);
        trySetAccessible(f1, false);
        trySetAccessible(f2, false);

        targetAddOpens("p2", thisModule);

        tryAccessConstructor(ctor1, false);
        tryAccessConstructor(ctor2, false);
        tryAccessMethod(m1, false);
        tryAccessMethod(m2, false);
        tryAccessObjectField(f1, false);
        tryAccessObjectField(f2, false);

        trySetAccessible(ctor1, true);
        trySetAccessible(ctor2, true);
        trySetAccessible(m1, true);
        trySetAccessible(m2, true);
        trySetAccessible(f1, true);
        trySetAccessible(f2, true);
    }

    static void testPublicClassInNonExportedPackage() throws Exception {
        Module thisModule = Main.class.getModule();
        Module targetModule = getTargetModule();

        assertFalse(targetModule.isExported("q1"));
        assertFalse(targetModule.isExported("q1", thisModule));
        assertTrue(targetModule.isExported("q1", targetModule));

        assertFalse(targetModule.isOpen("q1"));
        assertFalse(targetModule.isOpen("q1", thisModule));
        assertTrue(targetModule.isOpen("q1", targetModule));

        Class<?> clazz = Class.forName("q1.Public");
        Constructor<?> ctor1 = clazz.getConstructor();  // public
        Constructor<?> ctor2 = clazz.getDeclaredConstructor(Void.class);  // non-public

        Field f1 = clazz.getField("f1");    // public
        Field f2 = clazz.getDeclaredField("f2");    // non-public

        Method m1 = clazz.getMethod("foo");  // public
        Method m2 = clazz.getDeclaredMethod("bar");  // non-public

        tryAccessConstructor(ctor1, false);
        tryAccessConstructor(ctor2, false);
        tryAccessMethod(m1, false);
        tryAccessMethod(m2, false);
        tryAccessObjectField(f1, false);
        tryAccessObjectField(f2, false);

        trySetAccessible(ctor1, false);
        trySetAccessible(ctor2, false);
        trySetAccessible(m1, false);
        trySetAccessible(m2, false);
        trySetAccessible(f1, false);
        trySetAccessible(f2, false);

        targetAddExports("q1", thisModule);

        tryAccessConstructor(ctor1, true);
        tryAccessConstructor(ctor2, false);
        tryAccessMethod(m1, true);
        tryAccessMethod(m2, false);
        tryAccessObjectField(f1, true);
        tryAccessObjectField(f2, false);

        trySetAccessible(ctor1, true);
        trySetAccessible(ctor2, false);
        trySetAccessible(m1, true);
        trySetAccessible(m2, false);
        trySetAccessible(f1, true);
        trySetAccessible(f2, false);

        targetAddOpens("q1", thisModule);

        tryAccessConstructor(ctor1, true);
        tryAccessConstructor(ctor1, false);
        tryAccessMethod(m1, true);
        tryAccessMethod(m2, false);
        tryAccessObjectField(f1, true);
        tryAccessObjectField(f2, false);

        trySetAccessible(ctor1, true);
        trySetAccessible(ctor2, true);
        trySetAccessible(m1, true);
        trySetAccessible(m2, true);
        trySetAccessible(f1, true);
        trySetAccessible(f2, true);
    }

    static void testNonPublicClassInNonExportedPackage() throws Exception {
        Module thisModule = Main.class.getModule();
        Module targetModule = getTargetModule();

        assertFalse(targetModule.isExported("q2"));
        assertFalse(targetModule.isExported("q2", thisModule));
        assertTrue(targetModule.isExported("q2", targetModule));

        assertFalse(targetModule.isOpen("q2"));
        assertFalse(targetModule.isOpen("q2", thisModule));
        assertTrue(targetModule.isOpen("q2", targetModule));

        Class<?> clazz = Class.forName("q2.NonPublic");
        Constructor<?> ctor1 = clazz.getConstructor();  // public
        Constructor<?> ctor2 = clazz.getDeclaredConstructor(Void.class);  // non-public

        Field f1 = clazz.getField("f1");    // public
        Field f2 = clazz.getDeclaredField("f2");    // non-public

        Method m1 = clazz.getMethod("foo");  // public
        Method m2 = clazz.getDeclaredMethod("bar");  // non-public

        tryAccessConstructor(ctor1, false);
        tryAccessConstructor(ctor2, false);
        tryAccessMethod(m1, false);
        tryAccessMethod(m2, false);
        tryAccessObjectField(f1, false);
        tryAccessObjectField(f2, false);

        trySetAccessible(ctor1, false);
        trySetAccessible(ctor2, false);
        trySetAccessible(m1, false);
        trySetAccessible(m2, false);
        trySetAccessible(f1, false);
        trySetAccessible(f2, false);

        targetAddExports("q2", thisModule);

        tryAccessConstructor(ctor1, false);
        tryAccessConstructor(ctor2, false);
        tryAccessMethod(m1, false);
        tryAccessMethod(m2, false);
        tryAccessObjectField(f1, false);
        tryAccessObjectField(f2, false);

        trySetAccessible(ctor1, false);
        trySetAccessible(ctor2, false);
        trySetAccessible(m1, false);
        trySetAccessible(m2, false);
        trySetAccessible(f1, false);
        trySetAccessible(f2, false);

        targetAddOpens("q2", thisModule);

        tryAccessConstructor(ctor1, false);
        tryAccessConstructor(ctor2, false);
        tryAccessMethod(m1, false);
        tryAccessMethod(m2, false);
        tryAccessObjectField(f1, false);
        tryAccessObjectField(f2, false);

        trySetAccessible(ctor1, true);
        trySetAccessible(m1, true);
        trySetAccessible(m2, true);
        trySetAccessible(f1, true);
        trySetAccessible(f2, true);
    }


    static Module getTargetModule() {
        return ModuleLayer.boot().findModule("target").get();
    }

    static void tryAccessConstructor(Constructor<?> ctor, boolean shouldSucceed) {
        try {
            ctor.newInstance();
            assertTrue(shouldSucceed);
        } catch (Exception e) {
            assertFalse(shouldSucceed);
        }
    }

    static void tryAccessMethod(Method method, boolean shouldSucceed) {
        try {
            method.invoke(null);
            assertTrue(shouldSucceed);
        } catch (Exception e) {
            e.printStackTrace();
            assertFalse(shouldSucceed);
        }
    }

    static void tryAccessObjectField(Field f, boolean shouldSucceed) {
        try {
            f.get(null);
            assertTrue(shouldSucceed);
        } catch (Exception e) {
            assertFalse(shouldSucceed);
        }
        try {
            f.set(null, new Object());
            assertTrue(shouldSucceed);
        } catch (Exception e) {
            assertFalse(shouldSucceed);
        }
    }

    static void trySetAccessible(AccessibleObject ao, boolean shouldSucceed) {
        try {
            ao.setAccessible(true);
            assertTrue(shouldSucceed);
        } catch (Exception e) {
            assertFalse(shouldSucceed);
        }
    }

    /**
     * Update target module to export a package to the given module.
     */
    static void targetAddExports(String pn, Module who) throws Exception {
        Class<?> helper = Class.forName("p1.Helper");
        Method m = helper.getMethod("addExports", String.class, Module.class);
        m.invoke(null, pn, who);
    }

    /**
     * Update target module to open a package to the given module.
     */
    static void targetAddOpens(String pn, Module who) throws Exception {
        Class<?> helper = Class.forName("p1.Helper");
        Method m = helper.getMethod("addOpens", String.class, Module.class);
        m.invoke(null, pn, who);
    }

    static void assertTrue(boolean expr) {
        if (!expr) throw new RuntimeException();
    }

    static void assertFalse(boolean expr) {
        assertTrue(!expr);
    }
}
