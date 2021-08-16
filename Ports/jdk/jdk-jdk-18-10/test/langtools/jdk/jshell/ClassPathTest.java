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
 * @summary Tests for EvalState#addToClasspath
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask
 * @build KullaTesting TestingInputStream Compiler
 * @run testng ClassPathTest
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import org.testng.annotations.Test;

@Test
public class ClassPathTest extends KullaTesting {

    private final Compiler compiler = new Compiler();
    private final Path outDir = Paths.get("class_path_test");

    public void testDirectory() {
        compiler.compile(outDir, "package pkg; public class TestDirectory { }");
        assertDeclareFail("import pkg.TestDirectory;", "compiler.err.doesnt.exist");
        assertDeclareFail("new pkg.TestDirectory();", "compiler.err.doesnt.exist");
        addToClasspath(compiler.getPath(outDir));
        assertEval("new pkg.TestDirectory();");
    }

    public void testJar() {
        compiler.compile(outDir, "package pkg; public class TestJar { }");
        String jarName = "test.jar";
        compiler.jar(outDir, jarName, "pkg/TestJar.class");
        assertDeclareFail("import pkg.TestJar;", "compiler.err.doesnt.exist");
        assertDeclareFail("new pkg.TestJar();", "compiler.err.doesnt.exist");
        addToClasspath(compiler.getPath(outDir).resolve(jarName));
        assertEval("new pkg.TestJar();");
    }

    public void testAmbiguousDirectory() {
        Path p1 = outDir.resolve("dir1");
        compiler.compile(p1,
                "package p; public class TestAmbiguous {\n" +
                "   public String toString() {\n" +
                "       return \"first\";" +
                "   }\n" +
                "}");
        addToClasspath(compiler.getPath(p1));
        Path p2 = outDir.resolve("dir2");
        compiler.compile(p2,
                "package p; public class TestAmbiguous {\n" +
                "   public String toString() {\n" +
                "       return \"second\";" +
                "   }\n" +
                "}");
        addToClasspath(compiler.getPath(p2));
        assertEval("new p.TestAmbiguous();", "first");
    }

    public void testAmbiguousJar() {
        Path p1 = outDir.resolve("dir1");
        compiler.compile(p1,
                "package p; public class TestAmbiguous {\n" +
                "   public String toString() {\n" +
                "       return \"first\";" +
                "   }\n" +
                "}");
        String jarName = "test.jar";
        compiler.jar(p1, jarName, "p/TestAmbiguous.class");
        addToClasspath(compiler.getPath(p1.resolve(jarName)));
        Path p2 = outDir.resolve("dir2");
        compiler.compile(p2,
                "package p; public class TestAmbiguous {\n" +
                "   public String toString() {\n" +
                "       return \"second\";" +
                "   }\n" +
                "}");
        addToClasspath(compiler.getPath(p2));
        assertEval("new p.TestAmbiguous();", "first");
    }

    public void testEmptyClassPath() {
        addToClasspath("");
        assertEval("new java.util.ArrayList<String>();");
    }

    public void testUnknown() {
        addToClasspath(compiler.getPath(outDir.resolve("UNKNOWN")));
        assertDeclareFail("new Unknown();", "compiler.err.cant.resolve.location");
        addToClasspath(compiler.getPath(outDir.resolve("UNKNOWN.jar")));
        assertDeclareFail("new Unknown();", "compiler.err.cant.resolve.location");
    }
}
