/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jdk.test.lib.compiler.CompilerUtils;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * @test
 * @bug 8174826
 * @library /lib/testlibrary /test/lib
 * @modules jdk.charsets jdk.compiler jdk.jlink
 * @build SuggestProviders jdk.test.lib.compiler.CompilerUtils
 * @run testng SuggestProviders
 */

public class SuggestProviders {
    private static final String JAVA_HOME = System.getProperty("java.home");
    private static final String TEST_SRC = System.getProperty("test.src");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    private static final String MODULE_PATH =
        Paths.get(JAVA_HOME, "jmods").toString() +
        File.pathSeparator + MODS_DIR.toString();

    // the names of the modules in this test
    private static String[] modules = new String[] {"m1", "m2", "m3"};


    private static boolean hasJmods() {
        if (!Files.exists(Paths.get(JAVA_HOME, "jmods"))) {
            System.err.println("Test skipped. NO jmods directory");
            return false;
        }
        return true;
    }

    /*
     * Compiles all modules used by the test
     */
    @BeforeTest
    public void compileAll() throws Throwable {
        if (!hasJmods()) return;

        for (String mn : modules) {
            Path msrc = SRC_DIR.resolve(mn);
            assertTrue(CompilerUtils.compile(msrc, MODS_DIR,
                "--module-source-path", SRC_DIR.toString()));
        }
    }

    // check a subset of services used by java.base
    private final List<String> JAVA_BASE_USES = List.of(
        "uses java.lang.System$LoggerFinder",
        "uses java.net.ContentHandlerFactory",
        "uses java.net.spi.URLStreamHandlerProvider",
        "uses java.nio.channels.spi.AsynchronousChannelProvider",
        "uses java.nio.channels.spi.SelectorProvider",
        "uses java.nio.charset.spi.CharsetProvider",
        "uses java.nio.file.spi.FileSystemProvider",
        "uses java.nio.file.spi.FileTypeDetector",
        "uses java.security.Provider",
        "uses java.util.spi.ToolProvider"
    );

    private final List<String> JAVA_BASE_PROVIDERS = List.of(
        "java.base provides java.nio.file.spi.FileSystemProvider used by java.base"
    );

    private final List<String> SYSTEM_PROVIDERS = List.of(
        "jdk.charsets provides java.nio.charset.spi.CharsetProvider used by java.base",
        "jdk.compiler provides java.util.spi.ToolProvider used by java.base",
        "jdk.compiler provides javax.tools.JavaCompiler used by java.compiler",
        "jdk.jlink provides jdk.tools.jlink.plugin.Plugin used by jdk.jlink",
        "jdk.jlink provides java.util.spi.ToolProvider used by java.base"
    );

    private final List<String> APP_USES = List.of(
        "uses p1.S",
        "uses p2.T"
    );

    private final List<String> APP_PROVIDERS = List.of(
        "m1 provides p1.S used by m1",
        "m2 provides p1.S used by m1",
        "m2 provides p2.T used by m2",
        "m3 provides p2.T used by m2",
        "m3 provides p3.S not used by any observable module"
    );

    @Test
    public void suggestProviders() throws Throwable {
        if (!hasJmods()) return;

        List<String> output = JLink.run("--module-path", MODULE_PATH,
                                        "--suggest-providers").output();

        Stream<String> uses =
            Stream.concat(JAVA_BASE_USES.stream(), APP_USES.stream());
        Stream<String> providers =
            Stream.concat(SYSTEM_PROVIDERS.stream(), APP_PROVIDERS.stream());

        assertTrue(output.containsAll(Stream.concat(uses, providers)
                                            .collect(Collectors.toList())));
    }

    /**
     * find providers from the observable modules and --add-modules has no
     * effect on the suggested providers
     */
    @Test
    public void observableModules() throws Throwable {
        if (!hasJmods()) return;

        List<String> output = JLink.run("--module-path", MODULE_PATH,
                                        "--add-modules", "m1",
                                        "--suggest-providers").output();

        Stream<String> uses =
            Stream.concat(JAVA_BASE_USES.stream(), Stream.of("uses p1.S"));
        Stream<String> providers =
            Stream.concat(SYSTEM_PROVIDERS.stream(), APP_PROVIDERS.stream());

        assertTrue(output.containsAll(Stream.concat(uses, providers)
                                            .collect(Collectors.toList())));
    }

