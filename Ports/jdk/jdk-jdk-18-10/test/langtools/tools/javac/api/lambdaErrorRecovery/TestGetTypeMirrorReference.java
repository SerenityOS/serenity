/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8240658 8266281
 * @summary Verify that broken method invocations with lambdas get type inference done
 * @modules jdk.compiler
 * @run main TestGetTypeMirrorReference
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.*;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.lang.model.type.TypeMirror;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/*
 * This test verifies proper error recovery for method invocations which need
 * type inference, and have lambdas as arguments.
 *
 * The test will read the adjacent TestGetTypeMirrorReferenceData.java, parse and
 * attribute it, and call Trees.getTypeMirror on each place marked, in a block
 * comment, with:
 * getTypeMirror:<expected-typemirror-kind>:<expected-typemirror-toString>
 * The actual retrieved TypeMirror will be checked against the provided description,
 * verifying the return value of TypeMirror.getKind and TypeMirror.toString().
 *
 * The AST for TestGetTypeMirrorReferenceData.java will also be printed using
 * Tree.toString(), and compared to the expected AST form.
 */
public class TestGetTypeMirrorReference {

    private static final String JDK_VERSION =
            Integer.toString(Runtime.version().feature());

    public static void main(String... args) throws IOException {
        analyze("TestGetTypeMirrorReferenceData.java",
                """
                package test;

                public class TestGetTypeMirrorReferenceData {

                    public TestGetTypeMirrorReferenceData() {
                        super();
                    }

                    private static void test() {
                        Test.of(1).convert((c1)->{
                            Object o = c1;
                        });
                        Test.of(1).consume((c2)->{
                            Object o = c2;
                            return null;
                        });
                        Test.of(1).consumeWithParam((c3)->{
                            Object o = c3;
                        });
                        convert(0, (c4)->{
                            Object o = c4;
                        });
                        consume(0, (c5)->{
                            Object o = c5;
                        });
                        convertVarArgs(0, (c6)->{
                            Object o = c6;
                        }, 1, 2, 3, 4);
                        consumeVarArgs(0, (c7)->{
                            Object o = c7;
                        }, 1, 2, 3, 4);
                        convertVarArgs2(0, (c8)->{
                            Object o = c8;
                        }, (c8)->{
                            Object o = c8;
                        });
                        consumeVarArgs2(0, (c9)->{
                            Object o = c9;
                        }, (c9)->{
                            Object o = c9;
                        });
                    }

                    public <T, R>R convert(T t, Function<T, R> f, int i) {
                        return null;
                    }

                    public <T>void consume(T t, Consumer<T> c, int i) {
                    }

                    public <T, R>R convertVarArgs(T t, Function<T, R> c, int... i) {
                        return null;
                    }

                    public <T>void consumeVarArgs(T t, Consumer<T> c, int... i) {
                    }

                    public <T, R>R convertVarArgs2(T t, Function<T, R>... c) {
                        return null;
                    }

                    public <T>void consumeVarArgs2(T t, Consumer<T>... c) {
                    }

                    public static class Test<T> {

                        public Test() {
                            super();
                        }

                        public static <T>Test<T> of(T t) {
                            return new Test<>();
                        }

                        public <R>Test<R> convert(Function<T, R> c) {
                            return null;
                        }

                        public void consume(Consumer<T> c) {
                        }

                        public void consumeWithParam(Consumer<T> c, int i) {
                        }
                    }

                    public interface Function<T, R> {

                        public R map(T t);
                    }

                    public interface Consumer<T> {

                        public void run(T t);
                    }
                }""");
    }

    private static void analyze(String fileName, String expectedAST) throws IOException {
        try (StandardJavaFileManager fm = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null)) {
            List<JavaFileObject> files = new ArrayList<>();
            File source = new File(System.getProperty("test.src", "."), fileName.replace('/', File.separatorChar)).getAbsoluteFile();
            for (JavaFileObject f : fm.getJavaFileObjects(source)) {
                files.add(f);
            }
            DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
            List<String> options = List.of("-source", JDK_VERSION,
                                           "-XDshould-stop.at=FLOW");
            JavacTask ct = (JavacTask) ToolProvider.getSystemJavaCompiler().getTask(null, null, diagnostics, options, null, files);
            Trees trees = Trees.instance(ct);
            CompilationUnitTree cut = ct.parse().iterator().next();

            ct.analyze();

            String actualAST = Arrays.stream(cut.toString().split("\n"))
                                     .map(l -> l.stripTrailing())
                                     .collect(Collectors.joining("\n"));

            if (!expectedAST.equals(actualAST)) {
                throw new AssertionError("Unexpected AST shape!\n" + actualAST);
            }

            Pattern p = Pattern.compile("/\\*getTypeMirror:(.*?)\\*/");
            Matcher m = p.matcher(cut.getSourceFile().getCharContent(false));

            while (m.find()) {
                TreePath tp = pathFor(trees, cut, m.start() - 1);
                String expected = m.group(1);
                if (expected.startsWith("getParentPath:")) {
                    tp = tp.getParentPath();
                    expected = expected.substring("getParentPath:".length());
                }
                TypeMirror found = trees.getTypeMirror(tp);
                String actual = found != null ? found.getKind() + ":" + typeToString(found) : "<null>";

                if (!expected.equals(actual)) {
                    throw new IllegalStateException("expected=" + expected + "; actual=" + actual + "; tree: " + tp.getLeaf());
                }
            }
        }
    }

    private static TreePath pathFor(final Trees trees, final CompilationUnitTree cut, final int pos) {
        final TreePath[] result = new TreePath[1];

        new TreePathScanner<Void, Void>() {
            @Override public Void scan(Tree node, Void p) {
                if (   node != null
                    && trees.getSourcePositions().getStartPosition(cut, node) <= pos
                    && pos <= trees.getSourcePositions().getEndPosition(cut, node)) {
                    result[0] = new TreePath(getCurrentPath(), node);
                    return super.scan(node, p);
                }
                return null;
            }
        }.scan(cut, null);

        return result[0];
    }

    private static String typeToString(TypeMirror type) {
        return type.toString();
    }

    static class TestFileObject extends SimpleJavaFileObject {
        private final String text;
        public TestFileObject(String text) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        @Override public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }

}
