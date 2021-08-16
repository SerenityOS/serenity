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

/*
 * @test
 * @summary Basic test for Class.getPackage
 * @compile Foo.java
 * @run testng GetPackageTest
 */

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.testng.Assert.*;

public class GetPackageTest {
    private static Class<?> fooClass; // definePackage is not called for Foo class

    @BeforeTest
    public static void loadFooClass() throws ClassNotFoundException {
        TestClassLoader loader = new TestClassLoader();
        fooClass = loader.loadClass("foo.Foo");
        assertEquals(fooClass.getClassLoader(), loader);
    }

    @DataProvider(name = "testClasses")
    public Object[][] testClasses() {
        return new Object[][] {
                // primitive type, void, array types
                { int.class,            null },
                { long[].class,         null },
                { Object[][].class,     null },
                { void.class,           null },

                // unnamed package
                { GetPackageTest.class, "" },

                // named package
                { fooClass,             "foo" },
                { Object.class,         "java.lang" },
                { Properties.class,     "java.util" },
                { BigInteger.class,     "java.math" },
                { Test.class,           "org.testng.annotations" },
        };
    }

    @Test(dataProvider = "testClasses")
    public void testGetPackage(Class<?> type, String expected) {
        Package p = type.getPackage();
        if (expected == null) {
            assertTrue(p == null);
        } else {
            assertEquals(p.getName(), expected);
        }
    }

    static class TestClassLoader extends ClassLoader {
        public TestClassLoader() {
            super();
        }

        public TestClassLoader(ClassLoader parent) {
            super(parent);
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
}


