/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8216170
 * @summary java.lang.IllegalArgumentException: directories not supported
 * @library /tools/lib ../../lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.javadoc/jdk.javadoc.internal.api
 *          jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ToolBox javadoc.tester.*
 * @run main TestJavaPackage
 */

import java.io.IOException;
import java.nio.file.Path;

import toolbox.ModuleBuilder;
import toolbox.ToolBox;

import javadoc.tester.JavadocTester;

public class TestJavaPackage extends JavadocTester {
    public static void main(String... args) throws Exception {
        TestJavaPackage tester = new TestJavaPackage();
        tester.runTests();
    }

    private final ToolBox tb = new ToolBox();

    @Test
    public void testPackage() throws IOException {
        Path src = Path.of("src");
        tb.writeJavaFiles(src,
            "module com.example.java { exports com.example.java; }",
            "package com.example.java; public class C { }");

        // Disable the standard check that the output directory is empty.
        // It is a significant part of the test case that the output directory
        // is set to the current directory, which (in this case) also contains
        // the src/ directory.
        setOutputDirectoryCheck(DirectoryCheck.NONE);

        javadoc("-d", ".",  // using "." is important for the test case
                "-sourcepath", src.toString(),
                "com.example.java");
        checkExit(Exit.OK);
        checkFiles(true,
            "com.example.java/com/example/java/C.html");

        // repeat the call, to verify the package name "com.example.java" is not
        // confused with the output directory for the "com.example.java" module,
        // created by the first call

        javadoc("-d", ".",  // using "." is important for the test case
                "-sourcepath", src.toString(),
                "com.example.java");
        checkExit(Exit.OK);
        checkFiles(true,
            "com.example.java/com/example/java/C.html");
    }
}


