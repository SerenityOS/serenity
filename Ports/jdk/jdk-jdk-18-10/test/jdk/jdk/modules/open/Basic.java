/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @library modules
 * @build m1/* m2/*
 * @run testng/othervm --add-modules=m1,m2 Basic
 * @summary Basic test of open modules and open packages
 */

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InaccessibleObjectException;

import org.testng.annotations.Test;
import org.testng.annotations.BeforeTest;
import static org.testng.Assert.*;

/**
 * open module m1 {
 *     exports p;
 *     // contains p.internal
 * }
 *
 * module m2 {
 *     exports q;
 *     exports q;
 *     // contains q.internal
 * }
 *
 * Each package in m1 and m2 contains a public type a non-public type.
 */

public class Basic {

    @BeforeTest
    public void checkSetup() throws Exception {
        Module m1 = Class.forName("p.PublicType").getModule();
        assertTrue(m1.isNamed());
        assertTrue(m1.getDescriptor().isOpen());
        assertTrue(m1.getDescriptor().packages().size() == 2);
        assertTrue(m1.getDescriptor().packages().contains("p"));
        assertTrue(m1.getDescriptor().packages().contains("p.internal"));
        assertTrue(m1.getDescriptor().exports().size() == 1);
        ModuleDescriptor.Exports e = m1.getDescriptor().exports().iterator().next();
        assertTrue(e.source().equals("p"));
        assertTrue(m1.isOpen("p"));
        assertTrue(m1.isOpen("p.internal"));

        Module m2 = Class.forName("q.PublicType").getModule();
        assertTrue(m2.isNamed());
        assertFalse(m2.getDescriptor().isOpen());
        assertTrue(m2.getDescriptor().packages().size() == 2);
        assertTrue(m2.getDescriptor().packages().contains("q"));
        assertTrue(m2.getDescriptor().packages().contains("q.internal"));
        assertTrue(m2.getDescriptor().exports().size() == 1);
        e = m2.getDescriptor().exports().iterator().next();
        assertTrue(e.source().equals("q"));
        assertTrue(m2.isOpen("q"));
        assertFalse(m2.isOpen("p.internal"));
    }

