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
 * @bug 8142968 8166568 8166286 8170618 8168149 8240910
 * @summary Basic test for jmod
 * @library /test/lib
 * @modules jdk.compiler
 *          jdk.jlink
 * @build jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.util.FileUtils
 *        jdk.test.lib.Platform
 * @run testng/othervm -Djava.io.tmpdir=. JmodTest
 */

import java.io.*;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Method;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Stream;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.util.FileUtils;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static java.io.File.pathSeparator;
import static java.lang.module.ModuleDescriptor.Version;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toSet;
import static org.testng.Assert.*;

public class JmodTest {

    static final ToolProvider JMOD_TOOL = ToolProvider.findFirst("jmod")
        .orElseThrow(() ->
            new RuntimeException("jmod tool not found")
        );
    static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
        .orElseThrow(() ->
            new RuntimeException("jar tool not found")
        );

    static final String TEST_SRC = System.getProperty("test.src", ".");
    static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    static final Path EXPLODED_DIR = Paths.get("build");
    static final Path MODS_DIR = Paths.get("jmods");

    static final String CLASSES_PREFIX = "classes/";
    static final String CMDS_PREFIX = "bin/";
    static final String LIBS_PREFIX = "lib/";
    static final String CONFIGS_PREFIX = "conf/";

    @BeforeTest
    public void buildExplodedModules() throws IOException {
        if (Files.exists(EXPLODED_DIR))
            FileUtils.deleteFileTreeWithRetry(EXPLODED_DIR);

        for (String name : new String[] { "foo"/*, "bar", "baz"*/ } ) {
            Path dir = EXPLODED_DIR.resolve(name);
            assertTrue(compileModule(name, dir.resolve("classes")));
            copyResource(SRC_DIR.resolve("foo"),
                         dir.resolve("classes"),
                         "jdk/test/foo/resources/foo.properties");
            createCmds(dir.resolve("bin"));
            createLibs(dir.resolve("lib"));
            createConfigs(dir.resolve("conf"));
        }

        if (Files.exists(MODS_DIR))
            FileUtils.deleteFileTreeWithRetry(MODS_DIR);
        Files.createDirectories(MODS_DIR);
    }

    // JDK-8166286 - jmod fails on symlink to directory
    @Test
    public void testDirSymlinks() throws IOException {
        Path apaDir = EXPLODED_DIR.resolve("apa");
        Path classesDir = EXPLODED_DIR.resolve("apa").resolve("classes");
        assertTrue(compileModule("apa", classesDir));
        Path libDir = apaDir.resolve("lib");
        createFiles(libDir, List.of("foo/bar/libfoo.so"));
        try {
            Path link = Files.createSymbolicLink(
                libDir.resolve("baz"), libDir.resolve("foo").toAbsolutePath());
            assertTrue(Files.exists(link));
        } catch (IOException|UnsupportedOperationException uoe) {
            // OS does not support symlinks. Nothing to test!
            System.out.println("Creating symlink failed. Test passes vacuously.");
            uoe.printStackTrace();
            return;
        }

        Path jmod = MODS_DIR.resolve("apa.jmod");
        jmod("create",
             "--libs=" + libDir.toString(),
             "--class-path", classesDir.toString(),
             jmod.toString())
            .assertSuccess();
        Files.delete(jmod);
    }

    // JDK-8267583 - jmod fails on symlink to class file
    @Test
    public void testFileSymlinks() throws IOException {
        Path apaDir = EXPLODED_DIR.resolve("apa");
        Path classesDir = EXPLODED_DIR.resolve("apa").resolve("classes");
        assertTrue(compileModule("apa", classesDir));

        Files.move(classesDir.resolve("module-info.class"),
            classesDir.resolve("module-info.class1"));
        Files.move(classesDir.resolve(Paths.get("jdk", "test", "apa", "Apa.class")),
            classesDir.resolve("Apa.class1"));
        try {
            Path link = Files.createSymbolicLink(
                classesDir.resolve("module-info.class"),
                classesDir.resolve("module-info.class1").toAbsolutePath());
            assertTrue(Files.exists(link));
            link = Files.createSymbolicLink(
                classesDir.resolve(Paths.get("jdk", "test", "apa", "Apa.class")),
                classesDir.resolve("Apa.class1").toAbsolutePath());
            assertTrue(Files.exists(link));
        } catch (IOException|UnsupportedOperationException uoe) {
            // OS does not support symlinks. Nothing to test!
            System.out.println("Creating symlinks failed. Test passes vacuously.");
            uoe.printStackTrace();
            return;
        }

        Path jmod = MODS_DIR.resolve("apa.jmod");
        jmod("create",
             "--class-path", classesDir.toString(),
             jmod.toString())
            .assertSuccess();
        Files.delete(jmod);
    }