    /**
     * find providers from the observable modules with --limit-modules
     */
    @Test
    public void limitModules() throws Throwable {
        if (!hasJmods()) return;

        List<String> output = JLink.run("--module-path", MODULE_PATH,
                                        "--limit-modules", "m1",
                                        "--suggest-providers").output();

        Stream<String> uses =
            Stream.concat(JAVA_BASE_USES.stream(), Stream.of("uses p1.S"));
        Stream<String> providers =
            Stream.concat(JAVA_BASE_PROVIDERS.stream(),
                          Stream.of("m1 provides p1.S used by m1")
        );

        assertTrue(output.containsAll(Stream.concat(uses, providers)
                                            .collect(Collectors.toList())));
    }

    @Test
    public void providersForServices() throws Throwable {
        if (!hasJmods()) return;

        List<String> output =
            JLink.run("--module-path", MODULE_PATH,
                      "--suggest-providers",
                      "java.nio.charset.spi.CharsetProvider,p1.S").output();

        System.out.println(output);
        Stream<String> expected = Stream.concat(
            Stream.of("jdk.charsets provides java.nio.charset.spi.CharsetProvider used by java.base"),
            Stream.of("m1 provides p1.S used by m1",
                      "m2 provides p1.S used by m1")
        );

        assertTrue(output.containsAll(expected.collect(Collectors.toList())));
    }

    @Test
    public void unusedService() throws Throwable {
        if (!hasJmods()) return;

        List<String> output =
            JLink.run("--module-path", MODULE_PATH,
                      "--suggest-providers",
                      "p3.S").output();

        List<String> expected = List.of(
            "m3 provides p3.S not used by any observable module"
        );
        assertTrue(output.containsAll(expected));

        // should not print other services m3 provides
        assertFalse(output.contains("m3 provides p2.T used by m2"));
    }

    @Test
    public void nonExistentService() throws Throwable {
        if (!hasJmods()) return;

        List<String> output =
            JLink.run("--module-path", MODULE_PATH,
                      "--suggest-providers",
                      "nonExistentType").output();

        List<String> expected = List.of(
            "No provider found for service specified to --suggest-providers: nonExistentType"
        );
        assertTrue(output.containsAll(expected));
    }

    @Test
    public void noSuggestProviders() throws Throwable {
        if (!hasJmods()) return;

        List<String> output =
            JLink.run("--module-path", MODULE_PATH,
                      "--bind-services",
                      "--suggest-providers").output();

        String expected = "--bind-services option is specified. No additional providers suggested.";
        assertTrue(output.contains(expected));

    }

    @Test
    public void suggestTypeNotRealProvider() throws Throwable {
        if (!hasJmods()) return;

        List<String> output =
            JLink.run("--module-path", MODULE_PATH,
                      "--add-modules", "m1",
                      "--suggest-providers",
                      "java.util.List").output();

        System.out.println(output);
        List<String> expected = List.of(
            "No provider found for service specified to --suggest-providers: java.util.List"
        );

        assertTrue(output.containsAll(expected));
    }

    @Test
    public void addNonObservableModule() throws Throwable {
        if (!hasJmods()) return;

        List<String> output =
            JLink.run("--module-path", MODULE_PATH,
                      "--add-modules", "nonExistentModule",
                      "--suggest-providers",
                      "java.nio.charset.spi.CharsetProvider").output();

        System.out.println(output);
        List<String> expected = List.of(
            "jdk.charsets provides java.nio.charset.spi.CharsetProvider used by java.base"
        );

        assertTrue(output.containsAll(expected));
    }

    static class JLink {
        static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
            .orElseThrow(() ->
                new RuntimeException("jlink tool not found")
            );

        static JLink run(String... options) {
            JLink jlink = new JLink();
            assertTrue(jlink.execute(options) == 0);
            return jlink;
        }

        final List<String> output = new ArrayList<>();
        private int execute(String... options) {
            System.out.println("jlink " +
                Stream.of(options).collect(Collectors.joining(" ")));

            StringWriter writer = new StringWriter();
            PrintWriter pw = new PrintWriter(writer);
            int rc = JLINK_TOOL.run(pw, pw, options);
            System.out.println(writer.toString());
            Stream.of(writer.toString().split("\\v"))
                  .map(String::trim)
                  .forEach(output::add);
            return rc;
        }

        boolean contains(String s) {
            return output.contains(s);
        }

        List<String> output() {
            return output;
        }
    }
}
