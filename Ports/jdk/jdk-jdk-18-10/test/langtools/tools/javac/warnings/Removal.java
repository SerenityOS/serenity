/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8145471
 * @summary javac changes for enhanced deprecation
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 * @modules jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.JavacTask toolbox.TestRunner toolbox.ToolBox
 * @run main Removal
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import toolbox.JavacTask;
import toolbox.Task.Expect;
import toolbox.Task.OutputKind;
import toolbox.TestRunner;
import toolbox.ToolBox;

/*
 * From JEP 277, JDK-8085614
 *
 *        use site     |      API declaration site
 *        context      | not dep.   ord. dep.   term. dep.
 *                     +----------------------------------
 *        not dep.     |    N          W           W
 *                     |
 *        ord. dep.    |    N          N (2)       W (4)
 *                     |
 *        term. dep.   |    N          N (3)       W (5)
 */

public class Removal extends TestRunner {
    public static void main(String... args) throws Exception {
        Removal r = new Removal();
        r.runTests(m -> new Object[] { Paths.get(m.getName()) });
        r.report();
    }

    private final ToolBox tb = new ToolBox();
    private final Path libSrc = Paths.get("lib").resolve("src");
    private final Path libClasses = Paths.get("lib").resolve("classes");
    int testCount = 0;

    /**
     * Options that may be used during compilation.
     */
    enum Options {
        DEFAULT(),
        XLINT_DEPRECATED("-Xlint:deprecation"),
        XLINT_NO_REMOVAL("-Xlint:-removal");

        Options(String... opts) {
            this.opts = Arrays.asList(opts);
        }

        final List<String> opts;
    }

    /**
     * The kind of deprecation.
     */
    enum DeprKind {
        NONE("", null),
        DEPRECATED("@Deprecated ", "compiler.warn.has.been.deprecated"),
        REMOVAL("@Deprecated(forRemoval=true) ", "compiler.warn.has.been.deprecated.for.removal");
        DeprKind(String anno, String warn) {
            this.anno = anno;
            this.warn = warn;
        }
        final String anno;
        final String warn;
    }

    final String[] lib = {
        "package lib; public class Class {\n"
        + "    public static void method() { }\n"
        + "    @Deprecated public static void depMethod() { }\n"
        + "    @Deprecated(forRemoval=true) public static void remMethod() { }\n"
        + "    public static int field;\n"
        + "    @Deprecated public static int depField;\n"
        + "    @Deprecated(forRemoval=true) public static int remField;\n"
        + "}",
        "package lib; @Deprecated public class DepClass { }",
        "package lib; @Deprecated(forRemoval=true) public class RemClass { }"
    };

    /**
     * The kind of declaration to be referenced at the use site.
     */
    enum RefKind {
        CLASS("lib.%s c;", "Class", "DepClass", "RemClass"),
        METHOD("{ lib.Class.%s(); }", "method", "depMethod", "remMethod"),
        FIELD("int i = lib.Class.%s;", "field", "depField", "remField");

        RefKind(String template, String def, String dep, String rem) {
            fragments.put(DeprKind.NONE, String.format(template, def));
            fragments.put(DeprKind.DEPRECATED, String.format(template, dep));
            fragments.put(DeprKind.REMOVAL, String.format(template, rem));
        }

        String getFragment(DeprKind k) {
            return fragments.get(k);
        }

        private final Map<DeprKind, String> fragments = new EnumMap<>(DeprKind.class);
    }

    /**
     * Get source code for a reference to a possibly-deprecated item declared in a library.
     * @param refKind the kind of element (class, method, field) being referenced
     * @param declDeprKind the kind of deprecation on the declaration of the item being referenced
     * @param useDeprKind the kind of deprecation enclosing the use site
     * @return
     */
    static String getSource(RefKind refKind, DeprKind declDeprKind, DeprKind useDeprKind) {
        return "package p; "
                + useDeprKind.anno
                + "class Class { "
                + refKind.getFragment(declDeprKind)
                + " }";
    }

    private static final String NO_OUTPUT = null;

    public Removal() throws IOException {
        super(System.err);
        initLib();
    }

