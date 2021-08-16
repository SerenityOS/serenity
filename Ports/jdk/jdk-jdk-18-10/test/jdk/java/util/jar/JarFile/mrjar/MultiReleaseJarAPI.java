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
 * @bug 8132734 8144062 8165723 8199172
 * @summary Test the extended API and the aliasing additions in JarFile that
 *          support multi-release jar files
 * @library /lib/testlibrary/java/util/jar /test/lib
 * @build jdk.test.lib.RandomFactory
 *        CreateMultiReleaseTestJars
 *        jdk.test.lib.compiler.Compiler
 *        jdk.test.lib.util.JarBuilder
 * @run testng MultiReleaseJarAPI
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import jdk.test.lib.RandomFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class MultiReleaseJarAPI {

    private static final Random RANDOM = RandomFactory.getRandom();

    String userdir = System.getProperty("user.dir",".");
    CreateMultiReleaseTestJars creator =  new CreateMultiReleaseTestJars();
    File unversioned = new File(userdir, "unversioned.jar");
    File multirelease = new File(userdir, "multi-release.jar");
    File signedmultirelease = new File(userdir, "signed-multi-release.jar");


    @BeforeClass
    public void initialize() throws Exception {
        creator.compileEntries();
        creator.buildUnversionedJar();
        creator.buildMultiReleaseJar();
        creator.buildSignedMultiReleaseJar();
    }

    @AfterClass
    public void close() throws IOException {
        Files.delete(unversioned.toPath());
        Files.delete(multirelease.toPath());
        Files.delete(signedmultirelease.toPath());
    }

    @Test
    public void isMultiReleaseJar() throws Exception {
        try (JarFile jf = new JarFile(unversioned)) {
            Assert.assertFalse(jf.isMultiRelease());
        }

        try (JarFile jf = new JarFile(unversioned, true, ZipFile.OPEN_READ, Runtime.version())) {
            Assert.assertFalse(jf.isMultiRelease());
        }

        try (JarFile jf = new JarFile(multirelease)) {
            Assert.assertTrue(jf.isMultiRelease());
        }

        try (JarFile jf = new JarFile(multirelease, true, ZipFile.OPEN_READ, Runtime.version())) {
            Assert.assertTrue(jf.isMultiRelease());
        }

        testCustomMultiReleaseValue("true", true);
        testCustomMultiReleaseValue("true\r\nOther: value", true);
        testCustomMultiReleaseValue("true\nOther: value", true);
        // JDK-8200530: '\r' support in Manifest/Attributes will be addressed separately
        // testCustomMultiReleaseValue("true\rOther: value", true);

        testCustomMultiReleaseValue("false", false);
        testCustomMultiReleaseValue(" true", false);
        testCustomMultiReleaseValue("true ", false);
        testCustomMultiReleaseValue("true\n true", false);

        // JDK-8200530: '\r' support in Manifest/Attributes will be addressed separately
        testCustomMultiReleaseValue("true\r true", false);
        testCustomMultiReleaseValue("true\r\n true", false);

        // "Multi-Release: true/false" not in main attributes
        testCustomMultiReleaseValue("\r\n\r\nName: test\r\nMulti-Release: true\r\n",
                                    false);
        testCustomMultiReleaseValue("\n\nName: entryname\nMulti-Release: true\n",
                                    false);
        testCustomMultiReleaseValue("EndOfMainAttr: whatever\r\n" +
                                    "\r\nName: entryname\r\nMulti-Release: true\r\n",
                                    false);
        testCustomMultiReleaseValue("EndOfMainAttr: whatever\r\n" +
                                    "\nName: entryname\nMulti-Release: true\n",
                                    false);

        // generate "random" Strings to use as extra attributes, and
        // verify that Multi-Release: true is always properly matched
        for (int i = 0; i < 100; i++) {
            byte[] keyBytes = new byte[RANDOM.nextInt(70) + 1];
            Arrays.fill(keyBytes, (byte)('a' + RANDOM.nextInt(24)));
            byte[] valueBytes = new byte[RANDOM.nextInt(70) + 1];
            Arrays.fill(valueBytes, (byte)('a' + RANDOM.nextInt(24)));

            String key = new String(keyBytes, StandardCharsets.UTF_8);
            String value = new String(valueBytes, StandardCharsets.UTF_8);
            // test that Multi-Release: true anywhere in the manifest always
            // return true
            testCustomMultiReleaseValue("true", Map.of(key, value), true);

            // test that we don't get any false positives
            testCustomMultiReleaseValue("false", Map.of(key, value), false);
        }
    }

    private void testCustomMultiReleaseValue(String value, boolean expected)
            throws Exception {
        testCustomMultiReleaseValue(value, Map.of(), expected);
    }

    private static final AtomicInteger JAR_COUNT = new AtomicInteger(0);

    private void testCustomMultiReleaseValue(String value,
            Map<String, String> extraAttributes, boolean expected)
            throws Exception {
        String fileName = "custom-mr" + JAR_COUNT.incrementAndGet() + ".jar";
        creator.buildCustomMultiReleaseJar(fileName, value, extraAttributes);
        File custom = new File(userdir, fileName);
        try (JarFile jf = new JarFile(custom, true, ZipFile.OPEN_READ, Runtime.version())) {
            Assert.assertEquals(jf.isMultiRelease(), expected);
        }
        Files.delete(custom.toPath());
    }

    @DataProvider(name = "versions")
    public Object[][] createVersionData() throws Exception {
        return new Object[][]{
                {JarFile.baseVersion(), 8},
                {JarFile.runtimeVersion(), Runtime.version().major()},
                {Runtime.version(), Runtime.version().major()},
                {Runtime.Version.parse("7.1"), JarFile.baseVersion().major()},
                {Runtime.Version.parse("9"), 9},
                {Runtime.Version.parse("9.1.5-ea+200"), 9}
        };
    }

    @Test(dataProvider="versions")
    public void testVersioning(Runtime.Version value, int xpected) throws Exception {
        Runtime.Version expected = Runtime.Version.parse(String.valueOf(xpected));
        Runtime.Version base = JarFile.baseVersion();

        // multi-release jar, opened as unversioned
        try (JarFile jar = new JarFile(multirelease)) {
            Assert.assertEquals(jar.getVersion(), base);
        }

        System.err.println("test versioning for Release " + value);
        try (JarFile jf = new JarFile(multirelease, true, ZipFile.OPEN_READ, value)) {
            Assert.assertEquals(jf.getVersion(), expected);
        }

        // regular, unversioned, jar
        try (JarFile jf = new JarFile(unversioned, true, ZipFile.OPEN_READ, value)) {
            Assert.assertEquals(jf.getVersion(), base);
        }
    }

    @Test(dataProvider="versions")
    public void testAliasing(Runtime.Version version, int xpected) throws Exception {
        int n = Math.max(version.major(), JarFile.baseVersion().major());
        Runtime.Version value = Runtime.Version.parse(String.valueOf(n));
        System.err.println("test aliasing for Release " + version);
        String prefix;
        if (JarFile.baseVersion().equals(value)) {
            prefix = "";
        } else {
            prefix = "META-INF/versions/" + value.major() + "/";
        }
        // test both multi-release jars
        readAndCompare(multirelease, value, "README", prefix + "README");
        readAndCompare(multirelease, value, "version/Version.class", prefix + "version/Version.class");
        // and signed multi-release jars
        readAndCompare(signedmultirelease, value, "README", prefix + "README");
        readAndCompare(signedmultirelease, value, "version/Version.class", prefix + "version/Version.class");
    }

    private void readAndCompare(File jar, Runtime.Version version, String name, String realName) throws Exception {
        byte[] baseBytes;
        byte[] versionedBytes;
        try (JarFile jf = new JarFile(jar, true, ZipFile.OPEN_READ, JarFile.baseVersion())) {
            ZipEntry ze = jf.getEntry(realName);
            try (InputStream is = jf.getInputStream(ze)) {
                baseBytes = is.readAllBytes();
            }
        }
        assert baseBytes.length > 0;

        try (JarFile jf = new JarFile(jar, true, ZipFile.OPEN_READ, version)) {
            ZipEntry ze = jf.getEntry(name);
            try (InputStream is = jf.getInputStream(ze)) {
                versionedBytes = is.readAllBytes();
            }
        }
        assert versionedBytes.length > 0;

        Assert.assertTrue(Arrays.equals(baseBytes, versionedBytes));
    }

    @Test
    public void testNames() throws Exception {
        String rname = "version/Version.class";
        String vname = "META-INF/versions/9/version/Version.class";
        ZipEntry ze1;
        ZipEntry ze2;
        try (JarFile jf = new JarFile(multirelease)) {
            ze1 = jf.getEntry(vname);
        }
        Assert.assertEquals(ze1.getName(), vname);
        try (JarFile jf = new JarFile(multirelease, true, ZipFile.OPEN_READ, Runtime.Version.parse("9"))) {
            ze2 = jf.getEntry(rname);
        }
        Assert.assertEquals(ze2.getName(), rname);
        Assert.assertNotEquals(ze1.getName(), ze2.getName());
    }
}