    @Test
    public void testPublicTypeInExportedPackage() throws Throwable {
        // invokespecial
        new p.PublicType();

        Class<?> clazz = p.PublicType.class;

        // core reflection
        Constructor<?> ctor = clazz.getConstructor();
        ctor.newInstance();
        ctor.setAccessible(true);
        ctor.newInstance();

        // Class::newInstance
        clazz.newInstance();

        // method handles
        findNoArgConstructorAndInvoke(clazz, MethodHandles.publicLookup());
        findNoArgConstructorAndInvoke(clazz, MethodHandles.lookup());
        try {
            MethodHandles.privateLookupIn(clazz, MethodHandles.publicLookup());
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
        Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
        findNoArgConstructorAndInvoke(clazz, lookup);
    }

    @Test
    public void testNotPublicTypeInExportedPackage() throws Throwable {
        Class<?> clazz = Class.forName("p.NotPublicType");

        // core reflection
        Constructor<?> ctor = clazz.getConstructor();
        try {
            ctor.newInstance();
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
        ctor.setAccessible(true);
        ctor.newInstance();

        // Class::newInstance
        try {
            clazz.newInstance();
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        // method handles
        try {
            findNoArgConstructorAndInvoke(clazz, MethodHandles.publicLookup());
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
        try {
            findNoArgConstructorAndInvoke(clazz, MethodHandles.lookup());
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
        try {
            MethodHandles.privateLookupIn(clazz, MethodHandles.publicLookup());
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
        findNoArgConstructorAndInvoke(clazz, lookup);
    }

    @Test
    public void testPublicTypeInNonExportedPackage() throws Throwable {
        Class<?> clazz = Class.forName("p.internal.PublicType");

        // core reflection
        Constructor<?> ctor = clazz.getConstructor();
        ctor.newInstance(); // should succeed
        ctor.setAccessible(true);
        ctor.newInstance();

        // Class::newInstance
        clazz.newInstance();

        // method handles
        findNoArgConstructorAndInvoke(clazz, MethodHandles.publicLookup());
        findNoArgConstructorAndInvoke(clazz, MethodHandles.lookup());
        try {
            MethodHandles.privateLookupIn(clazz, MethodHandles.publicLookup());
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
        Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
        findNoArgConstructorAndInvoke(clazz, lookup);
    }

    @Test
    public void testNotPublicTypeInNonExportedPackage() throws Throwable {
        Class<?> clazz = Class.forName("p.internal.NotPublicType");

        // core reflection
        Constructor<?> ctor = clazz.getConstructor();
        try {
            ctor.newInstance();
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
        ctor.setAccessible(true);
        ctor.newInstance();

        // Class::newInstance
        try {
            clazz.newInstance();
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        // method handles
        try {
            findNoArgConstructorAndInvoke(clazz, MethodHandles.publicLookup());
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
        try {
            findNoArgConstructorAndInvoke(clazz, MethodHandles.lookup());
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
        try {
            MethodHandles.privateLookupIn(clazz, MethodHandles.publicLookup());
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
        findNoArgConstructorAndInvoke(clazz, lookup);
    }

    @Test
    public void testPublicTypeInOpenPackage() throws Throwable {
        // invokespecial
        // new q.PublicType();   TBD

        // core reflection
        Class<?> clazz = q.PublicType.class;
        clazz.getConstructor().newInstance();
        clazz.newInstance();

        // method handles
        findNoArgConstructorAndInvoke(clazz, MethodHandles.publicLookup());
        findNoArgConstructorAndInvoke(clazz, MethodHandles.lookup());
        try {
            MethodHandles.privateLookupIn(clazz, MethodHandles.publicLookup());
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
        Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
        findNoArgConstructorAndInvoke(clazz, lookup);

    }

    @Test
    public void testNotPublicTypeInOpenPackage() throws Throwable {
        Class<?> clazz = Class.forName("q.NotPublicType");

        // core reflection
        Constructor<?> ctor = clazz.getConstructor();
        try {
            ctor.newInstance();
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
        ctor.setAccessible(true);
        ctor.newInstance();

        // Class::newInstance
        try {
            clazz.newInstance();
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        // method handles
        try {
            findNoArgConstructorAndInvoke(clazz, MethodHandles.publicLookup());
        } catch (IllegalAccessException expected) { }
        try {
            findNoArgConstructorAndInvoke(clazz, MethodHandles.lookup());
        } catch (IllegalAccessException expected) { }
        try {
            MethodHandles.privateLookupIn(clazz, MethodHandles.publicLookup());
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
        Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
        findNoArgConstructorAndInvoke(clazz, lookup);
    }

    @Test
    public void testPublicTypeInConcealedPackage() throws Throwable {
        Class<?> clazz = Class.forName("q.internal.PublicType");

        // core reflection
        Constructor<?> ctor = clazz.getConstructor();
        try {
            ctor.newInstance();
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
        try {
            ctor.setAccessible(true);
            assertTrue(false);
        } catch (InaccessibleObjectException expected) { }

        // Class::newInstance
        try {
            clazz.newInstance();
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        // method handles
        try {
            findNoArgConstructorAndInvoke(clazz, MethodHandles.publicLookup());
        } catch (IllegalAccessException expected) { }
        try {
            findNoArgConstructorAndInvoke(clazz, MethodHandles.lookup());
        } catch (IllegalAccessException expected) { }
        try {
            MethodHandles.privateLookupIn(clazz, MethodHandles.publicLookup());
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
        try {
            MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
    }

    @Test
    public void testNotPublicTypeInConcealedPackage() throws Throwable {
        Class<?> clazz = Class.forName("q.internal.NotPublicType");

        // core reflection
        Constructor<?> ctor = clazz.getConstructor();
        try {
            ctor.newInstance();
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
        try {
            ctor.setAccessible(true);
            assertTrue(false);
        } catch (InaccessibleObjectException expected) { }

        // Class::newInstance
        try {
            clazz.newInstance();
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        // method handles
        try {
            findNoArgConstructorAndInvoke(clazz, MethodHandles.publicLookup());
        } catch (IllegalAccessException expected) { }
        try {
            findNoArgConstructorAndInvoke(clazz, MethodHandles.lookup());
        } catch (IllegalAccessException expected) { }
        try {
            MethodHandles.privateLookupIn(clazz, MethodHandles.publicLookup());
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
        try {
            MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
            assertTrue(false);
        } catch (IllegalAccessException expected) { }
    }

    /**
     * Produces the method handle for the no-arg constructor and invokes it
     */
    Object findNoArgConstructorAndInvoke(Class<?> clazz, Lookup lookup) throws Throwable {
        MethodType mt = MethodType.methodType(void.class);
        MethodHandle mh = lookup.findConstructor(clazz, mt);
        return mh.invoke();
    }
}
