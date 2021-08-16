/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @modules jdk.compiler
 *          jdk.jlink
 * @build jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.util.FileUtils
 *        jdk.test.lib.Platform
 * @run testng JmodNegativeTest
 * @summary Negative tests for jmod
 */

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.spi.ToolProvider;
import java.util.zip.ZipOutputStream;
import jdk.test.lib.util.FileUtils;
import jdk.test.lib.compiler.CompilerUtils;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.io.File.pathSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertTrue;

public class JmodNegativeTest {

    static final ToolProvider JMOD_TOOL = ToolProvider.findFirst("jmod")
        .orElseThrow(() ->
            new RuntimeException("jmod tool not found")
        );

    static final String TEST_SRC = System.getProperty("test.src", ".");
    static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    static final Path EXPLODED_DIR = Paths.get("build");
    static final Path MODS_DIR = Paths.get("jmods");

    @BeforeTest
    public void buildExplodedModules() throws IOException {
        if (Files.exists(EXPLODED_DIR))
            FileUtils.deleteFileTreeWithRetry(EXPLODED_DIR);

        for (String name : new String[] { "foo"/*, "bar", "baz"*/ } ) {
            Path dir = EXPLODED_DIR.resolve(name);
            assertTrue(compileModule(name, dir.resolve("classes")));
        }

        if (Files.exists(MODS_DIR))
            FileUtils.deleteFileTreeWithRetry(MODS_DIR);
        Files.createDirectories(MODS_DIR);
    }

