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
 * @bug 8168386 8205116
 * @summary Test option validation
 * @modules jdk.jdeps
 * @library lib
 * @build JdepsRunner
 * @run testng Options
 */


import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

public class Options {
    private static final String TEST_CLASSES = System.getProperty("test.classes");

    @DataProvider(name = "errors")
    public Object[][] errors() {
        return new Object[][]{
            {
                new String[] { "-summary", "-v", TEST_CLASSES },
                "-v, -verbose cannot be used with -s, -summary option"
            },
            {
                new String[] { "-jdkinternal", "-summary", TEST_CLASSES },
                "-summary or -verbose cannot be used with -jdkinternals option"
            },
            {
                new String[] { "-jdkinternal", "-p", "java.lang", TEST_CLASSES },
                "--package, --regex, --require cannot be used with -jdkinternals option"
            },
            {
                new String[] { "--missing-deps", "-summary", TEST_CLASSES },
                "-summary or -verbose cannot be used with --missing-deps option"
            },
            {
                new String[] { "--missing-deps", "-p", "java.lang", TEST_CLASSES },
                "--package, --regex, --require cannot be used with --missing-deps option"
            },
            {
                new String[] { "--inverse", TEST_CLASSES },
                "--package (-p), --regex (-e), --require option must be specified"
            },
            {
                new String[] { "--inverse", "-R", TEST_CLASSES },
                "-R cannot be used with --inverse option"
            },
            {
                new String[] { "--generate-module-info", "dots", "-cp", TEST_CLASSES },
                "-classpath cannot be used with --generate-module-info option"
            },
            {
                new String[] { "--list-deps", "-summary", TEST_CLASSES },
                "--list-deps and --list-reduced-deps options are specified"
            },
            {
                new String[] { "--list-deps", "--list-reduced-deps", TEST_CLASSES },
                "--list-deps and --list-reduced-deps options are specified"
            },
        };
    }

    @Test(dataProvider = "errors")
    public void test(String[] options, String expected) {
        jdepsError(options).outputContains(expected);
    }


    public static JdepsRunner jdepsError(String... args) {
        JdepsRunner jdeps = new JdepsRunner(args);
        assertTrue(jdeps.run(true) != 0);
        return jdeps;
    }

    @Test
    public void testSystemOption() {
        JdepsRunner jdeps;

        // valid path
        jdeps = new JdepsRunner("--check", "java.base", "--system", System.getProperty("java.home"));
        assertTrue(jdeps.run(true) == 0);

        // invalid path
        jdeps = new JdepsRunner("--check", "java.base", "--system", "bad");
        assertTrue(jdeps.run(true) != 0);
        jdeps.outputContains("invalid path: bad");
    }
}
