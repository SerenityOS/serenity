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

/*
 * @test
 * @bug 8190987
 * @summary Test verifies that individual Package.VersionInfo elements can be
 *          supplied and retrieved even if no other elements are set.
 * @run testng PackageVersionTest
 */


import java.net.URL;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PackageVersionTest {

    @Test
    public static void testSpecTitle() {
        TestClassLoader loader = new TestClassLoader();
        Package p = loader.definePackage("testSpecTitle", "SpecTitle",
                                         null, null, null,
                                         null, null, null);
        Assert.assertEquals(p.getSpecificationTitle(), "SpecTitle",
                            "Package specification titles do not match!");
    }

    @Test
    public static void testSpecVersion() {
        TestClassLoader loader = new TestClassLoader();
        Package p = loader.definePackage("testSpecVersion", null,
                                         "1.0", null, null,
                                         null, null, null);
        Assert.assertEquals(p.getSpecificationVersion(), "1.0",
                            "Package specification versions do not match!");
    }

    @Test
    public static void testSpecVendor() {
        TestClassLoader loader = new TestClassLoader();
        Package p = loader.definePackage("testSpecVendor", null,
                                         null, "SpecVendor", null,
                                         null, null, null);
        Assert.assertEquals(p.getSpecificationVendor(), "SpecVendor",
                            "Package specification vendors do not match!");
    }

    @Test
    public static void testImplTitle() {
        TestClassLoader loader = new TestClassLoader();
        Package p = loader.definePackage("testImplTitle", null,
                                         null, null, "ImplTitle",
                                         null, null, null);
        Assert.assertEquals(p.getImplementationTitle(), "ImplTitle",
                            "Package implementation titles do not match!");
    }

    @Test
    public static void testImplVersion() {
        TestClassLoader loader = new TestClassLoader();
        Package p = loader.definePackage("testImplVersion", null,
                                         null, null, null,
                                         "1.0", null, null);
        Assert.assertEquals(p.getImplementationVersion(), "1.0",
                            "Package implementation versions do not match!");
    }

    @Test
    public static void testImplVendor() {
        TestClassLoader loader = new TestClassLoader();
        Package p = loader.definePackage("testImplVendor", null,
                                         null, null, null,
                                         null, "ImplVendor", null);
        Assert.assertEquals(p.getImplementationVendor(), "ImplVendor",
                            "Package implementation vendors do not match!");
    }
}

class TestClassLoader extends ClassLoader {
    @Override
    protected Package definePackage(String name, String specTitle,
                                    String specVersion, String specVendor,
                                    String implTitle, String implVersion,
                                    String implVendor, URL sealBase) {
        return super.definePackage(name, specTitle, specVersion, specVendor,
                                   implTitle, implVersion, implVendor, sealBase);
    }
}
