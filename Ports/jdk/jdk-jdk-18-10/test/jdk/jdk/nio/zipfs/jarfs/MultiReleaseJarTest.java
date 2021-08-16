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
 * @bug 8144355 8144062 8176709 8194070 8193802 8231093
 * @summary Test aliasing additions to ZipFileSystem for multi-release jar files
 * @library /lib/testlibrary/java/util/jar /test/lib/
 * @modules jdk.compiler
 *          jdk.jartool
 *          jdk.zipfs
 * @build CreateMultiReleaseTestJars
 *        jdk.test.lib.util.JarBuilder
 *        jdk.test.lib.compiler.Compiler
 * @run testng MultiReleaseJarTest
 */

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.Runtime.Version;
import java.net.URI;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.Assert;
import org.testng.annotations.*;
import jdk.test.lib.util.JarBuilder;

public class MultiReleaseJarTest {
    final private int MAJOR_VERSION = Runtime.version().feature();
    private static final String PROPERTY_RELEASE_VERSION = "releaseVersion";
    private static final String PROPERTY_MULTI_RELEASE = "multi-release";

    final private String userdir = System.getProperty("user.dir",".");
    final private CreateMultiReleaseTestJars creator =  new CreateMultiReleaseTestJars();
    final private Map<String,String> stringEnv = new HashMap<>();
    final private Map<String,Integer> integerEnv = new HashMap<>();
    final private Map<String,Version> versionEnv = new HashMap<>();
    final private String className = "version.Version";
    final private MethodType mt = MethodType.methodType(int.class);

    private String entryName;
    private URI uvuri;
    private URI mruri;
    private URI smruri;

    @BeforeClass
    public void initialize() throws Exception {
        creator.compileEntries();
        creator.buildUnversionedJar();
        creator.buildMultiReleaseJar();
        creator.buildShortMultiReleaseJar();
        String ssp = Paths.get(userdir, "unversioned.jar").toUri().toString();
        uvuri = new URI("jar", ssp , null);
        ssp = Paths.get(userdir, "multi-release.jar").toUri().toString();
        mruri = new URI("jar", ssp, null);
        ssp = Paths.get(userdir, "short-multi-release.jar").toUri().toString();
        smruri = new URI("jar", ssp, null);
        entryName = className.replace('.', '/') + ".class";
    }

    public void close() throws IOException {
        Files.delete(Paths.get(userdir, "unversioned.jar"));
        Files.delete(Paths.get(userdir, "multi-release.jar"));
        Files.delete(Paths.get(userdir, "short-multi-release.jar"));
    }

    @DataProvider(name="strings")
    public Object[][] createStrings() {
        return new Object[][]{
                {"runtime", MAJOR_VERSION, "8"},
                {null, 8, Integer.toString(MAJOR_VERSION)},
                {"8", 8, "9"},
                {"9", 9, null},
                {Integer.toString(MAJOR_VERSION), MAJOR_VERSION, "8"},
                {Integer.toString(MAJOR_VERSION+1), MAJOR_VERSION, "8"},
                {"50", MAJOR_VERSION, "9"}
        };
    }

    @DataProvider(name="integers")
    public Object[][] createIntegers() {
        return new Object[][] {
                {null, 8, Integer.valueOf(9)},
                {Integer.valueOf(8), 8, Integer.valueOf(9)},
                {Integer.valueOf(9), 9, Integer.valueOf(MAJOR_VERSION)},
                {Integer.valueOf(MAJOR_VERSION), MAJOR_VERSION, Integer.valueOf(8)},
                {Integer.valueOf(MAJOR_VERSION + 1), MAJOR_VERSION, null},
                {Integer.valueOf(100), MAJOR_VERSION, Integer.valueOf(8)}
        };
    }

    @DataProvider(name="versions")
    public Object[][] createVersions() {
        return new Object[][] {
                {null, 8, Version.parse("14")},
                {Version.parse("8"), 8, Version.parse("7")},
                {Version.parse("9"), 9, null},
                {Version.parse(Integer.toString(MAJOR_VERSION)), MAJOR_VERSION, Version.parse("8")},
                {Version.parse(Integer.toString(MAJOR_VERSION) + 1), MAJOR_VERSION, Version.parse("9")},
                {Version.parse("100"), MAJOR_VERSION, Version.parse("14")}
        };
    }

