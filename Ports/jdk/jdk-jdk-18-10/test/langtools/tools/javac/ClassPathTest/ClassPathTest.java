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
 * @bug 4241229 4785453
 * @summary Test -classpath option and classpath defaults.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main ClassPathTest
 */

import java.nio.file.Paths;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

// Original test: test/tools/javac/ClassPathTest/ClassPathTest.sh
public class ClassPathTest {

    private static final String ClassPathTest1Src =
        "import pkg.*;\n" +
        "public class ClassPathTest1 {\n" +
        "    ClassPathTestAux1 x;\n" +
        "}";

    private static final String ClassPathTest2Src =
        "import pkg.*;\n" +
        "public class ClassPathTest2 {\n" +
        "    ClassPathTestAux2 x;\n" +
        "}";

    private static final String ClassPathTest3Src =
        "import pkg.*;\n" +
        "public class ClassPathTest3 {\n" +
        "    ClassPathTestAux3 x;\n" +
        "}";

    private static final String fooPkgClassPathTestAux1Src =
        "package pkg;\n" +
        "public class ClassPathTestAux1 {}";

    private static final String barPkgClassPathTestAux2Src =
        "package pkg;\n" +
        "public class ClassPathTestAux2 {}";

    private static final String pkgClassPathTestAux3Src =
        "package pkg;\n" +
        "public class ClassPathTestAux3 {}";

    public static void main(String[] args) throws Exception {
        new ClassPathTest().test();
    }

    ToolBox tb = new ToolBox();

    public void test() throws Exception {
        createOutputDirAndSourceFiles();
        checkCompileCommands();
    }

    void createOutputDirAndSourceFiles() throws Exception {
        //dirs and files creation
        tb.writeJavaFiles(Paths.get("."),
                ClassPathTest1Src,
                ClassPathTest2Src,
                ClassPathTest3Src);
        tb.writeJavaFiles(Paths.get("foo"),
                fooPkgClassPathTestAux1Src);
        tb.writeJavaFiles(Paths.get("bar"),
                barPkgClassPathTestAux2Src);
        tb.writeJavaFiles(Paths.get("."),
                pkgClassPathTestAux3Src);
    }

    void checkCompileCommands() throws Exception {
//        Without the -cp . parameter the command will fail seems like when called
//        from the command line, the current dir is added to the classpath
//        automatically but this is not happening when called using ProcessBuilder

//        testJavac success ClassPathTest3.java
        new JavacTask(tb, Task.Mode.EXEC)
                .classpath(".")
                .files("ClassPathTest3.java")
                .run();

//        testJavac failure ClassPathTest1.java
        new JavacTask(tb, Task.Mode.EXEC)
                .classpath(".")
                .files("ClassPathTest1.java")
                .run(Task.Expect.FAIL);

//        testJavac success ClassPathTest2.java
        new JavacTask(tb, Task.Mode.EXEC)
                .envVar("CLASSPATH", "bar")
                .files("ClassPathTest2.java")
                .run();

//        testJavac failure ClassPathTest1.java
        new JavacTask(tb, Task.Mode.EXEC)
                .envVar("CLASSPATH", "bar")
                .files("ClassPathTest1.java")
                .run(Task.Expect.FAIL);

//        testJavac failure ClassPathTest3.java
        new JavacTask(tb, Task.Mode.EXEC)
                .envVar("CLASSPATH", "bar")
                .files("ClassPathTest3.java")
                .run(Task.Expect.FAIL);

//        testJavac success -classpath foo ClassPathTest1.java
        new JavacTask(tb, Task.Mode.EXEC)
                .envVar("CLASSPATH", "bar")
                .classpath("foo")
                .files("ClassPathTest1.java")
                .run();

//        testJavac failure -classpath foo ClassPathTest2.java
        new JavacTask(tb, Task.Mode.EXEC)
                .envVar("CLASSPATH", "bar")
                .classpath("foo")
                .files("ClassPathTest2.java")
                .run(Task.Expect.FAIL);

//        testJavac failure -classpath foo ClassPathTest3.java
        new JavacTask(tb, Task.Mode.EXEC)
                .envVar("CLASSPATH", "bar")
                .classpath("foo")
                .files("ClassPathTest3.java")
                .run(Task.Expect.FAIL);
    }

}
