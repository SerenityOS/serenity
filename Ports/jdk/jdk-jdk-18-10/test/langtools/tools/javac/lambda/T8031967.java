/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8031967
 * @summary Ensure javac can handle very deeply nested chain of method invocations occurring as
 *          a parameter to other method invocations.
 * @modules jdk.compiler
 * @run main/othervm -Xss1m T8031967
 */

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;

public class T8031967 {

    public static void main(String... args) throws IOException {
        new T8031967().run();
    }

    final int depth = 50;

    private void run() throws IOException {
        runTestCase(true);
        runTestCase(false);
    }

    private void runTestCase(boolean withErrors) throws IOException {
        StringBuilder code = new StringBuilder();

        code.append("public class Test {\n" +
                    "    private void test() {\n" +
                    "        GroupLayout l = new GroupLayout();\n" +
                    "        l.setHorizontalGroup(\n");

        gen(code, depth);
        code.append("        );\n" +
                    "    }\n");
        if (!withErrors) {
            code.append("    class GroupLayout {\n" +
                        "        ParallelGroup createParallelGroup() {return null;}\n" +
                        "        ParallelGroup createParallelGroup(int i) {return null;}\n" +
                        "        ParallelGroup createParallelGroup(int i, int j) {return null;}\n" +
                        "        void setHorizontalGroup(Group g) { }\n" +
                        "    }\n" +
                        "    \n" +
                        "    class Group {\n" +
                        "        Group addGroup(Group g) { return this; }\n" +
                        "        Group addGroup(int i, Group g) { return this; }\n" +
                        "        Group addGap(int i) { return this; }\n" +
                        "        Group addGap(long l) { return this; }\n" +
                        "        Group addGap(int i, int j) { return this; }\n" +
                        "        Group addComponent(Object c) { return this; }\n" +
                        "        Group addComponent(int i, Object c) { return this; }\n" +
                        "    }\n" +
                        "    class ParallelGroup extends Group {\n" +
                        "        Group addGroup(Group g) { return this; }\n" +
                        "        Group addGroup(int i, Group g) { return this; }\n" +
                        "        Group addGap(int i) { return this; }\n" +
                        "        Group addGap(int i, int j) { return this; }\n" +
                        "        Group addComponent(Object c) { return this; }\n" +
                        "        Group addComponent(int i, Object c) { return this; }\n" +
                        "    }\n");
        }

        code.append("}\n");

        JavaSource source = new JavaSource(code.toString());
        List<JavaSource> sourceList = Arrays.asList(source);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticListener<JavaFileObject> noErrors = (diagnostic) -> {
            throw new IllegalStateException("Should not produce errors: " + diagnostic);
        };
        JavacTask task = (JavacTask) compiler.getTask(null, null, withErrors ? null : noErrors,
                null, null, sourceList);

        task.analyze();
    }

    private void gen(StringBuilder code, int depth) {
        code.append("l.createParallelGroup()\n");
        if (depth > 0) {
            code.append(".addGroup(\n");
            gen(code, depth - 1);
            code.append(")");
        }

        code.append(".addGap(1)\n" +
                    ".addComponent(new Object())\n" +
                    ".addGap(1)\n" +
                    ".addComponent(new Object())");
    }

    class JavaSource extends SimpleJavaFileObject {

        final String code;
        public JavaSource(String code) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }
}
