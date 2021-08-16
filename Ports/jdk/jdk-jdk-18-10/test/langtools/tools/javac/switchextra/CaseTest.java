/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8206986
 * @summary Ensure CaseTree methods return expected values
 * @modules jdk.compiler
 */

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.tools.*;

import com.sun.source.tree.CaseTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;

public class CaseTest {

    public static void main(String[] args) throws Exception {
        String sourceVersion = Integer.toString(Runtime.version().feature());
        new CaseTest().testLabels(sourceVersion);
        new CaseTest().testStatement(sourceVersion);
        new CaseTest().testRule(sourceVersion);
    }

    void testLabels(String sourceVersion) throws Exception {
        String code = "class Test {\n" +
                      "    void t(int i) {\n" +
                      "         switch(i) {\n" +
                      "              case 0: break;\n" +
                      "              case 1, 2: breal;\n" +
                      "              default: breal;\n" +
                      "         }\n" +
                      "    }\n" +
                      "}\n";
        List<String> labels = new ArrayList<>();
        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitCase(CaseTree node, Void p) {
                labels.add(String.valueOf(node.getExpression()));
                labels.add(node.getExpressions().stream()
                                                .map(String::valueOf)
                                                .collect(Collectors.joining(",", "[", "]")));
                return super.visitCase(node, p);
            }
        }.scan(parse(code, sourceVersion), null);

        List<String> expected = Arrays.asList("0", "[0]", "1", "[1,2]", "null", "[]");

        if (!expected.equals(labels)) {
            throw new AssertionError("Unexpected labels found: " + labels);
        }
    }

    void testStatement(String sourceVersion) throws Exception {
        String code = "class Test {\n" +
                      "    void t(int i) {\n" +
                      "         switch(i) {\n" +
                      "              case 0:" +
                      "                  System.err.println();\n" +
                      "                  break;\n" +
                      "         }\n" +
                      "    }\n" +
                      "}\n";
        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitCase(CaseTree node, Void p) {
                if (node.getStatements().size() != 2) {
                    throw new AssertionError("Unexpected statements: " + node.getStatements());
                }
                if (node.getBody() != null) {
                    throw new AssertionError("Unexpected body: " + node.getBody());
                }
                return super.visitCase(node, p);
            }
        }.scan(parse(code, sourceVersion), null);
    }

    void testRule(String sourceVersion) throws Exception {
        String code = "class Test {\n" +
                      "    void t(int i) {\n" +
                      "         switch(i) {\n" +
                      "              case 0 -> {" +
                      "                  System.err.println();\n" +
                      "              };\n" +
                      "         }\n" +
                      "    }\n" +
                      "}\n";
        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitCase(CaseTree node, Void p) {
                if (node.getStatements() != null) {
                    throw new AssertionError("Unexpected statements: " + node.getStatements());
                }
                if (node.getBody().getKind() != Tree.Kind.BLOCK) {
                    throw new AssertionError("Unexpected body: " + node.getBody());
                }
                return super.visitCase(node, p);
            }
        }.scan(parse(code, sourceVersion), null);
    }

    private CompilationUnitTree parse(String code, String sourceVersion) throws IOException {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;
        DiagnosticListener<JavaFileObject> noErrors = d -> {};

        StringWriter out = new StringWriter();
        JavacTask ct = (JavacTask) tool.getTask(out, null, noErrors,
            List.of("-XDdev"), null,
            Arrays.asList(new MyFileObject(code)));
        return ct.parse().iterator().next();
    }

    static class MyFileObject extends SimpleJavaFileObject {
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
