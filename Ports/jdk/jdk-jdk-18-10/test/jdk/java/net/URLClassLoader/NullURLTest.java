/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7179567
 * @summary Test that URLClassLoader public constructors and factory methods
 * throw NullPointerException when appropriate.
 *
 * Tests whether URLClassLoader public constructors and factory methods throw
 * appropriate NullPointerExceptions for 1) a null URL array parameter, and
 * 2) a non-null URL array containing a null element.
 */

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

public class NullURLTest {
    JarFile jarFile;

    public static void main(String[] args) throws Throwable {
        new NullURLTest();
    }

    NullURLTest() throws Throwable {
        File local = new File(System.getProperty("test.src", "."), "jars");
        String path = "jar:file:"
                + local.getPath()
                + "/class_path_test.jar!/Foo.class";

        URL validURL = new URL(path);
        URL[] validURLArray = new URL[] { validURL, validURL };
        URL[] invalidURLArray = new URL[] { validURL, null };

        int failures = 0;
        URLClassLoader loader;

        try {
            loader = new URLClassLoader(validURLArray);
        } catch (Throwable t) {
            System.err.println("URLClassLoader(validURLArray) threw " + t);
            failures++;
        }
        try {
            loader = new URLClassLoader(null);
            System.err.println("URLClassLoader(null) did not throw NPE");
            failures++;
        } catch (NullPointerException e) {
            // expected
        }

        try {
            loader = new URLClassLoader(invalidURLArray);
            System.err.println("URLClassLoader(invalidURLArray) did not throw NPE");
            failures++;
        } catch (NullPointerException e) {
            // expected
        }

        try {
            loader = new URLClassLoader(validURLArray, null);
        } catch (Throwable t) {
            System.err.println("URLClassLoader(validURLArray, null) threw " + t);
            failures++;
        }
        try {
            loader = new URLClassLoader(null, null);
            System.err.println("URLClassLoader(null, null) did not throw NPE");
            failures++;
        } catch (NullPointerException e) {
            // expected
        }

        try {
            loader = new URLClassLoader(invalidURLArray, null);
            System.err.println("URLClassLoader(invalidURLArray, null) did not throw NPE");
            failures++;
        } catch (NullPointerException e) {
            // expected
        }

        try {
            loader = new URLClassLoader(validURLArray, null, null);
        } catch (Throwable t) {
            System.err.println("URLClassLoader(validURLArray, null, null) threw " + t);
            failures++;
        }
        try {
            loader = new URLClassLoader((URL[])null, null, null);
            System.err.println("URLClassLoader(null, null, null) did not throw NPE");
            failures++;
        } catch (NullPointerException e) {
            // expected
        }

        try {
            loader = new URLClassLoader(invalidURLArray, null, null);
            System.err.println("URLClassLoader(invalidURLArray, null, null) did not throw NPE");
            failures++;
        } catch (NullPointerException e) {
            // expected
        }

        try {
            loader = URLClassLoader.newInstance(validURLArray);
        } catch (Throwable t) {
            System.err.println("URLClassLoader.newInstance(validURLArray) threw " + t);
            failures++;
        }
        try {
            loader = URLClassLoader.newInstance(null);
            System.err.println("URLClassLoader.newInstance(null) did not throw NPE");
            failures++;
        } catch (NullPointerException e) {
            // expected
        }

        try {
            loader = URLClassLoader.newInstance(invalidURLArray);
            System.err.println("URLClassLoader.newInstance(invalidURLArray) did not throw NPE");
            failures++;
        } catch (NullPointerException e) {
            // expected
        }

        try {
            loader = URLClassLoader.newInstance(validURLArray, null);
        } catch (Throwable t) {
            System.err.println("URLClassLoader.newInstance(validURLArray, null) threw " + t);
            failures++;
        }
        try {
            loader = URLClassLoader.newInstance(null, null);
            System.err.println("URLClassLoader.newInstance(null, null) did not throw NPE");
            failures++;
        } catch (NullPointerException e) {
            // expected
        }

        try {
            loader = URLClassLoader.newInstance(invalidURLArray, null);
            System.err.println("URLClassLoader.newInstance(invalidURLArray, null) did not throw NPE");
            failures++;
        } catch (NullPointerException e) {
            // expected
        }

        if (failures != 0) {
            throw new Exception("URLClassLoader NullURLTest had "+failures+" failures!");
        }
    }
}
