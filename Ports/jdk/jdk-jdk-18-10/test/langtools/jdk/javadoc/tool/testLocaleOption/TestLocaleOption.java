/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8222793 8238437
 * @summary Javadoc tool ignores "-locale" param and uses default locale for
 *          all messages and texts
 * @library /tools/lib
 * @modules jdk.javadoc/jdk.javadoc.internal.api
 *          jdk.javadoc/jdk.javadoc.internal.tool
 *          jdk.javadoc/jdk.javadoc.internal.tool.resources:open
 *          jdk.javadoc/jdk.javadoc.internal.doclets.toolkit.resources:open
 *          jdk.javadoc/jdk.javadoc.internal.doclets.formats.html.resources:open
 * @build   toolbox.JavadocTask toolbox.ToolBox
 * @run main TestLocaleOption
 */

import java.io.File;
import java.io.Writer;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.ResourceBundle;
import java.nio.file.Files;
import java.nio.file.Path;

import jdk.javadoc.internal.tool.Main;

import toolbox.JavadocTask;
import toolbox.Task;
import toolbox.TestRunner;
import toolbox.ToolBox;

/**
 * Tests the {@code -locale} option.
 *
 * The test generates a set of resources files for javadoc and the doclet
 * that can be used to "simulate" a non-default locale. These resource files
 * have to be patched into the {@code jdk.javadoc} module, which means that
 * the tool must be run in a separate VM, meaning that we cannot use the
 * standard {@code JavadocTester} framework. Instead, we fall back on ToolBox.
 */

public class TestLocaleOption extends TestRunner {
    public static void main(String... args) throws Exception {
        Locale.setDefault(Locale.US);
        TestLocaleOption t = new TestLocaleOption();
        t.run();
    }

    private ToolBox tb = new ToolBox();
    private Path patchDir;
    private Path srcDir;

    /**
     * Locale for the generated resource files with uppercase values.
     */
    private static final Locale ALLCAPS = Locale.forLanguageTag("en-GB-ALLCAPS");

    TestLocaleOption() {
        super(System.err);
    }

    public void run() throws Exception {
        patchDir = Path.of("patch");
        generateBundle(patchDir, "jdk.javadoc.internal.tool.resources.javadoc");
        generateBundle(patchDir, "jdk.javadoc.internal.doclets.toolkit.resources.doclets");
        generateBundle(patchDir, "jdk.javadoc.internal.doclets.formats.html.resources.standard");

        srcDir = Path.of("src");
        tb.writeJavaFiles(srcDir,
                """
                    package p;
                    public class HelloWorld {
                        public static void main(String... args) {
                            System.out.println("Hello World!");
                        }
                    }
                    """);

        runTests(m -> new Object[]{Path.of(m.getName())});
    }

    @Test
    public void testHelpDefault_US(Path base) {
        testHelp(null, null);
    }

    @Test
    public void testHelpDefault_ALLCAPS(Path base) {
        testHelp(ALLCAPS, null);
    }

    @Test
    public void testHelpLocale(Path base) {
        testHelp(null, ALLCAPS);
    }

    private void testHelp(Locale defaultLocale, Locale localeOption) {
        String stdOut = javadoc(defaultLocale, localeOption, "-help")
                .writeAll()
                .getOutput(Task.OutputKind.STDOUT);

        if (Objects.equals(defaultLocale, ALLCAPS)) {
            checkContains(stdOut,
                    """
                        USAGE:
                            JAVADOC [OPTIONS] [PACKAGENAMES] [SOURCEFILES] [@FILES]""");
        } else {
            checkContains(stdOut,
                    """
                        Usage:
                            javadoc [options] [packagenames] [sourcefiles] [@files]""");
        }
    }

    @Test
    public void testHelloWorldDefault_US(Path base) throws Exception {
        testHelloWorld(base, null, null);
    }

    @Test
    public void testHelloWorldDefault_ALLCAPS(Path base) throws Exception {
        testHelloWorld(base, ALLCAPS, null);
    }

    @Test
    public void testHelloWorldLocale(Path base) throws Exception {
        testHelloWorld(base, null, ALLCAPS);
    }

