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
 * @summary Ensure SimpleTreeVisitor.visitSwitchExpression behaves as it should
 * @modules jdk.compiler
 */

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.tools.*;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.SwitchExpressionTree;
import com.sun.source.tree.YieldTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePathScanner;

public class SwitchExpressionSimpleVisitorTest {

    public static void main(String[] args) throws Exception {
        new SwitchExpressionSimpleVisitorTest().run();
    }

    void run() throws Exception {
        String code = "class Test {\n" +
                      "    int t(int i) {\n" +
                      "         return switch(i) {\n" +
                      "              default: yield -1;\n" +
                      "         }\n" +
                      "    }\n" +
                      "}\n";
        int[] callCount = new int[1];
        int[] switchExprNodeCount = new int[1];
        int[] yieldNodeCount = new int[1];
        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitSwitchExpression(SwitchExpressionTree node, Void p) {
                node.accept(new SimpleTreeVisitor<Void, Void>() {
                    @Override
                    protected Void defaultAction(Tree defaultActionNode, Void p) {
                        callCount[0]++;
                        if (node == defaultActionNode) {
                            switchExprNodeCount[0]++;
                        }
                        return null;
                    }
                }, null);
                return super.visitSwitchExpression(node, p);
            }
            @Override
            public Void visitYield(YieldTree node, Void p) {
                node.accept(new SimpleTreeVisitor<Void, Void>() {
                    @Override
                    protected Void defaultAction(Tree defaultActionNode, Void p) {
                        callCount[0]++;
                        if (node == defaultActionNode) {
                            yieldNodeCount[0]++;
                        }
                        return null;
                    }
                }, null);
                return super.visitYield(node, p);
            }
        }.scan(parse(code), null);

        if (callCount[0] != 2 || switchExprNodeCount[0] != 1 ||
            yieldNodeCount[0] != 1) {
            throw new AssertionError("Unexpected counts; callCount=" + callCount[0] +
                                     ", switchExprNodeCount=" + switchExprNodeCount[0] +
                                     ", yieldNodeCount=" + yieldNodeCount[0]);
        }
    }

    private CompilationUnitTree parse(String code) throws IOException {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;
        DiagnosticListener<JavaFileObject> noErrors = d -> {};

        StringWriter out = new StringWriter();
        JavacTask ct = (JavacTask) tool.getTask(out, null, noErrors,
            List.of(), null,
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
