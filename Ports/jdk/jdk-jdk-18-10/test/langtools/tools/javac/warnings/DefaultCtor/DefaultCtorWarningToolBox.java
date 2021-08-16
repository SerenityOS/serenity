/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8071961
 * @summary Verify expected default constructor warnings are producted or not produced
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.TestRunner
 * @run main DefaultCtorWarningToolBox
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

public class DefaultCtorWarningToolBox extends TestRunner {

    private final ToolBox tb = new ToolBox();
    private final String fileSep = System.getProperty("file.separator");

    public DefaultCtorWarningToolBox() {
        super(System.err);
    }

    public static void main(String... args) throws Exception {
        new DefaultCtorWarningToolBox().runTests();
    }

    @Test
    public void testWithAndWithOutLint(Path base) throws IOException {
        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          MOD_INFO_SRC,
                          PKG1_BAR_SRC, PKG1_FOO_SRC,
                          PKG2_BAZ_SRC, PKG2_QUUX_SRC,
                          PKG3_CORGE_SRC, PKG3_GRAULT_SRC
                          );
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;
        List<String> expected = List.of("");

        // Warning disabled, no messages expected
        log = new JavacTask(tb)
                .options("-Xlint:-missing-explicit-ctor", "-Werror")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        expected =
            List.of("Foo.java:4:8: compiler.warn.missing-explicit-ctor: pkg1.Foo, pkg1, mod",
                    "Foo.java:12:12: compiler.warn.missing-explicit-ctor: pkg1.Foo.FooNest, pkg1, mod",
                    "Foo.java:16:19: compiler.warn.missing-explicit-ctor: pkg1.Foo.StaticFooNest, pkg1, mod",
                    "Foo.java:25:15: compiler.warn.missing-explicit-ctor: pkg1.Foo.ProtectedFooNest, pkg1, mod",
                    "Foo.java:27:19: compiler.warn.missing-explicit-ctor: pkg1.Foo.ProtectedFooNest.ProtectedFooNestNest, pkg1, mod",
                    "5 warnings");

