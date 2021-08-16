/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.List;

/**
 * @test
 * @bug 8076264
 * @summary Launching app shouldn't require enclosing class for the main class.
 * @modules jdk.compiler
 *          jdk.zipfs
 * @compile TestMainWithoutEnclosing.java
 * @run main TestMainWithoutEnclosing
 */
public final class TestMainWithoutEnclosing extends TestHelper {

    static final String EnclosingName = "Enclosing";

    static void createJarFile(File testJar) throws IOException {
        List<String> scratch = new ArrayList<>();
        scratch.add("public class Enclosing {");
        scratch.add("    public static final class Main {");
        scratch.add("        public static void main(String... args) {");
        scratch.add("            System.out.println(\"Hello World\");");
        scratch.add("        }");
        scratch.add("    }");
        scratch.add("}");
        File enclosingFile = new File(EnclosingName + ".java");
        createFile(enclosingFile, scratch);
        compile(enclosingFile.getName());
        // avoid side effects remove the Enclosing class
        getClassFile(enclosingFile).delete();
        createJar("cvfe", testJar.getName(), EnclosingName + "$Main",
                EnclosingName + "$Main" + ".class");
        // remove extraneous files in the current directory
        new File(EnclosingName + "$Main" + ".class").delete();
    }

    public static void main(String... args) throws IOException {
        File testJarFile = new File("test.jar");
        createJarFile(testJarFile);
        TestResult tr = doExec(javaCmd, "-jar", testJarFile.getName());
        if (!tr.isOK()) {
            System.out.println(tr);
            throw new RuntimeException("test returned non-positive value");
        }
        if (!tr.contains("Hello World")) {
            System.out.println(tr);
            throw new RuntimeException("expected output not found");
        }
    }
}
