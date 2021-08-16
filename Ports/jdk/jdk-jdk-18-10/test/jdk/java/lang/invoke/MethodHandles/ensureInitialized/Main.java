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

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8235521
 * @summary Tests for Lookup::ensureClassInitialized
 * @build java.base/* m1/* m2/* Main
 * @run testng/othervm --add-modules m1 Main
 */

public class Main {
    // Test access to public java.lang class
    @Test
    public void testPublic() throws Exception {
        assertFalse(Helper.isInitialized(PublicInit.class));
        MethodHandles.lookup().ensureInitialized(PublicInit.class);
        assertTrue(Helper.isInitialized(PublicInit.class));
        // no-op if already initialized
        MethodHandles.lookup().ensureInitialized(PublicInit.class);
    }

    // access denied to package-private java.lang class
    @Test(expectedExceptions = { IllegalAccessException.class })
    public void testPackagePrivate() throws Exception {
        Class<?> c = Class.forName("java.lang.DefaultInit", false, null);
        assertFalse(Helper.isInitialized(c));
        // access denied
        MethodHandles.lookup().ensureInitialized(c);
    }

    // access denied to public class in a non-exported package
    @Test(expectedExceptions = { IllegalAccessException.class })
    public void testNonExportedPackage() throws Exception {
        Class<?> c = Class.forName("jdk.internal.misc.VM", false, null);
        // access denied
        MethodHandles.lookup().ensureInitialized(c);
    }

    // invoke p1.Test::test to test module boundary access
    @Test
    public void testModuleAccess() throws Exception {
        Class<?> c = Class.forName("p1.Test");
        Method m = c.getMethod("test");
        m.invoke(null);
    }

    @Test(expectedExceptions = { IllegalArgumentException.class })
    public void testArrayType() throws Exception {
        Class<?> arrayType = PublicInit.class.arrayType();
        MethodHandles.lookup().ensureInitialized(arrayType);
    }
}
