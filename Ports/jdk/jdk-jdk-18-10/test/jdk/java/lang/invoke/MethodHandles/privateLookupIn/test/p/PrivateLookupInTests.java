/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
package p;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Modifier;

import static java.lang.invoke.MethodHandles.Lookup.*;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Unit tests for MethodHandles.privateLookupIn
 */

@Test
public class PrivateLookupInTests {
    /**
     * A public and non-public types in the test module but in a different
     * package to the test class.
     *
     * package p.internal;
     * public class PublicType {
     * }
     *
     * package p.internal;
     * class NonPublicType {
     *     private static final Object obj = ...
     * }
     */
    private Class<?> publicType;
    private Class<?> nonPublicType;

    // initialize and sanity check publicType/nonPublicType
    @BeforeTest
    public void init() throws Exception {
        publicType = Class.forName("p.internal.PublicType");
        assertTrue(this.getClass().getModule() == publicType.getModule());
        assertNotEquals(this.getClass().getPackageName(), publicType.getPackageName());
        assertTrue(Modifier.isPublic(publicType.getModifiers()));

        nonPublicType = Class.forName("p.internal.NonPublicType");
        assertTrue(this.getClass().getModule() == nonPublicType.getModule());
        assertNotEquals(this.getClass().getPackageName(), nonPublicType.getPackageName());
        assertFalse(Modifier.isPublic(nonPublicType.getModifiers()));
    }

    // Invoke MethodHandles.privateLookupIn with a full-power caller
    public void testAllAccessCallerSameModule() throws Throwable {
        Lookup lookup = MethodHandles.privateLookupIn(nonPublicType, MethodHandles.lookup());
        assertTrue(lookup.lookupClass() == nonPublicType);
        assertTrue(lookup.hasFullPrivilegeAccess());
        assertTrue((lookup.lookupModes() & ORIGINAL) == 0);

        // get obj field
        MethodHandle mh = lookup.findStaticGetter(nonPublicType, "obj", Object.class);
        Object obj = mh.invokeExact();
    }

    // Invoke MethodHandles.privateLookupIn with a reduced-power caller
    @Test(expectedExceptions = {IllegalAccessException.class})
    public void testReducedAccessCallerSameModule() throws Throwable {
        Lookup caller = MethodHandles.lookup().dropLookupMode(PACKAGE);
        assertTrue((caller.lookupModes() & PRIVATE) == 0);
        assertTrue((caller.lookupModes() & PACKAGE) == 0);
        assertTrue((caller.lookupModes() & MODULE) != 0);
        assertTrue((caller.lookupModes() & ORIGINAL) == 0);

        Lookup lookup = MethodHandles.privateLookupIn(nonPublicType, caller);
    }

    // Invoke MethodHandles.privateLookupIn with the public lookup as caller
    @Test(expectedExceptions = {IllegalAccessException.class})
    public void testPublicLookupSameModule() throws Exception {
        Lookup caller = MethodHandles.publicLookup();
        Lookup lookup = MethodHandles.privateLookupIn(publicType, caller);
    }

    // test reads m1, open module m1 containing p1
    public void testTargetClassInOpenModule() throws Throwable {
        // m1/p1.Type
        Class<?> clazz = Class.forName("p1.Type");
        assertEquals(clazz.getModule().getName(), "m1");

        // ensure that this module reads m1
        Module thisModule = getClass().getModule();
        Module m1 = clazz.getModule();
        thisModule.addReads(clazz.getModule());
        assertTrue(m1.isOpen("p1", thisModule));

        Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
        assertTrue(lookup.lookupClass() == clazz);
        assertTrue((lookup.lookupModes() & PRIVATE) == PRIVATE);
        assertTrue((lookup.lookupModes() & MODULE) == 0);

        // get obj field
        MethodHandle mh = lookup.findStaticGetter(clazz, "obj", Object.class);
        Object obj = mh.invokeExact();
    }

    // test target class in unnamed module
    public void testTargetClassInUnnamedModule() throws Throwable {
        Class<?> clazz = Class.forName("Unnamed");
        assertFalse(clazz.getModule().isNamed());

        // thisModule does not read the unnamed module
        Module thisModule = getClass().getModule();
        assertFalse(thisModule.canRead(clazz.getModule()));
        try {
            MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
            assertTrue(false);
        } catch (IllegalAccessException expected) { }

        // thisModule reads the unnamed module
        thisModule.addReads(clazz.getModule());
        Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
        assertTrue(lookup.lookupClass() == clazz);
        assertTrue((lookup.lookupModes() & PRIVATE) == PRIVATE);
        assertTrue((lookup.lookupModes() & MODULE) == 0);
    }

    // test does not read m2, m2 opens p2 to test
    @Test(expectedExceptions = {IllegalAccessException.class})
    public void testCallerDoesNotRead() throws Throwable {
        // m2/p2.Type
        Class<?> clazz = Class.forName("p2.Type");
        assertEquals(clazz.getModule().getName(), "m2");

        Module thisModule = getClass().getModule();
        Module m2 = clazz.getModule();
        assertFalse(thisModule.canRead(m2));
        assertTrue(m2.isOpen("p2", thisModule));

        Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
    }

    // test reads m3, m3 does not open p3 to test
    @Test(expectedExceptions = {IllegalAccessException.class})
    public void testNotOpenToCaller() throws Throwable {
        // m3/p2.Type
        Class<?> clazz = Class.forName("p3.Type");
        assertEquals(clazz.getModule().getName(), "m3");

        Module thisModule = getClass().getModule();
        Module m3 = clazz.getModule();
        thisModule.addReads(clazz.getModule());
        assertFalse(m3.isOpen("p3", thisModule));

        Lookup lookup = MethodHandles.privateLookupIn(clazz, MethodHandles.lookup());
    }

    // Invoke MethodHandles.privateLookupIn with a primitive class
    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testPrimitiveClassAsTargetClass() throws Exception {
        MethodHandles.privateLookupIn(int.class, MethodHandles.lookup());
    }

    // Invoke MethodHandles.privateLookupIn with an array class
    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testArrayClassAsTargetClass() throws Exception {
        MethodHandles.privateLookupIn(PrivateLookupInTests[].class, MethodHandles.lookup());
    }

    // Invoke MethodHandles.privateLookupIn with a primitive array class
    @Test(expectedExceptions = {IllegalArgumentException.class})
    public void testPrimitiveArrayClassAsTargetClass() throws Exception {
        MethodHandles.privateLookupIn(int[].class, MethodHandles.lookup());
    }

    // Invoke MethodHandles.privateLookupIn with null
    @Test(expectedExceptions = {NullPointerException.class})
    public void testNullTargetClass() throws Exception {
        MethodHandles.privateLookupIn(null, MethodHandles.lookup());
    }

    // Invoke MethodHandles.privateLookupIn with null
    @Test(expectedExceptions = {NullPointerException.class})
    public void testNullCaller() throws Exception {
        MethodHandles.privateLookupIn(getClass(), null);
    }
}