    @Test
    public void testNoArgs() {
        jmod()
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: one of create, extract, list, describe, or hash must be specified")
            );
    }

    @Test
    public void testBadAction() {
        jmod("badAction")
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: mode must be one of create, extract, list, describe, or hash")
            );

        jmod("--badOption")
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: badOption is not a recognized option")
            );
    }

    @Test
    public void testTooManyArgs() throws IOException {
        Path jmod = MODS_DIR.resolve("doesNotExist.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);

        jmod("create",
             jmod.toString(),
             "AAA")
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: unknown option(s): [AAA]")
            );
    }

    @Test
    public void testCreateNoArgs() {
        jmod("create")
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: jmod-file must be specified")
            );
    }

    @Test
    public void testListNoArgs() {
        jmod("list")
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: jmod-file must be specified")
            );
    }

    @Test
    public void testListFileDoesNotExist() throws IOException {
        Path jmod = MODS_DIR.resolve("doesNotExist.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);

        jmod("list",
             jmod.toString())
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: no jmod file found: "
                        + jmod.toString())
            );
    }

    @Test
    public void testListJmodIsDir() throws IOException {
        Path jmod = MODS_DIR.resolve("testListJmodIsDir.jmod");
        if (Files.notExists(jmod))
            Files.createDirectory(jmod);

        jmod("list",
             jmod.toString())
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: error opening jmod file")
            );
    }

    @Test
    public void testlistJmodMalformed() throws IOException {
        Path jmod = MODS_DIR.resolve("testlistJmodMalformed.jmod");
        if (Files.notExists(jmod))
            Files.createFile(jmod);

        jmod("list",
             jmod.toString())
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: error opening jmod file")
            );
    }

    @Test
    public void testHashModulesModulePathNotSpecified() {
        jmod("create",
             "--hash-modules", "anyPattern.*",
             "output.jmod")
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: --module-path must be "
                        +"specified when hashing modules")
            );
    }

    @Test
    public void testCreateJmodAlreadyExists() throws IOException {
        Path jmod = MODS_DIR.resolve("testCreateJmodAlreadyExists.jmod");
        if (Files.notExists(jmod))
            Files.createFile(jmod);

        jmod("create",
             "--class-path", Paths.get(".").toString(), // anything that exists
             jmod.toString())
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: file already exists: " + jmod.toString())
            );
    }

    @Test
    public void testCreateJmodIsDir() throws IOException {
        Path jmod = MODS_DIR.resolve("testCreateJmodAlreadyExists");
        if (Files.notExists(jmod))
            Files.createDirectory(jmod);

        jmod("create",
             "--class-path", Paths.get(".").toString(), // anything that exists
             jmod.toString())
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: file already exists: " + jmod.toString())
            );
    }

    @Test
    public void testInvalidModuleVersion() throws IOException {
        Path jmod = MODS_DIR.resolve("testEmptyModuleVersion.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        String cp = EXPLODED_DIR.resolve("foo").resolve("classes").toString();

        for (String version : new String[] { "", "NOT_A_VALID_VERSION" }) {
            jmod("create",
                 "--class-path", cp,
                 "--module-version", version,
                 jmod.toString())
                .assertFailure()
                .resultChecker(r ->
                    assertContains(r.output, "Error: invalid module version")
                );
        }
    }

    @Test
    public void testEmptyFileInClasspath() throws IOException {
        Path jmod = MODS_DIR.resolve("testEmptyFileInClasspath.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        Path jar = MODS_DIR.resolve("NotARealJar_Empty.jar");
        FileUtils.deleteFileIfExistsWithRetry(jar);
        Files.createFile(jar);

        jmod("create",
             "--class-path", jar.toString(),
             jmod.toString())
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: module-info.class not found")
            );
    }

    @Test
    public void testEmptyJarInClasspath() throws IOException {
        Path jmod = MODS_DIR.resolve("testEmptyJarInClasspath.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        Path jar = MODS_DIR.resolve("empty.jar");
        FileUtils.deleteFileIfExistsWithRetry(jar);
        try (FileOutputStream fos = new FileOutputStream(jar.toFile());
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            // empty
        }

        jmod("create",
             "--class-path", jar.toString(),
             jmod.toString())
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: module-info.class not found")
            );
    }

    @Test
    public void testModuleInfoNotFound() throws IOException {
        Path jmod = MODS_DIR.resolve("output.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        Path jar = MODS_DIR.resolve("empty");
        FileUtils.deleteFileIfExistsWithRetry(jar);
        Files.createDirectory(jar);

        jmod("create",
             "--class-path", jar.toString(),
             jmod.toString())
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: module-info.class not found")
            );
    }

    @Test
    public void testModuleInfoIsDir() throws IOException {
        Path jmod = MODS_DIR.resolve("output.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        Path cp = MODS_DIR.resolve("module-info.class");
        FileUtils.deleteFileIfExistsWithRetry(cp);
        Files.createDirectory(cp);
        Files.createFile(cp.resolve("nada.txt"));

        jmod("create",
             "--class-path", cp.toString(),
             jmod.toString())
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: module-info.class not found")
            );
    }

    @Test
    public void testNoModuleHash() throws IOException {
        Path jmod = MODS_DIR.resolve("output.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        Path emptyDir = Paths.get("empty");
        if (Files.exists(emptyDir))
            FileUtils.deleteFileTreeWithRetry(emptyDir);
        Files.createDirectory(emptyDir);
        String cp = EXPLODED_DIR.resolve("foo").resolve("classes").toString();

        jmod("create",
             "--class-path", cp,
             "--hash-modules", ".*",
             "--module-path", emptyDir.toString(),
            jmod.toString())
            .resultChecker(r ->
                assertContains(r.output, "No hashes recorded: " +
                    "no module specified for hashing depends on foo")
            );
    }

    @Test
    public void testEmptyFileInModulePath() throws IOException {
        Path jmod = MODS_DIR.resolve("output.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        Path empty = MODS_DIR.resolve("emptyFile.jmod");
        FileUtils.deleteFileIfExistsWithRetry(empty);
        Files.createFile(empty);
        try {
            String cp = EXPLODED_DIR.resolve("foo").resolve("classes").toString();

            jmod("create",
                 "--class-path", cp,
                 "--hash-modules", ".*",
                 "--module-path", MODS_DIR.toString(),
                 jmod.toString())
                .assertFailure();
        } finally {
            FileUtils.deleteFileWithRetry(empty);
        }
    }

    @Test
    public void testFileInModulePath() throws IOException {
        Path jmod = MODS_DIR.resolve("output.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        Path file = MODS_DIR.resolve("testFileInModulePath.txt");
        FileUtils.deleteFileIfExistsWithRetry(file);
        Files.createFile(file);

        jmod("create",
             "--hash-modules", ".*",
             "--module-path", file.toString(),
             jmod.toString())
            .assertFailure()
            .resultChecker(r ->
                assertContains(r.output, "Error: path must be a directory")
            );
    }

    @DataProvider(name = "pathDoesNotExist")
    public Object[][] pathDoesNotExist() throws IOException {
        Path jmod = MODS_DIR.resolve("output.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        FileUtils.deleteFileIfExistsWithRetry(Paths.get("doesNotExist"));

        List<Supplier<JmodResult>> tasks = Arrays.asList(
                () -> jmod("create",
                           "--hash-modules", "anyPattern",
                           "--module-path", "doesNotExist",
                           "output.jmod"),
                () -> jmod("create",
                           "--class-path", "doesNotExist",
                           "output.jmod"),
                () -> jmod("create",
                           "--class-path", "doesNotExist.jar",
                           "output.jmod"),
                () -> jmod("create",
                           "--cmds", "doesNotExist",
                           "output.jmod"),
                () -> jmod("create",
                           "--config", "doesNotExist",
                           "output.jmod"),
                () -> jmod("create",
                           "--libs", "doesNotExist",
                           "output.jmod") );

        String errMsg = "Error: path not found: doesNotExist";
        return tasks.stream().map(t -> new Object[] {t, errMsg} )
                             .toArray(Object[][]::new);
    }

    @Test(dataProvider = "pathDoesNotExist")
    public void testPathDoesNotExist(Supplier<JmodResult> supplier,
                                     String errMsg)
    {
        supplier.get()
                .assertFailure()
                .resultChecker(r -> {
                    assertContains(r.output, errMsg);
                });
    }

    @DataProvider(name = "partOfPathDoesNotExist")
    public Object[][] partOfPathDoesNotExist() throws IOException {
        Path jmod = MODS_DIR.resolve("output.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        FileUtils.deleteFileIfExistsWithRetry(Paths.get("doesNotExist"));

        Path emptyDir = Paths.get("empty");
        if (Files.exists(emptyDir))
            FileUtils.deleteFileTreeWithRetry(emptyDir);
        Files.createDirectory(emptyDir);

        List<Supplier<JmodResult>> tasks = Arrays.asList(
            () -> jmod("create",
                       "--hash-modules", "anyPattern",
                       "--module-path","empty" + pathSeparator + "doesNotExist",
                       "output.jmod"),
            () -> jmod("create",
                       "--class-path", "empty" + pathSeparator + "doesNotExist",
                       "output.jmod"),
            () -> jmod("create",
                       "--class-path", "empty" + pathSeparator + "doesNotExist.jar",
                       "output.jmod"),
            () -> jmod("create",
                       "--cmds", "empty" + pathSeparator + "doesNotExist",
                       "output.jmod"),
            () -> jmod("create",
                       "--config", "empty" + pathSeparator + "doesNotExist",
                       "output.jmod"),
            () -> jmod("create",
                       "--libs", "empty" + pathSeparator + "doesNotExist",
                       "output.jmod") );

        String errMsg = "Error: path not found: doesNotExist";
        return tasks.stream().map(t -> new Object[] {t, errMsg} )
                             .toArray(Object[][]::new);
    }

    @Test(dataProvider = "partOfPathDoesNotExist")
    public void testPartOfPathNotExist(Supplier<JmodResult> supplier,
                                       String errMsg)
    {
        supplier.get()
                .assertFailure()
                .resultChecker(r -> {
                    assertContains(r.output, errMsg);
                });
    }

    @DataProvider(name = "pathIsFile")
    public Object[][] pathIsFile() throws IOException {
        Path jmod = MODS_DIR.resolve("output.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        Path aFile = Paths.get("aFile.txt");
        if (Files.exists(aFile) && !Files.isRegularFile(aFile))
            throw new InternalError("Unexpected file:" + aFile);
        else
            Files.createFile(aFile);

        List<Supplier<JmodResult>> tasks = Arrays.asList(
                () -> jmod("create",
                           "--class-path", "aFile.txt",
                           "output.jmod"),
                () -> jmod("create",
                           "--module-path", "aFile.txt",
                           "output.jmod"),
                () -> jmod("create",
                           "--cmds", "aFile.txt",
                           "output.jmod"),
                () -> jmod("create",
                           "--config", "aFile.txt",
                           "output.jmod"),
                () -> jmod("create",
                           "--libs", "aFile.txt",
                           "output.jmod") );

        String errMsg = "Error: path must be a directory: aFile.txt";
        Object[][] a = tasks.stream().map(t -> new Object[] {t, errMsg} )
                                     .toArray(Object[][]::new);
        a[0][1] = "invalid class path entry: aFile.txt";  // class path err msg
        return a;
    }

    @Test(dataProvider = "pathIsFile")
    public void testPathIsFile(Supplier<JmodResult> supplier,
                               String errMsg)
    {
        supplier.get()
                .assertFailure()
                .resultChecker(r -> {
                    assertContains(r.output, errMsg);
                });
    }

    @Test
    public void testNoMatchingHashModule() throws IOException {
        Path lib = Paths.get("hashes");
        Files.createDirectories(lib);
        // create jmod file with no module depending on it
        Path jmod = lib.resolve("foo.jmod");
        jmod("create",
             "--class-path", EXPLODED_DIR.resolve("foo").resolve("classes").toString(),
             jmod.toString());

        // jmod hash command should report no module found to record hashes
        jmod("hash",
             "--module-path", lib.toString(),
             "--hash-modules", ".*",
             jmod.toString())
             .resultChecker(r ->
                     assertContains(r.output, "No hashes recorded: " +
                             "no module matching \".*\" found to record hashes")
             );
        jmod("hash",
             "--module-path", lib.toString(),
             "--hash-modules", "foo")
             .resultChecker(r ->
                     assertContains(r.output, "No hashes recorded: " +
                             "no module matching \"foo\" found to record hashes")
             );
    }

    // ---

    static boolean compileModule(String name, Path dest) throws IOException {
        return CompilerUtils.compile(SRC_DIR.resolve(name), dest);
    }

    static void assertContains(String output, String subString) {
        if (output.contains(subString))
            assertTrue(true);
        else
            assertTrue(false,"Expected to find [" + subString + "], in output ["
                             + output + "]");
    }

    static JmodResult jmod(String... args) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        System.out.println("jmod " + Arrays.asList(args));
        int ec = JMOD_TOOL.run(ps, ps, args);
        return new JmodResult(ec, new String(baos.toByteArray(), UTF_8));
    }

    static class JmodResult {
        final int exitCode;
        final String output;

        JmodResult(int exitValue, String output) {
            this.exitCode = exitValue;
            this.output = output;
        }
        JmodResult assertFailure() { assertTrue(exitCode != 0, output); return this; }
        JmodResult resultChecker(Consumer<JmodResult> r) { r.accept(this); return this; }
    }
}
