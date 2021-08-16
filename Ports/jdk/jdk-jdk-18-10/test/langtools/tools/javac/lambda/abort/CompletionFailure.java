/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8009227
 * @summary Certain diagnostics should not be deferred
 * @modules jdk.compiler
 */

import com.sun.source.util.JavacTask;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class CompletionFailure {

    public static void main(String... args) throws Exception {

        String SCRATCH_DIR = System.getProperty("user.dir");
        JavaCompiler javacTool = ToolProvider.getSystemJavaCompiler();
        File scratchDir = new File(SCRATCH_DIR);

        sourceA.dumpTo(scratchDir);
        sourceB.dumpTo(scratchDir);

        JavacTask ct = (JavacTask)javacTool.getTask(null, null, null,
                null, null, Arrays.asList(sourceA.asJFO(scratchDir), sourceB.asJFO(scratchDir)));

        ct.generate();

        remove(scratchDir, "A.java");
        remove(scratchDir, "B.java");
        remove(scratchDir, "A.class");

        sourceC.dumpTo(scratchDir);
        sourceD.dumpTo(scratchDir);

        DiagnosticChecker diagChecker = new DiagnosticChecker();
        ct = (JavacTask)javacTool.getTask(null, null, diagChecker,
                Arrays.asList("-XDrawDiagnostics", "-cp", scratchDir.getAbsolutePath()),
                null, Arrays.asList(sourceC.asJFO(scratchDir), sourceD.asJFO(scratchDir)));
        try {
            ct.analyze();
        } catch (Throwable ex) {
            //ignore abort exception thrown by javac
        }

        if (!diagChecker.errorFound) {
            throw new AssertionError("Missing diagnostic");
        }
    }

    static void remove(File dir, String fileName) {
        File fileToRemove = new File(dir, fileName);
        fileToRemove.delete();
    }

    static class JavaSource {
        String contents;
        String filename;

        public JavaSource(String filename, String contents) {
            this.filename =  filename;
            this.contents = contents;
        }

        void dumpTo(java.io.File loc) throws Exception {
            File file = new File(loc, filename);
            BufferedWriter bw = new java.io.BufferedWriter(new FileWriter(file));
            bw.append(contents);
            bw.close();
        }

        SimpleJavaFileObject asJFO(java.io.File dir) {
            return new SimpleJavaFileObject(new File(dir, filename).toURI(), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                    return contents;
                }
            };
        }
    }

    static JavaSource sourceA = new JavaSource("A.java", "public interface A { }\n");

    static JavaSource sourceB = new JavaSource("B.java", "public class B implements A {\n" +
                                               "   public static Object n() { return null; }\n" +
                                               "}\n");

    static JavaSource sourceC = new JavaSource("C.java", "public class C {\n" +
                                               "   void test(B b) {\n" +
                                               "      D.m(B.n());\n" +
                                               "}  }\n");

    static JavaSource sourceD = new JavaSource("D.java", "public class D {\n" +
                                               "   static void m(Object o) { }\n" +
                                               "}\n");

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
