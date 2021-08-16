/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4204897 4256097 4785453 4863609
 * @summary Test that '.jar' files in -extdirs are found.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask
 * @run main ExtDirTest
 */

import java.io.File;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.ToolBox;

// Original test: test/tools/javac/ExtDirs/ExtDirs.sh
public class ExtDirTest {

    private static final String ExtDirTestClass1Src =
        "package pkg1;\n" +
        "\n" +
        "public class ExtDirTestClass1 {}";

    private static final String ExtDirTestClass2Src =
        "package pkg2;\n" +
        "\n" +
        "public class ExtDirTestClass2 {}";

    private static final String ExtDirTest_1Src =
        "import pkg1.*;\n" +
        "\n" +
        "public class ExtDirTest_1 {\n" +
        "  ExtDirTestClass1 x;\n" +
        "}";

    private static final String ExtDirTest_2Src =
        "import pkg1.*;\n" +
        "import pkg2.*;\n" +
        "\n" +
        "public class ExtDirTest_2 {\n" +
        "  ExtDirTestClass1 x;\n" +
        "  ExtDirTestClass2 y;\n" +
        "}";

    private static final String ExtDirTest_3Src =
        "import pkg1.*;\n" +
        "import pkg2.*;\n" +
        "\n" +
        "public class ExtDirTest_3 {\n" +
        "  ExtDirTestClass1 x;\n" +
        "  ExtDirTestClass2 y;\n" +
        "}";

    private static final String jar1Manifest =
        "Manifest-Version: 1.0\n" +
        "\n" +
        "Name: pkg1/ExtDirTestClass1.class\n" +
        "Digest-Algorithms: SHA MD5 \n" +
        "SHA-Digest: 9HEcO9LJmND3cvOlq/AbUsbD9S0=\n" +
        "MD5-Digest: hffPBwfqcUcnEdNv4PXu1Q==\n" +
        "\n" +
        "Name: pkg1/ExtDirTestClass1.java\n" +
        "Digest-Algorithms: SHA MD5 \n" +
        "SHA-Digest: 2FQVe6w3n2Ma1ACYpe8a988EBU8=\n" +
        "MD5-Digest: /Ivr4zVI9MSM26NmqWtZpQ==\n";

    private static final String jar2Manifest =
        "Manifest-Version: 1.0\n" +
        "\n" +
        "Name: pkg2/ExtDirTestClass2.class\n" +
        "Digest-Algorithms: SHA MD5 \n" +
        "SHA-Digest: elbPaqWf8hjj1+ZkkdW3PGTsilo=\n" +
        "MD5-Digest: 57Nn0e2t1yEQfu/4kSw8yg==\n" +
        "\n" +
        "Name: pkg2/ExtDirTestClass2.java\n" +
        "Digest-Algorithms: SHA MD5 \n" +
        "SHA-Digest: ILJOhwHg5US+yuw1Sc1d+Avu628=\n" +
        "MD5-Digest: j8wnz8wneEcuJ/gjXBBQNA==\n";

    public static void main(String args[]) throws Exception {
        new ExtDirTest().run();
    }

    private final ToolBox tb = new ToolBox();

    void run() throws Exception {
        createJars();
        compileWithExtDirs();
    }

    void createJars() throws Exception {
        new JavacTask(tb)
                .outdir(".")
                .sources(ExtDirTestClass1Src)
                .run();

        new JarTask(tb, "pkg1.jar")
                .manifest(jar1Manifest)
                .files("pkg1/ExtDirTestClass1.class")
                .run();

        new JavacTask(tb)
                .outdir(".")
                .sources(ExtDirTestClass2Src)
                .run();

        new JarTask(tb, "pkg2.jar")
                .manifest(jar2Manifest)
                .files("pkg2/ExtDirTestClass2.class")
                .run();

        tb.createDirectories("ext1", "ext2", "ext3");
        tb.copyFile("pkg1.jar", "ext1");
        tb.copyFile("pkg2.jar", "ext2");
        tb.copyFile("pkg1.jar", "ext3");
        tb.copyFile("pkg2.jar", "ext3");

        tb.deleteFiles(
                "pkg1.jar",
                "pkg2.jar",
                "pkg1/ExtDirTestClass1.class",
                "pkg1",
                "pkg2/ExtDirTestClass2.class",
                "pkg2"
        );
    }

    void compileWithExtDirs() throws Exception {
        new JavacTask(tb)
                .outdir(".")
                .options("-source", "8",
                        "-extdirs", "ext1")
                .sources(ExtDirTest_1Src)
                .run()
                .writeAll();

        new JavacTask(tb)
                .outdir(".")
                .options("-source", "8",
                        "-extdirs", "ext1" + File.pathSeparator + "ext2")
                .sources(ExtDirTest_2Src)
                .run()
                .writeAll();

        new JavacTask(tb)
                .outdir(".")
                .options("-source", "8",
                        "-extdirs", "ext3")
                .sources(ExtDirTest_3Src)
                .run()
                .writeAll();
    }

}
