/*
 * Copyright (c) 2006, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6557752
 * @summary Test for wrapping the original type in ErrorType.
 * @library ../lib
 * @modules jdk.compiler/com.sun.tools.javac.util
 * @compile T6557752.java
 * @run main T6557752
 */

import com.sun.source.tree.AssignmentTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.util.List;
import java.io.IOException;
import java.net.URI;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import javax.lang.model.util.Types;

public class T6557752 {
    static class MyFileObject extends SimpleJavaFileObject {
        public MyFileObject() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
        }
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return "import java.util.*;\n"
                + "public class Test {\n"
                + "    void foobar() {\n"
                + "        Iterator<Number> itr = null;\n"
                + "        String str = itr.next();\n"
                + "        FooBar fooBar = FooBar.foobar();\n"
                + "    }\n"
                + "}";
        }
    }
    static Trees trees;
    static JavacTask task = null;
    public static void main(String[] args) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        task = (JavacTask) compiler.getTask(null, null, null, null, null, List.of(new MyFileObject()));
        Iterable<? extends CompilationUnitTree> asts = task.parse();
        task.analyze();
        trees = Trees.instance(task);
        MyVisitor myVisitor = new MyVisitor();
        for (CompilationUnitTree ast : asts) {
            myVisitor.compilationUnit = ast;
            myVisitor.scan(ast, null);
        }

        if (!myVisitor.foundError) {
            throw new AssertionError("Expected error not found!");
        }
    }

    static class MyVisitor extends TreePathScanner<Void,Void> {
        public boolean foundError = false;
        CompilationUnitTree compilationUnit = null;
        int i = 0;
        @Override
        public Void visitMethodInvocation(MethodInvocationTree node, Void ignored) {
            TreePath path = TreePath.getPath(compilationUnit, node);
            TypeMirror typeMirror = trees.getTypeMirror(path);
            if (typeMirror.getKind() == TypeKind.ERROR) {
              if (i == 0) {
                String str1 = trees.getOriginalType((ErrorType)typeMirror).toString();
                if (!str1.equals("java.lang.Number")) {
                    throw new AssertionError("Trees.getOriginalType() error!");
                }

                Types types = task.getTypes();

                str1 = types.asElement(trees.getOriginalType((ErrorType)typeMirror)).toString();
                if (!str1.equals("java.lang.Number")) {
                    throw new AssertionError("Types.asElement() error!");
                }

                i++;
              }
              else if (i == 1) {
                String str1 = trees.getOriginalType((ErrorType)typeMirror).toString();
                if (!str1.equals("FooBar")) {
                    throw new AssertionError("Trees.getOriginalType() error!");
                }

                Types types = task.getTypes();

                str1 = types.asElement(trees.getOriginalType((ErrorType)typeMirror)).toString();
                if (!str1.equals("FooBar")) {
                    throw new AssertionError("Types.asElement() error!");
                }
                foundError = true;
              }
            }


            return null;
        }

    }
}
