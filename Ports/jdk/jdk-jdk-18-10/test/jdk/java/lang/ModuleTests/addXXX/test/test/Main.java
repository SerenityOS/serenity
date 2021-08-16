/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Basic test case for Module::addXXXX methods
 */

@Test
public class Main {

    /**
     * Test Module::addReads
     *
     *     module test { }
     *
     *     module m1 {
     *         exports p1;
     *     }
     */
    public void testAddReads() throws Throwable {
        Module thisModule = Main.class.getModule();
        Class<?> clazz = Class.forName("p1.C");
        Module m1 = clazz.getModule();

        // test does not read m1
        assertFalse(thisModule.canRead(m1));
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType mt = MethodType.methodType(void.class);
        try {
            lookup.findConstructor(clazz, mt);
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        // update test to read m1
        Module result = thisModule.addReads(m1);
        assertTrue(result== thisModule);
        assertTrue(thisModule.canRead(m1));
        MethodHandle mh = lookup.findConstructor(clazz, mt);
        Object obj = mh.invoke();

        // attempt to update m1 to read test
        try {
            m1.addReads(thisModule);
            assertTrue(false);
        } catch (IllegalCallerException expected) { }
    }


    /**
     * Test Module::addExports
     *
     *     module test {
     *         requires m2;
     *     }
     *     module m2 {
     *         exports p2;
     *         contains package p2.internal;
     *     }
     */
    public void testAddExports() throws Exception {
        Module thisModule = Main.class.getModule();
        Module m2 = p2.C.class.getModule();
        Class<?> targetClass = Class.forName("p2.internal.C");
        String p2Internal = targetClass.getPackageName();
        assertTrue(targetClass.getModule() == m2);

        // m2 does not export p2.internal to test
        assertFalse(m2.isExported(p2Internal, thisModule));
        Constructor<?> ctor = targetClass.getDeclaredConstructor();
        try {
            ctor.newInstance();
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        // update m2 to export p2.internal to test
        p2.C.export(p2Internal, thisModule);
        assertTrue(m2.isExported(p2Internal, thisModule));
        ctor.newInstance(); // should succeed

        // attempt to update m2 to export a package to test
        try {
            m2.addExports("p2.other", thisModule);
            assertTrue(false);
        } catch (IllegalCallerException expected) { }
    }

    /**
     * Test Module::addOpens
     *
     *     module test {
     *         requires m3;
     *         requires m4;
     *     }
     *
     *     module m3 {
     *         exports p3 to test;
     *         opens p3 to test;
     *     }
     *
     *     module m4 {
     *         exports p4;
     *     }
     */
    public void testAddOpens() throws Exception {
        Module thisModule = Main.class.getModule();
        Module m3 = p3.C.class.getModule();
        Module m4 = p4.C.class.getModule();

        // test does not open package test to m4
        assertFalse(thisModule.isOpen("test", m4));
        try {
            p4.C.tryNewInstance(test.C.class);
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        // open test to m4
        thisModule.addOpens("test", m4);
        p4.C.tryNewInstance(test.C.class);  // should succeed


        // m3 does not open p3 to m4
        assertFalse(m3.isOpen("p3", m4));
        try {
            p4.C.tryNewInstance(p3.C.class);
            assertTrue(false);
        } catch (IllegalAccessException expected) { }


        // m3 opens p3 to test => test allowed to open m3/p3 to m4
        assertTrue(m3.isOpen("p3", thisModule));
        m3.addOpens("p3", m4);
        assertTrue(m3.isOpen("p3", m4));
        p4.C.tryNewInstance(p3.C.class);   // should succeed


        // attempt to update m4 to open package to m3
        try {
            m4.addOpens("p4", m3);
            assertTrue(false);
        } catch (IllegalCallerException expected) { }
    }


    /**
     * Test Module::addUses
     */
    public void testAddUses() {
        Module thisModule = Main.class.getModule();

        assertFalse(thisModule.canUse(Service.class));
        try {
            ServiceLoader.load(Service.class);
            assertTrue(false);
        } catch (ServiceConfigurationError expected) { }

        Module result = thisModule.addUses(Service.class);
        assertTrue(result== thisModule);

        assertTrue(thisModule.canUse(Service.class));
        ServiceLoader.load(Service.class); // no exception
    }

}
