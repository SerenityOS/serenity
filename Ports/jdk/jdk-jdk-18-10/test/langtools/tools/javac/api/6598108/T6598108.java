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
 * @bug     6598108
 * @summary com.sun.source.util.Trees.isAccessible incorrect
 * @author  Jan Lahoda
 * @modules jdk.compiler
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Scope;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import java.net.URI;
import java.util.Arrays;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class T6598108 {
    public static void main(String[] args) throws Exception {
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        assert tool != null;
        final JavacTask ct = (JavacTask)tool.getTask(null, null, null, null, null, Arrays.asList(new MyFileObject()));

        CompilationUnitTree cut = ct.parse().iterator().next();
        TreePath tp = new TreePath(new TreePath(cut), cut.getTypeDecls().get(0));
        Scope s = Trees.instance(ct).getScope(tp);
        TypeElement type = ct.getElements().getTypeElement("com.sun.java.util.jar.pack.Package.File");

        if (Trees.instance(ct).isAccessible(s, type)) {
            //com.sun.java.util.jar.pack.Package.File is a public innerclass inside a non-accessible class, so
            //"false" would be expected here.
            throw new IllegalStateException("");
        }
    }

    static class MyFileObject extends SimpleJavaFileObject {
        public MyFileObject() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
        }
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return "public class Test<TTT> { public void test() {TTT ttt;}}";
        }
    }
}
