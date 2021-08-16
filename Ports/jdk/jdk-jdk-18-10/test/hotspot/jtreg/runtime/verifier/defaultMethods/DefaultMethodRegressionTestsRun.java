/*
 * Copyright (c) 2013, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8003639
 * @summary defaultMethod resolution and verification using an URLClassLoader
 * @modules jdk.compiler
 *          jdk.zipfs
 * @compile -XDignore.symbol.file=true DefaultMethodRegressionTestsRun.java
 * @run main DefaultMethodRegressionTestsRun
 */
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
/**
 * This test is a variant of DefaultMethodRegressionTests, this one creates
 * an URLClassLoader to load the support classes.
 *
 */
public class DefaultMethodRegressionTestsRun {
    public static void main(String... args) throws Exception {
        File scratchDir = new File(".");
        File testDir = new File(scratchDir, "testdir");
        testDir.mkdirs();
        File srcFile = new File(new File(System.getProperty("test.src")),
                "DefaultMethodRegressionTests.java");
        String[] javacargs = {
            srcFile.getAbsolutePath(),
            "-d",
            testDir.getAbsolutePath()
        };
        com.sun.tools.javac.Main.compile(javacargs);
        runClass(testDir, "DefaultMethodRegressionTests");
    }
    static void runClass(
            File classPath,
            String classname) throws Exception {
        URL[] urls = {classPath.toURI().toURL()};
        ClassLoader loader = new URLClassLoader(urls);
        Class<?> c = loader.loadClass(classname);

        Class<?>[] argTypes = new Class<?>[]{String[].class};
        Object[] methodArgs = new Object[]{null};

        Method method = c.getMethod("main", argTypes);
        method.invoke(c, methodArgs);
    }
}
