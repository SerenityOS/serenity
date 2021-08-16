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
 * @bug 4491755 4785453
 * @summary Prob w/static inner class with same name as a regular class
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main InnerClassFileTest
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import toolbox.JavacTask;
import toolbox.ToolBox;

// Original test: test/tools/javac/innerClassFile/Driver.sh
public class InnerClassFileTest {

    private static final String BSrc =
        "package x;\n" +
        "\n" +
        "import x.*;\n" +
        "\n" +
        "public class B {\n" +
        "    public static class C {}\n" +
        "}";

    private static final String CSrc =
        "package x;\n" +
        "\n" +
        "import x.*;\n" +
        "\n" +
        "public class C {}";

    private static final String MainSrc =
        "package y;\n" +
        "\n" +
        "class Main {\n" +
        "        private R1 a;\n" +
        "        private R2 b;\n" +
        "        private R3 c;\n" +
        "}";

    private static final String R1Src =
        "package y;\n" +
        "\n" +
        "public final class R1 {\n" +
        "    x.B.C a = null;\n" +
        "    x.C b = null;\n" +
        "    R2 c = new R2();\n" +
        "}";

    private static final String R2Src =
        "package y;\n" +
        "\n" +
        "public final class R2 {\n" +
        "    x.B.C a = null;\n" +
        "    x.C b = null;\n" +
        "}";

    private static final String R3Src =
        "package y;\n" +
        "\n" +
        "public final class R3 {\n" +
        "    x.B.C a = null;\n" +
        "    x.C b = null;\n" +
        "    R1 c = new R1();\n" +
        "}";

    public static void main(String args[]) throws Exception {
        new InnerClassFileTest().run();
    }

    private final ToolBox tb = new ToolBox();

    void run() throws Exception {
        createFiles();
        compileFiles();
    }

    void createFiles() throws Exception {
        Path srcDir = Paths.get("src");
        tb.writeJavaFiles(srcDir, BSrc, CSrc, MainSrc, R1Src, R2Src, R3Src);
    }

    void compileFiles() throws Exception {
        new JavacTask(tb)
                .outdir(".")
                .classpath(".")
                .sourcepath("src")
                .files("src/x/B.java", "src/x/C.java", "src/y/Main.java")
                .run()
                .writeAll();

        tb.deleteFiles("y/R3.class");

        new JavacTask(tb)
                .outdir(".")
                .classpath(".")
                .sourcepath("src")
                .files("src/y/Main.java")
                .run()
                .writeAll();

    }

}
