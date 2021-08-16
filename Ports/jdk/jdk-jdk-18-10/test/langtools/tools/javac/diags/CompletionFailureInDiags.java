/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8198315
 * @summary Verify that diagnostics can be printed without throwing CompletionFailures
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.TestRunner toolbox.ToolBox CompletionFailureInDiags
 * @run main CompletionFailureInDiags
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

import toolbox.JavacTask;
import toolbox.Task.Expect;
import toolbox.Task.OutputKind;
import toolbox.TestRunner;
import toolbox.TestRunner.Test;
import toolbox.ToolBox;

public class CompletionFailureInDiags extends TestRunner {

    public static void main(String... args) throws Exception {
        new CompletionFailureInDiags().runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    private final ToolBox tb = new ToolBox();

    public CompletionFailureInDiags() {
        super(System.err);
    }

    @Test
    public void testCrashInDiagReport(Path outerBase) throws Exception {
        int c = 0;

        for (Bound bound : Bound.values()) {
            for (AdditionalParams params : AdditionalParams.values()) {
                for (Delete delete : Delete.values()) {
                    doRunCrashTest(outerBase.resolve(String.valueOf(c++)), bound, params, delete);
                }
            }
        }
    }

    private enum Bound {
        A("A"),
        AA2("A&A2");
        private final String bound;

        private Bound(String bound) {
            this.bound = bound;
        }

    }

    private enum AdditionalParams {
        NONE(new String[0], null),
        RAD_DIAGS(new String[] {"-XDrawDiagnostics"},
                  List.of("Test.java:3:10: compiler.err.cant.apply.symbol: kindname.method, " +
                                                                          "f, " +
                                                                          "java.lang.String, " +
                                                                          "compiler.misc.type.null,compiler.misc.type.null, " +
                                                                          "kindname.class, " +
                                                                          "D<T>, " +
                                                                          "(compiler.misc.arg.length.mismatch)",
                        "1 error"));
        private final String[] options;
        private final List<String> expected;

        private AdditionalParams(String[] options, List<String> expected) {
            this.options = options;
            this.expected = expected;
        }

    }

    private enum Delete {
        NONE(),
        A("A.class"),
        AA2("A.class", "A2.class");
        private final String[] toDelete;

        private Delete(String... toDelete) {
            this.toDelete = toDelete;
        }

    }

    private void doRunCrashTest(Path outerBase, Bound bound, AdditionalParams params, Delete delete) throws Exception {
        Path libSrc = outerBase.resolve("lib-src");
        tb.writeJavaFiles(libSrc,
                          "public class A {}",
                          "public interface A2 {}",
                          "public class B extends A implements A2 {}",
                          "public class C extends D<B> {}\n",
                          "public class D<T extends " + bound.bound + "> {\n" +
                          "    public void f(String s) {}\n" +
                          "}");
        Path lib = outerBase.resolve("lib");
        Files.createDirectories(lib);
        new JavacTask(tb)
                .outdir(lib.toString())
                .files(tb.findJavaFiles(libSrc))
                .run()
                .writeAll();
        for (String del : delete.toDelete) {
            Files.delete(lib.resolve(del));
        }
        Path src = outerBase.resolve("src");
        tb.writeJavaFiles(src,
                          "public class Test {\n" +
                          "    public void test(C c) {\n" +
                          "        c.f(null, null);\n" +
                          "    }\n" +
                          "}");
        Path classes = outerBase.resolve("classes");
        Files.createDirectories(classes);
        List<String> actual = new JavacTask(tb)
                .options(params.options)
                .classpath(lib)
                .outdir(classes.toString())
                .sourcepath()
                .files(tb.findJavaFiles(src))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        if (params.expected != null && !Objects.equals(params.expected, actual)) {
            throw new AssertionError("Unexpected output!");
        }
    }

}
