/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/* @test
 * @compile TestLookup.java TestCls.java
 * @run testng/othervm -ea -esa test.java.lang.invoke.TestLookup
 */
package test.java.lang.invoke;

import org.testng.annotations.Test;

import static java.lang.invoke.MethodHandles.*;

import static org.testng.AssertJUnit.*;

public class TestLookup {

    @Test
    public void testClassLoaderChange() {
        Lookup lookup = lookup();
        assertNotNull(lookup.lookupClass().getClassLoader());
        Lookup lookup2 = lookup.in(Object.class);
        assertNull(lookup2.lookupClass().getClassLoader());
    }

    @Test(expectedExceptions = {ClassNotFoundException.class})
    public void testPublicCannotLoadUserClass() throws IllegalAccessException, ClassNotFoundException {
        Lookup lookup = publicLookup();
        lookup.findClass("test.java.lang.invoke.TestCls");
    }

    @Test
    public void testPublicCanLoadSystemClass() throws IllegalAccessException, ClassNotFoundException {
        Lookup lookup = publicLookup();
        lookup.findClass("java.util.HashMap");
    }

    @Test
    public void testPublicInChangesClassLoader() {
        Lookup lookup = publicLookup();
        // Temporarily exclude until 8148697 is resolved, specifically:
        // "publicLookup changed so that the lookup class is in an unnamed module"
        //assertNull(lookup.lookupClass().getClassLoader());
        Lookup lookup2 = lookup.in(TestCls.class);
        assertNotNull(lookup2.lookupClass().getClassLoader());
    }

}
