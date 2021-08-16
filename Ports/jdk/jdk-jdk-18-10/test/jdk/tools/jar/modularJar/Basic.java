/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.lang.module.ModuleDescriptor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.regex.Pattern;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.test.lib.util.FileUtils;
import jdk.test.lib.JDKToolFinder;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.lang.String.format;
import static java.lang.System.out;

/*
 * @test
 * @bug 8167328 8171830 8165640 8174248 8176772 8196748 8191533 8210454
 * @library /test/lib
 * @modules jdk.compiler
 *          jdk.jartool
 * @build jdk.test.lib.Platform
 *        jdk.test.lib.util.FileUtils
 *        jdk.test.lib.JDKToolFinder
 * @compile Basic.java
 * @run testng Basic
 * @summary Tests for plain Modular jars & Multi-Release Modular jars
 */

public class Basic {

    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
            .orElseThrow(()
                    -> new RuntimeException("jar tool not found")
            );
    private static final ToolProvider JAVAC_TOOL = ToolProvider.findFirst("javac")
            .orElseThrow(()
                    -> new RuntimeException("javac tool not found")
            );

    static final Path TEST_SRC = Paths.get(System.getProperty("test.src", "."));
    static final Path TEST_CLASSES = Paths.get(System.getProperty("test.classes", "."));
    static final Path MODULE_CLASSES = TEST_CLASSES.resolve("build");
    static final Path MRJAR_DIR = MODULE_CLASSES.resolve("mrjar");

    static final String VM_OPTIONS = System.getProperty("test.vm.opts", "");
    static final String TOOL_VM_OPTIONS = System.getProperty("test.tool.vm.opts", "");
    static final String JAVA_OPTIONS = System.getProperty("test.java.opts", "");

    // Details based on the checked in module source
    static TestModuleData FOO = new TestModuleData("foo",
                                                   "1.123",
                                                   "jdk.test.foo.Foo",
                                                   "Hello World!!!",
                                                   null, // no hashes
                                                   Set.of("java.base"),
                                                   Set.of("jdk.test.foo"),
                                                   null, // no uses
                                                   null, // no provides
                                                   Set.of("jdk.test.foo.internal",
                                                          "jdk.test.foo.resources"));
    static TestModuleData BAR = new TestModuleData("bar",
                                                   "4.5.6.7",
                                                   "jdk.test.bar.Bar",
                                                   "Hello from Bar!",
                                                   null, // no hashes
                                                   Set.of("java.base", "foo"),
                                                   null, // no exports
                                                   null, // no uses
                                                   null, // no provides
                                                   Set.of("jdk.test.bar",
                                                          "jdk.test.bar.internal"));

    static class TestModuleData {
        final String moduleName;
        final Set<String> requires;
        final Set<String> exports;
        final Set<String> uses;
        final Set<String> provides;
        final String mainClass;
        final String version;
        final String message;
        final String hashes;
        final Set<String> packages;

        TestModuleData(String mn, String v, String mc, String m, String h,
                       Set<String> requires, Set<String> exports, Set<String> uses,
                       Set<String> provides, Set<String> contains) {
            moduleName = mn; mainClass = mc; version = v; message = m; hashes = h;
            this.requires = requires != null ? requires : Collections.emptySet();
            this.exports = exports != null ? exports : Collections.emptySet();
            this.uses = uses != null ? uses : Collections.emptySet();;
            this.provides = provides != null ? provides : Collections.emptySet();
            this.packages = Stream.concat(this.exports.stream(), contains.stream())
                                  .collect(Collectors.toSet());
        }
        static TestModuleData from(String s) {
            try {
                BufferedReader reader = new BufferedReader(new StringReader(s));
                String line;
                String message = null;
                String name = null, version = null, mainClass = null;
                String hashes = null;
                Set<String> requires, exports, uses, provides, conceals;
                requires = exports = uses = provides = conceals = null;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("message:")) {
                        message = line.substring("message:".length());
                    } else if (line.startsWith("nameAndVersion:")) {
                        line = line.substring("nameAndVersion:".length());
                        int i = line.indexOf('@');
                        if (i != -1) {
                            name = line.substring(0, i);
                            version = line.substring(i + 1, line.length());
                        } else {
                            name = line;
                        }
                    } else if (line.startsWith("mainClass:")) {
                        mainClass = line.substring("mainClass:".length());
                    } else if (line.startsWith("requires:")) {
                        line = line.substring("requires:".length());
                        requires = stringToSet(line);
                    } else if (line.startsWith("exports:")) {
                        line = line.substring("exports:".length());
                        exports = stringToSet(line);
                    } else if (line.startsWith("uses:")) {
                        line = line.substring("uses:".length());
                        uses = stringToSet(line);
                    } else if (line.startsWith("provides:")) {
                        line = line.substring("provides:".length());
                        provides = stringToSet(line);
                    } else if (line.startsWith("hashes:")) {
                        hashes = line.substring("hashes:".length());
                    } else if (line.startsWith("contains:")) {
                        line = line.substring("contains:".length());
                        conceals = stringToSet(line);
                    } else if (line.contains("VM warning:")) {
                        continue;  // ignore server vm warning see#8196748
                    } else if (line.contains("WARNING: JNI access from module not specified in --enable-native-access:")) {
                        continue;
                    } else {
                        throw new AssertionError("Unknown value " + line);
                    }
                }

