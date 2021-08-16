/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8250768
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main PreviewAutoSuppress
 */
import com.sun.tools.classfile.ClassFile;
import java.io.InputStream;
import java.nio.file.Files;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.TestRunner;
import toolbox.ToolBox;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class PreviewAutoSuppress extends TestRunner {

    protected ToolBox tb;

    PreviewAutoSuppress() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        PreviewAutoSuppress t = new PreviewAutoSuppress();
        t.runTests();
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void declarationWarning(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          """
                          package test;
                          public class Outer {
                              record R(int i) {}
                              R r;
                          }
                          """,
                          """
                          package test;
                          public class Use {
                            Outer.R r;
                          }
                          """);
        Path classes = base.resolve("classes");

        List<String> log = new JavacTask(tb, Task.Mode.CMDLINE)
                .outdir(classes)
                .options("--enable-preview",
                         "-source", String.valueOf(Runtime.version().feature()),
                         "-Xlint:preview",
                         "-XDforcePreview",
                         "-XDrawDiagnostics")
                .files(tb.findJavaFiles(src))
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected =
                List.of("Outer.java:3:5: compiler.warn.preview.feature.use.plural: (compiler.misc.feature.records)",
                        "Outer.java:3:5: compiler.warn.preview.feature.use.plural: (compiler.misc.feature.records)",
                        "2 warnings");
        if (!log.equals(expected))
            throw new Exception("expected output not found" + log);
        checkPreviewClassfile(classes.resolve("test").resolve("Outer.class"), true); //TODO: correct?
        checkPreviewClassfile(classes.resolve("test").resolve("Outer$R.class"),true);
        checkPreviewClassfile(classes.resolve("test").resolve("Use.class"),false);
    }

    @Test
    public void previewAPI(Path base) throws Exception {
        Path apiSrc = base.resolve("api-src");
        tb.writeJavaFiles(apiSrc,
                          """
                          package preview.api;
                          @jdk.internal.javac.PreviewFeature(feature=jdk.internal.javac.PreviewFeature.Feature.TEST)
                          public class Outer {
                              public void test() {}
                          }
                          """,
                          """
                          package preview.impl;
                          import preview.api.Outer;
                          public class Impl {
                              public void run() {
                                  new Outer().test();
                              }
                              public static class C extends Outer {}
                          }
                          """);
        Path apiClasses = base.resolve("api-classes");

        new JavacTask(tb, Task.Mode.CMDLINE)
                .outdir(apiClasses)
                .options("-XDforcePreview",
                         "-XDrawDiagnostics",
                         "--patch-module", "java.base=" + apiSrc.toString(),
                         "-Werror")
                .files(tb.findJavaFiles(apiSrc))
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        checkPreviewClassfile(apiClasses.resolve("preview").resolve("api").resolve("Outer.class"),
                              false);
        checkPreviewClassfile(apiClasses.resolve("preview").resolve("impl").resolve("Impl.class"),
                              false);
        checkPreviewClassfile(apiClasses.resolve("preview").resolve("impl").resolve("Impl$C.class"),
                              false);

        Path testSrc = base.resolve("test-src");
        tb.writeJavaFiles(testSrc,
                          """
                          package test;
                          import preview.api.Outer;
                          public class Use {
                              public void run() {
                                  new Outer().test();
                              }
                              public static class C extends Outer {}
                          }
                          """);
        Path testClasses = base.resolve("test-classes");
        List<String> log = new JavacTask(tb, Task.Mode.CMDLINE)
                .outdir(testClasses)
                .options("--patch-module", "java.base=" + apiClasses.toString(),
                         "--add-exports", "java.base/preview.api=ALL-UNNAMED",
                         "-XDrawDiagnostics")
                .files(tb.findJavaFiles(testSrc))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected =
                List.of("Use.java:2:19: compiler.err.is.preview: preview.api.Outer",
                        "Use.java:7:35: compiler.err.is.preview: preview.api.Outer",
                        "Use.java:5:13: compiler.err.is.preview: preview.api.Outer",
                        "3 errors");

        if (!log.equals(expected))
            throw new Exception("expected output not found" + log);

        log = new JavacTask(tb, Task.Mode.CMDLINE)
                .outdir(testClasses)
                .options("--patch-module", "java.base=" + apiClasses.toString(),
                         "--add-exports", "java.base/preview.api=ALL-UNNAMED",
                         "--enable-preview",
                         "-Xlint:preview",
                         "-source", String.valueOf(Runtime.version().feature()),
                         "-XDrawDiagnostics")
                .files(tb.findJavaFiles(testSrc))
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        expected =
                List.of("Use.java:5:13: compiler.warn.is.preview: preview.api.Outer",
                        "Use.java:7:35: compiler.warn.is.preview: preview.api.Outer",
                        "2 warnings");

        if (!log.equals(expected))
            throw new Exception("expected output not found" + log);

        checkPreviewClassfile(testClasses.resolve("test").resolve("Use.class"),
                              true);
        checkPreviewClassfile(testClasses.resolve("test").resolve("Use$C.class"),
                              true);
    }

    private void checkPreviewClassfile(Path p, boolean preview) throws Exception {
        try (InputStream in = Files.newInputStream(p)) {
            ClassFile cf = ClassFile.read(in);
            if (preview && cf.minor_version != 65535) {
                throw new IllegalStateException("Expected preview class, but got: " + cf.minor_version);
            } else if (!preview && cf.minor_version != 0) {
                throw new IllegalStateException("Expected minor version == 0 but got: " + cf.minor_version);
            }
        }
    }
}
