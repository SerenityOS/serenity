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
 * @bug     8184739
 * @summary Check correct end position of the PackageTree
 * @modules jdk.compiler
 */

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Trees;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import static javax.tools.JavaFileObject.Kind.SOURCE;
import javax.tools.ToolProvider;

import com.sun.source.tree.ImportTree;
import com.sun.source.tree.PackageTree;

public class T8184739 {
    public static void main(String... args) throws IOException {
        class MyFileObject extends SimpleJavaFileObject {
            MyFileObject() {
                super(URI.create("myfo:///Test.java"), SOURCE);
            }
            @Override
            public String getCharContent(boolean ignoreEncodingErrors) {
                //      0         1         2         3         4
                //      0123456789012345678901234567890123456789012345
                return "package a.b; /*foo*/ import java.lang.Object; /*foo*/ class Test { Test() { } }";
            }
        }
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        List<JavaFileObject> compilationUnits =
                Collections.<JavaFileObject>singletonList(new MyFileObject());
        JavacTask task = (JavacTask)javac.getTask(null, null, null, null, null, compilationUnits);
        Trees trees = Trees.instance(task);
        CompilationUnitTree toplevel = task.parse().iterator().next();
        PackageTree pt = toplevel.getPackage();
        ImportTree impt = toplevel.getImports().get(0);
        long pos = trees.getSourcePositions().getStartPosition(toplevel, pt);
        if (pos != 0)
            throw new AssertionError(String.format("Start pos for %s is incorrect (%s)!",
                                                   pt, pos));
        pos = trees.getSourcePositions().getEndPosition(toplevel, pt);
        if (pos != 12)
            throw new AssertionError(String.format("End pos for %s is incorrect (%s)!",
                                                   pt, pos));
        pos = trees.getSourcePositions().getStartPosition(toplevel, impt);
        if (pos != 21)
            throw new AssertionError(String.format("Start pos for %s is incorrect (%s)!",
                                                   impt, pos));
        pos = trees.getSourcePositions().getEndPosition(toplevel, impt);
        if (pos != 45)
            throw new AssertionError(String.format("End pos for %s is incorrect (%s)!",
                                                   impt, pos));
    }
}
