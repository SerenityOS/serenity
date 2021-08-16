/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8158123 8161906 8162713 8202832 8235229
 * @summary tests for module declarations
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main ModuleInfoTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Attributes;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.Task;

public class ModuleInfoTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        ModuleInfoTest t = new ModuleInfoTest();
        t.runTests();
    }

    /**
     * Check error message if module declaration not in module-info.java.
     */
    @Test
    public void testModuleDeclNotInModuleJava(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeFile(src.resolve("M.java"), "module M { }");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("M.java:1:1: compiler.err.module.decl.sb.in.module-info.java"))
            throw new Exception("expected output not found");
    }

    /**
     * Verify that a package private class can be put in module-info.java.
     */
    @Test
    public void testNotModuleDeclInModuleJava_1(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeFile(src.resolve("module-info.java"), "class C { }");
        new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    /**
     * Verify that a public class cannot be put in module-info.java.
     */
    @Test
    public void testNotModuleDeclInModuleJava_2(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeFile(src.resolve("module-info.java"), "public class C { }");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:8: compiler.err.class.public.should.be.in.file: kindname.class, C"))
            throw new Exception("expected output not found");
    }

    /**
     * Verify that only one module decl can be put in module-info.java.
     */
    @Test
    public void testSingleModuleDecl(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "module M1 { } /*...*/ module M2 { }");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:14: compiler.err.expected: token.end-of-input"))
            throw new Exception("expected output not found");
    }

    /**
     * Verify that missing requires are reported.
     */
    @Test
    public void testRequiresNotFound(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "module M1 { requires M2; }");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:22: compiler.err.module.not.found: M2"))
            throw new Exception("expected output not found");
    }

    /**
     * Verify that missing exports targets are reported.
     */
    @Test
    public void testExportsNotFound(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module M { exports p to N; }",
                          "package p; public class C {}");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "-Xlint:module")
                .files(findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:25: compiler.warn.module.not.found: N"))
            throw new Exception("expected output not found, actual output: " + log);
    }

    /**
     * Verify that duplicated qualified missing exports targets are reported.
     */
    @Test
    public void testExportsNotFoundDuplicated(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module M { exports p to N, N; }",
                          "package p; public class C {}");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "-Xlint:module")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:28: compiler.err.conflicting.exports.to.module: N"))
            throw new Exception("expected output not found, actual output: " + log);
    }

    /**
     * Verify that missing exports target warning can be suppressed.
     */
    @Test
    public void testExportsNotFoundSuppress(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "@SuppressWarnings(\"module\") module M { exports p to N; }",
                          "package p; public class C {}");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "-Xlint:module")
                .files(findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty())
            throw new Exception("expected output not found, actual output: " + log);
    }

    /**
     * Verify that missing opens targets are reported.
     */
    @Test
    public void testOpensNotFound(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module M { opens p to N; }",
                          "package p; public class C {}");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "-Xlint:module")
                .files(findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:23: compiler.warn.module.not.found: N"))
            throw new Exception("expected output not found, actual output: " + log);
    }

    /**
     * Verify that duplicated qualified missing opens targets are reported.
     */
    @Test
    public void testOpensNotFoundDuplicated(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "module M { opens p to N, N; }",
                          "package p; public class C {}");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "-Xlint:module")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:26: compiler.err.conflicting.opens.to.module: N"))
            throw new Exception("expected output not found, actual output: " + log);
    }

    /**
     * Verify that missing opens target warning can be suppressed.
     */
    @Test
    public void testOpensNotFoundSuppress(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "@SuppressWarnings(\"module\") module M { opens p to N; }",
                          "package p; public class C {}");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "-Xlint:module")
                .files(findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty())
            throw new Exception("expected output not found, actual output: " + log);
    }

    /**
     * Verify that a simple loop is detected.
     */
    @Test
    public void testRequiresSelf(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "module M { requires M; }");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:21: compiler.err.cyclic.requires: M"))
            throw new Exception("expected output not found");
    }

    /**
     * Verify that a multi-module loop is detected.
     */
    @Test
    public void testRequiresLoop(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeFile(src_m1.resolve("module-info.java"), "module m1x { requires m2x; }");
        Path src_m2 = src.resolve("m2x");
        tb.writeFile(src_m2.resolve("module-info.java"), "module m2x { requires m3x; }");
        Path src_m3 = src.resolve("m3x");
        tb.writeFile(src_m3.resolve("module-info.java"), "module m3x { requires m1x; }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:23: compiler.err.cyclic.requires: m3x"))
            throw new Exception("expected output not found");
    }

    /**
     * Verify that a multi-module loop is detected.
     */
    @Test
    public void testRequiresTransitiveLoop(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeFile(src_m1.resolve("module-info.java"), "module m1x { requires m2x; }");
        Path src_m2 = src.resolve("m2x");
        tb.writeFile(src_m2.resolve("module-info.java"), "module m2x { requires transitive m3x; }");
        Path src_m3 = src.resolve("m3x");
        tb.writeFile(src_m3.resolve("module-info.java"), "module m3x { requires m1x; }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:34: compiler.err.cyclic.requires: m3x"))
            throw new Exception("expected output not found");
    }

    /**
     * Verify that duplicate requires are detected.
     */
    @Test
    public void testDuplicateRequires(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeFile(src_m1.resolve("module-info.java"), "module m1x { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeFile(src_m2.resolve("module-info.java"), "module m2x { requires m1x; requires m1x; }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:37: compiler.err.duplicate.requires: m1x"))
            throw new Exception("expected output not found");
    }

    /**
     * Verify that duplicate requires are detected.
     */
    @Test
    public void testDuplicateRequiresTransitiveStatic(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeFile(src_m1.resolve("module-info.java"), "module m1x { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeFile(src_m2.resolve("module-info.java"), "module m2x { requires transitive m1x; requires static m1x; }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:55: compiler.err.duplicate.requires: m1x"))
            throw new Exception("expected output not found");
    }

    /**
     * Verify that duplicate exported packages are detected correctly.
     */
    @Test
    public void testConflictingExports_packages(Path base) throws Exception {
        verifyConflictingExports_packages(base,
                                          "exports p; exports q;",
                                          null);
        verifyConflictingExports_packages(base,
                                          "exports p; exports p;",
                                          "module-info.java:1:33: compiler.err.conflicting.exports: p");
        verifyConflictingExports_packages(base,
                                          "exports p; opens p;",
                                          null);
        verifyConflictingExports_packages(base,
                                          "exports p; exports p to m2x;",
                                          "module-info.java:1:33: compiler.err.conflicting.exports: p");
        verifyConflictingExports_packages(base,
                                          "exports p; opens p to m2x;",
                                          null);
        verifyConflictingExports_packages(base,
                                          "opens p; exports p;",
                                          null);
        verifyConflictingExports_packages(base,
                                          "opens p; opens p;",
                                          "module-info.java:1:29: compiler.err.conflicting.opens: p");
        verifyConflictingExports_packages(base,
                                          "opens p; exports p to m2x;",
                                          null);
        verifyConflictingExports_packages(base,
                                          "opens p; opens p to m2x;",
                                          "module-info.java:1:29: compiler.err.conflicting.opens: p");
        verifyConflictingExports_packages(base,
                                          "exports p to m2x; exports p;",
                                          "module-info.java:1:40: compiler.err.conflicting.exports: p");
        verifyConflictingExports_packages(base,
                                          "exports p to m2x; opens p;",
                                          null);
        verifyConflictingExports_packages(base,
                                          "exports p to m2x; exports p to m2x;",
                                          "module-info.java:1:45: compiler.err.conflicting.exports.to.module: m2x");
        verifyConflictingExports_packages(base,
                                          "exports p to m2x; opens p to m2x;",
                                          null);
        verifyConflictingExports_packages(base,
                                          "opens p to m2x; exports p;",
                                          null);
        verifyConflictingExports_packages(base,
                                          "opens p to m2x; opens p;",
                                          "module-info.java:1:36: compiler.err.conflicting.opens: p");
        verifyConflictingExports_packages(base,
                                          "opens p to m2x; exports p to m2x;",
                                          null);
        verifyConflictingExports_packages(base,
                                          "opens p to m2x; opens p to m2x;",
                                          "module-info.java:1:36: compiler.err.conflicting.opens: p");
        verifyConflictingExports_packages(base,
                                          "exports p to m2x; exports p to m3x;",
                                          "module-info.java:1:40: compiler.err.conflicting.exports: p");
        verifyConflictingExports_packages(base,
                                          "exports p to m2x; opens p to m3x;",
                                          null);
        verifyConflictingExports_packages(base,
                                          "opens p to m2x; exports p to m3x;",
                                          null);
        verifyConflictingExports_packages(base,
                                          "opens p to m2x; opens p to m3x;",
                                          "module-info.java:1:36: compiler.err.conflicting.opens: p");
    }

    private void verifyConflictingExports_packages(Path base, String code, String expected) throws Exception {
        Files.createDirectories(base);
        tb.cleanDirectory(base);

        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("m1x"),
                          "module m1x { " + code + " }",
                          "package p; public class P {}",
                          "package q; public class Q {}");
        tb.writeJavaFiles(src.resolve("m2x"),
                          "module m2x { requires m1x; }");
        tb.writeJavaFiles(src.resolve("m3x"),
                          "module m3x { requires m1x; }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(expected != null ? Task.Expect.FAIL : Task.Expect.SUCCESS)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (expected != null && !log.contains(expected))
            throw new Exception("expected output not found, actual output: " + log);
    }

    /**
     * Verify that duplicate exported packages are detected.
     */
    @Test
    public void testConflictingExports_modules(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeFile(src_m1.resolve("module-info.java"), "module m1x { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeFile(src_m2.resolve("module-info.java"), "module m2x { exports p to m1x, m1x; }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:32: compiler.err.conflicting.exports.to.module: m1x"))
            throw new Exception("expected output not found");
    }

    /**
     * Verify that annotations are not permitted at
     * any of the module names or the package names.
     */
    @Test
    public void testAnnotations(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x.sub");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String code = "module @m1.@sub { " +
                "requires @p1.@p2; " +
                "exports @p1.@p2; " +
                "exports @p1.@p2 to @m2.@sub; " +
                "exports @p1.@p2 to @m2.@sub, @m3.@sub; " +
                "uses @p1.@Interface; " +
                "provides @p1.@Interface with @p2.@Concrete; " +
                "}";
        String[] splittedCode = code.split("@");
        int length = splittedCode.length;
        String anno = "@Anno ";

        for (int i = 1; i < length; i++) {
            String preAnno = String.join("", Arrays.copyOfRange(splittedCode, 0, i));
            String postAnno = String.join("", Arrays.copyOfRange(splittedCode, i, length));
            String moduleInfo = preAnno + anno + postAnno;
            tb.writeFile(src_m1.resolve("module-info.java"), moduleInfo);

            String log = new JavacTask(tb)
                    .options("-XDrawDiagnostics", "--module-source-path", src.toString())
                    .outdir(classes)
                    .files(findJavaFiles(src))
                    .run(Task.Expect.FAIL)
                    .writeAll()
                    .getOutput(Task.OutputKind.DIRECT);

            String expect_prefix = "(?s)^module\\-info\\.java:\\d+:\\d+: ";
            String expect_message = "compiler\\.err\\.expected: token\\.identifier";
            String expect_suffix = ".*";
            String expect = expect_prefix + expect_message + expect_suffix;
            if (!log.matches(expect))
                throw new Exception("expected output not found for: " + moduleInfo + "; actual: " + log);
        }
    }

    @Test
    public void testMalformedModuleNames(Path base) throws Exception {
        testMalformedName(base, "m1.package", "module-info.java:1:11: compiler.err.expected: token.identifier");
        testMalformedName(base, "m1/package", "module-info.java:1:10: compiler.err.expected: '{'");
        testMalformedName(base, "m1->long", "module-info.java:1:10: compiler.err.expected: '{'");
        testMalformedName(base, "m1::long", "module-info.java:1:10: compiler.err.expected: '{'");
        testMalformedName(base, "m1&long", "module-info.java:1:10: compiler.err.expected: '{'");
        testMalformedName(base, "m1%long", "module-info.java:1:10: compiler.err.expected: '{'");
        testMalformedName(base, "m1@long", "module-info.java:1:10: compiler.err.expected: '{'");
        testMalformedName(base, "@m1", "module-info.java:1:7: compiler.err.expected: token.identifier");
        testMalformedName(base, "!", "module-info.java:1:7: compiler.err.expected: token.identifier");
        testMalformedName(base, "m1#long", "module-info.java:1:10: compiler.err.illegal.char: #");
        testMalformedName(base, "m1\\long", "module-info.java:1:10: compiler.err.illegal.char: \\");
        testMalformedName(base, "module.", "module-info.java:1:15: compiler.err.expected: token.identifier");
        testMalformedName(base, ".module", "module-info.java:1:7: compiler.err.expected: token.identifier");
        testMalformedName(base, "1module", "module-info.java:1:7: compiler.err.expected: token.identifier");
        testMalformedName(base, "module module", "module-info.java:1:14: compiler.err.expected: '{'");
    }

    private void testMalformedName(Path base, String name, String expected) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1");
        tb.writeJavaFiles(src_m1, "module " + name + " { }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics", "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains(expected))
            throw new Exception("expected output not found. Name: " + name + " Expected: " + expected);
    }

    @Test
    public void testWrongOpensTransitiveFlag(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "module M { opens transitive p1; }",
                "package p1; public class A { }");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:28: compiler.err.expected: ';'"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testWrongOpensStaticFlag(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "module M { opens static p1; }",
                "package p1; public class A { }");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:17: compiler.err.expected: token.identifier"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testSeveralOpensDirectives(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "module M { opens opens p1; }",
                "package p1; public class A { }");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:23: compiler.err.expected: ';'"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testUnknownDirective(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "module M { boolean p1; }",
                "package p1; public class A { }");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:11: compiler.err.expected: '}'"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testUnknownModuleFlag(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "private module M { }",
                "package p1; public class A { }");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:9: compiler.err.mod.not.allowed.here: private"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testDirectiveOnModuleDeclaration(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "opens module M { }",
                "package p1; public class A { }");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:1: compiler.err.expected.module.or.open"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testTooOpenModule(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "open open module M { }",
                "package p1; public class A { }");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:6: compiler.err.expected.module"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testEnumAsModuleFlag(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "enum module M { }",
                "package p1; public class A { }");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:12: compiler.err.expected: '{'"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testClassInModule(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "module M { class B { } }",
                "package p1; public class A { }");
        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.contains("module-info.java:1:11: compiler.err.expected: '}'"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testJDK8202832(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("m1a"),
                          """
                              module m1a {
                                  requires m2a;
                                  requires m2b;
                              }""");
        tb.writeJavaFiles(src.resolve("m1b"),
                          """
                              module m1b {
                                  requires m2b;
                                  requires m2a;
                              }""");
        tb.writeJavaFiles(src.resolve("m2a"),
                          """
                              module m2a {
                                  requires m3;
                                  requires m1a;
                                  requires m1b;
                              }""");
        tb.writeJavaFiles(src.resolve("m2b"),
                          """
                              module m2b {
                                  requires m3;
                                  requires m1a;
                                  requires m1b;
                              }""");
        tb.writeJavaFiles(src.resolve("m3"),
                          "module m3 { }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        List<String> log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--module-source-path", src.toString())
                .outdir(classes)
                .files(src.resolve("m1a").resolve("module-info.java"),
                       src.resolve("m1b").resolve("module-info.java"),
                       src.resolve("m2a").resolve("module-info.java"),
                       src.resolve("m2b").resolve("module-info.java"),
                       src.resolve("m3").resolve("module-info.java"))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = List.of("module-info.java:2:14: compiler.err.cyclic.requires: m2b",
                                        "module-info.java:3:14: compiler.err.cyclic.requires: m2a",
                                        "module-info.java:3:14: compiler.err.cyclic.requires: m1a",
                                        "module-info.java:4:14: compiler.err.cyclic.requires: m1b",
                                        "module-info.java:2:14: compiler.err.cyclic.requires: m2a",
                                        "module-info.java:3:14: compiler.err.cyclic.requires: m2b",
                                        "module-info.java:3:14: compiler.err.cyclic.requires: m1a",
                                        "module-info.java:4:14: compiler.err.cyclic.requires: m1b",
                                        "8 errors");
        if (!expected.equals(log))
            throw new Exception("expected output not found");
    }

    @Test
    public void testMultiReleaseJarAndReleaseOption(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "package api; public class A { }");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);
        new JavacTask(tb)
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();

        Path src9 = base.resolve("src9");
        tb.writeJavaFiles(src9, "module m { exports api; }");
        Path classes9 = classes.resolve("META-INF").resolve("versions").resolve("9");
        Files.createDirectories(classes9);
        new JavacTask(tb)
                .sourcepath(src, src9)
                .outdir(classes9)
                .files(findJavaFiles(src9))
                .run()
                .writeAll();
        Path jar = base.resolve("lib.jar");
        new JarTask(tb, jar)
                .baseDir(classes)
                .files(Arrays.stream(tb.findFiles("class", classes)).map(f -> classes.relativize(f).toString()).toArray(i -> new String[i]))
                .manifest(Attributes.Name.MULTI_RELEASE + ": true\n\n")
                .run();
        Path testSrc = base.resolve("test-src");
        tb.writeJavaFiles(testSrc, "module test { requires transitive m; }", "package impl; public class I { api.A a; }");
        Path testClasses = base.resolve("test-classes");
        Files.createDirectories(testClasses);
        new JavacTask(tb)
                .options("-Werror", "--module-path", jar.toString())
                .sourcepath(testSrc)
                .outdir(testClasses)
                .files(findJavaFiles(testSrc))
                .run()
                .writeAll();
        new JavacTask(tb)
                .options("-Werror", "--module-path", jar.toString(), "--release", "9", "-doe")
                .sourcepath(testSrc)
                .outdir(testClasses)
                .files(findJavaFiles(testSrc))
                .run()
                .writeAll();
    }

}
