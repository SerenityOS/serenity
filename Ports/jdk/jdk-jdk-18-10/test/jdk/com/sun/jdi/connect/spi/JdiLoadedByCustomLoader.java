/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5055681
 * @summary Test loading JDI classes via custom class loader
 *
 * @library /test/lib
 * @build JdiLoadedByCustomLoader
 * @run main/othervm JdiLoadedByCustomLoader
 */

/*
 * Creates a URLClassLoader from a file URL. The file URL
 * is constructed from the given argument. Once created the test
 * attempts to load another test case (ListConnectors)
 * using the class loader and then it invokes the list()
 * method.
 */
import jdk.test.lib.Utils;
import jdk.test.lib.compiler.CompilerUtils;

import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;

public class JdiLoadedByCustomLoader {

    public static void main(String args[]) throws Exception {
        // Compile the list connectors class into a directory that isn't on
        // any class path.
        Path src = Paths.get(Utils.TEST_SRC).toAbsolutePath().resolve("ListConnectors.java");
        Path newClassDir = Paths.get(Utils.TEST_CLASSES).toAbsolutePath().resolve("someotherdir");
        if (!CompilerUtils.compile(src, newClassDir, false, "-classpath", Utils.TEST_CLASSES)) {
            throw new RuntimeException("failed to compile " + src);
        }

        test(newClassDir.toString());
    }

    private static void test(String classPath) throws Exception {
        // create files from given arguments and tools.jar
        File f1 = new File(classPath);

        // create class loader
        URL[] urls = { f1.toURL() };
        URLClassLoader cl = new URLClassLoader(urls);

        // load ListConnectors using the class loader
        // and then invoke the list method.
        Class c = Class.forName("ListConnectors", true, cl);
        Method m = c.getDeclaredMethod("list");
        Object o = c.newInstance();
        m.invoke(o);
    }
}
