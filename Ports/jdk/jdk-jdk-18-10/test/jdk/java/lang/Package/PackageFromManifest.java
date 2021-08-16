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
 * @bug 8201528
 * @summary The test will create JAR file(s) with the manifest file
 *          that customized package versioning information (different info for
 *          same package if multiple jars). Then verify package versioning info
 * @library /test/lib
 * @modules jdk.compiler
 * @run main PackageFromManifest setup test
 * @run main PackageFromManifest runJar test1.jar
 * @run main PackageFromManifest runJar test1.jar test2.jar foo.Foo1
 * @run main PackageFromManifest runJar test1.jar test2.jar foo.Foo2
 * @run main/othervm PackageFromManifest runUrlLoader test1.jar
 * @run main/othervm PackageFromManifest runUrlLoader test1.jar test2.jar foo.Foo1
 * @run main/othervm PackageFromManifest runUrlLoader test1.jar test2.jar foo.Foo2
 */

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.FileUtils;
import jdk.test.lib.util.JarUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This test accept at least two input parameters, first one is run type like
 * 'setup', 'runJar', 'runTest', 'runUrlLoader', the rest parameters are options
 * to each run type. 'setup' run type should be placed at first since it will
 * create necessary jars for the rest tests. 'runTest' will be called in test
 * logic only, it should not be used in @run
 *
 * #1 test will do setup only to generate required jars before other tests run
 * PackageFromManifest setup test
 *
 * #2 test will run against single jar file to verify package versioning
 * PackageFromManifest runJar test1.jar
 *
 * #4 test will run against two jar files, load class foo.Foo1 first, then
 * verify package versioning
 * PackageFromManifest runJar test1.jar test2.jar foo.Foo1
 *
 * #5 test will run against two jar files, load class foo.Foo2 first, then
 * verify package versioning
 * PackageFromManifest runJar test1.jar test2.jar foo.Foo2
 *
 * #3 test will use URLCLassLoader to load single jar file, then verify
 * package versioning
 * PackageFromManifest runUrlLoader test1.jar
 *
 * #6 test will use URLCLassLoader to load two jars, load class foo.Foo1 first,
 * then verify package versioning
 * PackageFromManifest runUrlLoader test1.jar test2.jar foo.Foo1
 *
 * #7 test will use URLCLassLoader to load two jars, load class foo.Foo2 first,
 * then verify package versioning
 * PackageFromManifest runUrlLoader test1.jar test2.jar foo.Foo2
 */
public class PackageFromManifest {

    private static final String PACKAGE_NAME = "foo";
    private static final String TEST_JAR_FILE1 = "test1.jar";
    private static final String TEST_JAR_FILE2 = "test2.jar";
    private static final String TEST_SUFFIX1 = "1";
    private static final String TEST_SUFFIX2 = "2";
    private static final String TEST_CLASS_PREFIX = "Foo";
    private static final String TEST_CLASS_NAME1 =
            TEST_CLASS_PREFIX + TEST_SUFFIX1;
    private static final String TEST_CLASS_NAME2 =
            TEST_CLASS_PREFIX + TEST_SUFFIX2;
    private static final String MANIFEST_FILE = "test.mf";
    private static final String SPEC_TITLE = "testSpecTitle";
    private static final String SPEC_VENDOR = "testSpecVendor";
    private static final String IMPL_TITLE = "testImplTitle";
    private static final String IMPL_VENDOR = "testImplVendor";
    private static final Path WORKING_PATH = Paths.get(".");

    public static void main(String[] args) throws Exception {
        if (args != null && args.length > 1) {
            String runType = args[0];
            String[] options = Arrays.copyOfRange(args, 1, args.length);
            switch (runType) {
                case "setup":
                    setup();
                    break;
                case "runTest":
                    runTest(options);
                    break;
                case "runJar":
                    runJar(options);
                    break;
                case "runUrlLoader":
                    testUrlLoader(options);
                    break;
                default:
                    throw new RuntimeException("Invalid run type : " + runType);
            }
        } else {
            throw new RuntimeException("Invalid input arguments");
        }
    }

    private static void createTestClass(String name) throws IOException {
        List<String> content = new ArrayList<>();
        content.add("package " + PACKAGE_NAME + ";");
        content.add("public class " + name + " {");
        content.add("}");

        Path javaFile = WORKING_PATH.resolve(name + ".java");

        Files.write(javaFile, content);

        CompilerUtils.compile(WORKING_PATH, WORKING_PATH);

        // clean up created java file
        Files.delete(javaFile);
    }

    private static void createManifest(String suffix) throws IOException {
        List<String> content = new ArrayList<>();
        content.add("Manifest-version: 1.1");
        content.add("Name: " + PACKAGE_NAME + "/");
        content.add("Specification-Title: " + SPEC_TITLE + suffix);
        content.add("Specification-Version: " + suffix);
        content.add("Specification-Vendor: " + SPEC_VENDOR + suffix);
        content.add("Implementation-Title: " + IMPL_TITLE + suffix);
        content.add("Implementation-Version: " + suffix);
        content.add("Implementation-Vendor: " + IMPL_VENDOR + suffix);

        Files.write(WORKING_PATH.resolve(MANIFEST_FILE), content);
    }

