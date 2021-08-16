/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4640520 6354623 7198496
 * @summary Unit test for java.util.ServiceLoader
 * @library /test/lib
 * @build jdk.test.lib.process.*
 *        jdk.test.lib.util.JarUtils
 *        Basic Load FooService FooProvider1 FooProvider2 FooProvider3 BarProvider
 * @run testng ServiceLoaderBasicTest
 */


import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Arrays.asList;

public class ServiceLoaderBasicTest {

    private static final String METAINFO = "META-INF/services/FooService";
    private static final Path XTEST_CONFIG = Path.of("x.test").resolve(METAINFO);
    private static final Path XMETA_CONFIG = Path.of("x.meta").resolve(METAINFO);
    private static final Path P2JAR = Path.of("p2.jar");
    private static final Path P2DUPJAR = Path.of("p2dup.jar");
    private static final Path P3JAR = Path.of("x.ext", "p3.jar");

    private static final String XTEST = File.pathSeparator + "x.test";
    private static final String XMETA = File.pathSeparator + "x.meta";
    private static final String P2 = File.pathSeparator + P2JAR.toString();
    private static final String P2DUP = File.pathSeparator + P2DUPJAR.toString();
    private static final String P3 = File.pathSeparator + P3JAR.toString();

    private static final String XTEST_CP = Utils.TEST_CLASS_PATH + XTEST;
    private static final String P2_CP = Utils.TEST_CLASS_PATH + P2;
    private static final String P2DUP_CP = P2_CP + P2DUP;
    private static final String P3P2_CP = Utils.TEST_CLASS_PATH + P3 + P2;
    private static final String XTESTP2_CP = XTEST_CP + P2;
    private static final String P3XTEST_CP = Utils.TEST_CLASS_PATH + P3 + XTEST;
    private static final String P3XTESTP2_CP = P3XTEST_CP + P2;
    private static final String XMETA_CP = Utils.TEST_CLASS_PATH + XMETA;
    private static final String XMETAXTEST_CP = XMETA_CP + XTEST;
    private static final String XTESTXMETA_CP = XTEST_CP + XMETA;
    private static final String XTESTXMETAP2_CP = XTESTXMETA_CP + P2;

    @BeforeClass
    public void initialize() throws Exception {
        createProviderConfig(XTEST_CONFIG, "FooProvider1");
        createProviderConfig(XMETA_CONFIG, "FooProvider42");
        createJar(P2JAR, "FooProvider2", List.of("FooProvider2"));
        createJar(P3JAR, "FooProvider3", List.of("FooProvider3", "FooService"));
        Files.copy(P2JAR, P2DUPJAR, REPLACE_EXISTING);
    }

    @DataProvider
    public Object[][] testCases() {
        return new Object[][]{
            //       CLI options,            Test,       Runtime arguments
            // Success cases
            {List.of("-cp", XTESTP2_CP,      "Basic")},
            {List.of("-cp", XTEST_CP,        "Load",     "FooProvider1")},
            {List.of("-cp", P2_CP,           "Load",     "FooProvider2")},
            {List.of("-cp", P2DUP_CP,        "Load",     "FooProvider2")},
            {List.of("-cp", P3P2_CP,         "Load",     "FooProvider3", "FooProvider2")},
            {List.of("-cp", XTESTP2_CP,      "Load",     "FooProvider1", "FooProvider2")},
            {List.of("-cp", P3XTEST_CP,      "Load",     "FooProvider3", "FooProvider1")},
            {List.of("-cp", P3XTESTP2_CP,    "Load",     "FooProvider3",
                                                         "FooProvider1",
                                                         "FooProvider2")},
            // Failures followed by successes
            {List.of("-cp", XTESTXMETA_CP,   "Load",     "FooProvider1", "fail")},
            {List.of("-cp", XMETAXTEST_CP,   "Load",     "fail", "FooProvider1")},
            {List.of("-cp", XTESTXMETAP2_CP, "Load",     "FooProvider1", "fail", "FooProvider2")}
        };
    }

    @DataProvider
    public Object[][] negativeTestCases() {
        return new Object[][]{
            {"blah blah"},
            {"9234"},
            {"X!"},
            {"BarProvider"},
            {"FooProvider42"}
        };
    }

    @Test(dataProvider = "testCases")
    public void testProvider(List<String> args) throws Throwable {
        runJava(args);
    }

    @Test(dataProvider = "negativeTestCases")
    public void testBadProvider(String providerName) throws Throwable {
        Files.write(XMETA_CONFIG, providerName.getBytes());
        runJava(List.of("-cp", XMETA_CP, "Load", "fail"));
    }

    private void runJava(List<String> opts) throws Throwable {
        List<String> cmds = new ArrayList<>();
        cmds.add(JDKToolFinder.getJDKTool("java"));
        cmds.addAll(asList(Utils.getTestJavaOpts()));
        cmds.addAll(opts);

        ProcessTools.executeCommand(cmds.stream()
                    .filter(t -> !t.isEmpty())
                    .toArray(String[]::new))
                    .shouldHaveExitValue(0);
    }

    private void createProviderConfig(Path config, String providerName) throws Exception {
        Files.createDirectories(config.getParent());
        Files.write(config, providerName.getBytes(), CREATE);
    }

    private void createJar(Path jar, String provider, List<String> files) throws Exception {
        Path xdir = Path.of(provider);
        createProviderConfig(xdir.resolve(METAINFO), provider);

        for (String f : files) {
            Path source = Path.of(Utils.TEST_CLASSES, f + ".class");
            Path target = xdir.resolve(source.getFileName());
            Files.copy(source, target, REPLACE_EXISTING);
        }
        JarUtils.createJarFile(jar, xdir);
    }

}
