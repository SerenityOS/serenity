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
 * @bug 6472751
 * @summary SourcePositions.getStartPos returns incorrect value for enum constants
 * @author Peter Ahe
 * @modules jdk.compiler/com.sun.tools.javac.util
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.Tree.Kind;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.util.List;
import java.io.IOException;
import java.net.URI;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

public class T6472751 {
    static class MyFileObject extends SimpleJavaFileObject {
        public MyFileObject() {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
        }
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return "public enum Test { ABC, DEF; }";
        }
    }
    static Trees trees;
    static SourcePositions positions;
    public static void main(String[] args) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JavacTask task = (JavacTask) compiler.getTask(null, null, null, null, null, List.of(new MyFileObject()));
        trees = Trees.instance(task);
        positions = trees.getSourcePositions();
        Iterable<? extends CompilationUnitTree> asts = task.parse();
        for (CompilationUnitTree ast : asts) {
            new MyVisitor().scan(ast, null);
        }
    }

    static class MyVisitor extends TreeScanner<Void,Void> {
        @Override
        public Void scan(Tree node, Void ignored) {
            if (node == null)
                return null;
            Kind k = node.getKind();
            long pos = positions.getStartPosition(null,node);
            System.out.format("%s: %s%n", k, pos);
            if (k != Kind.MODIFIERS && pos < 0)
                throw new Error("unexpected position found");
            return super.scan(node, ignored);
        }
    }
}