    // JDK-8170618 - jmod should validate if any exported or open package is missing
    @Test
    public void testMissingPackages() throws IOException {
        Path apaDir = EXPLODED_DIR.resolve("apa");
        Path classesDir = EXPLODED_DIR.resolve("apa").resolve("classes");
        if (Files.exists(classesDir))
            FileUtils.deleteFileTreeWithRetry(classesDir);
        assertTrue(compileModule("apa", classesDir));
        FileUtils.deleteFileTreeWithRetry(classesDir.resolve("jdk"));
        Path jmod = MODS_DIR.resolve("apa.jmod");
        jmod("create",
             "--class-path", classesDir.toString(),
             jmod.toString())
            .assertFailure()
            .resultChecker(r -> {
                assertContains(r.output, "Packages that are exported or open in apa are not present: [jdk.test.apa]");
            });
        if (Files.exists(classesDir))
            FileUtils.deleteFileTreeWithRetry(classesDir);
    }

    @Test
    public void testList() throws IOException {
        String cp = EXPLODED_DIR.resolve("foo").resolve("classes").toString();
        jmod("create",
             "--class-path", cp,
             MODS_DIR.resolve("foo.jmod").toString())
            .assertSuccess();

        jmod("list",
             MODS_DIR.resolve("foo.jmod").toString())
            .assertSuccess()
            .resultChecker(r -> {
                // asserts dependent on the exact contents of foo
                assertContains(r.output, CLASSES_PREFIX + "module-info.class");
                assertContains(r.output, CLASSES_PREFIX + "jdk/test/foo/Foo.class");
                assertContains(r.output, CLASSES_PREFIX + "jdk/test/foo/internal/Message.class");
                assertContains(r.output, CLASSES_PREFIX + "jdk/test/foo/resources/foo.properties");
            });
    }

    @Test
    public void testExtractCWD() throws IOException {
        Path cp = EXPLODED_DIR.resolve("foo").resolve("classes");
        jmod("create",
             "--class-path", cp.toString(),
             MODS_DIR.resolve("fooExtractCWD.jmod").toString())
            .assertSuccess();

        jmod("extract",
             MODS_DIR.resolve("fooExtractCWD.jmod").toString())
            .assertSuccess()
            .resultChecker(r -> {
                // module-info should exist, but jmod will have added its Packages attr.
                assertTrue(Files.exists(Paths.get("classes/module-info.class")));
                assertSameContent(cp.resolve("jdk/test/foo/Foo.class"),
                                  Paths.get("classes/jdk/test/foo/Foo.class"));
                assertSameContent(cp.resolve("jdk/test/foo/internal/Message.class"),
                                  Paths.get("classes/jdk/test/foo/internal/Message.class"));
                assertSameContent(cp.resolve("jdk/test/foo/resources/foo.properties"),
                                  Paths.get("classes/jdk/test/foo/resources/foo.properties"));
            });
    }

