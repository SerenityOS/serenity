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

/*
 * @test
 * @bug 8170859
 * @summary Basic test for incubator modules in jmods and images
 * @library /test/lib
 * @key intermittent
 * @modules jdk.compiler jdk.jartool jdk.jlink
 * @build jdk.test.lib.Platform
 *        jdk.test.lib.util.FileUtils
 *        jdk.test.lib.compiler.CompilerUtils
 * @run testng/othervm ImageModules
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.util.FileUtils;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.nio.charset.StandardCharsets.UTF_8;
import static jdk.test.lib.process.ProcessTools.executeCommand;
import static org.testng.Assert.*;

public class ImageModules {
    private static final String JAVA_HOME = System.getProperty("java.home");
    private static final Path JDK_JMODS = Paths.get(JAVA_HOME, "jmods");

    private static final Path TEST_SRC = Paths.get(System.getProperty("test.src"));
    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path CP_DIR = Paths.get("cp");
    private static final Path JARS_DIR = Paths.get("jars");
    private static final Path JMODS_DIR = Paths.get("jmods");
    private static final Path IMAGE = Paths.get("image");

    private static final String JAVA_BASE = "java.base";
    private final String[] modules = new String[] { "message.writer",
                                                    "message.converter" };

    @BeforeTest
    private void setup() throws Throwable {
        Path src = TEST_SRC.resolve("src");
        for (String name : modules) {
            assertTrue(CompilerUtils.compile(src.resolve(name),
                                             MODS_DIR,
                                             "--module-source-path", src.toString()));
        }

        assertTrue(CompilerUtils.compile(src.resolve("cp"),
                                         CP_DIR,
                                         "--module-path", MODS_DIR.toString(),
                                         "--add-modules", "message.writer"));
    }

    @DataProvider(name = "singleModule")
    public Object[][] singleModuleValues() throws IOException {
        Object[][] values = new Object[][]{
         // { Extra args to the build the message.converter jmod
         //   Tokens to pass to the run time --add-modules option
         //   SUCCESS or FAILURE expected
         //   Messages expected in the run time output
         //   Messages that must not appear in the run time output },
            { "",
              List.of("ALL-DEFAULT", "ALL-SYSTEM"),
              ToolResult.ASSERT_SUCCESS,
              List.of("hello world", "message.converter", "java.base"),
              List.of("WARNING") },
            { "--do-not-resolve-by-default",
              List.of("ALL-DEFAULT"),
              ToolResult.ASSERT_FAILURE,
              List.of("java.base", "java.lang.ClassNotFoundException: converter.MessageConverter"),
              List.of("WARNING", "message.converter") },
            { "--warn-if-resolved=incubating",
              List.of("ALL-DEFAULT", "ALL-SYSTEM"),
              ToolResult.ASSERT_SUCCESS,
              List.of("hello world", "message.converter", "java.base",
                      "WARNING: Using incubator modules: message.converter"),
              List.of() },
            { "--do-not-resolve-by-default --warn-if-resolved=incubating",
              List.of("ALL-DEFAULT"),
              ToolResult.ASSERT_FAILURE,
              List.of("java.base", "java.lang.ClassNotFoundException: converter.MessageConverter"),
              List.of("WARNING", "message.converter") },
            { "--do-not-resolve-by-default --warn-if-resolved=incubating",
              List.of("message.converter"),
              ToolResult.ASSERT_SUCCESS,
              List.of("hello world", "message.converter", "java.base", "WARNING"),
              List.of() }
        };
        return values;
    }

    @Test(dataProvider = "singleModule")
    public void singleModule(String extraJmodArg,
                             List<String> addModsTokens,
                             Consumer<ToolResult> assertExitCode,
                             List<String> expectedOutput,
                             List<String> unexpectedOutput)
        throws Throwable
    {
        if (Files.notExists(JDK_JMODS)) {
            System.out.println("JDK jmods not found test cannot run.");
            return;
        }

        FileUtils.deleteFileTreeUnchecked(JMODS_DIR);
        FileUtils.deleteFileTreeUnchecked(IMAGE);
        Files.createDirectories(JMODS_DIR);
        Path converterJmod = JMODS_DIR.resolve("converter.jmod");

        jmod("create",
             "--class-path", MODS_DIR.resolve("message.converter").toString(),
             extraJmodArg,
             converterJmod.toString())
            .assertSuccess();

        String mpath = JDK_JMODS.toString() + File.pathSeparator + JMODS_DIR.toString();
        jlink("--module-path", mpath,
              "--add-modules", JAVA_BASE + ",message.converter",
              "--output", IMAGE.toString())
             .assertSuccess();

        for (String addModsToken : addModsTokens) {
            String[] props = new String[] {"", "-Djdk.system.module.finder.disabledFastPath"};
            for (String systemProp : props)
                java(IMAGE,
                     systemProp,
                     "--add-modules", addModsToken,
                     "-cp", CP_DIR.toString(),
                     "test.ConvertToLowerCase", "HEllo WoRlD")
                    .resultChecker(assertExitCode)
                    .resultChecker(r -> {
                        expectedOutput.forEach(e -> r.assertContains(e));
                        unexpectedOutput.forEach(e -> r.assertDoesNotContains(e));
                    });
        }
    }

    @Test
    public void singleModularJar() throws Throwable {
        FileUtils.deleteFileTreeUnchecked(JARS_DIR);
        Files.createDirectories(JARS_DIR);
        Path converterJar = JARS_DIR.resolve("converter.jar");

        jar("--create",
            "--file", converterJar.toString(),
            "--warn-if-resolved=incubating",
            "-C", MODS_DIR.resolve("message.converter").toString() , ".")
            .assertSuccess();


        java(Paths.get(JAVA_HOME),
             "--module-path", JARS_DIR.toString(),
             "--add-modules", "message.converter",
             "-cp", CP_DIR.toString(),
             "test.ConvertToLowerCase", "HEllo WoRlD")
            .assertSuccess()
            .resultChecker(r -> {
                r.assertContains("WARNING: Using incubator modules: message.converter");
            });
    }

    @DataProvider(name = "twoModules")
    public Object[][] twoModulesValues() throws IOException {
        Object[][] values = new Object[][]{
         // { Extra args to the build the message.writer jmod
         //   Extra args to the build the message.converter jmod
         //   Tokens to pass to the run time --add-modules option
         //   SUCCESS or FAILURE expected
         //   Messages expected in the run time output
         //   Messages that must not appear in the run time output },
            { "",
              "",
              List.of("ALL-DEFAULT", "ALL-SYSTEM"),
              ToolResult.ASSERT_SUCCESS,
              List.of("HELLO CHEGAR !!!", "message.writer", "message.converter", "java.base"),
              List.of() },
            { "",
              "--do-not-resolve-by-default",
              List.of("ALL-DEFAULT", "ALL-SYSTEM"),
              ToolResult.ASSERT_SUCCESS,
              List.of("HELLO CHEGAR !!!", "message.writer", "message.converter", "java.base"),
              List.of() },
            { "--do-not-resolve-by-default",
              "",
              List.of("ALL-DEFAULT"),
              ToolResult.ASSERT_FAILURE,
              List.of("java.lang.ClassNotFoundException: writer.MessageWriter", "java.base"),
              List.of("message.writer") },
            { "--do-not-resolve-by-default",
              "--do-not-resolve-by-default",
              List.of("ALL-DEFAULT"),
              ToolResult.ASSERT_FAILURE,
              List.of("java.lang.ClassNotFoundException: writer.MessageWriter", "java.base"),
              List.of("message.converter", "message.writer") },
        // now add in warnings
            { "--do-not-resolve-by-default --warn-if-resolved=incubating",
              "",
              List.of("message.writer"),
              ToolResult.ASSERT_SUCCESS,
              List.of("HELLO CHEGAR !!!", "message.writer", "message.converter", "java.base",
                      "WARNING: Using incubator modules: message.writer"),
              List.of() },
            { "",
              "--do-not-resolve-by-default --warn-if-resolved=incubating",
              List.of("message.writer"),
              ToolResult.ASSERT_SUCCESS,
              List.of("HELLO CHEGAR !!!", "message.writer", "message.converter", "java.base",
                      "WARNING: Using incubator modules: message.converter"),
              List.of() }
            };
        return values;
    }

    @Test(dataProvider = "twoModules")
    public void doNotResolveByDefaultTwoModules(String extraFirstJmodArg,
                                                String extraSecondJmodArg,
                                                List<String> addModsTokens,
                                                Consumer<ToolResult> assertExitCode,
                                                List<String> expectedOutput,
                                                List<String> unexpectedOutput)
        throws Throwable
    {
        if (Files.notExists(JDK_JMODS)) {
            System.out.println("JDK jmods not found test cannot run.");
            return;
        }

        FileUtils.deleteFileTreeUnchecked(JMODS_DIR);
        FileUtils.deleteFileTreeUnchecked(IMAGE);
        Files.createDirectories(JMODS_DIR);
        Path writerJmod = JMODS_DIR.resolve("writer.jmod");
        Path converterJmod = JMODS_DIR.resolve("converter.jmod");

        jmod("create",
             extraFirstJmodArg,
             "--class-path", MODS_DIR.resolve("message.writer").toString(),
             writerJmod.toString());

        jmod("create",
             "--class-path", MODS_DIR.resolve("message.converter").toString(),
             extraSecondJmodArg,
             converterJmod.toString())
            .assertSuccess();

        String mpath = JDK_JMODS.toString() + File.pathSeparator + JMODS_DIR.toString();
        jlink("--module-path", mpath,
              "--add-modules", JAVA_BASE + ",message.writer,message.converter",
              "--output", IMAGE.toString())
             .assertSuccess();

        for (String addModsToken : addModsTokens) {
            String[] props = new String[] {"", "-Djdk.system.module.finder.disabledFastPath"};
            for (String systemProp : props)
                java(IMAGE,
                     systemProp,
                     "--add-modules", addModsToken,
                     "-cp", CP_DIR.toString(),
                     "test.WriteUpperCase", "hello chegar !!!")
                    .resultChecker(assertExitCode)
                    .resultChecker(r -> {
                        expectedOutput.forEach(e -> r.assertContains(e));
                        unexpectedOutput.forEach(e -> r.assertDoesNotContains(e));
                    });
        }
    }

    static final ToolProvider JMOD_TOOL = ToolProvider.findFirst("jmod")
            .orElseThrow(() -> new RuntimeException("jmod tool not found"));
    static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
            .orElseThrow(() -> new RuntimeException("jar tool not found"));
    static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
            .orElseThrow(() -> new RuntimeException("jlink tool not found"));

    static ToolResult jmod(String... args) { return execTool(JMOD_TOOL, args); }

    static ToolResult jar(String... args) { return execTool(JAR_TOOL, args); }

    static ToolResult jlink(String... args) { return execTool(JLINK_TOOL, args); }

    static ToolResult java(Path image, String... opts) throws Throwable {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        String[] options = Stream.concat(Stream.of(getJava(image)),
                                         Stream.of(opts).filter(s -> !s.equals("")))
                                 .toArray(String[]::new);

        ProcessBuilder pb = new ProcessBuilder(options);
        int exitValue = executeCommand(pb).outputTo(ps)
                                          .errorTo(ps)
                                          .getExitValue();

        return new ToolResult(exitValue, new String(baos.toByteArray(), UTF_8));
    }

    static ToolResult execTool(ToolProvider tool, String... args) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        List<String> filteredArgs = Stream.of(args)
                                          .map(s -> s.split(" ")).flatMap(Stream::of)
                                          .filter(s -> !s.equals(""))
                                          .collect(Collectors.toList());
        System.out.println(tool + " " + filteredArgs);
        int ec = tool.run(ps, ps, filteredArgs.toArray(new String[] {}));
        return new ToolResult(ec, new String(baos.toByteArray(), UTF_8));
    }

    static class ToolResult {
        final int exitCode;
        final String output;

        ToolResult(int exitValue, String output) {
            this.exitCode = exitValue;
            this.output = output;
        }

        static Consumer<ToolResult> ASSERT_SUCCESS = r ->
            assertEquals(r.exitCode, 0,
                        "Expected exit code 0, got " + r.exitCode
                                + ", with output[" + r.output + "]");
        static Consumer<ToolResult> ASSERT_FAILURE = r ->
            assertNotEquals(r.exitCode, 0,
                           "Expected exit code != 0, got " + r.exitCode
                                   + ", with output[" + r.output + "]");

        ToolResult assertSuccess() { ASSERT_SUCCESS.accept(this); return this; }
        ToolResult assertFailure() { ASSERT_FAILURE.accept(this); return this; }
        ToolResult resultChecker(Consumer<ToolResult> r) { r.accept(this); return this; }

        ToolResult assertContains(String subString) {
            assertTrue(output.contains(subString),
                       "Expected to find [" + subString + "], in output ["
                            + output + "]" + "\n");
            return this;
        }
        ToolResult assertDoesNotContains(String subString) {
            assertFalse(output.contains(subString),
                       "Expected to NOT find [" + subString + "], in output ["
                           + output + "]" + "\n");
            return this;
        }
    }

    static String getJava(Path image) {
        boolean isWindows = System.getProperty("os.name").startsWith("Windows");
        Path java = image.resolve("bin").resolve(isWindows ? "java.exe" : "java");
        if (Files.notExists(java))
            throw new RuntimeException(java + " not found");
        return java.toAbsolutePath().toString();
    }
}
