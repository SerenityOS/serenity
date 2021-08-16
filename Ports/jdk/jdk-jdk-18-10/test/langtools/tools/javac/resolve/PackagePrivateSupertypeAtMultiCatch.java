/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8264696
 * @summary Multi-catch clause causes compiler exception because it uses the package-private supertype
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.api
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main PackagePrivateSupertypeAtMultiCatch
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;

import toolbox.ToolBox;
import toolbox.TestRunner;
import toolbox.JavacTask;

public class PackagePrivateSupertypeAtMultiCatch extends TestRunner {
    ToolBox tb;

    private static String testCode = """
            package pkg1;
            import pkg2.Child1Exception;
            import pkg2.Child2Exception;
            class Test {
                void success() {
                    try {
                        foo();
                    } catch (Child1Exception e) {
                        e.getMessage();
                    } catch (Child2Exception e) {
                        e.getMessage();
                    }
                }
                void fail() {
                    try {
                        foo();
                    } catch (Child1Exception | Child2Exception e) {
                        e.getMessage();
                    }
                }
                void foo() throws Child1Exception, Child2Exception {
                }
            }""";

    private static String parentExceptionCode = """
            package pkg2;
            class ParentException extends Exception {
            }""";

    private static String child1ExceptionCode = """
            package pkg2;
            public class Child1Exception extends ParentException {
            }""";

    private static String child2ExceptionCode = """
            package pkg2;
            public class Child2Exception extends ParentException {
            }""";

    public PackagePrivateSupertypeAtMultiCatch() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String[] args) throws Exception {
        new PackagePrivateSupertypeAtMultiCatch().runTests();
    }

    @Test
    public void testPackagePrivateSupertypeAtMultiCatch() throws Exception {
        Path src = Paths.get("src");
        tb.writeJavaFiles(src, testCode, parentExceptionCode, child1ExceptionCode, child2ExceptionCode);
        Path out = Paths.get("out");
        Files.createDirectories(out);
        new JavacTask(tb)
                .files(tb.findJavaFiles(src))
                .outdir(out)
                .run();
    }
}