    @Test
    public void testExtractDir() throws IOException {
        if (Files.exists(Paths.get("extractTestDir")))
            FileUtils.deleteFileTreeWithRetry(Paths.get("extractTestDir"));
        Path cp = EXPLODED_DIR.resolve("foo").resolve("classes");
        Path bp = EXPLODED_DIR.resolve("foo").resolve("bin");
        Path lp = EXPLODED_DIR.resolve("foo").resolve("lib");
        Path cf = EXPLODED_DIR.resolve("foo").resolve("conf");

        jmod("create",
             "--conf", cf.toString(),
             "--cmds", bp.toString(),
             "--libs", lp.toString(),
             "--class-path", cp.toString(),
             MODS_DIR.resolve("fooExtractDir.jmod").toString())
            .assertSuccess();

        jmod("extract",
             "--dir", "extractTestDir",
             MODS_DIR.resolve("fooExtractDir.jmod").toString())
            .assertSuccess();

        jmod("extract",
             "--dir", "extractTestDir",
             MODS_DIR.resolve("fooExtractDir.jmod").toString())
            .assertSuccess()
            .resultChecker(r -> {
                // check a sample of the extracted files
                Path p = Paths.get("extractTestDir");
                assertTrue(Files.exists(p.resolve("classes/module-info.class")));
                assertSameContent(cp.resolve("jdk/test/foo/Foo.class"),
                                  p.resolve("classes/jdk/test/foo/Foo.class"));
                assertSameContent(bp.resolve("first"),
                                  p.resolve(CMDS_PREFIX).resolve("first"));
                assertSameContent(lp.resolve("first.so"),
                                  p.resolve(LIBS_PREFIX).resolve("second.so"));
                assertSameContent(cf.resolve("second.cfg"),
                                  p.resolve(CONFIGS_PREFIX).resolve("second.cfg"));
            });
    }

