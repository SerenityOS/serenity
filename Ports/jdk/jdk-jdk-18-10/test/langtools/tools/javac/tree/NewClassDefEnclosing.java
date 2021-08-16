/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8044853
 * @summary Check that enclosing element is not cleared in JCNewClass
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.tree
 */

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.tree.JCTree;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class NewClassDefEnclosing {
    private final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();

    public static void main(String... args) throws Exception {
        NewClassDefEnclosing nap = new NewClassDefEnclosing();
        nap.run("O.I i = new O().new I() {}; class O { class I { } }",
                "O.I i = new O().new I(){ (.Test.O x0) { x0.super(); } }");
        nap.run("O.I i = new O().new I(1, \"2\") {}; class O { class I { public I(int i, String s) { } }",
                "O.I i = new O().new I(1, \"2\"){ (.Test.O x0, int i, java.lang.String s) { x0.super(i, s); } }");
    }

    void run(String code, String expected) throws IOException {
        String src = "public class Test {" + code + "}";

        JavacTaskImpl ct = (JavacTaskImpl) tool.getTask(null, null, null, List.of("-doe"),
                null, Arrays.asList(new MyFileObject(src)));

        Iterable<? extends CompilationUnitTree> units = ct.parse();
        ct.analyze();

        for (CompilationUnitTree cut : units) {
            JCTree.JCVariableDecl var =
                    (JCTree.JCVariableDecl) ((ClassTree) cut.getTypeDecls().get(0)).getMembers().get(1);

            expected = expected.replaceAll("\\s+", " ");
            String actual = var.toString().replaceAll("\\s+", " ");

            if (!expected.equals(actual)) {
                System.err.println("Expected: " + expected);
                System.err.println("Obtained: " + actual);
                throw new RuntimeException("strings do not match!");
            }
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
