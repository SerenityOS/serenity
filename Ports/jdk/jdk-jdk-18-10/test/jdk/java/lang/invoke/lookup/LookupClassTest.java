/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8173975
 * @summary Lookup::in throws IAE if the target class is a primitive class or array class
 * @run testng/othervm LookupClassTest
 */

import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class LookupClassTest {
    private static final LookupClassTest[] ARRAY = new LookupClassTest[0];
    @BeforeTest
    public void test() {
        assertTrue(ARRAY.getClass().isArray());
        assertSamePackage(MethodHandles.lookup(), ARRAY.getClass());
        assertSamePackage(MethodHandles.publicLookup(), int.class);
    }

    private void assertSamePackage(Lookup lookup, Class<?> targetClass) {
        assertEquals(lookup.lookupClass().getPackageName(), targetClass.getPackageName());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void arrayLookupClass() {
        Lookup lookup = MethodHandles.lookup();
        lookup.in(ARRAY.getClass());
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void primitiveLookupClass() {
        Lookup lookup = MethodHandles.publicLookup();
        lookup.in(int.class);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void voidLookupClass() {
        Lookup lookup = MethodHandles.publicLookup();
        lookup.in(void.class);
    }
}