    void initLib() throws IOException {
        tb.writeJavaFiles(libSrc, lib);

        new JavacTask(tb)
                .outdir(Files.createDirectories(libClasses))
                .files(tb.findJavaFiles(libSrc))
                .run()
                .writeAll();
    }

    void report() {
        out.println(testCount + " test cases");
    }

    /*
     * Declaration site: not deprecated; use site: not deprecated
     * Options: default
     * Expect: no warnings
     */
    @Test
    public void test_DeclNone_UseNone(Path base) throws IOException {
        for (RefKind rk : RefKind.values()) {
            test(base,
                    getSource(rk, DeprKind.NONE, DeprKind.NONE),
                    Options.DEFAULT,
                    NO_OUTPUT);
        }
    }

    /*
     * Declaration site: not deprecated; use site: deprecated
     * Options: default
     * Expect: no warnings
     */
    @Test
    public void test_DeclNone_UseDeprecated(Path base) throws IOException {
        for (RefKind rk : RefKind.values()) {
            test(base,
                    getSource(rk, DeprKind.NONE, DeprKind.DEPRECATED),
                    Options.DEFAULT,
                    NO_OUTPUT);
        }
    }

    /*
     * Declaration site: not deprecated; use site: deprecated for removal
     * Options: default
     * Expect: no warnings
     */
    @Test
    public void test_DeclNone_UseRemoval(Path base) throws IOException {
        for (RefKind rk : RefKind.values()) {
            test(base,
                    getSource(rk, DeprKind.NONE, DeprKind.REMOVAL),
                    Options.DEFAULT,
                    NO_OUTPUT);
        }
    }

    /*
     * Declaration site: deprecated; use site: not deprecated
     * Options: default
     * Expect: deprecated note
     */
    @Test
    public void test_DeclDeprecated_UseNone_Default(Path base) throws IOException {
        for (RefKind rk : RefKind.values()) {
            test(base,
                    getSource(rk, DeprKind.DEPRECATED, DeprKind.NONE),
                    Options.DEFAULT,
                    "compiler.note.deprecated.filename: Class.java");
        }
    }

    /*
     * Declaration site: deprecated; use site: not deprecated
     * Options: -Xlint:deprecation
     * Expect: deprecated warning
     */
    @Test
    public void test_DeclDeprecated_UseNone_XlintDep(Path base) throws IOException {
        for (RefKind rk : RefKind.values()) {
            String error = "<unset>";
            switch (rk) {
                case CLASS:
                    error = "Class.java:1:29: compiler.warn.has.been.deprecated: lib.DepClass, lib";
                    break;

                case METHOD:
                    error = "Class.java:1:37: compiler.warn.has.been.deprecated: depMethod(), lib.Class";
                    break;

                case FIELD:
                    error = "Class.java:1:43: compiler.warn.has.been.deprecated: depField, lib.Class";
                    break;
            }

            test(base,
                    getSource(rk, DeprKind.DEPRECATED, DeprKind.NONE),
                    Options.XLINT_DEPRECATED,
                    error);
        }
    }

    /*
     * Declaration site: deprecated; use site: deprecated
     * Options: default
     * Expect: no warnings
     */
    @Test
    public void test_DeclDeprecated_UseDeprecated(Path base) throws IOException {
        for (RefKind rk : RefKind.values()) {
            test(base,
                    getSource(rk, DeprKind.DEPRECATED, DeprKind.DEPRECATED),
                    Options.DEFAULT,
                    NO_OUTPUT);
        }
    }

    /*
     * Declaration site: deprecated; use site: deprecated for removal
     * Options: default
     * Expect: no warnings
     */
    @Test
    public void test_DeclDeprecated_UseRemoval(Path base) throws IOException {
        for (RefKind rk : RefKind.values()) {
            test(base,
                    getSource(rk, DeprKind.DEPRECATED, DeprKind.REMOVAL),
                    Options.DEFAULT,
                    NO_OUTPUT);
        }
    }