    private void testHelloWorld(Path base, Locale defaultLocale, Locale localeOption) throws Exception {
        Path apiDir = base.resolve("api");
        String stdErr = javadoc(defaultLocale,
                                localeOption,
                                "-sourcepath", srcDir.toString(),
                                "-d", apiDir.toString(),
                                "p")
                .writeAll()
                .getOutput(Task.OutputKind.STDERR);

        // check console messages
        if (Objects.equals(defaultLocale, ALLCAPS)) {
            checkContains(stdErr,
                    """
                        LOADING SOURCE FILES FOR PACKAGE p...
                        CONSTRUCTING JAVADOC INFORMATION...""");
        } else {
            checkContains(stdErr,
                    """
                        Loading source files for package p...
                        Constructing Javadoc information...""");
        }

        // check generated files
        String hw = Files.readString(apiDir.resolve("p/HelloWorld.html"));
        Locale docLocale = localeOption != null ? localeOption : defaultLocale;
        if (Objects.equals(docLocale, ALLCAPS)) {
            checkContains(hw,
                    "<h2>METHOD SUMMARY</h2>",
                    """
                        <div class="table-header col-first">MODIFIER AND TYPE</div>""",
                    """
                        <div class="table-header col-second">METHOD</div>""",
                    """
                        <div class="table-header col-last">DESCRIPTION</div>""");
        } else {
            checkContains(hw,
                    "<h2>Method Summary</h2>",
                    """
                        <div class="table-header col-first">Modifier and Type</div>""",
                    """
                        <div class="table-header col-second">Method</div>""",
                    """
                        <div class="table-header col-last">Description</div>""");
        }
    }

    /**
     * Generates a copy of a resource bundle, with the values converted to uppercase.
     *
     * @param dir  the root directory in which to write the bundle
     * @param name the name of the bundle
     * @throws Exception if an error occurs
     */
    private void generateBundle(Path dir, String name) throws Exception {
        Module m = Main.class.getModule();
        ResourceBundle rb = ResourceBundle.getBundle(name, m);
        Properties p = new Properties();
        Enumeration<String> e = rb.getKeys();
        while (e.hasMoreElements()) {
            String key = e.nextElement();
            String value = rb.getString(key);
            p.put(key, value.toUpperCase(Locale.US));
        }
        String localeSuffix = ALLCAPS.toString().replace("-", "_");
        Path outPath = dir.resolve(name.replace(".", File.separator) + "_" + localeSuffix + ".properties");
        Files.createDirectories(outPath.getParent());
        try (Writer out = Files.newBufferedWriter(outPath)) {
            p.store(out, "Generated by TestLocaleOption");
            System.err.println("wrote: " + outPath);
        }
    }

    /**
     * Runs javadoc, with the specified arguments,
     * optionally specifying the default locale and locale option
     *
     * @param defaultLocale the default locale for the VM, or null if not specified
     * @param localeOption  the value for the locale option, or null if not specified
     * @param args          additional command-line args
     * @return the task result
     */
    private Task.Result javadoc(Locale defaultLocale, Locale localeOption, String... args) {
        List<String> options = new ArrayList<>();
        options.add("-J--patch-module=jdk.javadoc=" + patchDir);
        if (defaultLocale != null) {
            options.add("-J-Duser.language=" + defaultLocale.getLanguage());
            options.add("-J-Duser.country=" + defaultLocale.getCountry());
            options.add("-J-Duser.variant=" + defaultLocale.getVariant());
        }
        if (localeOption != null) {
            options.addAll(List.of("-locale", localeOption.toString()));
        }
        options.addAll(List.of(args));
        System.err.println("Options: " + options);

        return new JavadocTask(tb, Task.Mode.EXEC)
                .options(options)
                .run();
    }

    private String NL = System.lineSeparator();
    private void checkContains(String found, String... expect) {
        for (String e : expect) {
            String e2 = e.replace("\n", NL);
            if (!found.contains(e2)) {
                error("expected string not found: '" + e2 + "'");
            }
        }
    }
}
