/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8202178
 * @summary Make sure that var kind != None
 * @compile -Werror -Xlint:all VarInstanceMemberTest.java
 * @run main VarInstanceMemberTest
 */

import java.net.URI;
import java.util.Arrays;

import javax.lang.model.type.TypeKind;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.*;

public class VarInstanceMemberTest {

    static class JavaSource extends SimpleJavaFileObject {

        final static String sourceStub = "class Test {var v1;}";

        public JavaSource() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return sourceStub;
        }

    }

    public static void main(String[] args) throws Exception {
        check();

    }

    static void check() throws Exception {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
        JavacTask ct = (JavacTask) tool.getTask(null, null, dc,
                null, null, Arrays.asList(new JavaSource()));
        CompilationUnitTree tree = ct.parse().iterator().next();

        ct.analyze();
        new TreePathScanner<Object, Object>() {
            @Override
            public Object visitVariable(VariableTree node, Object p) {
                TypeKind kind = Trees.instance(ct).getElement(getCurrentPath()).asType().getKind();
                if (kind != TypeKind.ERROR) {
                    throw new AssertionError("Kind = " + Trees.instance(ct).getElement(getCurrentPath()).asType().getKind());
                }
                return super.visitVariable(node, p);
            }
        }.scan(tree, null);
    }

}

