/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Platform;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.OutputAnalyzer;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;
import static jdk.test.lib.process.ProcessTools.*;

/**
 * @test
 * @bug 8168205
 * @summary Test the default class path if -Djava.class.path is set
 * @library /test/lib
 * @modules jdk.compiler
 *          jdk.jartool
 * @build jdk.test.lib.compiler.CompilerUtils
 * @run testng JavaClassPathTest
 */

public class JavaClassPathTest {
    private static final Path SRC_DIR = Paths.get(System.getProperty("test.src"),
                                                  "src");
    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path LIB_DIR = Paths.get("lib");
    private static final String TEST_MODULE = "m";
    private static final String TEST_MAIN = "jdk.test.Main";

    @BeforeTest
    public void setup() throws Exception {
        boolean compiled = CompilerUtils.compile(SRC_DIR.resolve(TEST_MODULE),
                                                 MODS_DIR.resolve(TEST_MODULE));
        assertTrue(compiled, "module " + TEST_MODULE + " did not compile");

        // add the class and a resource to the current working directory
        Path file = Paths.get("jdk/test/Main.class");
        Files.createDirectories(file.getParent());
        Files.copy(MODS_DIR.resolve(TEST_MODULE).resolve(file), file);

        Path res = Paths.get("jdk/test/res.properties");
        Files.createFile(res);

        ToolProvider jartool = ToolProvider.findFirst("jar").orElseThrow(
            () -> new RuntimeException("jar tool not found")
        );

        Path jarfile = LIB_DIR.resolve("m.jar");
        Files.createDirectories(LIB_DIR);
        assertTrue(jartool.run(System.out, System.err, "cfe",
                               jarfile.toString(), TEST_MAIN,
                               file.toString()) == 0);

        Path manifest = LIB_DIR.resolve("manifest");
        try (BufferedWriter writer = Files.newBufferedWriter(manifest)) {
            writer.write("CLASS-PATH: lib/m.jar");
        }
        jarfile = LIB_DIR.resolve("m1.jar");
        assertTrue(jartool.run(System.out, System.err, "cfme",
                               jarfile.toString(), manifest.toString(), TEST_MAIN,
                               file.toString()) == 0);
    }

    @DataProvider(name = "classpath")
    public Object[][] classpath() {
        return new Object[][]{
            // true indicates that class path default to current working directory
            { List.of(),                          "." },
            { List.of("-cp", ""),                 "" },
            { List.of("-cp", "."),                "." },
            { List.of("-Djava.class.path"),       "." },
            { List.of("-Djava.class.path="),      ""  },
            { List.of("-Djava.class.path=."),     "." },
        };
    }

    @Test(dataProvider = "classpath")
    public void testUnnamedModule(List<String> options, String expected)
        throws Throwable
    {
        List<String> args = new ArrayList<>(options);
        args.add(TEST_MAIN);
        args.add(Boolean.toString(true));
        args.add(expected);

        assertTrue(execute(args).getExitValue() == 0);
    }

    @DataProvider(name = "moduleAndClassPath")
    public Object[][] moduleAndClassPath() {
        return new Object[][]{
            // true indicates that class path default to current working directory
            { "",                              ""  },
            { "-Djava.class.path",             ""  },
            { "-Djava.class.path=",            ""  },
        };
    }

    @Test(dataProvider = "moduleAndClassPath")
    public void testNamedModule(String option, String expected) throws Throwable {
        List<String> args = new ArrayList<>();
        if (!option.isEmpty()) {
            args.add(option);
        }
        args.add("--module-path");
        args.add(MODS_DIR.toString());
        args.add("-m");
        args.add(TEST_MODULE + "/" + TEST_MAIN);
        // not default to CWD
        args.add(Boolean.toString(false));
        args.add(expected);


        assertTrue(execute(args).getExitValue() == 0);
    }

    @Test
    public void testClassPath() throws Throwable {
        List<String> args = new ArrayList<>();
        args.add("-Djava.class.path=.");
        args.add("--module-path");
        args.add(MODS_DIR.toString());
        args.add("-m");
        args.add(TEST_MODULE + "/" + TEST_MAIN);
        args.add(Boolean.toString(true));
        args.add(".");

        assertTrue(execute(args).getExitValue() == 0);
    }

    @Test
    public void testJAR() throws Throwable {
        String jarfile = LIB_DIR.resolve("m.jar").toString();
        List<String> args = new ArrayList<>();
        args.add("-jar");
        args.add(jarfile);
        args.add(Boolean.toString(false));
        args.add(jarfile);

        assertTrue(execute(args).getExitValue() == 0);
    }

    /*
     * Test CLASS-PATH attribute in manifest
     */
    @Test
    public void testClassPathAttribute() throws Throwable {
        String jarfile = LIB_DIR.resolve("m1.jar").toString();

        List<String> args = new ArrayList<>();
        args.add("-jar");
        args.add(jarfile);
        args.add(Boolean.toString(false));
        args.add(jarfile);

        assertTrue(execute(args).getExitValue() == 0);

        args.clear();
        args.add("-cp");
        args.add(jarfile);
        args.add(TEST_MAIN);
        args.add(Boolean.toString(false));
        args.add(jarfile);

        assertTrue(execute(args).getExitValue() == 0);
    }

    private OutputAnalyzer execute(List<String> options) throws Throwable {
        // can't use ProcessTools.createJavaProcessBuilder as it always adds -cp
        ProcessBuilder pb = new ProcessBuilder(
                Stream.concat(Stream.of(JDKToolFinder.getTestJDKTool("java")),
                              options.stream()
                                     .map(this::autoQuote))
                      .toArray(String[]::new)
        );

        Map<String,String> env = pb.environment();
        // remove CLASSPATH environment variable
        env.remove("CLASSPATH");
        return executeCommand(pb)
                    .outputTo(System.out)
                    .errorTo(System.out);
    }

    /*
     * Autoquote empty string argument on Windows
     */
    private String autoQuote(String arg) {
        if (Platform.isWindows() && arg.isEmpty()) {
            return "\"\"";
        }
        return arg;
    }
}
