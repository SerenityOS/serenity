/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8159393
 * @summary Test signed jars involved in image creation
 * @modules java.base/jdk.internal.jimage
 *          java.base/sun.security.tools.keytool
 *          jdk.compiler/com.sun.tools.javac
 *          jdk.jartool/sun.security.tools.jarsigner
 *          jdk.jartool/sun.tools.jar
 *          jdk.jlink/jdk.tools.jlink.internal
 * @run main/othervm JLinkSigningTest
 */


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.spi.ToolProvider;

public class JLinkSigningTest {
    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
            .orElseThrow(() -> new RuntimeException("jar tool not found"));
    private static final ToolProvider JAVAC_TOOL = ToolProvider.findFirst("javac")
            .orElseThrow(() -> new RuntimeException("javac tool not found"));
    private static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
            .orElseThrow(() -> new RuntimeException("jlink tool not found"));

    static final String[] MODULE_INFO = {
        "module test {",
        "}",
    };

    static final String[] TEST_CLASS = {
        "package test;",
        "public class test {",
        "    public static void main(String[] args) {",
        "    }",
        "}",
    };

    static void report(String command, String[] args) {
        System.out.println(command + " " + String.join(" ", Arrays.asList(args)));
    }

    static void jar(String[] args) {
        report("jar", args);
        JAR_TOOL.run(System.out, System.err, args);
    }

    static void jarsigner(String[] args) {
        report("jarsigner", args);

        try {
            sun.security.tools.jarsigner.Main.main(args);
        } catch (Exception ex) {
            throw new RuntimeException("jarsigner not found");
        }
    }

    static void javac(String[] args) {
        report("javac", args);
        JAVAC_TOOL.run(System.out, System.err, args);
    }

    static void jlink(String[] args) {
        report("jlink", args);
        JLINK_TOOL.run(System.out, System.err, args);
    }

    static void keytool(String[] args) {
        report("keytool", args);

        try {
            sun.security.tools.keytool.Main.main(args);
        } catch (Exception ex) {
            throw new RuntimeException("keytool failed");
        }
    }

    public static void main(String[] args) {
        final String JAVA_HOME = System.getProperty("java.home");
        Path moduleInfoJavaPath = Paths.get("module-info.java");
        Path moduleInfoClassPath = Paths.get("module-info.class");
        Path testDirectoryPath = Paths.get("test");
        Path testJavaPath = testDirectoryPath.resolve("test.java");
        Path testClassPath = testDirectoryPath.resolve("test.class");
        Path testModsDirectoryPath = Paths.get("testmods");
        Path jmodsPath = Paths.get(JAVA_HOME, "jmods");
        Path testjarPath = testModsDirectoryPath.resolve("test.jar");
        String modulesPath = testjarPath.toString() +
                             File.pathSeparator +
                             jmodsPath.toString();

        try {
            Files.write(moduleInfoJavaPath, Arrays.asList(MODULE_INFO));
            Files.createDirectories(testDirectoryPath);
            Files.write(testJavaPath, Arrays.asList(TEST_CLASS));
            Files.createDirectories(testModsDirectoryPath);
        } catch (IOException ex) {
            throw new RuntimeException("file construction failed");
        }

        javac(new String[] {
            testJavaPath.toString(),
            moduleInfoJavaPath.toString(),
        });

        jar(new String[] {
            "-c",
            "-f", testjarPath.toString(),
            "--module-path", jmodsPath.toString(),
            testClassPath.toString(),
            moduleInfoClassPath.toString(),
        });

        keytool(new String[] {
            "-genkey",
            "-keyalg", "RSA",
            "-dname", "CN=John Doe, OU=JPG, O=Oracle, L=Santa Clara, ST=California, C=US",
            "-alias", "examplekey",
            "-storepass", "password",
            "-keypass", "password",
            "-keystore", "examplekeystore",
            "-validity", "365",
        });

        jarsigner(new String[] {
            "-keystore", "examplekeystore",
            "-verbose", testjarPath.toString(),
            "-storepass", "password",
            "-keypass", "password",
            "examplekey",
        });

        try {
            jlink(new String[] {
                "--module-path", modulesPath,
                "--add-modules", "test",
                "--output", "foo",
            });
        } catch (Throwable ex) {
            System.out.println("Failed as should");
        }

        try {
            jlink(new String[] {
                "--module-path", modulesPath,
                "--add-modules", "test",
                "--ignore-signing-information",
                "--output", "foo",
            });
            System.out.println("Suceeded as should");
        } catch (Throwable ex) {
            System.err.println("Should not have failed");
            throw new RuntimeException(ex);
        }

        System.out.println("Done");
    }
}