    @Test
    public void testMainClass() throws IOException {
        Path jmod = MODS_DIR.resolve("fooMainClass.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        String cp = EXPLODED_DIR.resolve("foo").resolve("classes").toString();

        jmod("create",
             "--class-path", cp,
             "--main-class", "jdk.test.foo.Foo",
             jmod.toString())
            .assertSuccess()
            .resultChecker(r -> {
                Optional<String> omc = getModuleDescriptor(jmod).mainClass();
                assertTrue(omc.isPresent());
                assertEquals(omc.get(), "jdk.test.foo.Foo");
            });
    }

    @Test
    public void testModuleVersion() throws IOException {
        Path jmod = MODS_DIR.resolve("fooVersion.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        String cp = EXPLODED_DIR.resolve("foo").resolve("classes").toString();

        jmod("create",
             "--class-path", cp,
             "--module-version", "5.4.3",
             jmod.toString())
            .assertSuccess()
            .resultChecker(r -> {
                Optional<Version> ov = getModuleDescriptor(jmod).version();
                assertTrue(ov.isPresent());
                assertEquals(ov.get().toString(), "5.4.3");
            });
    }

    @Test
    public void testConfig() throws IOException {
        Path jmod = MODS_DIR.resolve("fooConfig.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        Path cp = EXPLODED_DIR.resolve("foo").resolve("classes");
        Path cf = EXPLODED_DIR.resolve("foo").resolve("conf");

        jmod("create",
             "--class-path", cp.toString(),
             "--config", cf.toString(),
             jmod.toString())
            .assertSuccess()
            .resultChecker(r -> {
                try (Stream<String> s1 = findFiles(cf).map(p -> CONFIGS_PREFIX + p);
                     Stream<String> s2 = findFiles(cp).map(p -> CLASSES_PREFIX + p)) {
                    Set<String> expectedFilenames = Stream.concat(s1, s2)
                                                          .collect(toSet());
                    assertJmodContent(jmod, expectedFilenames);
                }
            });
    }

    @Test
    public void testCmds() throws IOException {
        Path jmod = MODS_DIR.resolve("fooCmds.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        Path cp = EXPLODED_DIR.resolve("foo").resolve("classes");
        Path bp = EXPLODED_DIR.resolve("foo").resolve("bin");

        jmod("create",
             "--cmds", bp.toString(),
             "--class-path", cp.toString(),
             jmod.toString())
            .assertSuccess()
            .resultChecker(r -> {
                try (Stream<String> s1 = findFiles(bp).map(p -> CMDS_PREFIX + p);
                     Stream<String> s2 = findFiles(cp).map(p -> CLASSES_PREFIX + p)) {
                    Set<String> expectedFilenames = Stream.concat(s1,s2)
                                                          .collect(toSet());
                    assertJmodContent(jmod, expectedFilenames);
                }
            });
    }

    @Test
    public void testLibs() throws IOException {
        Path jmod = MODS_DIR.resolve("fooLibs.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        Path cp = EXPLODED_DIR.resolve("foo").resolve("classes");
        Path lp = EXPLODED_DIR.resolve("foo").resolve("lib");

        jmod("create",
             "--libs=" + lp.toString(),
             "--class-path", cp.toString(),
             jmod.toString())
            .assertSuccess()
            .resultChecker(r -> {
                try (Stream<String> s1 = findFiles(lp).map(p -> LIBS_PREFIX + p);
                     Stream<String> s2 = findFiles(cp).map(p -> CLASSES_PREFIX + p)) {
                    Set<String> expectedFilenames = Stream.concat(s1,s2)
                                                          .collect(toSet());
                    assertJmodContent(jmod, expectedFilenames);
                }
            });
    }

    @Test
    public void testAll() throws IOException {
        Path jmod = MODS_DIR.resolve("fooAll.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        Path cp = EXPLODED_DIR.resolve("foo").resolve("classes");
        Path bp = EXPLODED_DIR.resolve("foo").resolve("bin");
        Path lp = EXPLODED_DIR.resolve("foo").resolve("lib");
        Path cf = EXPLODED_DIR.resolve("foo").resolve("conf");

        jmod("create",
             "--conf", cf.toString(),
             "--cmds=" + bp.toString(),
             "--libs=" + lp.toString(),
             "--class-path", cp.toString(),
             jmod.toString())
            .assertSuccess()
            .resultChecker(r -> {
                try (Stream<String> s1 = findFiles(lp).map(p -> LIBS_PREFIX + p);
                     Stream<String> s2 = findFiles(cp).map(p -> CLASSES_PREFIX + p);
                     Stream<String> s3 = findFiles(bp).map(p -> CMDS_PREFIX + p);
                     Stream<String> s4 = findFiles(cf).map(p -> CONFIGS_PREFIX + p)) {
                    Set<String> expectedFilenames = Stream.concat(Stream.concat(s1,s2),
                                                                  Stream.concat(s3, s4))
                                                          .collect(toSet());
                    assertJmodContent(jmod, expectedFilenames);
                }
            });
    }

    @Test
    public void testExcludes() throws IOException {
        Path jmod = MODS_DIR.resolve("fooLibs.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        Path cp = EXPLODED_DIR.resolve("foo").resolve("classes");
        Path lp = EXPLODED_DIR.resolve("foo").resolve("lib");

        jmod("create",
             "--libs=" + lp.toString(),
             "--class-path", cp.toString(),
             "--exclude", "**internal**",
             "--exclude", "first.so",
             jmod.toString())
             .assertSuccess()
             .resultChecker(r -> {
                 Set<String> expectedFilenames = new HashSet<>();
                 expectedFilenames.add(CLASSES_PREFIX + "module-info.class");
                 expectedFilenames.add(CLASSES_PREFIX + "jdk/test/foo/Foo.class");
                 expectedFilenames.add(CLASSES_PREFIX + "jdk/test/foo/resources/foo.properties");
                 expectedFilenames.add(LIBS_PREFIX + "second.so");
                 expectedFilenames.add(LIBS_PREFIX + "third/third.so");
                 assertJmodContent(jmod, expectedFilenames);

                 Set<String> unexpectedFilenames = new HashSet<>();
                 unexpectedFilenames.add(CLASSES_PREFIX + "jdk/test/foo/internal/Message.class");
                 unexpectedFilenames.add(LIBS_PREFIX + "first.so");
                 assertJmodDoesNotContain(jmod, unexpectedFilenames);
             });
    }

    @Test
    public void describe() throws IOException {
        String cp = EXPLODED_DIR.resolve("foo").resolve("classes").toString();
        jmod("create",
             "--class-path", cp,
              MODS_DIR.resolve("describeFoo.jmod").toString())
             .assertSuccess();

        jmod("describe",
             MODS_DIR.resolve("describeFoo.jmod").toString())
             .assertSuccess()
             .resultChecker(r -> {
                 // Expect similar output: "foo... exports jdk.test.foo ...
                 //   ... requires java.base mandated... contains jdk.test.foo.internal"
                 Pattern p = Pattern.compile("foo\\s+exports\\s+jdk.test.foo");
                 assertTrue(p.matcher(r.output).find(),
                           "Expecting to find \"foo... exports jdk.test.foo\"" +
                                "in output, but did not: [" + r.output + "]");
                 p = Pattern.compile(
                        "requires\\s+java.base\\s+mandated\\s+contains\\s+jdk.test.foo.internal");
                 assertTrue(p.matcher(r.output).find(),
                           "Expecting to find \"requires java.base mandated..., " +
                                "contains jdk.test.foo.internal ...\"" +
                                "in output, but did not: [" + r.output + "]");
             });
    }

    @Test
    public void testDuplicateEntries() throws IOException {
        Path jmod = MODS_DIR.resolve("testDuplicates.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        String cp = EXPLODED_DIR.resolve("foo").resolve("classes").toString();
        Path lp = EXPLODED_DIR.resolve("foo").resolve("lib");

        jmod("create",
             "--class-path", cp + pathSeparator + cp,
             jmod.toString())
             .assertSuccess()
             .resultChecker(r ->
                 assertContains(r.output, "Warning: ignoring duplicate entry")
             );

        FileUtils.deleteFileIfExistsWithRetry(jmod);
        jmod("create",
             "--class-path", cp,
             "--libs", lp.toString() + pathSeparator + lp.toString(),
             jmod.toString())
             .assertSuccess()
             .resultChecker(r ->
                 assertContains(r.output, "Warning: ignoring duplicate entry")
             );
    }

    @Test
    public void testDuplicateEntriesFromJarFile() throws IOException {
        String cp = EXPLODED_DIR.resolve("foo").resolve("classes").toString();
        Path jar = Paths.get("foo.jar");
        Path jmod = MODS_DIR.resolve("testDuplicates.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jar);
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        // create JAR file
        assertTrue(JAR_TOOL.run(System.out, System.err, "cf", jar.toString(), "-C", cp, ".") == 0);

        jmod("create",
             "--class-path", jar.toString() + pathSeparator + jar.toString(),
             jmod.toString())
             .assertSuccess()
             .resultChecker(r ->
                 assertContains(r.output, "Warning: ignoring duplicate entry")
             );
    }

    @Test
    public void testIgnoreModuleInfoInOtherSections() throws IOException {
        Path jmod = MODS_DIR.resolve("testIgnoreModuleInfoInOtherSections.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        String cp = EXPLODED_DIR.resolve("foo").resolve("classes").toString();

        jmod("create",
            "--class-path", cp,
            "--libs", cp,
            jmod.toString())
            .assertSuccess()
            .resultChecker(r ->
                assertContains(r.output, "Warning: ignoring entry")
            );

        FileUtils.deleteFileIfExistsWithRetry(jmod);
        jmod("create",
             "--class-path", cp,
             "--cmds", cp,
             jmod.toString())
             .assertSuccess()
             .resultChecker(r ->
                 assertContains(r.output, "Warning: ignoring entry")
             );
    }

    @Test
    public void testLastOneWins() throws IOException {
        Path workDir = Paths.get("lastOneWins");
        if (Files.exists(workDir))
            FileUtils.deleteFileTreeWithRetry(workDir);
        Files.createDirectory(workDir);
        Path jmod = MODS_DIR.resolve("lastOneWins.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        Path cp = EXPLODED_DIR.resolve("foo").resolve("classes");
        Path bp = EXPLODED_DIR.resolve("foo").resolve("bin");
        Path lp = EXPLODED_DIR.resolve("foo").resolve("lib");
        Path cf = EXPLODED_DIR.resolve("foo").resolve("conf");

        Path shouldNotBeAdded = workDir.resolve("shouldNotBeAdded");
        Files.createDirectory(shouldNotBeAdded);
        Files.write(shouldNotBeAdded.resolve("aFile"), "hello".getBytes(UTF_8));

        // Pairs of options. For options with required arguments the last one
        // should win ( first should be effectively ignored, but may still be
        // validated ).
        jmod("create",
             "--conf", shouldNotBeAdded.toString(),
             "--conf", cf.toString(),
             "--cmds", shouldNotBeAdded.toString(),
             "--cmds", bp.toString(),
             "--libs", shouldNotBeAdded.toString(),
             "--libs", lp.toString(),
             "--class-path", shouldNotBeAdded.toString(),
             "--class-path", cp.toString(),
             "--main-class", "does.NotExist",
             "--main-class", "jdk.test.foo.Foo",
             "--module-version", "00001",
             "--module-version", "5.4.3",
             "--do-not-resolve-by-default",
             "--do-not-resolve-by-default",
             "--warn-if-resolved=incubating",
             "--warn-if-resolved=deprecated",
             MODS_DIR.resolve("lastOneWins.jmod").toString())
            .assertSuccess()
            .resultChecker(r -> {
                ModuleDescriptor md = getModuleDescriptor(jmod);
                Optional<String> omc = md.mainClass();
                assertTrue(omc.isPresent());
                assertEquals(omc.get(), "jdk.test.foo.Foo");
                Optional<Version> ov = md.version();
                assertTrue(ov.isPresent());
                assertEquals(ov.get().toString(), "5.4.3");

                try (Stream<String> s1 = findFiles(lp).map(p -> LIBS_PREFIX + p);
                     Stream<String> s2 = findFiles(cp).map(p -> CLASSES_PREFIX + p);
                     Stream<String> s3 = findFiles(bp).map(p -> CMDS_PREFIX + p);
                     Stream<String> s4 = findFiles(cf).map(p -> CONFIGS_PREFIX + p)) {
                    Set<String> expectedFilenames = Stream.concat(Stream.concat(s1,s2),
                                                                  Stream.concat(s3, s4))
                                                          .collect(toSet());
                    assertJmodContent(jmod, expectedFilenames);
                }
            });

        jmod("extract",
             "--dir", "blah",
             "--dir", "lastOneWinsExtractDir",
             jmod.toString())
            .assertSuccess()
            .resultChecker(r -> {
                assertTrue(Files.exists(Paths.get("lastOneWinsExtractDir")));
                assertTrue(Files.notExists(Paths.get("blah")));
            });
    }

    @Test
    public void testPackagesAttribute() throws IOException {
        Path jmod = MODS_DIR.resolve("foo.jmod");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        String cp = EXPLODED_DIR.resolve("foo").resolve("classes").toString();

        Set<String> expectedPackages = Set.of("jdk.test.foo",
                                              "jdk.test.foo.internal",
                                              "jdk.test.foo.resources");

        jmod("create",
             "--class-path", cp,
             jmod.toString())
             .assertSuccess()
             .resultChecker(r -> {
                 Set<String> pkgs = getModuleDescriptor(jmod).packages();
                 assertEquals(pkgs, expectedPackages);
             });
        }

    @Test
    public void testVersion() {
        jmod("--version")
            .assertSuccess()
            .resultChecker(r -> {
                assertContains(r.output, System.getProperty("java.version"));
            });
    }

    @Test
    public void testHelp() {
        jmod("--help")
            .assertSuccess()
            .resultChecker(r -> {
                assertTrue(r.output.startsWith("Usage: jmod"), "Help not printed");
                assertFalse(r.output.contains("--do-not-resolve-by-default"));
                assertFalse(r.output.contains("--warn-if-resolved"));
            });
    }

    @Test
    public void testHelpExtra() {
        jmod("--help-extra")
            .assertSuccess()
            .resultChecker(r -> {
                assertTrue(r.output.startsWith("Usage: jmod"), "Extra help not printed");
                assertContains(r.output, "--do-not-resolve-by-default");
                assertContains(r.output, "--warn-if-resolved");
            });
    }

    @Test
    public void testTmpFileRemoved() throws IOException {
        // Implementation detail: jmod tool creates <jmod-file>.tmp
        // Ensure that it is removed in the event of a failure.
        // The failure in this case is a class in the unnamed package.

        Path jmod = MODS_DIR.resolve("testTmpFileRemoved.jmod");
        Path tmp = MODS_DIR.resolve(".testTmpFileRemoved.jmod.tmp");
        FileUtils.deleteFileIfExistsWithRetry(jmod);
        FileUtils.deleteFileIfExistsWithRetry(tmp);
        String cp = EXPLODED_DIR.resolve("foo").resolve("classes") + File.pathSeparator +
                    EXPLODED_DIR.resolve("foo").resolve("classes")
                                .resolve("jdk").resolve("test").resolve("foo").toString();

        jmod("create",
             "--class-path", cp,
             jmod.toString())
            .assertFailure()
            .resultChecker(r -> {
                assertContains(r.output, "unnamed package");
                assertTrue(Files.notExists(tmp), "Unexpected tmp file:" + tmp);
            });
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
                           + output + "]" + "\n");
    }

    static ModuleDescriptor getModuleDescriptor(Path jmod) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        try (FileSystem fs = FileSystems.newFileSystem(jmod, cl)) {
            String p = "/classes/module-info.class";
            try (InputStream is = Files.newInputStream(fs.getPath(p))) {
                return ModuleDescriptor.read(is);
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    static Stream<String> findFiles(Path dir) {
        try {
            return Files.find(dir, Integer.MAX_VALUE, (p, a) -> a.isRegularFile())
                        .map(dir::relativize)
                        .map(Path::toString)
                        .map(p -> p.replace(File.separator, "/"));
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
    }

    static Set<String> getJmodContent(Path jmod) {
        JmodResult r = jmod("list", jmod.toString()).assertSuccess();
        return Stream.of(r.output.split("\r?\n")).collect(toSet());
    }

    static void assertJmodContent(Path jmod, Set<String> expected) {
        Set<String> actual = getJmodContent(jmod);
        if (!Objects.equals(actual, expected)) {
            Set<String> unexpected = new HashSet<>(actual);
            unexpected.removeAll(expected);
            Set<String> notFound = new HashSet<>(expected);
            notFound.removeAll(actual);
            StringBuilder sb = new StringBuilder();
            sb.append("Unexpected but found:\n");
            unexpected.forEach(s -> sb.append("\t" + s + "\n"));
            sb.append("Expected but not found:\n");
            notFound.forEach(s -> sb.append("\t" + s + "\n"));
            assertTrue(false, "Jmod content check failed.\n" + sb.toString());
        }
    }

    static void assertJmodDoesNotContain(Path jmod, Set<String> unexpectedNames) {
        Set<String> actual = getJmodContent(jmod);
        Set<String> unexpected = new HashSet<>();
        for (String name : unexpectedNames) {
            if (actual.contains(name))
                unexpected.add(name);
        }
        if (!unexpected.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String s : unexpected)
                sb.append("Unexpected but found: " + s + "\n");
            sb.append("In :");
            for (String s : actual)
                sb.append("\t" + s + "\n");
            assertTrue(false, "Jmod content check failed.\n" + sb.toString());
        }
    }

    static void assertSameContent(Path p1, Path p2) {
        try {
            byte[] ba1 = Files.readAllBytes(p1);
            byte[] ba2 = Files.readAllBytes(p2);
            assertEquals(ba1, ba2);
        } catch (IOException x) {
            throw new UncheckedIOException(x);
        }
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
        JmodResult assertSuccess() { assertTrue(exitCode == 0, output); return this; }
        JmodResult assertFailure() { assertTrue(exitCode != 0, output); return this; }
        JmodResult resultChecker(Consumer<JmodResult> r) { r.accept(this); return this; }
    }

    static void createCmds(Path dir) throws IOException {
        List<String> files = Arrays.asList(
                "first", "second", "third" + File.separator + "third");
        createFiles(dir, files);
    }

    static void createLibs(Path dir) throws IOException {
        List<String> files = Arrays.asList(
                "first.so", "second.so", "third" + File.separator + "third.so");
        createFiles(dir, files);
    }

    static void createConfigs(Path dir) throws IOException {
        List<String> files = Arrays.asList(
                "first.cfg", "second.cfg", "third" + File.separator + "third.cfg");
        createFiles(dir, files);
    }

    static void createFiles(Path dir, List<String> filenames) throws IOException {
        for (String name : filenames) {
            Path file = dir.resolve(name);
            Files.createDirectories(file.getParent());
            Files.createFile(file);
            try (OutputStream os  = Files.newOutputStream(file)) {
                os.write("blahblahblah".getBytes(UTF_8));
            }
        }
    }

    static void copyResource(Path srcDir, Path dir, String resource) throws IOException {
        Path dest = dir.resolve(resource);
        Files.deleteIfExists(dest);

        Files.createDirectories(dest.getParent());
        Files.copy(srcDir.resolve(resource), dest);
    }

    // Standalone entry point.
    public static void main(String[] args) throws Throwable {
        JmodTest test = new JmodTest();
        test.buildExplodedModules();
        for (Method m : JmodTest.class.getDeclaredMethods()) {
            if (m.getAnnotation(Test.class) != null) {
                System.out.println("Invoking " + m.getName());
                m.invoke(test);
            }
        }
    }
}
