/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;

/**
 * @test
 * @bug 8187442
 * @summary Launching app shouldn't produce any jni warnings.
 * @modules jdk.compiler
 *          jdk.zipfs
 * @compile TestXcheckJNIWarnings.java
 * @run main TestXcheckJNIWarnings
 */
public final class TestXcheckJNIWarnings extends TestHelper {

    static void createJarFile(File testJar) throws IOException {
        StringBuilder tsrc = new StringBuilder();
        tsrc.append("public static void main(String... args) {\n");
        tsrc.append("    System.out.println(\"Hello World\");\n");
        tsrc.append("}\n");
        createJar(testJar, new File("Foo"), tsrc.toString());
    }

    public static void main(String... args) throws IOException {
        File testJarFile = new File("test.jar");
        createJarFile(testJarFile);
        TestResult tr = doExec(javaCmd, "-jar", "-Xcheck:jni",
                               testJarFile.getName());
        if (!tr.isOK()) {
            System.out.println(tr);
            throw new RuntimeException("test returned non-positive value");
        }
        if (!tr.contains("Hello World")) {
            System.out.println(tr);
            throw new RuntimeException("expected output not found");
        }
        if (tr.contains("WARNING")) {
            System.out.println(tr);
            throw new RuntimeException("WARNING was found in the output");
        }
    }
}
