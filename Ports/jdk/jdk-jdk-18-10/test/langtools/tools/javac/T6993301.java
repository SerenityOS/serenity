/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6993301
 * @summary catch parameters do not have correct kind (i.e. ElementKind.EXCEPTION_PARAMETER)
 * @modules jdk.compiler/com.sun.tools.javac.api
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTaskImpl;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * @author Jan Lahoda
 */
public class T6993301 {
    public static void main(String... args) throws Exception {
        new T6993301().testExceptionParameterCorrectKind();
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

    public void testExceptionParameterCorrectKind() throws IOException {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;

        String code = "package test; public class Test { { try { } catch (NullPointerException ex) {} } }";

        final JavacTaskImpl ct = (JavacTaskImpl)tool.getTask(null, null, null,
                null, null, Arrays.asList(new MyFileObject(code)));
        CompilationUnitTree cut = ct.parse().iterator().next();

        ct.analyze();

        new TreePathScanner<Void, Void>() {
            @Override
            public Void visitVariable(VariableTree node, Void p) {
                Element el = Trees.instance(ct).getElement(getCurrentPath());

                assertNotNull(el);
                assertEquals(ElementKind.EXCEPTION_PARAMETER, el.getKind());

                return super.visitVariable(node, p);
            }
        }.scan(cut, null);
    }

    private void assertNotNull(Object o) {
        if (o == null)
            throw new AssertionError();
    }

    private <T> void assertEquals(T expected, T actual) {
        if (expected == null ? actual == null : expected.equals(actual))
            return;
        throw new AssertionError("expected: " + expected + ", actual: " + actual);
    }

}
