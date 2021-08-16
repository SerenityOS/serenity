/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4098712 6304984 6388453
 * @summary check that source files inside zip files on the class path are ignored
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask
 * @run main JavaZipTest
 */

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

// Original test: test/tools/javac/javazip/Test.sh
public class JavaZipTest {

    private static final String ASrc =
        "class A {\n" +
        "    B b;\n" +
        "}";

    private static final String BGoodSrc =
        "public class B {}";

    private static final String BBadSrc =
        "class B";

    private static final String[][] jarArgs = {
        {"cf", "good.jar", "-C", "good", "B.java"},
        {"cf", "good.zip", "-C", "good", "B.java"},
        {"cf", "bad.jar", "-C", "bad", "B.java"},
        {"cf", "bad.zip", "-C", "bad", "B.java"},
    };

    private static final String[][] successfulCompilationArgs = {
        {"-d", "output", "A.java", "good/B.java"},
        {"-d", "output", "-cp", "good", "A.java"},
        {"-d", "output", "-sourcepath", "good", "A.java"},
        {"-d", "output", "-cp", "good.zip", "A.java"},
        {"-d", "output", "-cp", "good.jar", "A.java"},
    };

    private static final String[][] unsuccessfulCompilationArgs = {
        {"-d", "output", "A.java", "bad/B.java"},
        {"-d", "output", "-cp", "bad", "A.java"},
        {"-d", "output", "-sourcepath", "bad", "A.java"},
        {"-d", "output", "-sourcepath", "bad.zip", "A.java"},
        {"-d", "output", "-sourcepath", "bad.jar", "A.java"},
    };

    public static void main(String[] args) throws Exception {
        new JavaZipTest().test();
    }

    private final ToolBox tb = new ToolBox();

    public void test() throws Exception {
        createOutputDirAndSourceFiles();
        createZipsAndJars();
        check(Task.Expect.SUCCESS, successfulCompilationArgs);
        check(Task.Expect.FAIL, unsuccessfulCompilationArgs);
    }

    void createOutputDirAndSourceFiles() throws Exception {
        //create output dir
        Files.createDirectory(Paths.get("output"));

        //source file creation
        tb.writeJavaFiles(Paths.get("good"), BGoodSrc);
        tb.writeJavaFiles(Paths.get("bad"), BBadSrc);
        tb.writeJavaFiles(ToolBox.currDir, ASrc);
    }

    void createZipsAndJars() throws Exception {
        //jar and zip creation
        for (String[] args: jarArgs) {
            new JarTask(tb).run(args).writeAll();
        }
    }

    void check(Task.Expect expectedStatus, String[][] theArgs) throws Exception {


        for (String[] allArgs: theArgs) {
            new JavacTask(tb)
                    .options(opts(allArgs))
                    .files(files(allArgs))
                    .run(expectedStatus)
                    .writeAll();

        }
    }

    private String[] opts(String... allArgs) {
        int i = allArgs.length;
        while (allArgs[i - 1].endsWith(".java"))
            i--;
        return Arrays.copyOfRange(allArgs, 0, i);
    }

    private String[] files(String... allArgs) {
        int i = allArgs.length;
        while (allArgs[i - 1].endsWith(".java"))
            i--;
        return Arrays.copyOfRange(allArgs, i, allArgs.length);
    }

}
