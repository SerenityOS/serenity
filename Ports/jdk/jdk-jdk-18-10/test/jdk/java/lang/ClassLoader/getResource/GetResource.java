/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6760902
 * @library /test/lib
 * @build jdk.test.lib.process.ProcessTools
 * @run testng GetResource
 * @summary Empty path on bootclasspath is not default to current working
 *          directory for both class lookup and resource lookup whereas
 *          empty path on classpath is default to current working directory.
 */

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.test.lib.JDKToolFinder;
import static jdk.test.lib.process.ProcessTools.*;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class GetResource {
    private static final Path CWD = Paths.get(System.getProperty("user.dir"));
    private static final String DIR_A = "a";
    private static final String DIR_B = "b";

    private static final String RESOURCE_NAME = "test.properties";
    private static final String GETRESOURCE_CLASS = "GetResource.class";

    public static void main(String... args) {
        String expect = args[0] + "/" + RESOURCE_NAME;
        URL url = GetResource.class.getResource(RESOURCE_NAME);
        System.out.println("getResource found: " + url);
        if (!url.toString().endsWith(expect)) {
            throw new RuntimeException(url + " != expected resource " + expect);
        }

        url = ClassLoader.getSystemResource(RESOURCE_NAME);
        System.out.println("getSystemResource found: " + url);
        if (!url.toString().endsWith(expect)) {
            throw new RuntimeException(url + " != expected resource " + expect);
        }
    }

    @BeforeTest
    public void setup() throws IOException {
        // setup two directories "a" and "b"
        // each directory contains both test.properties and this test class
        Path testSrc = Paths.get(System.getProperty("test.src"));
        Path testClasses = Paths.get(System.getProperty("test.classes"));

        Files.createDirectories(Paths.get(DIR_A));
        Files.createDirectories(Paths.get(DIR_B));

        Files.copy(testSrc.resolve(RESOURCE_NAME),
                   Paths.get(DIR_A, RESOURCE_NAME));
        Files.copy(testSrc.resolve(RESOURCE_NAME),
                   Paths.get(DIR_B, RESOURCE_NAME));

        Files.copy(testClasses.resolve(GETRESOURCE_CLASS),
                   Paths.get(DIR_A, GETRESOURCE_CLASS));
        Files.copy(testClasses.resolve(GETRESOURCE_CLASS),
                   Paths.get(DIR_B, GETRESOURCE_CLASS));
    }

    private String concat(String... dirs) {
        return Stream.of(dirs).collect(Collectors.joining(File.pathSeparator));
    }

    @DataProvider
    public Object[][] options() {
        return new Object[][] {
            new Object[] { List.of("-Xbootclasspath/a:a"), "a"},
            new Object[] { List.of("-Xbootclasspath/a:b"), "b"},
            new Object[] { List.of("-Xbootclasspath/a:" + concat("a", "b")), "a"},
            new Object[] { List.of("-Xbootclasspath/a:" + concat("b", "a")), "b"},

            new Object[] { List.of("-cp", "a"), "a"},
            new Object[] { List.of("-cp", "b"), "b"},
            new Object[] { List.of("-cp", concat("a", "b")), "a"},
            new Object[] { List.of("-cp", concat("b", "a")), "b"},
        };
    }

    @Test(dataProvider = "options")
    public void test(List<String> options, String expected) throws Throwable {
        runTest(CWD, options, expected);
    }

    @DataProvider
    public Object[][] dirA() {
        String dirB = ".." + File.separator + "b";
        return new Object[][] {
            new Object[] { List.of("-Xbootclasspath/a:."), "a"},

            new Object[] { List.of("-Xbootclasspath/a:" + dirB), "b"},
            // empty path in first element
            new Object[] { List.of("-Xbootclasspath/a:" + File.pathSeparator + dirB), "b"},

            new Object[] { List.of("-cp", File.pathSeparator), "a"},
            new Object[] { List.of("-cp", dirB), "b"},
            new Object[] { List.of("-cp", File.pathSeparator + dirB), "a"},
        };
    }

    @Test(dataProvider = "dirA")
    public void testCurrentDirA(List<String> options, String expected) throws Throwable {
        // current working directory is "a"
        runTest(CWD.resolve(DIR_A), options, expected);
    }

    private void runTest(Path dir, List<String> options, String expected)
        throws Throwable
    {
        String javapath = JDKToolFinder.getJDKTool("java");

        List<String> cmdLine = new ArrayList<>();
        cmdLine.add(javapath);
        options.forEach(cmdLine::add);

        cmdLine.add("GetResource");
        cmdLine.add(expected);

        System.out.println("Command line: " + cmdLine);
        ProcessBuilder pb =
            new ProcessBuilder(cmdLine.stream().toArray(String[]::new));

        // change working directory
        pb.directory(dir.toFile());

        // remove CLASSPATH environment variable
        Map<String,String> env = pb.environment();
        String value = env.remove("CLASSPATH");

        executeCommand(pb).shouldHaveExitValue(0);
    }

}
