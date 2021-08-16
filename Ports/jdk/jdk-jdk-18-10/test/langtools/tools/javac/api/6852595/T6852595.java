/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6852595
 * @summary Accessing scope using JSR199 API on erroneous tree causes Illegal Argument Exception
 * @author  mcimadamore
 * @modules jdk.compiler/com.sun.tools.javac.tree
 */

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import com.sun.source.tree.*;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;
import com.sun.tools.javac.tree.JCTree.*;

import static javax.tools.JavaFileObject.Kind;

public class T6852595 {
    public static void main(String[] args) throws IOException {
        JavaFileObject sfo = new SimpleJavaFileObject(URI.create("myfo:/Test.java"),Kind.SOURCE) {
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return "class BadName { Object o = j; }";
            }
        };
        List<? extends JavaFileObject> files = Arrays.asList(sfo);
        JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        JavacTask ct = (JavacTask)tool.getTask(null, null, null, null, null, files);
        Iterable<? extends CompilationUnitTree> compUnits = ct.parse();
        CompilationUnitTree cu = compUnits.iterator().next();
        ClassTree cdef = (ClassTree)cu.getTypeDecls().get(0);
        JCVariableDecl vdef = (JCVariableDecl)cdef.getMembers().get(0);
        TreePath path = TreePath.getPath(cu, vdef.init);
        Trees.instance(ct).getScope(path);
    }
}
