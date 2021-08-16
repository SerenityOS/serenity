/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @modules java.base/jdk.internal.misc
 *          jdk.compiler
 *          jdk.jartool
 * @build jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 *        MRTestBase
 * @run testng Basic1
 */

import org.testng.annotations.*;

import java.nio.file.*;
import java.util.*;

public class Basic1 extends MRTestBase {

    @BeforeTest
    public void setup() throws Throwable {
        String test = "test01";
        Path classes = Paths.get("classes");

        Path base = classes.resolve("base");
        Files.createDirectories(base);
        Path source = Paths.get(src, "data", test, "base", "version");
        javac(base, source.resolve("Main.java"), source.resolve("Version.java"));

        Path v9 = classes.resolve("v9");
        Files.createDirectories(v9);
        source = Paths.get(src, "data", test, "v9", "version");
        javac(v9, source.resolve("Version.java"));

        Path v10 = classes.resolve("v10");
        Files.createDirectories(v10);
        source = Paths.get(src, "data", test, "v10", "version");
        javac(v10, source.resolve("Version.java"));

        Path v10_1 = classes.resolve("v10_1").resolve("META-INF").resolve("versions").resolve("v10");
        Files.createDirectories(v10_1);
        source = Paths.get(src, "data", test, "v10", "version");
        javac(v10_1, source.resolve("Version.java"));
    }

    @Test
    public void test() throws Throwable {
        String jarfile = "test.jar";
        Path classes = Paths.get("classes");

        Path base = classes.resolve("base");
        Path v9 = classes.resolve("v9");
        Path v10 = classes.resolve("v10");

        jar("cf", jarfile, "-C", base.toString(), ".",
                "--release", "9", "-C", v9.toString(), ".",
                "--release", "10", "-C", v10.toString(), ".")
                .shouldHaveExitValue(SUCCESS);

        checkMultiRelease(jarfile, true);

        Map<String, String[]> names = Map.of(
                "version/Main.class",
                new String[]{"base", "version", "Main.class"},

                "version/Version.class",
                new String[]{"base", "version", "Version.class"},

            "META-INF/versions/9/version/Version.class",
            new String[] {"v9", "version", "Version.class"},

            "META-INF/versions/10/version/Version.class",
            new String[] {"v10", "version", "Version.class"}
        );

        compare(jarfile, names);
    }

    @Test
    public void testFail() throws Throwable {
        String jarfile = "test.jar";
        Path classes = Paths.get("classes");
        Path base = classes.resolve("base");
        Path v10 = classes.resolve("v10_1");

        jar("cf", jarfile, "-C", base.toString(), ".",
                "--release", "10", "-C", v10.toString(), ".")
                .shouldNotHaveExitValue(SUCCESS)
                .shouldContain("unexpected versioned entry META-INF/versions/");
    }
}
