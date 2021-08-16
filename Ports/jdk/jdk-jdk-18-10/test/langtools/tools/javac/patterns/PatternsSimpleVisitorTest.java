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
 * @bug 8231827
 * @summary Ensure SimpleTreeVisitor.visitBindingPattern and visitInstanceOf behaves as it should
 * @modules jdk.compiler
 */

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.tools.*;

import com.sun.source.tree.BindingPatternTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.InstanceOfTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SimpleTreeVisitor;
import com.sun.source.util.TreePathScanner;

public class PatternsSimpleVisitorTest {

    public static void main(String[] args) throws Exception {
        new PatternsSimpleVisitorTest().run();
    }

    void run() throws Exception {
        String code = "class Test {\n" +
                      "    boolean t(Object o) {\n" +
                      "         return o instanceof String s ? s.isEmpty() : false;\n" +
                      "    }\n" +
                      "}\n";
        int[] callCount = new int[1];
        int[] instanceOfNodeCount = new int[1];
        int[] bindingPatternNodeCount = new int[1];
        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitInstanceOf(InstanceOfTree node, Void p) {
                node.accept(new SimpleTreeVisitor<Void, Void>() {
                    @Override
                    protected Void defaultAction(Tree defaultActionNode, Void p) {
                        callCount[0]++;
                        if (node == defaultActionNode) {
                            instanceOfNodeCount[0]++;
                        }
                        return null;
                    }
                }, null);
                return super.visitInstanceOf(node, p);
            }
            @Override
            public Void visitBindingPattern(BindingPatternTree node, Void p) {
                node.accept(new SimpleTreeVisitor<Void, Void>() {
                    @Override
                    protected Void defaultAction(Tree defaultActionNode, Void p) {
                        callCount[0]++;
                        if (node == defaultActionNode) {
                            bindingPatternNodeCount[0]++;
                        }
                        return null;
                    }
                }, null);
                return super.visitBindingPattern(node, p);
            }
        }.scan(parse(code), null);

        if (callCount[0] != 2 || instanceOfNodeCount[0] != 1 ||
            bindingPatternNodeCount[0] != 1) {
            throw new AssertionError("Unexpected counts; callCount=" + callCount[0] +
                                     ", switchExprNodeCount=" + instanceOfNodeCount[0] +
                                     ", yieldNodeCount=" + bindingPatternNodeCount[0]);
        }
    }

    private CompilationUnitTree parse(String code) throws IOException {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;
        DiagnosticListener<JavaFileObject> noErrors = d -> {};

        StringWriter out = new StringWriter();
        JavacTask ct = (JavacTask) tool.getTask(out, null, noErrors,
            null, null,
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
