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
 * @summary Ensure BreakTree.getLabel returns reasonable values
 * @modules jdk.compiler
 */

import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.lang.model.element.Name;
import javax.tools.*;

import com.sun.source.tree.BreakTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePathScanner;

public class BreakTest {

    private static final String CODE =
            "public class C {" +
            "    void t1(Integer i) {" +
            "        LABEL: switch (i) {" +
            "            case null: i++; break LABEL;" +
            "            default: i++; break;" +
            "        }" +
            "    }" +
            "    int t2(Integer i) {" +
            "        return switch (i) {" +
            "            case null: break LABEL;" +
            "            default: yield 2;" +
            "        }" +
            "    }" +
            "}";

    public static void main(String[] args) throws Exception {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        String sourceVersion = Integer.toString(Runtime.version().feature());
        assert tool != null;
        DiagnosticListener<JavaFileObject> noErrors = d -> {};

        StringWriter out = new StringWriter();
        JavacTask ct = (JavacTask) tool.getTask(out, null, noErrors,
            List.of("-XDdev"), null,
            Arrays.asList(new MyFileObject(CODE)));
        List<String> labels = new ArrayList<>();
        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitBreak(BreakTree node, Void p) {
                Name label = node.getLabel();
                labels.add(label != null ? label.toString() : null);
                return super.visitBreak(node, p);
            }
        }.scan(ct.parse(), null);

        List<String> expected = Arrays.asList("LABEL", null, "LABEL");

        if (!expected.equals(labels)) {
            throw new AssertionError("Unexpected labels found: " + labels);
        }
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
