/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 # @bug 8186087 8196748 8212807
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          jdk.compiler
 *          jdk.jartool
 * @build jdk.test.lib.util.FileUtils
 *        jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 *        MRTestBase
 * @run testng Basic
 */

import jdk.test.lib.util.FileUtils;
import org.testng.annotations.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;
import java.util.zip.ZipFile;

import static org.testng.Assert.*;

public class Basic extends MRTestBase {

    @Test
    // create a regular, non-multi-release jar
    public void test00() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");  //use same data as test01

        Path classes = Paths.get("classes");
        jarTool("cf", jarfile, "-C", classes.resolve("base").toString(), ".")
                .shouldHaveExitValue(SUCCESS);

        checkMultiRelease(jarfile, false);

        Map<String, String[]> names = Map.of(
                "version/Main.class",
                new String[]{"base", "version", "Main.class"},

                "version/Version.class",
                new String[]{"base", "version", "Version.class"}
        );

        compare(jarfile, names);

        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }

    @Test
    // create a multi-release jar
    public void test01() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");

        Path classes = Paths.get("classes");
        jarTool("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "9", "-C", classes.resolve("v9").toString(), ".",
                "--release", "10", "-C", classes.resolve("v10").toString(), ".")
                .shouldHaveExitValue(SUCCESS);

        checkMultiRelease(jarfile, true);

        Map<String, String[]> names = Map.of(
                "version/Main.class",
                new String[]{"base", "version", "Main.class"},

                "version/Version.class",
                new String[]{"base", "version", "Version.class"},

                "META-INF/versions/9/version/Version.class",
                new String[]{"v9", "version", "Version.class"},

                "META-INF/versions/10/version/Version.class",
                new String[]{"v10", "version", "Version.class"}
        );

        compare(jarfile, names);

        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }

    @Test
    public void versionFormat() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");

        Path classes = Paths.get("classes");

        // valid
        for (String release : List.of("10000", "09", "00010", "10")) {
            jarTool("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                    "--release", release, "-C", classes.resolve("v10").toString(), ".")
                    .shouldHaveExitValue(SUCCESS)
                    .shouldBeEmptyIgnoreVMWarnings();
        }
        // invalid
        for (String release : List.of("9.0", "8", "v9",
                "9v", "0", "-10")) {
            jarTool("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                    "--release", release, "-C", classes.resolve("v10").toString(), ".")
                    .shouldNotHaveExitValue(SUCCESS)
                    .shouldContain("release " + release + " not valid");
        }
        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }

    @Test
    // update a regular jar to a multi-release jar
    public void test02() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");  //use same data as test01

        Path classes = Paths.get("classes");
        jarTool("cf", jarfile, "-C", classes.resolve("base").toString(), ".")
                .shouldHaveExitValue(SUCCESS);

        checkMultiRelease(jarfile, false);

        jarTool("uf", jarfile,
                "--release", "9", "-C", classes.resolve("v9").toString(), ".")
                .shouldHaveExitValue(SUCCESS);

        checkMultiRelease(jarfile, true);

        Map<String, String[]> names = Map.of(
                "version/Main.class",
                new String[]{"base", "version", "Main.class"},

                "version/Version.class",
                new String[]{"base", "version", "Version.class"},

                "META-INF/versions/9/version/Version.class",
                new String[]{"v9", "version", "Version.class"}
        );

        compare(jarfile, names);

        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }

    @Test
    // replace a base entry and a versioned entry
    public void test03() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");  //use same data as test01

        Path classes = Paths.get("classes");
        jarTool("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "9", "-C", classes.resolve("v9").toString(), ".")
                .shouldHaveExitValue(SUCCESS);

        checkMultiRelease(jarfile, true);

        Map<String, String[]> names = Map.of(
                "version/Main.class",
                new String[]{"base", "version", "Main.class"},

                "version/Version.class",
                new String[]{"base", "version", "Version.class"},

                "META-INF/versions/9/version/Version.class",
                new String[]{"v9", "version", "Version.class"}
        );

        compare(jarfile, names);

        // write the v9 version/Version.class entry in base and the v10
        // version/Version.class entry in versions/9 section
        jarTool("uf", jarfile, "-C", classes.resolve("v9").toString(), "version",
                "--release", "9", "-C", classes.resolve("v10").toString(), ".")
                .shouldHaveExitValue(SUCCESS);

        checkMultiRelease(jarfile, true);

        names = Map.of(
                "version/Main.class",
                new String[]{"base", "version", "Main.class"},

                "version/Version.class",
                new String[]{"v9", "version", "Version.class"},

                "META-INF/versions/9/version/Version.class",
                new String[]{"v10", "version", "Version.class"}
        );

        compare(jarfile, names);

        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }

    /*
     * The following tests exercise the jar validator
     */

    @Test
    // META-INF/versions/9 class has different api than base class
    public void test04() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");  //use same data as test01

        Path classes = Paths.get("classes");

        // replace the v9 class
        Path source = Paths.get(src, "data", "test04", "v9", "version");
        javac(classes.resolve("v9"), source.resolve("Version.java"));

        jarTool("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "9", "-C", classes.resolve("v9").toString(), ".")
                .shouldNotHaveExitValue(SUCCESS)
                .shouldContain("different api from earlier");

        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }

    @Test
    // META-INF/versions/9 contains an extra public class
    public void test05() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");  //use same data as test01

        Path classes = Paths.get("classes");

        // add the new v9 class
        Path source = Paths.get(src, "data", "test05", "v9", "version");
        javac(classes.resolve("v9"), source.resolve("Extra.java"));

        jarTool("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "9", "-C", classes.resolve("v9").toString(), ".")
                .shouldNotHaveExitValue(SUCCESS)
                .shouldContain("contains a new public class");

        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }

    @Test
    // META-INF/versions/9 contains an extra package private class -- this is okay
    public void test06() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");  //use same data as test01

        Path classes = Paths.get("classes");

        // add the new v9 class
        Path source = Paths.get(src, "data", "test06", "v9", "version");
        javac(classes.resolve("v9"), source.resolve("Extra.java"));

        jarTool("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "9", "-C", classes.resolve("v9").toString(), ".")
                .shouldHaveExitValue(SUCCESS);

        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }

    @Test
    // META-INF/versions/9 contains an identical class to base entry class
    // this is okay but produces warning
    public void test07() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");  //use same data as test01

        Path classes = Paths.get("classes");

        // add the new v9 class
        Path source = Paths.get(src, "data", "test01", "base", "version");
        javac(classes.resolve("v9"), source.resolve("Version.java"));

        jarTool("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "9", "-C", classes.resolve("v9").toString(), ".")
                .shouldHaveExitValue(SUCCESS)
                .shouldContain("contains a class that")
                .shouldContain("is identical");

        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }

    @Test
    // META-INF/versions/9 contains an identical class to previous version entry class
    // this is okay but produces warning
    public void identicalClassToPreviousVersion() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");  //use same data as test01

        Path classes = Paths.get("classes");

        jarTool("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "9", "-C", classes.resolve("v9").toString(), ".")
                .shouldHaveExitValue(SUCCESS)
                .shouldBeEmptyIgnoreVMWarnings();
        jarTool("uf", jarfile,
                "--release", "10", "-C", classes.resolve("v9").toString(), ".")
                .shouldHaveExitValue(SUCCESS)
                .shouldContain("contains a class that")
                .shouldContain("is identical");

        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }

    @Test
    // a class with an internal name different from the external name
    public void test09() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");  //use same data as test01

        Path classes = Paths.get("classes");

        Path base = classes.resolve("base").resolve("version");

        Files.copy(base.resolve("Main.class"), base.resolve("Foo.class"));

        jarTool("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "9", "-C", classes.resolve("v9").toString(), ".")
                .shouldNotHaveExitValue(SUCCESS)
                .shouldContain("names do not match");

        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }

    @Test
    // assure that basic nested classes are acceptable
    public void test10() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");  //use same data as test01

        Path classes = Paths.get("classes");

        // add a base class with a nested class
        Path source = Paths.get(src, "data", "test10", "base", "version");
        javac(classes.resolve("base"), source.resolve("Nested.java"));

        // add a versioned class with a nested class
        source = Paths.get(src, "data", "test10", "v9", "version");
        javac(classes.resolve("v9"), source.resolve("Nested.java"));

        jarTool("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "9", "-C", classes.resolve("v9").toString(), ".")
                .shouldHaveExitValue(SUCCESS);

        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }

    @Test
    // a base entry contains a nested class that doesn't have a matching top level class
    public void test11() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");  //use same data as test01

        Path classes = Paths.get("classes");

        // add a base class with a nested class
        Path source = Paths.get(src, "data", "test10", "base", "version");
        javac(classes.resolve("base"), source.resolve("Nested.java"));

        // remove the top level class, thus isolating the nested class
        Files.delete(classes.resolve("base").resolve("version").resolve("Nested.class"));

        // add a versioned class with a nested class
        source = Paths.get(src, "data", "test10", "v9", "version");
        javac(classes.resolve("v9"), source.resolve("Nested.java"));

        List<String> output = jarTool("cf", jarfile,
                "-C", classes.resolve("base").toString(), ".",
                "--release", "9", "-C", classes.resolve("v9").toString(), ".")
                .shouldNotHaveExitValue(SUCCESS)
                .asLinesWithoutVMWarnings();

        /* "META-INF/versions/9/version/Nested$nested.class" is really NOT isolated
        assertTrue(output.size() == 4);
        assertTrue(output.size() == 3);
        assertTrue(output.get(0).contains("an isolated nested class"),
                output.get(0));
        assertTrue(output.get(1).contains("contains a new public class"),
                output.get(1));
        assertTrue(output.get(2).contains("an isolated nested class"),
                output.get(2));
        assertTrue(output.get(3).contains("invalid multi-release jar file"),
                output.get(3));
        assertTrue(output.get(2).contains("invalid multi-release jar file"),
               output.get(2));
        */

        assertTrue(output.size() == 3);
        assertTrue(output.get(0).contains("an isolated nested class"),
                output.get(0));
        assertTrue(output.get(1).contains("contains a new public class"),
                output.get(1));
        assertTrue(output.get(2).contains("invalid multi-release jar file"),
               output.get(2));

        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }

    @Test
    // a versioned entry contains a nested class that doesn't have a matching top level class
    public void test12() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");  //use same data as test01

        Path classes = Paths.get("classes");

        // add a base class with a nested class
        Path source = Paths.get(src, "data", "test10", "base", "version");
        javac(classes.resolve("base"), source.resolve("Nested.java"));

        // add a versioned class with a nested class
        source = Paths.get(src, "data", "test10", "v9", "version");
        javac(classes.resolve("v9"), source.resolve("Nested.java"));

        // remove the top level class, thus isolating the nested class
        Files.delete(classes.resolve("v9").resolve("version").resolve("Nested.class"));

        jarTool("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "9", "-C", classes.resolve("v9").toString(), ".")
                .shouldNotHaveExitValue(SUCCESS)
                .shouldContain("an isolated nested class");

        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }

    @Test
    // assure the nested-nested classes are acceptable
    public void test13() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");  //use same data as test01

        Path classes = Paths.get("classes");

        // add a base class with a nested and nested-nested class
        Path source = Paths.get(src, "data", "test13", "base", "version");
        javac(classes.resolve("base"), source.resolve("Nested.java"));

        // add a versioned class with a nested and nested-nested class
        source = Paths.get(src, "data", "test13", "v10", "version");
        javac(classes.resolve("v10"), source.resolve("Nested.java"));

        jarTool("cf", jarfile, "-C", classes.resolve("base").toString(), ".",
                "--release", "10", "-C", classes.resolve("v10").toString(), ".")
                .shouldHaveExitValue(SUCCESS);

        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }

    @Test
    public void testCustomManifest() throws Throwable {
        String jarfile = "test.jar";

        compile("test01");

        Path classes = Paths.get("classes");
        Path manifest = Paths.get("Manifest.txt");

        // create
        Files.write(manifest, "Class-Path: MyUtils.jar\n".getBytes());

        jarTool("cfm", jarfile, manifest.toString(),
                "-C", classes.resolve("base").toString(), ".",
                "--release", "10", "-C", classes.resolve("v10").toString(), ".")
                .shouldHaveExitValue(SUCCESS)
                .shouldBeEmptyIgnoreVMWarnings();

        try (JarFile jf = new JarFile(new File(jarfile), true,
                ZipFile.OPEN_READ, JarFile.runtimeVersion())) {
            assertTrue(jf.isMultiRelease(), "Not multi-release jar");
            assertEquals(jf.getManifest()
                            .getMainAttributes()
                            .getValue("Class-Path"),
                    "MyUtils.jar");
        }

        // update
        Files.write(manifest, "Multi-release: false\n".getBytes());

        jar("ufm", jarfile, manifest.toString(),
                "-C", classes.resolve("base").toString(), ".",
                "--release", "9", "-C", classes.resolve("v10").toString(), ".")
                .shouldHaveExitValue(SUCCESS)
                .shouldContain("WARNING: Duplicate name in Manifest: Multi-release.");

        try (JarFile jf = new JarFile(new File(jarfile), true,
                ZipFile.OPEN_READ, JarFile.runtimeVersion())) {
            assertTrue(jf.isMultiRelease(), "Not multi-release jar");
            assertEquals(jf.getManifest()
                            .getMainAttributes()
                            .getValue("Class-Path"),
                    "MyUtils.jar");
        }

        FileUtils.deleteFileIfExistsWithRetry(Paths.get(jarfile));
        FileUtils.deleteFileTreeWithRetry(Paths.get(usr, "classes"));
    }
}