    /*
     * Declaration site: deprecated for removal; use site: not deprecated
     * Options: default
     * Expect: removal warning
     */
    @Test
    public void test_DeclRemoval_UseNone_Default(Path base) throws IOException {
        for (RefKind rk : RefKind.values()) {
            String error = "<unset>";
            switch (rk) {
                case CLASS:
                    error = "Class.java:1:29: compiler.warn.has.been.deprecated.for.removal: lib.RemClass, lib";
                    break;

                case METHOD:
                    error = "Class.java:1:37: compiler.warn.has.been.deprecated.for.removal: remMethod(), lib.Class";
                    break;

                case FIELD:
                    error = "Class.java:1:43: compiler.warn.has.been.deprecated.for.removal: remField, lib.Class";
                    break;
            }

            test(base,
                    getSource(rk, DeprKind.REMOVAL, DeprKind.NONE),
                    Options.DEFAULT,
                    error);
        }
    }

    /*
     * Declaration site: deprecated for removal; use site: not deprecated
     * Options: default, @SuppressWarnings("removal")
     * Expect: removal warning
     */
    @Test
    public void test_DeclRemoval_UseNone_SuppressRemoval(Path base) throws IOException {
        for (RefKind rk : RefKind.values()) {
            String source =
                    getSource(rk, DeprKind.REMOVAL, DeprKind.NONE)
                    .replace("class Class", "@SuppressWarnings(\"removal\") class Class");

            test(base,
                    source,
                    Options.DEFAULT,
                    null);
        }
    }

    /*
     * Declaration site: deprecated for removal; use site: not deprecated
     * Options: -Xlint:-removal
     * Expect: removal note
     */
    @Test
    public void test_DeclRemoval_UseNone_XlintNoRemoval(Path base) throws IOException {
        for (RefKind rk : RefKind.values()) {
            test(base,
                    getSource(rk, DeprKind.REMOVAL, DeprKind.NONE),
                    Options.XLINT_NO_REMOVAL,
                    "compiler.note.removal.filename: Class.java");
        }
    }

    /*
     * Declaration site: deprecated for removal; use site: deprecated
     * Options: default
     * Expect: removal warning
     */
    @Test
    public void test_DeclRemoval_UseDeprecated_Default(Path base) throws IOException {
        for (RefKind rk : RefKind.values()) {
            String error = "<unset>";
            switch (rk) {
                case CLASS:
                    error = "Class.java:1:41: compiler.warn.has.been.deprecated.for.removal: lib.RemClass, lib";
                    break;

                case METHOD:
                    error = "Class.java:1:49: compiler.warn.has.been.deprecated.for.removal: remMethod(), lib.Class";
                    break;

                case FIELD:
                    error = "Class.java:1:55: compiler.warn.has.been.deprecated.for.removal: remField, lib.Class";
                    break;
            }

            test(base,
                    getSource(rk, DeprKind.REMOVAL, DeprKind.DEPRECATED),
                    Options.DEFAULT,
                    error);
        }
    }

    /*
     * Declaration site: deprecated for removal; use site: deprecated
     * Options: -Xlint:-removal
     * Expect: removal note
     */
    @Test
    public void test_DeclRemoval_UseDeprecated_XlintNoRemoval(Path base) throws IOException {
        for (RefKind rk : RefKind.values()) {
            test(base,
                    getSource(rk, DeprKind.REMOVAL, DeprKind.DEPRECATED),
                    Options.XLINT_NO_REMOVAL,
                    "compiler.note.removal.filename: Class.java");
        }
    }

    /*
     * Declaration site: deprecated for removal; use site: deprecated for removal
     * Options: default
     * Expect: removal warning
     */
    @Test
    public void test_DeclRemoval_UseRemoval_Default(Path base) throws IOException {
        for (RefKind rk : RefKind.values()) {
            String error = "<unset>";
            switch (rk) {
                case CLASS:
                    error = "Class.java:1:58: compiler.warn.has.been.deprecated.for.removal: lib.RemClass, lib";
                    break;

                case METHOD:
                    error = "Class.java:1:66: compiler.warn.has.been.deprecated.for.removal: remMethod(), lib.Class";
                    break;

                case FIELD:
                    error = "Class.java:1:72: compiler.warn.has.been.deprecated.for.removal: remField, lib.Class";
                    break;
            }

            test(base,
                    getSource(rk, DeprKind.REMOVAL, DeprKind.REMOVAL),
                    Options.DEFAULT,
                    error);
        }
    }