    @DataProvider(name="invalidVersions")
    public Object[][] invalidVersions() {
        return new Object[][] {
                {Map.of(PROPERTY_RELEASE_VERSION, "")},
                {Map.of(PROPERTY_RELEASE_VERSION, "invalid")},
                {Map.of(PROPERTY_RELEASE_VERSION, "0")},
                {Map.of(PROPERTY_RELEASE_VERSION, "-1")},
                {Map.of(PROPERTY_RELEASE_VERSION, "11.0.1")},
                {Map.of(PROPERTY_RELEASE_VERSION, new ArrayList<Long>())},
                {Map.of(PROPERTY_RELEASE_VERSION, Integer.valueOf(0))},
                {Map.of(PROPERTY_RELEASE_VERSION, Integer.valueOf(-1))}
        };
    }

    // Not the best test but all I can do since ZipFileSystem
    // is not public, so I can't use (fs instanceof ...)
    @Test
    public void testNewFileSystem() throws Exception {
        Map<String,String> env = new HashMap<>();
        // no configuration, treat multi-release jar as unversioned
        try (FileSystem fs = FileSystems.newFileSystem(mruri, env)) {
            Assert.assertTrue(readAndCompare(fs, 8));
        }
        env.put(PROPERTY_RELEASE_VERSION, "runtime");
        // a configuration and jar file is multi-release
        try (FileSystem fs = FileSystems.newFileSystem(mruri, env)) {
            Assert.assertTrue(readAndCompare(fs, MAJOR_VERSION));
        }
        // a configuration but jar file is unversioned
        try (FileSystem fs = FileSystems.newFileSystem(uvuri, env)) {
            Assert.assertTrue(readAndCompare(fs, 8));
        }
    }

    private boolean readAndCompare(FileSystem fs, int expected) throws IOException {
        Path path = fs.getPath("version/Version.java");
        String src = new String(Files.readAllBytes(path));
        return src.contains("return " + expected);
    }

    @Test(dataProvider="strings")
    public void testStrings(String value, int expected, String ignorable) throws Throwable {
        stringEnv.clear();
        stringEnv.put(PROPERTY_RELEASE_VERSION, value);
        // we check, that values for "multi-release" are ignored
        stringEnv.put(PROPERTY_MULTI_RELEASE, ignorable);
        runTest(stringEnv, expected);
    }

    @Test(dataProvider="integers")
    public void testIntegers(Integer value, int expected, Integer ignorable) throws Throwable {
        integerEnv.clear();
        integerEnv.put(PROPERTY_RELEASE_VERSION, value);
        // we check, that values for "multi-release" are ignored
        integerEnv.put(PROPERTY_MULTI_RELEASE, value);
        runTest(integerEnv, expected);
    }

    @Test(dataProvider="versions")
    public void testVersions(Version value, int expected, Version ignorable) throws Throwable {
        versionEnv.clear();
        versionEnv.put(PROPERTY_RELEASE_VERSION, value);
        // we check, that values for "multi-release" are ignored
        versionEnv.put(PROPERTY_MULTI_RELEASE, ignorable);
        runTest(versionEnv, expected);
    }

    @Test
    public void testShortJar() throws Throwable {
        integerEnv.clear();
        integerEnv.put(PROPERTY_RELEASE_VERSION, Integer.valueOf(MAJOR_VERSION));
        runTest(smruri, integerEnv, MAJOR_VERSION);
        integerEnv.put(PROPERTY_RELEASE_VERSION, Integer.valueOf(9));
        runTest(smruri, integerEnv, 8);
    }

    /**
     * Validate that an invalid value for the "releaseVersion" property throws
     * an {@code IllegalArgumentException}
     * @param env Zip FS map
     * @throws Throwable  Exception thrown for anything other than the expected
     * IllegalArgumentException
     */
    @Test(dataProvider="invalidVersions")
    public void testInvalidVersions(Map<String,?> env) throws Throwable {
        Assert.assertThrows(IllegalArgumentException.class, () ->
                FileSystems.newFileSystem(Path.of(userdir,
                        "multi-release.jar"), env));
    }

    // The following tests are for backwards compatibility to validate that
    // the original property still works
    @Test(dataProvider="strings")
    public void testMRStrings(String value, int expected, String ignorable) throws Throwable {
        stringEnv.clear();
        stringEnv.put(PROPERTY_MULTI_RELEASE, value);
        runTest(stringEnv, expected);
    }

