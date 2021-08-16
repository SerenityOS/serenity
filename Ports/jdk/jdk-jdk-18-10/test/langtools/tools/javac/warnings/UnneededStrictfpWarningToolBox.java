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
 * @bug 8244146
 * @summary Verify expected strictfp warnings are producted or not produced
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.TestRunner
 * @run main UnneededStrictfpWarningToolBox
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import java.io.InputStream;
import java.nio.file.Files;
import java.util.EnumSet;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Expect;
import toolbox.TestRunner;
import toolbox.ToolBox;

public class UnneededStrictfpWarningToolBox extends TestRunner {

    private final ToolBox tb = new ToolBox();
    private final String fileSep = System.getProperty("file.separator");

    public UnneededStrictfpWarningToolBox() {
        super(System.err);
    }

    public static void main(String... args) throws Exception {
        new UnneededStrictfpWarningToolBox().runTests();
    }

    private void checkLog(List<String> log, List<String> expected) {
        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    private void checkEmptyLog(List<String> log) {
        checkLog(log, List.of(""));
    }

    @Test
    public void testWithAndWithOutLint(Path base) throws IOException {
        Path src = base.resolve("src");

        tb.writeJavaFiles(src, UNNEEDED_STRICTFP_WARNING1_NO_SUPPRESSION);
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;

        // Warning not enabled, no messages expected
        log = new JavacTask(tb)
                .options("--release", "16", "-Werror")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        checkEmptyLog(log);

        // Warning not enabled, no messages expected
        log = new JavacTask(tb)
                .options("-Xlint:-strictfp", "-Werror")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        checkEmptyLog(log);

        // Warning enabled, 5 messages expected
        log = new JavacTask(tb)
            .options("-XDrawDiagnostics")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        var expected = List.of("UnneededStrictfpWarning1.java:1:17: compiler.warn.strictfp",
                               "UnneededStrictfpWarning1.java:10:10: compiler.warn.strictfp",
                               "UnneededStrictfpWarning1.java:12:29: compiler.warn.strictfp",
                               "UnneededStrictfpWarning1.java:16:28: compiler.warn.strictfp",
                               "UnneededStrictfpWarning1.java:18:21: compiler.warn.strictfp",
                               "5 warnings");
        checkLog(log, expected);
    }

    @Test
    public void testTopLevelSuppression(Path base) throws IOException {
        Path src = base.resolve("src");

        tb.writeJavaFiles(src, UNNEEDED_STRICTFP_WARNING2_TOP_LEVEL_SUPPRESSION);
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;

        // Warning implicitly enabled, no messages expected
        log = new JavacTask(tb)
                .options("-Werror")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        checkEmptyLog(log);

        // Warning explicitly enabled, no messages expected
        log = new JavacTask(tb)
                .options("-Xlint:strictfp", "-Werror")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        checkEmptyLog(log);
    }

    @Test
    public void testInnerSuppression(Path base) throws IOException {
        Path src = base.resolve("src");

        tb.writeJavaFiles(src, UNNEEDED_STRICTFP_WARNING3_INNER_SUPPRESSION);
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        // Warning enabled, 3 messages expected
        List<String> log = new JavacTask(tb)
            .options("-XDrawDiagnostics")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        var expected = List.of("UnneededStrictfpWarning3.java:1:17: compiler.warn.strictfp",
                               "UnneededStrictfpWarning3.java:10:10: compiler.warn.strictfp",
                               "UnneededStrictfpWarning3.java:17:28: compiler.warn.strictfp",
                               "3 warnings");
        checkLog(log, expected);
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()).toAbsolutePath() });
    }

    // If warnings are enabled, should generate 5 warnings for the 5
    // explicit uses of strictfp.
    private static final String UNNEEDED_STRICTFP_WARNING1_NO_SUPPRESSION =
        """
        public strictfp class UnneededStrictfpWarning1 {
            // Implicit strictfp, no warning
            public UnneededStrictfpWarning1() {super();}

            public static void main(String... args) {
                return;
            }
        }

        strictfp interface StrictfpInterface {
            default double foo() {return 42.0;};
            default strictfp double bar() {return 21.0;};
        }

        class StrictfpMethod {
            static strictfp double quux() {return 42.0;}

            static strictfp class NestedStrictfpClass {}
        }
        """;

    // If warnings are enabled, no warnings should be generated due to
    // suppression at the top-most level.
    private static final String UNNEEDED_STRICTFP_WARNING2_TOP_LEVEL_SUPPRESSION =
        """
        @SuppressWarnings("strictfp")
        public strictfp class UnneededStrictfpWarning2 {
            // Implicit strictfp, no warning
            public UnneededStrictfpWarning2() {super();}

            public static void main(String... args) {
                return;
            }
        }

        @SuppressWarnings("strictfp")
        strictfp interface StrictfpInterface {
            default double foo() {return 42.0;};
            default strictfp double bar() {return 21.0;};
        }

        @SuppressWarnings("strictfp")
        class StrictfpMethod {
            static strictfp double quux() {return 42.0;}

            static strictfp class NestedStrictfpClass {}
        }
        """;

    // If warnings are enabled, 3 warnings should be generated.
    private static final String UNNEEDED_STRICTFP_WARNING3_INNER_SUPPRESSION =
        """
        public strictfp class UnneededStrictfpWarning3 {
            // Implicit strictfp, no warning
            public UnneededStrictfpWarning3() {super();}

            public static void main(String... args) {
                return;
            }
        }

        strictfp interface StrictfpInterface {
            default double foo() {return 42.0;};
            @SuppressWarnings("strictfp")
            default strictfp double bar() {return 21.0;};
        }

        class StrictfpMethod {
            static strictfp double quux() {return 42.0;}

            @SuppressWarnings("strictfp")
            static strictfp class NestedStrictfpClass {}
        }
        """;
}
