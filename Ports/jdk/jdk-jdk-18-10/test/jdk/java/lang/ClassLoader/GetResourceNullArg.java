/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.stream.Stream;
import org.testng.annotations.*;

/*
 * @test
 * @bug 8136831
 * @summary Test null argument to ClassLoader.getResourceXXXX()
 * @run testng GetResourceNullArg
 */

public class GetResourceNullArg {
    private static class MyClassLoader extends ClassLoader {
        public MyClassLoader() {
            super(null);
        }
        @Override
        public Class findClass(String name) throws ClassNotFoundException {
            throw new ClassNotFoundException("Why are you using this?");
        }
    }

    @DataProvider
    public static ClassLoader[][] provider() {
        try {
            return new ClassLoader[][] {
                { ClassLoader.getSystemClassLoader() },
                { new MyClassLoader() },
                { new URLClassLoader(new URL[]{ Paths.get(".").toUri().toURL() },
                                     ClassLoader.getSystemClassLoader()) }
            };
        } catch (MalformedURLException e) { throw new RuntimeException(e); }
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void classGetResource() {
        this.getClass().getResource(null);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void classGetResourceAsStream() {
        this.getClass().getResourceAsStream(null);
    }

    @Test(dataProvider = "provider",
          expectedExceptions = NullPointerException.class)
    public void loaderGetResource(ClassLoader cl) {
        cl.getResource(null);
    }

    @Test(dataProvider = "provider",
          expectedExceptions = NullPointerException.class)
    public static void loaderGetResources(ClassLoader cl) throws IOException {
        Enumeration<URL> retVal = cl.getResources(null);
    }

    @Test(dataProvider = "provider",
          expectedExceptions = NullPointerException.class)
    public static void loaderResources(ClassLoader cl) throws IOException {
        Stream<URL> retVal = cl.resources(null);
    }

    @Test(dataProvider = "provider",
          expectedExceptions = NullPointerException.class)
    public void loaderGetResourceAsStream(ClassLoader cl) {
        cl.getResourceAsStream(null);
    }
}
