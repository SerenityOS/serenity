/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8017216 8019422 8019421 8054956 8205418
 * @summary verify start and end positions
 * @modules java.compiler
 *          jdk.compiler
 * @run main TreeEndPosTest
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

public class TreeEndPosTest {
    private static JavaFileManager getJavaFileManager(JavaCompiler compiler,
            DiagnosticCollector dc) {
        return compiler.getStandardFileManager(dc, null, null);
    }

    static class JavaSource extends SimpleJavaFileObject {

        final String source;
        int startPos;
        int endPos;

        private JavaSource(String filename, String source) {
            super(URI.create("myfo:/" + filename), JavaFileObject.Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }

        static JavaSource createJavaSource(String preamble, String body,
                String postamble, String expected) {
            JavaSource js = createJavaSource(preamble, body, postamble, -1, -1);
            js.startPos = js.source.indexOf(expected);
            js.endPos   = js.startPos + expected.length();
            return js;
        }

        static JavaSource createJavaSource(String body, String expected) {
            return createJavaSource(null, body, null, expected);
        }

        private static JavaSource createJavaSource(String preamble, String body,
                String postamble, int start, int end) {
            final String name = "Bug";
            StringBuilder code = new StringBuilder();
            if (preamble != null) {
                code.append(preamble);
            }
            code.append("public class " + name + "{");
            if (body != null) {
                code.append(body);
            }
            code.append("}");
            if (postamble != null) {
                code.append(postamble);
            }
            JavaSource js = new JavaSource(name + ".java", code.toString());
            js.startPos = start;
            js.endPos = end;
            return js;
        }

        static JavaSource createFullJavaSource(String code) {
            final String name = "Bug";
            String[] parts = code.split("\\|", 3);
            JavaSource js = new JavaSource(name + ".java", parts[0] + parts[1] + parts[2]);
            js.startPos = parts[0].length();
            js.endPos = parts[0].length() + parts[1].length();
            return js;
        }
    }

    public static void main(String... args) throws IOException {
        testUninitializedVariable();
        testMissingAnnotationValue();
        testUnresolvableAnnotationAttribute();
        testFinalVariableWithDefaultConstructor();
        testFinalVariableWithConstructor();
        testWholeTextSpan();
    }

    static void testUninitializedVariable() throws IOException {
        compile(JavaSource.createJavaSource("Object o = new A().new BT(); class A { }",
                "BT"));
    }
    static void testMissingAnnotationValue() throws IOException {
        compile(JavaSource.createJavaSource("@Foo(\"vvvv\")",
                null, "@interface Foo { }", "\"vvvv\""));
    }

    static void testUnresolvableAnnotationAttribute() throws IOException {
        compile(JavaSource.createJavaSource("@Foo(value=\"vvvv\")",
                null, "@interface Foo { }", "value"));
    }

    static void testFinalVariableWithDefaultConstructor() throws IOException {
        compile(JavaSource.createJavaSource("private static final String Foo; public void bar() { }",
                "private static final String Foo;"));
    }

    static void testFinalVariableWithConstructor() throws IOException {
        compile(JavaSource.createJavaSource("public Bug (){} private static final String Foo; public void bar() { }",
                "{}"));
    }

    static void testWholeTextSpan() throws IOException {
        treeSpan(JavaSource.createFullJavaSource("|class X    |"));
    }

    static void compile(JavaSource src) throws IOException {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(ba);
        File tempDir = new File(".");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector dc = new DiagnosticCollector();
        try (JavaFileManager javaFileManager = getJavaFileManager(compiler, dc)) {
            List<String> options = new ArrayList<>();
            options.add("-cp");
            options.add(tempDir.getPath());
            options.add("-d");
            options.add(tempDir.getPath());
            options.add("--should-stop=at=GENERATE");

            List<JavaFileObject> sources = new ArrayList<>();
            sources.add(src);
            JavaCompiler.CompilationTask task =
                    compiler.getTask(writer, javaFileManager,
                    dc, options, null,
                    sources);
            task.call();
            for (Diagnostic diagnostic : (List<Diagnostic>) dc.getDiagnostics()) {
                long actualStart = diagnostic.getStartPosition();
                long actualEnd = diagnostic.getEndPosition();
                System.out.println("Source: " + src.source);
                System.out.println("Diagnostic: " + diagnostic);
                System.out.print("Start position: Expected: " + src.startPos);
                System.out.println(", Actual: " + actualStart);
                System.out.print("End position: Expected: " + src.endPos);
                System.out.println(", Actual: " + actualEnd);
                if (src.startPos != actualStart || src.endPos != actualEnd) {
                    throw new RuntimeException("error: trees don't match");
                }
            }
        }
    }

    static void treeSpan(JavaSource src) throws IOException {
        ByteArrayOutputStream ba = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(ba);
        File tempDir = new File(".");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector dc = new DiagnosticCollector();
        try (JavaFileManager javaFileManager = getJavaFileManager(compiler, dc)) {
            List<String> options = new ArrayList<>();
            options.add("-cp");
            options.add(tempDir.getPath());
            options.add("-d");
            options.add(tempDir.getPath());
            options.add("--should-stop=at=GENERATE");

            List<JavaFileObject> sources = new ArrayList<>();
            sources.add(src);
            JavacTask task = (JavacTask) compiler.getTask(writer, javaFileManager,
                                                          dc, options, null,
                                                          sources);
            SourcePositions sp = Trees.instance(task).getSourcePositions();
            boolean[] found = new boolean[1];
            new TreeScanner<Void, Void>() {
                CompilationUnitTree cut;
                @Override
                public Void scan(Tree tree, Void p) {
                    if (tree == null)
                        return null;
                    if (tree.getKind() == Kind.COMPILATION_UNIT) {
                        cut = (CompilationUnitTree) tree;
                    }
                    found[0] |= (sp.getStartPosition(cut, tree) == src.startPos) &&
                                (sp.getEndPosition(cut, tree) == src.endPos);
                    return super.scan(tree, p);
                }
            }.scan(task.parse(), null);

            if (!found[0]) {
                throw new IllegalStateException();
            }
        }
    }
}