    /*
     * Declaration site: deprecated for removal; use site: deprecated for removal
     * Options: -Xlint:-removal
     * Expect: removal note
     */
    @Test
    public void test_DeclRemoval_UseRemoval_XlintNoRemoval(Path base) throws IOException {
        for (RefKind rk : RefKind.values()) {
            test(base,
                    getSource(rk, DeprKind.REMOVAL, DeprKind.REMOVAL),
                    Options.XLINT_NO_REMOVAL,
                    "compiler.note.removal.filename: Class.java");
        }
    }

    /*
     * Additional special case:
     * there should not be any warnings for any reference in a type-import statement.
     */
    @Test
    public void test_UseImports(Path base) throws IOException {
        String source =
                "import lib.Class;\n"
                + "import lib.DepClass;\n"
                + "import lib.RemClass;\n"
                + "class C { }";
        for (Options o : Options.values()) {
            test(base, source, o, NO_OUTPUT);
        }
    }

    /**
     * Compile source code with given options, and check for expected output.
     * The compilation is done twice, first against the library in source form,
     * and then again, against the compiled library.
     * @param base base working directory
     * @param source the source code to be compiled
     * @param options the options for the compilation
     * @param expectText the expected output, or NO_OUTPUT, if none expected.
     * @throws IOException if an error occurs during the compilation
     */
    private void test(Path base, String source, Options options, String expectText) throws IOException {
        test(base.resolve("lib-source"), libSrc, source, options, expectText);
        test(base.resolve("lib-classes"), libClasses, source, options, expectText);
    }

    /**
     * Compile source code with given options against a given version of the library,
     * and check for expected output.
     * @param base base working directory
     * @param lib the directory containing the library, in either source or compiled form
     * @param source the source code to be compiled
     * @param options the options for the compilation
     * @param expectText the expected output, or NO_OUTPUT, if none expected.
     * @throws IOException if an error occurs during the compilation
     */
    private void test(Path base, Path lib, String source, Options options, String expectText)
            throws IOException {
        Expect expect = (expectText != null && expectText.contains("compiler.warn.")) ? Expect.FAIL : Expect.SUCCESS;
        test(base, lib, source, options.opts, expect, expectText);
    }

    /**
     * Compile source code with given options against a given version of the library,
     * and check for expected exit code and expected output.
     * @param base base working directory
     * @param lib the directory containing the library, in either source or compiled form
     * @param source the source code to be compiled
     * @param options the options for the compilation
     * @param expect the expected outcome of the compilation
     * @param expectText the expected output, or NO_OUTPUT, if none expected.
     * @throws IOException if an error occurs during the compilation
     */
    private void test(Path base, Path lib, String source, List<String> options,
            Expect expect, String expectText) throws IOException {
        testCount++;

        Path src = base.resolve("src");
        Path classes = Files.createDirectories(base.resolve("classes"));
        tb.writeJavaFiles(src, source);

        List<String> allOptions = new ArrayList<>();
        allOptions.add("-XDrawDiagnostics");
        allOptions.add("-Werror");
        allOptions.addAll(options);

        out.println("Source: " + source);
        out.println("Classpath: " + lib);
        out.println("Options: " + options.stream().collect(Collectors.joining(" ")));

        String log = new JavacTask(tb)
                .outdir(classes)
                .classpath(lib) // use classpath for libSrc or libClasses
                .files(tb.findJavaFiles(src))
                .options(allOptions.toArray(new String[0]))
                .run(expect)
                .writeAll()
                .getOutput(OutputKind.DIRECT);

        if (expectText == null) {
            if (!log.trim().isEmpty())
                error("Unexpected text found: >>>" + log + "<<<");
        } else {
            if (!log.contains(expectText))
                error("expected text not found: >>>" + expectText + "<<<");
        }
    }
}

