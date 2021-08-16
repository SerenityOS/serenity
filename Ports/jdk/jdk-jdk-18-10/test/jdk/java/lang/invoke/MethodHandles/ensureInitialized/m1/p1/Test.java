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
package p1;

import p1.internal.*;
import static java.lang.invoke.MethodHandles.Lookup.*;

import java.lang.invoke.MethodHandles;

/*
 * Test::test is invoked by Main class
 */
public class Test {
    public static void test() throws Exception {
        clinitInvokeEnsureClassInitialized();
        testModuleAccess();
        testPackageAccess();
        testFullPrivilegeAccess();
    }

    /*
     * A::<clinit> calls Lookup::ensureClassInitialized(A.class)
     */
    public static void clinitInvokeEnsureClassInitialized() throws Exception {
        assertFalse(Helper.isInitialized(A.class));

        // same package and public type
        MethodHandles.lookup().ensureInitialized(A.class);
        assertTrue(Helper.isInitialized(A.class));
    }

    /*
     * Test Lookup with MODULE access to initialize class in this module
     * and another module it reads.
     */
    public static void testModuleAccess() throws Exception {
        MethodHandles.Lookup lookup = MethodHandles.lookup().dropLookupMode(PACKAGE);
        assertTrue((lookup.lookupModes() & PACKAGE) == 0);

        assertFalse(Helper.isInitialized(p2.T.class));
        // exported by another module
        lookup.ensureInitialized(p2.T.class);
        assertTrue(Helper.isInitialized(p2.T.class));

        // non-exported type in another module
        Class<?> x = Class.forName(p2.T.class.getModule(), "p2.internal.X");
        assertFalse(Helper.isInitialized(x));
        try {
            lookup.ensureInitialized(x);
            throw new AssertionError(lookup + " should not have access to " + x);
        } catch (IllegalAccessException e) {}

        // no access to package-private class in the same module
        try {
            lookup.ensureInitialized(E.class);
            throw new AssertionError(lookup + " should not have access to " + E.class);
        } catch (IllegalAccessException e) {}
    }

    /*
     * Test Lookup with PACKAGE access
     */
    public static void testPackageAccess() throws Exception {
        MethodHandles.Lookup lookup = MethodHandles.lookup().dropLookupMode(PRIVATE);
        assertTrue((lookup.lookupModes() & PRIVATE) == 0);
        // verify these classes are not initialized
        assertFalse(Helper.isInitialized(B.class));
        assertFalse(Helper.isInitialized(C.class));
        assertFalse(Helper.isInitialized(E.class));

        // same package but package-private
        lookup.ensureInitialized(B.class);
        assertTrue(Helper.isInitialized(B.class));

        // different package and public type
        lookup.ensureInitialized(C.class);
        assertTrue(Helper.isInitialized(C.class));

        // different package and non-public types
        Class<?> d = Class.forName(Test.class.getModule(), "p1.internal.D");
        assertFalse(Helper.isInitialized(d));
        try {
            lookup.ensureInitialized(d);
            throw new AssertionError(lookup + " should not have access to " + d);
        } catch (IllegalAccessException e) {}
    }

    /*
     * Test Lookup with full privilege access
     */
    public static void testFullPrivilegeAccess() throws Exception {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        // these classes have been initialized
        assertTrue(Helper.isInitialized(A.class)); // same package and public type
        assertTrue(Helper.isInitialized(B.class)); // same package but package-private
        assertTrue(Helper.isInitialized(C.class)); // different package and public type

        // verify access to these classes
        lookup.ensureInitialized(A.class);
        lookup.ensureInitialized(B.class);
        lookup.ensureInitialized(C.class);

        // different package and non-public types
        Class<?> d = Class.forName(Test.class.getModule(), "p1.internal.D");
        try {
            assertFalse(Helper.isInitialized(d));
            lookup.ensureInitialized(d);
            throw new AssertionError(lookup + " should not have access to " + d);
        } catch (IllegalAccessException e) {}

        // nestmate and same package
        assertFalse(Helper.isInitialized(E.class));
        assertTrue(Test.class.isNestmateOf(E.class));
        lookup.ensureInitialized(E.class);
    }

    public static void assertTrue(boolean v) {
        if (!v) {
            throw new AssertionError("unexpected result");
        }
    }

    public static void assertFalse(boolean v) {
        if (v) {
            throw new AssertionError("unexpected result");
        }
    }

    // nestmate
    static class E {
    }
}
