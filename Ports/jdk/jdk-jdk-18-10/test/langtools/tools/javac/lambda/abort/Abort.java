/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003280
 * @summary Add lambda tests
 *  check that all diagnostics are dumped to output when compiler exits abruptly
 * @modules jdk.compiler
 */

import com.sun.source.util.JavacTask;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class Abort {

    public static void main(String... args) throws Exception {

        String SCRATCH_DIR = System.getProperty("user.dir");
        JavaCompiler javacTool = ToolProvider.getSystemJavaCompiler();
        java.io.File testDir = new java.io.File(SCRATCH_DIR);

        sourceA.dumpTo(testDir);
        sourceB.dumpTo(testDir);

        DiagnosticChecker diagChecker = new DiagnosticChecker();
        JavacTask ct = (JavacTask)javacTool.getTask(null, null, diagChecker,
                Arrays.asList("-XDrawDiagnostics", "-cp", testDir.getAbsolutePath()),
                null, Arrays.asList(sourceA.asJFO(testDir)));
        try {
            ct.analyze();
        } catch (Throwable ex) {
            //ignore abort exception thrown by javac
        }

        if (!diagChecker.errorFound) {
            throw new AssertionError("Missing diagnostic");
        }
    }

    static class JavaSource {
        String contents;
        String filename;

        public JavaSource(String filename, String contents) {
            this.filename =  filename;
            this.contents = contents;
        }

        void dumpTo(java.io.File loc) throws Exception {
            java.io.File file = new java.io.File(loc, filename);
            java.io.BufferedWriter bw = new java.io.BufferedWriter(new java.io.FileWriter(file));
            bw.append(contents);
            bw.close();
        }

        SimpleJavaFileObject asJFO(java.io.File dir) {
            return new SimpleJavaFileObject(new java.io.File(dir, filename).toURI(), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                    return contents;
                }
            };
        }
    }

    static JavaSource sourceA = new JavaSource("Abort.java", "public class Abort {\n" +
                                "    public static void main(String[] args) {\n" +
                                "        System.out.println(C.m());\n" +
                                "    }\n" +
                                "}");

    static JavaSource sourceB = new JavaSource("C.java", "package com.example;\n" +
                                "public class C {\n" +
                                "    public static String m() { return null; }\n" +
                                "}");

    static class DiagnosticChecker implements javax.tools.DiagnosticListener<JavaFileObject> {

        boolean errorFound;

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR &&
                    diagnostic.getCode().contains("compiler.err.cant.access")) {
                errorFound = true;
            }
        }
    }
}