                return new TestModuleData(name, version, mainClass, message,
                                          hashes, requires, exports, uses,
                                          provides, conceals);
            } catch (IOException x) {
                throw new UncheckedIOException(x);
            }
        }
        static Set<String> stringToSet(String commaList) {
            Set<String> s = new HashSet<>();
            int i = commaList.indexOf(',');
            if (i != -1) {
                String[] p = commaList.split(",");
                Stream.of(p).forEach(s::add);
            } else {
                s.add(commaList);
            }
            return s;
        }
    }

    static void assertModuleData(Result r, TestModuleData expected) {
        //out.printf("%s%n", r.output);
        TestModuleData received = TestModuleData.from(r.output);
        if (expected.message != null)
            assertTrue(expected.message.equals(received.message),
                       "Expected message:", expected.message, ", got:", received.message);
        assertTrue(expected.moduleName.equals(received.moduleName),
                   "Expected moduleName: ", expected.moduleName, ", got:", received.moduleName);
        assertTrue(expected.version.equals(received.version),
                   "Expected version: ", expected.version, ", got:", received.version);
        assertTrue(expected.mainClass.equals(received.mainClass),
                   "Expected mainClass: ", expected.mainClass, ", got:", received.mainClass);
        assertSetsEqual(expected.requires, received.requires);
        assertSetsEqual(expected.exports, received.exports);
        assertSetsEqual(expected.uses, received.uses);
        assertSetsEqual(expected.provides, received.provides);
        assertSetsEqual(expected.packages, received.packages);
    }

    static void assertSetsEqual(Set<String> s1, Set<String> s2) {
        if (!s1.equals(s2)) {
            org.testng.Assert.assertTrue(false, s1 + " vs " + s2);
        }
     }

    @BeforeTest
    public void compileModules() throws Exception {
        compileModule(FOO.moduleName);
        compileModule(BAR.moduleName, MODULE_CLASSES);
        compileModule("baz");  // for service provider consistency checking

        // copy resources
        copyResource(TEST_SRC.resolve("src").resolve(FOO.moduleName),
                     MODULE_CLASSES.resolve(FOO.moduleName),
                     "jdk/test/foo/resources/foo.properties");

        setupMRJARModuleInfo(FOO.moduleName);
        setupMRJARModuleInfo(BAR.moduleName);
        setupMRJARModuleInfo("baz");
    }

    @Test
    public void createFoo() throws IOException {
        Path mp = Paths.get("createFoo");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path modularJar = mp.resolve(FOO.moduleName + ".jar");

        jar("--create",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "--no-manifest",
            "-C", modClasses.toString(), ".")
            .assertSuccess();

        assertSetsEqual(readPackagesAttribute(modularJar),
                        Set.of("jdk.test.foo",
                               "jdk.test.foo.resources",
                               "jdk.test.foo.internal"));

        java(mp, FOO.moduleName + "/" + FOO.mainClass)
            .assertSuccess()
            .resultChecker(r -> assertModuleData(r, FOO));
        try (InputStream fis = Files.newInputStream(modularJar);
             JarInputStream jis = new JarInputStream(fis)) {
            assertTrue(!jarContains(jis, "./"),
                       "Unexpected ./ found in ", modularJar.toString());
        }
    }

    /** Similar to createFoo, but with a Multi-Release Modular jar. */
    @Test
    public void createMRMJarFoo() throws IOException {
        Path mp = Paths.get("createMRMJarFoo");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path mrjarDir = MRJAR_DIR.resolve(FOO.moduleName);
        Path modularJar = mp.resolve(FOO.moduleName + ".jar");

        // Positive test, create
        jar("--create",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "-m", mrjarDir.resolve("META-INF/MANIFEST.MF").toRealPath().toString(),
            "-C", mrjarDir.toString(), "META-INF/versions/9/module-info.class",
            "-C", modClasses.toString(), ".")
            .assertSuccess();
        java(mp, FOO.moduleName + "/" + FOO.mainClass)
            .assertSuccess()
            .resultChecker(r -> assertModuleData(r, FOO));
    }


    @Test
    public void updateFoo() throws IOException {
        Path mp = Paths.get("updateFoo");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path modularJar = mp.resolve(FOO.moduleName + ".jar");

        jar("--create",
            "--file=" + modularJar.toString(),
            "--no-manifest",
            "-C", modClasses.toString(), "jdk")
            .assertSuccess();
        jar("--update",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "--no-manifest",
            "-C", modClasses.toString(), "module-info.class")
            .assertSuccess();
        java(mp, FOO.moduleName + "/" + FOO.mainClass)
            .assertSuccess()
            .resultChecker(r -> assertModuleData(r, FOO));
    }

    @Test
    public void updateMRMJarFoo() throws IOException {
        Path mp = Paths.get("updateMRMJarFoo");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path mrjarDir = MRJAR_DIR.resolve(FOO.moduleName);
        Path modularJar = mp.resolve(FOO.moduleName + ".jar");

        jar("--create",
            "--file=" + modularJar.toString(),
            "--no-manifest",
            "-C", modClasses.toString(), "jdk")
            .assertSuccess();
        jar("--update",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "-m", mrjarDir.resolve("META-INF/MANIFEST.MF").toRealPath().toString(),
            "-C", mrjarDir.toString(), "META-INF/versions/9/module-info.class",
            "-C", modClasses.toString(), "module-info.class")
            .assertSuccess();
        java(mp, FOO.moduleName + "/" + FOO.mainClass)
            .assertSuccess()
            .resultChecker(r -> assertModuleData(r, FOO));
    }

    @Test
    public void partialUpdateFooMainClass() throws IOException {
        Path mp = Paths.get("partialUpdateFooMainClass");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path modularJar = mp.resolve(FOO.moduleName + ".jar");

        // A "bad" main class in first create ( and no version )
        jar("--create",
            "--file=" + modularJar.toString(),
            "--main-class=" + "jdk.test.foo.IAmNotTheEntryPoint",
            "--no-manifest",
            "-C", modClasses.toString(), ".")  // includes module-info.class
           .assertSuccess();
        jar("--update",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "--no-manifest")
            .assertSuccess();
        java(mp, FOO.moduleName + "/" + FOO.mainClass)
            .assertSuccess()
            .resultChecker(r -> assertModuleData(r, FOO));
    }

    @Test
    public void partialUpdateFooVersion() throws IOException {
        Path mp = Paths.get("partialUpdateFooVersion");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path modularJar = mp.resolve(FOO.moduleName + ".jar");

        // A "bad" version in first create ( and no main class )
        jar("--create",
            "--file=" + modularJar.toString(),
            "--module-version=" + "100000000",
            "--no-manifest",
            "-C", modClasses.toString(), ".")  // includes module-info.class
            .assertSuccess();
        jar("--update",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "--no-manifest")
            .assertSuccess();
        java(mp, FOO.moduleName + "/" + FOO.mainClass)
            .assertSuccess()
            .resultChecker(r -> assertModuleData(r, FOO));
    }

    @Test
    public void partialUpdateFooNotAllFiles() throws IOException {
        Path mp = Paths.get("partialUpdateFooNotAllFiles");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path modularJar = mp.resolve(FOO.moduleName + ".jar");

        // Not all files, and none from non-exported packages,
        // i.e. no concealed list in first create
        jar("--create",
            "--file=" + modularJar.toString(),
            "--no-manifest",
            "-C", modClasses.toString(), "module-info.class",
            "-C", modClasses.toString(), "jdk/test/foo/Foo.class",
            "-C", modClasses.toString(), "jdk/test/foo/resources/foo.properties")
            .assertSuccess();
        jar("--update",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "--no-manifest",
            "-C", modClasses.toString(), "jdk/test/foo/internal/Message.class")
            .assertSuccess();
        java(mp, FOO.moduleName + "/" + FOO.mainClass)
            .assertSuccess()
            .resultChecker(r -> assertModuleData(r, FOO));
    }

    @Test
    public void partialUpdateMRMJarFooNotAllFiles() throws IOException {
        Path mp = Paths.get("partialUpdateMRMJarFooNotAllFiles");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path mrjarDir = MRJAR_DIR.resolve(FOO.moduleName);
        Path modularJar = mp.resolve(FOO.moduleName + ".jar");

        jar("--create",
            "--file=" + modularJar.toString(),
            "--module-version=" + FOO.version,
            "-C", modClasses.toString(), ".")
            .assertSuccess();
        jar("--update",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "-m", mrjarDir.resolve("META-INF/MANIFEST.MF").toRealPath().toString(),
            "-C", mrjarDir.toString(), "META-INF/versions/9/module-info.class")
            .assertSuccess();
        java(mp, FOO.moduleName + "/" + FOO.mainClass)
            .assertSuccess()
            .resultChecker(r -> assertModuleData(r, FOO));
    }

    @Test
    public void partialUpdateFooAllFilesAndAttributes() throws IOException {
        Path mp = Paths.get("partialUpdateFooAllFilesAndAttributes");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path modularJar = mp.resolve(FOO.moduleName + ".jar");

        // all attributes and files
        jar("--create",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "--no-manifest",
            "-C", modClasses.toString(), ".")
            .assertSuccess();
        jar("--update",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "--no-manifest",
            "-C", modClasses.toString(), ".")
            .assertSuccess();
        java(mp, FOO.moduleName + "/" + FOO.mainClass)
            .assertSuccess()
            .resultChecker(r -> assertModuleData(r, FOO));
    }

    @Test
    public void partialUpdateFooModuleInfo() throws IOException {
        Path mp = Paths.get("partialUpdateFooModuleInfo");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path modularJar = mp.resolve(FOO.moduleName + ".jar");
        Path barModInfo = MODULE_CLASSES.resolve(BAR.moduleName);

        jar("--create",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "--no-manifest",
            "-C", modClasses.toString(), ".")
            .assertSuccess();
        jar("--update",
            "--file=" + modularJar.toString(),
            "--no-manifest",
            "-C", barModInfo.toString(), "module-info.class")  // stuff in bar's info
            .assertSuccess();
        jar("-d",
            "--file=" + modularJar.toString())
            .assertSuccess()
            .resultChecker(r -> {
                // Expect "bar jar:file:/...!/module-info.class"
                // conceals jdk.test.foo, conceals jdk.test.foo.internal"
                String uri = "jar:" + modularJar.toUri().toString() + "!/module-info.class";
                assertTrue(r.output.contains("bar " + uri),
                           "Expecting to find \"bar " + uri + "\"",
                           "in output, but did not: [" + r.output + "]");
                Pattern p = Pattern.compile(
                        "contains\\s+jdk.test.foo\\s+contains\\s+jdk.test.foo.internal");
                assertTrue(p.matcher(r.output).find(),
                           "Expecting to find \"contains jdk.test.foo,...\"",
                           "in output, but did not: [" + r.output + "]");
            });
    }


    @Test
    public void partialUpdateFooPackagesAttribute() throws IOException {
        Path mp = Paths.get("partialUpdateFooPackagesAttribute");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path modularJar = mp.resolve(FOO.moduleName + ".jar");

        // Not all files, and none from non-exported packages,
        // i.e. no concealed list in first create
        jar("--create",
            "--file=" + modularJar.toString(),
            "--no-manifest",
            "-C", modClasses.toString(), "module-info.class",
            "-C", modClasses.toString(), "jdk/test/foo/Foo.class")
            .assertSuccess();

        assertSetsEqual(readPackagesAttribute(modularJar),
                        Set.of("jdk.test.foo"));

        jar("--update",
            "--file=" + modularJar.toString(),
            "-C", modClasses.toString(), "jdk/test/foo/resources/foo.properties")
            .assertSuccess();

        assertSetsEqual(readPackagesAttribute(modularJar),
                        Set.of("jdk.test.foo", "jdk.test.foo.resources"));

        jar("--update",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "--no-manifest",
            "-C", modClasses.toString(), "jdk/test/foo/internal/Message.class")
            .assertSuccess();

        assertSetsEqual(readPackagesAttribute(modularJar),
                        Set.of("jdk.test.foo",
                               "jdk.test.foo.resources",
                               "jdk.test.foo.internal"));

        java(mp, FOO.moduleName + "/" + FOO.mainClass)
            .assertSuccess()
            .resultChecker(r -> assertModuleData(r, FOO));
    }

    private Set<String> readPackagesAttribute(Path jar) {
        return getModuleDescriptor(jar).packages();
    }

    @Test
    public void hashBarInFooModule() throws IOException {
        Path mp = Paths.get("dependencesFooBar");
        createTestDir(mp);

        Path modClasses = MODULE_CLASSES.resolve(BAR.moduleName);
        Path modularJar = mp.resolve(BAR.moduleName + ".jar");
        jar("--create",
            "--file=" + modularJar.toString(),
            "--main-class=" + BAR.mainClass,
            "--module-version=" + BAR.version,
            "--no-manifest",
            "-C", modClasses.toString(), ".")
            .assertSuccess();

        modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        modularJar = mp.resolve(FOO.moduleName + ".jar");
        jar("--create",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "--module-path=" + mp.toString(),
            "--hash-modules=" + "bar",
            "--no-manifest",
            "-C", modClasses.toString(), ".")
            .assertSuccess();

        java(mp, BAR.moduleName + "/" + BAR.mainClass,
             "--add-exports", "java.base/jdk.internal.misc=bar",
             "--add-exports", "java.base/jdk.internal.module=bar")
            .assertSuccess()
            .resultChecker(r -> {
                assertModuleData(r, BAR);
                TestModuleData received = TestModuleData.from(r.output);
                assertTrue(received.hashes != null, "Expected non-null hashes value.");
            });
    }

    @Test
    public void invalidHashInFooModule() throws IOException {
        Path mp = Paths.get("badDependencyFooBar");
        createTestDir(mp);

        Path barClasses = MODULE_CLASSES.resolve(BAR.moduleName);
        Path barJar = mp.resolve(BAR.moduleName + ".jar");
        jar("--create",
            "--file=" + barJar.toString(),
            "--main-class=" + BAR.mainClass,
            "--module-version=" + BAR.version,
            "--no-manifest",
            "-C", barClasses.toString(), ".").assertSuccess();

        Path fooClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path fooJar = mp.resolve(FOO.moduleName + ".jar");
        jar("--create",
            "--file=" + fooJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "-p", mp.toString(),  // test short-form
            "--hash-modules=" + "bar",
            "--no-manifest",
            "-C", fooClasses.toString(), ".").assertSuccess();

        // Rebuild bar.jar with a change that will cause its hash to be different
        FileUtils.deleteFileWithRetry(barJar);
        jar("--create",
            "--file=" + barJar.toString(),
            "--main-class=" + BAR.mainClass,
            "--module-version=" + BAR.version + ".1", // a newer version
            "--no-manifest",
            "-C", barClasses.toString(), ".").assertSuccess();

        java(mp, BAR.moduleName + "/" + BAR.mainClass,
             "--add-exports", "java.base/jdk.internal.misc=bar",
             "--add-exports", "java.base/jdk.internal.module=bar")
            .assertFailure()
            .resultChecker(r -> {
                // Expect similar output: "java.lang.module.ResolutionException: Hash
                // of bar (WdktSIQSkd4+CEacpOZoeDrCosMATNrIuNub9b5yBeo=) differs to
                // expected hash (iepvdv8xTeVrFgMtUhcFnmetSub6qQHCHc92lSaSEg0=)"
                Pattern p = Pattern.compile(".*Hash of bar.*differs to expected hash.*");
                assertTrue(p.matcher(r.output).find(),
                      "Expecting error message containing \"Hash of bar ... differs to"
                              + " expected hash...\" but got: [", r.output + "]");
            });
    }

    @Test
    public void badOptionsFoo() throws IOException {
        Path mp = Paths.get("badOptionsFoo");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path modularJar = mp.resolve(FOO.moduleName + ".jar");

        jar("--create",
            "--file=" + modularJar.toString(),
            "--module-version=" + 1.1,   // no module-info.class
            "-C", modClasses.toString(), "jdk")
            .assertFailure();      // TODO: expected failure message

         jar("--create",
             "--file=" + modularJar.toString(),
             "--hash-modules=" + ".*",   // no module-info.class
             "-C", modClasses.toString(), "jdk")
             .assertFailure();      // TODO: expected failure message
    }

    @Test
    public void servicesCreateWithoutFailure() throws IOException {
        Path mp = Paths.get("servicesCreateWithoutFailure");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve("baz");
        Path modularJar = mp.resolve("baz" + ".jar");

        // Positive test, create
        jar("--create",
            "--file=" + modularJar.toString(),
            "-C", modClasses.toString(), "module-info.class",
            "-C", modClasses.toString(), "jdk/test/baz/BazService.class",
            "-C", modClasses.toString(), "jdk/test/baz/internal/BazServiceImpl.class")
            .assertSuccess();

        for (String option : new String[]  {"--describe-module", "-d" }) {
            jar(option,
                "--file=" + modularJar.toString())
                .assertSuccess()
                .resultChecker(r ->
                    assertTrue(r.output.contains("provides jdk.test.baz.BazService with jdk.test.baz.internal.BazServiceImpl"),
                               "Expected to find ", "provides jdk.test.baz.BazService with jdk.test.baz.internal.BazServiceImpl",
                               " in [", r.output, "]")
                );
        }
    }

    @Test
    public void servicesCreateWithoutServiceImpl() throws IOException {
        Path mp = Paths.get("servicesWithoutServiceImpl");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve("baz");
        Path modularJar = mp.resolve("baz" + ".jar");

        // Omit service impl
        jar("--create",
            "--file=" + modularJar.toString(),
            "-C", modClasses.toString(), "module-info.class",
            "-C", modClasses.toString(), "jdk/test/baz/BazService.class")
            .assertFailure();
    }

    @Test
    public void servicesUpdateWithoutFailure() throws IOException {
        Path mp = Paths.get("servicesUpdateWithoutFailure");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve("baz");
        Path modularJar = mp.resolve("baz" + ".jar");

        // Positive test, update
        jar("--create",
            "--file=" + modularJar.toString(),
            "-C", modClasses.toString(), "jdk/test/baz/BazService.class",
            "-C", modClasses.toString(), "jdk/test/baz/internal/BazServiceImpl.class")
            .assertSuccess();
        jar("--update",
            "--file=" + modularJar.toString(),
            "-C", modClasses.toString(), "module-info.class")
            .assertSuccess();
    }

    @Test
    public void servicesUpdateWithoutServiceImpl() throws IOException {
        Path mp = Paths.get("servicesUpdateWithoutServiceImpl");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve("baz");
        Path modularJar = mp.resolve("baz" + ".jar");

        // Omit service impl
        jar("--create",
            "--file=" + modularJar.toString(),
            "-C", modClasses.toString(), "jdk/test/baz/BazService.class")
            .assertSuccess();
        jar("--update",
            "--file=" + modularJar.toString(),
            "-C", modClasses.toString(), "module-info.class")
            .assertFailure();
    }

    @Test
    public void servicesCreateWithoutFailureMRMJAR() throws IOException {
        Path mp = Paths.get("servicesCreateWithoutFailureMRMJAR");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve("baz");
        Path mrjarDir = MRJAR_DIR.resolve("baz");
        Path modularJar = mp.resolve("baz" + ".jar");

        jar("--create",
            "--file=" + modularJar.toString(),
            "-m", mrjarDir.resolve("META-INF/MANIFEST.MF").toRealPath().toString(),
            "-C", modClasses.toString(), "module-info.class",
            "-C", mrjarDir.toString(), "META-INF/versions/9/module-info.class",
            "-C", modClasses.toString(), "jdk/test/baz/BazService.class",
            "-C", modClasses.toString(), "jdk/test/baz/internal/BazServiceImpl.class")
            .assertSuccess();
    }

    @Test
    public void servicesCreateWithoutFailureNonRootMRMJAR() throws IOException {
        // without a root module-info.class
        Path mp = Paths.get("servicesCreateWithoutFailureNonRootMRMJAR");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve("baz");
        Path mrjarDir = MRJAR_DIR.resolve("baz");
        Path modularJar = mp.resolve("baz.jar");

        jar("--create",
            "--file=" + modularJar.toString(),
            "--main-class=" + "jdk.test.baz.Baz",
            "-m", mrjarDir.resolve("META-INF/MANIFEST.MF").toRealPath().toString(),
            "-C", mrjarDir.toString(), "META-INF/versions/9/module-info.class",
            "-C", modClasses.toString(), "jdk/test/baz/BazService.class",
            "-C", modClasses.toString(), "jdk/test/baz/Baz.class",
            "-C", modClasses.toString(), "jdk/test/baz/internal/BazServiceImpl.class")
            .assertSuccess();


        for (String option : new String[]  {"--describe-module", "-d" }) {

            jar(option,
                "--file=" + modularJar.toString(),
                "--release", "9")
                .assertSuccess()
                .resultChecker(r ->
                    assertTrue(r.output.contains("main-class jdk.test.baz.Baz"),
                              "Expected to find ", "main-class jdk.test.baz.Baz",
                               " in [", r.output, "]"));

            jarWithStdin(modularJar.toFile(), option, "--release", "9")
                .assertSuccess()
                .resultChecker(r ->
                    assertTrue(r.output.contains("main-class jdk.test.baz.Baz"),
                              "Expected to find ", "main-class jdk.test.baz.Baz",
                               " in [", r.output, "]"));

        }
        // run module main class
        java(mp, "baz/jdk.test.baz.Baz")
            .assertSuccess()
            .resultChecker(r ->
               assertTrue(r.output.contains("mainClass:jdk.test.baz.Baz"),
                          "Expected to find ", "mainClass:jdk.test.baz.Baz",
                          " in [", r.output, "]"));
    }

    @Test
    public void exportCreateWithMissingPkg() throws IOException {

        Path foobar = TEST_SRC.resolve("src").resolve("foobar");
        Path dst = Files.createDirectories(MODULE_CLASSES.resolve("foobar"));
        javac(dst, null, sourceList(foobar));

        Path mp = Paths.get("exportWithMissingPkg");
        createTestDir(mp);
        Path modClasses = dst;
        Path modularJar = mp.resolve("foofoo.jar");

        jar("--create",
            "--file=" + modularJar.toString(),
            "-C", modClasses.toString(), "module-info.class",
            "-C", modClasses.toString(), "jdk/test/foo/Foo.class")
            .assertFailure();
    }

    @Test
    public void describeModuleFoo() throws IOException {
        Path mp = Paths.get("describeModuleFoo");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path modularJar = mp.resolve(FOO.moduleName + ".jar");

        jar("--create",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "--no-manifest",
            "-C", modClasses.toString(), ".")
            .assertSuccess();

        for (String option : new String[]  {"--describe-module", "-d" }) {
            jar(option,
                "--file=" + modularJar.toString())
                .assertSuccess()
                .resultChecker(r -> {
                    assertTrue(r.output.contains(FOO.moduleName + "@" + FOO.version),
                               "Expected to find ", FOO.moduleName + "@" + FOO.version,
                               " in [", r.output, "]");
                    assertTrue(r.output.contains(modularJar.toUri().toString()),
                               "Expected to find ", modularJar.toUri().toString(),
                               " in [", r.output, "]");
                    }
                );

            jar(option,
                "--file=" + modularJar.toString(),
                modularJar.toString())
            .assertFailure();

            jar(option, modularJar.toString())
            .assertFailure();
        }
    }

    @Test
    public void describeModuleFooFromStdin() throws IOException {
        Path mp = Paths.get("describeModuleFooFromStdin");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path modularJar = mp.resolve(FOO.moduleName + ".jar");

        jar("--create",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "--no-manifest",
            "-C", modClasses.toString(), ".")
            .assertSuccess();

        for (String option : new String[]  {"--describe-module", "-d" }) {
            jarWithStdin(modularJar.toFile(),
                         option)
                         .assertSuccess()
                         .resultChecker(r ->
                             assertTrue(r.output.contains(FOO.moduleName + "@" + FOO.version),
                                "Expected to find ", FOO.moduleName + "@" + FOO.version,
                                " in [", r.output, "]")
                );
        }
    }

    /**
     * Validate that you can update a jar only specifying --module-version
     * @throws IOException
     */
    @Test
    public void updateFooModuleVersion() throws IOException {
        Path mp = Paths.get("updateFooModuleVersion");
        createTestDir(mp);
        Path modClasses = MODULE_CLASSES.resolve(FOO.moduleName);
        Path modularJar = mp.resolve(FOO.moduleName + ".jar");
        String newFooVersion = "87.0";

        jar("--create",
            "--file=" + modularJar.toString(),
            "--main-class=" + FOO.mainClass,
            "--module-version=" + FOO.version,
            "--no-manifest",
            "-C", modClasses.toString(), ".")
            .assertSuccess();

        jarWithStdin(modularJar.toFile(), "--describe-module")
                .assertSuccess()
                .resultChecker(r ->
                        assertTrue(r.output.contains(FOO.moduleName + "@" + FOO.version),
                                "Expected to find ", FOO.moduleName + "@" + FOO.version,
                                " in [", r.output, "]")
                );

        jar("--update",
            "--file=" + modularJar.toString(),
            "--module-version=" + newFooVersion)
            .assertSuccess();

        for (String option : new String[]  {"--describe-module", "-d" }) {
            jarWithStdin(modularJar.toFile(),
                         option)
                         .assertSuccess()
                         .resultChecker(r ->
                             assertTrue(r.output.contains(FOO.moduleName + "@" + newFooVersion),
                                "Expected to find ", FOO.moduleName + "@" + newFooVersion,
                                " in [", r.output, "]")
                );
        }
    }

    @DataProvider(name = "autoNames")
    public Object[][] autoNames() {
        return new Object[][] {
            // JAR file name                module-name[@version]
            { "foo.jar",                    "foo" },
            { "foo1.jar",                   "foo1" },
            { "foo4j.jar",                  "foo4j", },
            { "foo-1.2.3.4.jar",            "foo@1.2.3.4" },
            { "foo-bar.jar",                "foo.bar" },
            { "foo-1.2-SNAPSHOT.jar",       "foo@1.2-SNAPSHOT" },
        };
    }

    @Test(dataProvider = "autoNames")
    public void describeAutomaticModule(String jarName, String mid)
        throws IOException
    {
        Path mp = Paths.get("describeAutomaticModule");
        createTestDir(mp);
        Path regularJar = mp.resolve(jarName);
        Path t = Paths.get("t");
        if (Files.notExists(t))
            Files.createFile(t);

        jar("--create",
            "--file=" + regularJar.toString(),
            t.toString())
            .assertSuccess();

        for (String option : new String[]  {"--describe-module", "-d" }) {
            jar(option,
                "--file=" + regularJar.toString())
                .assertSuccess()
                .resultChecker(r -> {
                    assertTrue(r.output.contains("No module descriptor found"));
                    assertTrue(r.output.contains("Derived automatic module"));
                    assertTrue(r.output.contains(mid + " automatic"),
                               "Expected [", "module " + mid,"] in [", r.output, "]");
                    }
                );
        }
    }

    // -- Infrastructure

    static Result jarWithStdin(File stdinSource, String... args) {
        String jar = getJDKTool("jar");
        List<String> commands = new ArrayList<>();
        commands.add(jar);
        if (!TOOL_VM_OPTIONS.isEmpty()) {
            commands.addAll(Arrays.asList(TOOL_VM_OPTIONS.split("\\s+", -1)));
        }
        Stream.of(args).forEach(commands::add);
        ProcessBuilder p = new ProcessBuilder(commands);
        if (stdinSource != null) {
            p.redirectInput(stdinSource);
        }
        return run(p);
    }

    static Result jar(String... args) {
        return run(JAR_TOOL, args);
    }

    static Path compileModule(String mn) throws IOException {
        return compileModule(mn, null);
    }

    static Path compileModule(String mn, Path mp)
        throws IOException
    {
        Path sourcePath = TEST_SRC.resolve("src").resolve(mn);
        Path build = Files.createDirectories(MODULE_CLASSES.resolve(mn));
        javac(build, mp, sourceList(sourcePath));
        return build;
    }

    static void copyResource(Path srcDir, Path dir, String resource)
        throws IOException
    {
        Path dest = dir.resolve(resource);
        Files.deleteIfExists(dest);

        Files.createDirectories(dest.getParent());
        Files.copy(srcDir.resolve(resource), dest);
    }

    static void setupMRJARModuleInfo(String moduleName) throws IOException {
        Path modClasses = MODULE_CLASSES.resolve(moduleName);
        Path metaInfDir = MRJAR_DIR.resolve(moduleName).resolve("META-INF");
        Path versionSection = metaInfDir.resolve("versions").resolve("9");
        createTestDir(versionSection);

        Path versionModuleInfo = versionSection.resolve("module-info.class");
        System.out.println("copying " + modClasses.resolve("module-info.class") + " to " + versionModuleInfo);
        Files.copy(modClasses.resolve("module-info.class"), versionModuleInfo);

        Manifest manifest = new Manifest();
        manifest.getMainAttributes().putValue("Manifest-Version", "1.0");
        manifest.getMainAttributes().putValue("Multi-Release", "true");
        try (OutputStream os = Files.newOutputStream(metaInfDir.resolve("MANIFEST.MF"))) {
            manifest.write(os);
        }
    }

    static ModuleDescriptor getModuleDescriptor(Path jar) {
        ClassLoader cl = ClassLoader.getSystemClassLoader();
        try (JarFile jf = new JarFile(jar.toFile())) {
            JarEntry entry = jf.getJarEntry("module-info.class");
            try (InputStream in = jf.getInputStream(entry)) {
                return ModuleDescriptor.read(in);
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    // Re-enable when there is support in javax.tools for module path
//    static void javac(Path dest, Path... sourceFiles) throws IOException {
//        out.printf("Compiling %d source files %s%n", sourceFiles.length,
//                   Arrays.asList(sourceFiles));
//        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//        try (StandardJavaFileManager fileManager =
//                     compiler.getStandardFileManager(null, null, null)) {
//
//            List<File> files = Stream.of(sourceFiles)
//                                     .map(p -> p.toFile())
//                                     .collect(Collectors.toList());
//            List<File> dests = Stream.of(dest)
//                                     .map(p -> p.toFile())
//                                     .collect(Collectors.toList());
//            Iterable<? extends JavaFileObject> compilationUnits =
//                    fileManager.getJavaFileObjectsFromFiles(files);
//            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, dests);
//            JavaCompiler.CompilationTask task =
//                    compiler.getTask(null, fileManager, null, null, null, compilationUnits);
//            boolean passed = task.call();
//            if (!passed)
//                throw new RuntimeException("Error compiling " + files);
//        }
//    }

    static void javac(Path dest, Path... sourceFiles) throws IOException {
        javac(dest, null, sourceFiles);
    }

    static void javac(Path dest, Path modulePath, Path... sourceFiles)
        throws IOException
    {

        List<String> commands = new ArrayList<>();
        if (!TOOL_VM_OPTIONS.isEmpty()) {
            commands.addAll(Arrays.asList(TOOL_VM_OPTIONS.split("\\s+", -1)));
        }
        commands.add("-d");
        commands.add(dest.toString());
        if (dest.toString().contains("bar")) {
            commands.add("--add-exports");
            commands.add("java.base/jdk.internal.misc=bar");
            commands.add("--add-exports");
            commands.add("java.base/jdk.internal.module=bar");
        }
        if (modulePath != null) {
            commands.add("--module-path");
            commands.add(modulePath.toString());
        }
        Stream.of(sourceFiles).map(Object::toString).forEach(x -> commands.add(x));

        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            int rc = JAVAC_TOOL.run(pw, pw, commands.toArray(new String[0]));
            if(rc != 0) {
                throw new RuntimeException(sw.toString());
            }
        }
    }

    static Result java(Path modulePath, String entryPoint, String... args) {
        String java = getJDKTool("java");

        List<String> commands = new ArrayList<>();
        commands.add(java);
        if (!VM_OPTIONS.isEmpty()) {
            commands.addAll(Arrays.asList(VM_OPTIONS.split("\\s+", -1)));
        }
        if (!JAVA_OPTIONS.isEmpty()) {
            commands.addAll(Arrays.asList(JAVA_OPTIONS.split("\\s+", -1)));
        }
        Stream.of(args).forEach(x -> commands.add(x));
        commands.add("--module-path");
        commands.add(modulePath.toString());
        commands.add("-m");
        commands.add(entryPoint);

        return run(new ProcessBuilder(commands));
    }

    static Path[] sourceList(Path directory) throws IOException {
        return Files.find(directory, Integer.MAX_VALUE,
                          (file, attrs) -> (file.toString().endsWith(".java")))
                    .toArray(Path[]::new);
    }

    static void createTestDir(Path p) throws IOException{
        if (Files.exists(p))
            FileUtils.deleteFileTreeWithRetry(p);
        Files.createDirectories(p);
    }

    static boolean jarContains(JarInputStream jis, String entryName)
        throws IOException
    {
        JarEntry e;
        while((e = jis.getNextJarEntry()) != null) {
            if (e.getName().equals(entryName))
                return true;
        }
        return false;
    }

    static Result run(ToolProvider tp, String[] commands) {
        int rc = 0;
        StringWriter sw = new StringWriter();
        try (PrintWriter pw = new PrintWriter(sw)) {
            rc = tp.run(pw, pw, commands);
        }
        return new Result(rc, sw.toString());
    }

    static Result run(ProcessBuilder pb) {
        Process p;
        out.printf("Running: %s%n", pb.command());
        try {
            p = pb.start();
        } catch (IOException e) {
            throw new RuntimeException(
                    format("Couldn't start process '%s'", pb.command()), e);
        }

        String output;
        try {
            output = toString(p.getInputStream(), p.getErrorStream());
        } catch (IOException e) {
            throw new RuntimeException(
                    format("Couldn't read process output '%s'", pb.command()), e);
        }

        try {
            p.waitFor();
        } catch (InterruptedException e) {
            throw new RuntimeException(
                    format("Process hasn't finished '%s'", pb.command()), e);
        }
        return new Result(p.exitValue(), output);
    }

    static final String DEFAULT_IMAGE_BIN = System.getProperty("java.home")
            + File.separator + "bin" + File.separator;

    static String getJDKTool(String name) {
        try {
            return JDKToolFinder.getJDKTool(name);
        } catch (Exception x) {
            return DEFAULT_IMAGE_BIN + name;
        }
    }

    static String toString(InputStream in1, InputStream in2) throws IOException {
        try (ByteArrayOutputStream dst = new ByteArrayOutputStream();
             InputStream concatenated = new SequenceInputStream(in1, in2)) {
            concatenated.transferTo(dst);
            return new String(dst.toByteArray(), "UTF-8");
        }
    }

    static class Result {
        final int ec;
        final String output;

        private Result(int ec, String output) {
            this.ec = ec;
            this.output = output;
        }
        Result assertSuccess() {
            assertTrue(ec == 0, "Expected ec 0, got: ", ec, " , output [", output, "]");
            return this;
        }
        Result assertFailure() {
            assertTrue(ec != 0, "Expected ec != 0, got:", ec, " , output [", output, "]");
            return this;
        }
        Result resultChecker(Consumer<Result> r) { r.accept(this); return this; }
    }

    static void assertTrue(boolean cond, Object ... failedArgs) {
        if (cond)
            return;
        StringBuilder sb = new StringBuilder();
        for (Object o : failedArgs)
            sb.append(o);
        org.testng.Assert.assertTrue(false, sb.toString());
    }

    // Standalone entry point.
    public static void main(String[] args) throws Throwable {
        Basic test = new Basic();
        test.compileModules();
        for (Method m : Basic.class.getDeclaredMethods()) {
            if (m.getAnnotation(Test.class) != null) {
                System.out.println("Invoking " + m.getName());
                m.invoke(test);
            }
        }
    }
}