    private static void buildJar(String jarFileName, boolean isIncludeSelf)
            throws IOException {
        try (InputStream is = Files.newInputStream(Paths.get(MANIFEST_FILE))) {
            if (isIncludeSelf) {
                Path selfPath = WORKING_PATH
                        .resolve("PackageFromManifest.class");
                if (!Files.exists(selfPath)) {
                    Files.copy(Paths.get(System.getProperty("test.classes"))
                            .resolve("PackageFromManifest.class"), selfPath);
                }
                JarUtils.createJarFile(Paths.get(jarFileName), new Manifest(is),
                        WORKING_PATH, selfPath,
                        WORKING_PATH.resolve(PACKAGE_NAME));
            } else {
                JarUtils.createJarFile(Paths.get(jarFileName), new Manifest(is),
                        WORKING_PATH, WORKING_PATH.resolve(PACKAGE_NAME));
            }
        }

        // clean up build files
        FileUtils.deleteFileTreeWithRetry(WORKING_PATH.resolve(PACKAGE_NAME));
        Files.delete(WORKING_PATH.resolve(MANIFEST_FILE));
    }

    private static void runJar(String[] options) throws Exception {
        String[] cmds;
        String classPath = Stream.of(options).takeWhile(s -> s.endsWith(".jar"))
                .collect(Collectors.joining(File.pathSeparator));
        if (options.length == 1) {
            cmds = new String[] { "-cp", classPath, "PackageFromManifest",
                    "runTest", "single" };
        } else {
            cmds = new String[] { "-cp", classPath, "PackageFromManifest",
                    "runTest", options[options.length - 1] };
        }

        ProcessTools.executeTestJava(cmds).outputTo(System.out)
                .errorTo(System.err).shouldHaveExitValue(0);
    }

    private static void runTest(String[] options)
            throws ClassNotFoundException {
        String option = options[0];
        if (option.equalsIgnoreCase("single")) {
            runTest(Class.forName(PACKAGE_NAME + "." + TEST_CLASS_NAME1)
                    .getPackage(), TEST_SUFFIX1);
        } else {
            // Load one specified class first
            System.out.println("Load " + Class.forName(option) + " first");

            String suffix = option.endsWith(TEST_SUFFIX1) ?
                    TEST_SUFFIX1 :
                    TEST_SUFFIX2;

            runTest(Class.forName(PACKAGE_NAME + "." + TEST_CLASS_NAME1)
                    .getPackage(), suffix);
            runTest(Class.forName(PACKAGE_NAME + "." + TEST_CLASS_NAME2)
                    .getPackage(), suffix);
        }
    }

    private static void runTest(Package testPackage, String suffix) {
        checkValue("Package Name", PACKAGE_NAME, testPackage.getName());
        checkValue("Spec Title", SPEC_TITLE + suffix,
                testPackage.getSpecificationTitle());
        checkValue("Spec Vendor", SPEC_VENDOR + suffix,
                testPackage.getSpecificationVendor());
        checkValue("Spec Version", suffix,
                testPackage.getSpecificationVersion());
        checkValue("Impl Title", IMPL_TITLE + suffix,
                testPackage.getImplementationTitle());
        checkValue("Impl Vendor", IMPL_VENDOR + suffix,
                testPackage.getImplementationVendor());
        checkValue("Impl Version", suffix,
                testPackage.getImplementationVersion());
    }

    private static void checkValue(String name, String expect, String actual) {
        if (!expect.equals(actual)) {
            throw new RuntimeException(
                    "Failed, unexpected value for " + name + ", expect: "
                            + expect + ", actual: " + actual);
        } else {
            System.out.println(name + " : " + actual);
        }
    }

    private static void setup() throws IOException {
        if (!Files.exists(WORKING_PATH.resolve(TEST_JAR_FILE1))) {
            createTestClass(TEST_CLASS_NAME1);
            createManifest(TEST_SUFFIX1);
            buildJar(TEST_JAR_FILE1, true);
        }

        if (!Files.exists(WORKING_PATH.resolve(TEST_JAR_FILE2))) {
            createTestClass(TEST_CLASS_NAME2);
            createManifest(TEST_SUFFIX2);
            buildJar(TEST_JAR_FILE2, false);
        }
    }

    private static void testUrlLoader(String[] options)
            throws ClassNotFoundException {
        URLClassLoader cl = new URLClassLoader(
                Stream.of(options).takeWhile(s -> s.endsWith(".jar")).map(s -> {
                    try {
                        return WORKING_PATH.resolve(s).toUri().toURL();
                    } catch (MalformedURLException e) {
                        return null;
                    }
                }).toArray(URL[]::new));
        if (options.length == 1) {
            runTest(Class
                    .forName(PACKAGE_NAME + "." + TEST_CLASS_NAME1, true, cl)
                    .getPackage(), TEST_SUFFIX1);
        } else {
            // Load one specified class first
            System.out.println("Load " + Class
                    .forName(options[options.length - 1], true, cl) + " first");

            String suffix = options[options.length - 1].endsWith(TEST_SUFFIX1) ?
                    TEST_SUFFIX1 :
                    TEST_SUFFIX2;

            runTest(Class
                    .forName(PACKAGE_NAME + "." + TEST_CLASS_NAME1, true, cl)
                    .getPackage(), suffix);
            runTest(Class
                    .forName(PACKAGE_NAME + "." + TEST_CLASS_NAME2, true, cl)
                    .getPackage(), suffix);
        }
    }
}
