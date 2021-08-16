/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test Package object is local to each ClassLoader.
 *          There can be one Package object of "foo" name defined by
 *          different class loader.
 * @compile Foo.java
 * @run testng GetPackages
 */

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.lang.reflect.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class GetPackages {
    final TestClassLoader loader;
    final Class<?> fooClass;
    /*
     * Each TestClassLoader defines a "foo.Foo" class which has a unique
     * Package object of "foo" regardless whether its ancestor class loader
     * defines a package "foo" or not.
     */
    GetPackages(TestClassLoader loader) throws ClassNotFoundException {
        this.loader = loader;
        this.fooClass = loader.loadClass("foo.Foo");
    }

    /** For TestNG */
    public GetPackages() {
        loader = null;
        fooClass = null;
    }

    /*
     * Check package "foo" defined locally in the TestClassLoader
     * as well as its ancestors.
     */
    void checkPackage() throws ClassNotFoundException {
        // Name of an unnamed package is empty string
        assertEquals(this.getClass().getPackage().getName(), "");

        assertEquals(fooClass.getClassLoader(), loader);

        Package p = loader.getDefinedPackage("foo");
        assertEquals(p.getName(), "foo");
        assertEquals(p, loader.getPackage("foo"));

        if (loader.getParent() != null) {
            Package p2 = ((TestClassLoader)loader.getParent()).getDefinedPackage("foo");
            assertTrue(p != p2);
        }

        long count = Arrays.stream(loader.getDefinedPackages())
                            .map(Package::getName)
                            .filter(pn -> pn.equals("foo"))
                            .count();
        assertEquals(count, 1);
    }

    /*
     * Check the number of package "foo" defined to this class loader and
     * its ancestors
     */
    Package[] getPackagesFromLoader() {
        return loader.packagesInClassLoaderChain();
    }

    /*
     * Package.getPackages is caller-sensitve.  Call through Foo class
     * to find all Packages visible to this TestClassLoader and its ancestors
     */
    Package[] getPackagesFromFoo() throws Exception {
        Method m = fooClass.getMethod("getPackages");
        return (Package[])m.invoke(null);
    }

    private static long numFooPackages(Package[] pkgs) throws Exception {
        return Arrays.stream(pkgs)
                     .filter(p -> p.getName().equals("foo"))
                     .count();
    }

    @DataProvider(name = "loaders")
    public static Object[][] testLoaders() {
        TestClassLoader loader1 = new TestClassLoader(null);
        TestClassLoader loader2 = new TestClassLoader(loader1);
        TestClassLoader loader3 = new TestClassLoader(loader2);

        // Verify the number of expected Package object of "foo" visible
        // to the class loader
        return new Object[][] {
                { loader1, 1 },
                { loader2, 2 },
                { loader3, 3 }
        };
    }

    @Test(dataProvider = "loaders")
    public static void test(TestClassLoader loader, long expected) throws Exception {
        GetPackages test = new GetPackages(loader);
        // check package "foo" existence
        test.checkPackage();

        assertEquals(numFooPackages(test.getPackagesFromLoader()), expected);
        assertEquals(numFooPackages(test.getPackagesFromFoo()), expected);
    }
}

class TestClassLoader extends ClassLoader {
    public TestClassLoader() {
        super();
    }

    public TestClassLoader(ClassLoader parent) {
        super(parent);
    }

    public Package getPackage(String pn) {
        return super.getPackage(pn);
    }

    public Package[] packagesInClassLoaderChain() {
        return super.getPackages();
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        Path p = Paths.get(System.getProperty("test.classes", "."));

        try {
            byte[] bb = Files.readAllBytes(p.resolve("foo/Foo.class"));
            return defineClass(name, bb, 0, bb.length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    @Override
    protected Class<?> loadClass(String cn, boolean resolve) throws ClassNotFoundException {
        if (!cn.equals("foo.Foo"))
            return super.loadClass(cn, resolve);
        return findClass(cn);
    }
}
