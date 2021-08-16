/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8188225 8204674
 * @summary Check that variables of type var have a consistent model
 * @modules jdk.compiler/com.sun.tools.javac.api
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;

public class VarTree {
    private final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();

    public static void main(String... args) throws Exception {
        VarTree test = new VarTree();
        test.run("|var testVar = 0;| ",
                 "int testVar = 0");
        test.run("|var testVar = 0;| undef undef;",
                 "int testVar = 0");
        test.run("|final var testVar = 0;| ",
                 "final int testVar = 0");
        test.run("for (|var testVar| : java.util.Arrays.asList(0, 1)) {}",
                 "java.lang.Integer testVar");
        test.run("for (|final var testVar| : java.util.Arrays.asList(0, 1)) {}",
                 "final java.lang.Integer testVar");
        test.run("java.util.function.Consumer<String> c = |testVar| -> {};",
                 "java.lang.String testVar");
        test.run("java.util.function.Consumer<String> c = (|testVar|) -> {};",
                 "java.lang.String testVar");
        test.run("java.util.function.Consumer<String> c = (|var testVar|) -> {};",
                 "java.lang.String testVar");
        test.run("java.util.function.IntBinaryOperator c = (var x, |testType|) -> 1;",
                 "testType ");
    }

    void run(String code, String expected) throws IOException {
        String[] parts = code.split("\\|");

        if (parts.length != 3) {
            throw new IllegalStateException("Incorrect number of markers.");
        }

        String prefix = "public class Test { void test() { ";
        String src = prefix + parts[0] + parts[1] + parts[2] + " } }";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, d -> {},
                                                        List.of("--should-stop=at=FLOW"),
                                                        null, Arrays.asList(new MyFileObject(src)));

        Iterable<? extends CompilationUnitTree> units = ct.parse();

        runSpanCheck(ct, units, src, prefix.length() + parts[0].length(), prefix.length() + parts[0].length() + parts[1].length());

        ct.analyze();

        runSpanCheck(ct, units, src, prefix.length() + parts[0].length(), prefix.length() + parts[0].length() + parts[1].length());

        for (CompilationUnitTree cut : units) {
            new TreeScanner<Void, Void>() {
                @Override
                public Void visitVariable(VariableTree node, Void p) {
                    if (node.getName().contentEquals("testVar")) {
                        if (!expected.equals(node.toString())) {
                            throw new AssertionError("Unexpected tree: " + node.toString());
                        }
                    }
                    if (String.valueOf(node.getType()).equals("testType")) {
                        if (!expected.equals(node.toString())) {
                            throw new AssertionError("Unexpected tree: " + node.toString());
                        }
                    }
                    return super.visitVariable(node, p);
                }

            }.scan(cut, null);
        }
    }

    private void runSpanCheck(JavacTask ct, Iterable<? extends CompilationUnitTree> units, String src, int spanStart, int spanEnd) {
        Trees trees = Trees.instance(ct);
        boolean[] found = new boolean[1];

        for (CompilationUnitTree cut : units) {
            new TreeScanner<Void, Void>() {
                @Override
                public Void visitVariable(VariableTree node, Void p) {
                    if (node.getName().contentEquals("testVar")) {
                        int start = (int) trees.getSourcePositions().getStartPosition(cut, node);
                        int end   = (int) trees.getSourcePositions().getEndPosition(cut, node);

                        String snip = src.substring(start, end);

                        if (start != spanStart || end != spanEnd) {
                            throw new AssertionError("Unexpected span: " + snip);
                        }

                        int typeStart = (int) trees.getSourcePositions().getStartPosition(cut, node.getType());
                        int typeEnd   = (int) trees.getSourcePositions().getEndPosition(cut, node.getType());

                        if (typeStart != (-1) && typeEnd != (-1)) {
                            throw new AssertionError("Unexpected type position: " + typeStart + ", " + typeEnd);
                        }

                        found[0] = true;
                    }
                    if (String.valueOf(node.getType()).equals("testType")) {
                        int start = (int) trees.getSourcePositions().getStartPosition(cut, node);
                        int end   = (int) trees.getSourcePositions().getEndPosition(cut, node);

                        String snip = src.substring(start, end);

                        if (start != spanStart || end != spanEnd) {
                            throw new AssertionError("Unexpected span: " + snip);
                        }

                        found[0] = true;
                    }
                    return super.visitVariable(node, p);
                }

            }.scan(cut, null);
        }

        if (!found[0]) {
            throw new AssertionError("Didn't find the test variable.");
        }
    }

    class MyFileObject extends SimpleJavaFileObject {

        private String text;

        public MyFileObject(String text) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }
}
