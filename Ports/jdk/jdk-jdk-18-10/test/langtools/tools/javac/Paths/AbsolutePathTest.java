/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8030046 8055500
 * @summary javac incorrectly handles absolute paths in manifest classpath
 * @author govereau
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @ignore 8055768 ToolBox does not close opened files
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.JarTask
 * @run main AbsolutePathTest
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.ToolBox;

public class AbsolutePathTest {
    public static void main(String... cmdline) throws Exception {
        ToolBox tb = new ToolBox();

        // compile test.Test
        new JavacTask(tb)
                .outdir(".") // this is needed to get the classfiles in test
                .sources("package test; public class Test{}")
                .run();

        // build test.jar containing test.Test
        // we need the jars in a directory different from the working
        // directory to trigger the bug.
        Files.createDirectory(Paths.get("jars"));
        new JarTask(tb, "jars/test.jar")
                .files("test/Test.class")
                .run();

        // build second jar in jars directory using
        // an absolute path reference to the first jar
        new JarTask(tb, "jars/test2.jar")
                .classpath(new File("jars/test.jar").getAbsolutePath())
                .run();

        // this should not fail
        new JavacTask(tb)
                .outdir(".")
                .classpath("jars/test2.jar")
                .sources("import test.Test; class Test2 {}")
                .run()
                .writeAll();
    }
}