    @Test(dataProvider="integers")
    public void testMRIntegers(Integer value, int expected, Integer ignorable) throws Throwable {
        integerEnv.clear();
        integerEnv.put(PROPERTY_MULTI_RELEASE, value);
        runTest(integerEnv, expected);
    }

    @Test(dataProvider="versions")
    public void testMRVersions(Version value, int expected, Version ignorable) throws Throwable {
        versionEnv.clear();
        versionEnv.put(PROPERTY_MULTI_RELEASE, value);
        runTest(versionEnv, expected);
    }

    private void runTest(Map<String,?> env, int expected) throws Throwable {
        runTest(mruri, env, expected);
    }

    private void runTest(URI uri, Map<String,?> env, int expected) throws Throwable {
        try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
            Path version = fs.getPath(entryName);
            byte [] bytes = Files.readAllBytes(version);
            Class<?> vcls = (new ByteArrayClassLoader(fs)).defineClass(className, bytes);
            MethodHandle mh = MethodHandles.lookup().findVirtual(vcls, "getVersion", mt);
            Assert.assertEquals((int)mh.invoke(vcls.getDeclaredConstructor().newInstance()), expected);
        }
    }

    @Test
    public void testIsMultiReleaseJar() throws Exception {
        // Re-examine commented out tests as part of JDK-8176843
        testCustomMultiReleaseValue("true", true);
        testCustomMultiReleaseValue("true\r\nOther: value", true);
        testCustomMultiReleaseValue("true\nOther: value", true);
        //testCustomMultiReleaseValue("true\rOther: value", true);

        testCustomMultiReleaseValue("false", false);
        testCustomMultiReleaseValue(" true", false);
        testCustomMultiReleaseValue("true ", false);
        //testCustomMultiReleaseValue("true\n ", false);
        //testCustomMultiReleaseValue("true\r ", false);
        //testCustomMultiReleaseValue("true\n true", false);
        //testCustomMultiReleaseValue("true\r\n true", false);
    }

    @Test
    public void testMultiReleaseJarWithNonVersionDir() throws Exception {
        String jfname = "multi-release-non-ver.jar";
        Path jfpath = Paths.get(jfname);
        URI uri = new URI("jar", jfpath.toUri().toString() , null);
        JarBuilder jb = new JarBuilder(jfname);
        jb.addAttribute("Multi-Release", "true");
        jb.build();
        Map<String,String> env = Map.of(PROPERTY_RELEASE_VERSION, "runtime");
        try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
            Assert.assertTrue(true);
        }
        Files.delete(jfpath);
    }

    private static final AtomicInteger JAR_COUNT = new AtomicInteger(0);

    private void testCustomMultiReleaseValue(String value, boolean expected)
            throws Exception {
        String fileName = "custom-mr" + JAR_COUNT.incrementAndGet() + ".jar";
        creator.buildCustomMultiReleaseJar(fileName, value, Map.of(),
                /*addEntries*/true);

        Map<String,String> env = Map.of(PROPERTY_RELEASE_VERSION, "runtime");
        Path filePath = Paths.get(userdir, fileName);
        String ssp = filePath.toUri().toString();
        URI customJar = new URI("jar", ssp , null);
        try (FileSystem fs = FileSystems.newFileSystem(customJar, env)) {
            if (expected) {
                Assert.assertTrue(readAndCompare(fs, MAJOR_VERSION));
            } else {
                Assert.assertTrue(readAndCompare(fs, 8));
            }
        }
        Files.delete(filePath);
    }

    private static class ByteArrayClassLoader extends ClassLoader {
        final private FileSystem fs;

        ByteArrayClassLoader(FileSystem fs) {
            super(null);
            this.fs = fs;
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            try {
                return super.loadClass(name);
            } catch (ClassNotFoundException x) {}
            Path cls = fs.getPath(name.replace('.', '/') + ".class");
            try {
                byte[] bytes = Files.readAllBytes(cls);
                return defineClass(name, bytes);
            } catch (IOException x) {
                throw new ClassNotFoundException(x.getMessage());
            }
        }

        public Class<?> defineClass(String name, byte[] bytes) throws ClassNotFoundException {
            if (bytes == null) throw new ClassNotFoundException("No bytes for " + name);
            return defineClass(name, bytes, 0, bytes.length);
        }
    }
}