        // Warning enable,
        log = new JavacTask(tb)
            .options("-Xlint:missing-explicit-ctor", "-XDrawDiagnostics")
                .outdir(classes)
                .files(tb.findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    protected void runTests() throws Exception {
        runTests(m -> new Object[] { Paths.get(m.getName()).toAbsolutePath() });
    }

    private static final String MOD_INFO_SRC =
        """
        module mod {
            exports pkg1;
            // Do *not* export pkg2.
             exports pkg3 to java.base;
        }
        """;

    private static final String PKG1_BAR_SRC =
        """
        package pkg1;

        // Neither the top-level class nor the nested classes should generate
        // a warning since Bar is not public.

        // No explicit constructor; use a default.
        class Bar {

            // No explicit constructor; use a default.
            public class BarNest {
            }

            // No explicit constructor; use a default.
            public static class StaticBarNest {
            }

            // No explicit constructor; use a default.
            protected class ProtectedBarNest {
            }

            // Package-access classes

            // No explicit constructor; use a default.
            /*package*/ class PkgBarNest {
            }

            // No explicit constructor; use a default.
            /*package*/ static class PkgStaticBarNest {
            }
            // Private classes

            // No explicit constructor; use a default.
            private class PrvBarNest {
            }

            // No explicit constructor; use a default.
            private static class PrvStaticBarNest {
            }
        }
        """;

    private  static final String PKG1_FOO_SRC =
        """
        package pkg1;

        // No explicit constructor; use a default.
        public class Foo {

            /*
             * Of the nexted classes, only FooNest and StaticFooNest should
             * generate warnings.
             */

            // No explicit constructor; use a default.
            public class FooNest {
            }

            // No explicit constructor; use a default.
            public static class StaticFooNest {
            }

            // No explicit constructor; use a default.
            @SuppressWarnings("missing-explicit-ctor")
            public static class SuppressedStaticFooNest {
            }

            // No explicit constructor; use a default.
            protected class ProtectedFooNest {
                // No explicit constructor; use a default.
                protected class ProtectedFooNestNest {}
            }

            // Package-access classes

            // No explicit constructor; use a default.
            /*package*/ class PkgFooNest {
                // No explicit constructor; use a default.
                protected class PkgFooNestNest {}
            }

            // No explicit constructor; use a default.
            /*package*/ static class PkgStaticFooNest {
            }
            // Private classes

            // No explicit constructor; use a default.
            private class PrvFooNest {
                // No explicit constructor; use a default.
                protected class PrvFooNestNest {}
            }

            // No explicit constructor; use a default.
            private static class PrvStaticFooNest {
            }
        }
        """;

    private static final String PKG2_BAZ_SRC =
        """
        package pkg2;

        // None of these classes should generate warnings since pkg2 is not
        // exported unconditionally.

        // No explicit constructor; use a default.
        public class Baz {

            // No explicit constructor; use a default.
            public class FooNest {
            }

            // No explicit constructor; use a default.
            public static class StaticFooNest {
            }

            // Package-access classes

            // No explicit constructor; use a default.
            /*package*/ class PkgFooNest {
            }

            // No explicit constructor; use a default.
            /*package*/ static class PkgStaticFooNest {
            }
            // Private classes

            // No explicit constructor; use a default.
            private class PrvFooNest {
            }

            // No explicit constructor; use a default.
            private static class PrvStaticFooNest {
            }
        }
        """;

    private static final String PKG2_QUUX_SRC =
        """
        package pkg2;

        // Neither the top-level class nor the nested classes should generate
        // a warning since Bar is not public.

        // No explicit constructor; use a default.
        class Quux {

            // No explicit constructor; use a default.
            public class FooNest {
            }

            // No explicit constructor; use a default.
            public static class StaticFooNest {
            }

            // Package-access classes

            // No explicit constructor; use a default.
            /*package*/ class PkgFooNest {
            }

            // No explicit constructor; use a default.
            /*package*/ static class PkgStaticFooNest {
            }
            // Private classes

            // No explicit constructor; use a default.
            private class PrvFooNest {
            }

            // No explicit constructor; use a default.
            private static class PrvStaticFooNest {
            }
        }
        """;

    private static final String PKG3_CORGE_SRC =
        """
        package pkg3;

        // None of these classes should generate warnings since pkg3 is not
        // exported unconditionally.

        // No explicit constructor; use a default.
        public class Corge {

            // No explicit constructor; use a default.
            public class FooNest {
            }

            // No explicit constructor; use a default.
            public static class StaticFooNest {
            }

            // Package-access classes

            // No explicit constructor; use a default.
            /*package*/ class PkgFooNest {
            }

            // No explicit constructor; use a default.
            /*package*/ static class PkgStaticFooNest {
            }
            // Private classes

            // No explicit constructor; use a default.
            private class PrvFooNest {
            }

            // No explicit constructor; use a default.
            private static class PrvStaticFooNest {
            }
        }
        """;

    private static final String PKG3_GRAULT_SRC =
        """
        package pkg3;

        // None of these classes should generate warnings since pkg3 is not
        // exported unconditionally.

        // No explicit constructor; use a default.
        class Grault {

            // No explicit constructor; use a default.
            public class FooNest {
            }

            // No explicit constructor; use a default.
            public static class StaticFooNest {
            }

            // Package-access classes

            // No explicit constructor; use a default.
            /*package*/ class PkgFooNest {
            }

            // No explicit constructor; use a default.
            /*package*/ static class PkgStaticFooNest {
            }
            // Private classes

            // No explicit constructor; use a default.
            private class PrvFooNest {
            }

            // No explicit constructor; use a default.
            private static class PrvStaticFooNest {
            }
        }
        """;
}
