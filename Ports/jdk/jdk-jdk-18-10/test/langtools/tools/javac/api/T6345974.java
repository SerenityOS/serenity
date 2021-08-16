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
 * @bug 6345974
 * @summary JCPrimitiveTypeTree.getPrimitiveTypeKind() inappropriately throws an
 *              AssertionError for "void"
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 */

import com.sun.tools.javac.api.JavacTool;
import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.PrimitiveTypeTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import javax.tools.StandardJavaFileManager;


public class T6345974 {
    public static void main(String[] args) throws Exception {
        PrintWriter out = new PrintWriter(System.out, true);
        JavacTool tool = JavacTool.create();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            File testSrc = new File(System.getProperty("test.src"));
            Iterable<? extends JavaFileObject> f =
                fm.getJavaFileObjectsFromFiles(Arrays.asList(new File(testSrc, "T6345974.java")));
            JavacTask task = tool.getTask(out, fm, null, null, null, f);
            Iterable<? extends CompilationUnitTree> trees = task.parse();
            out.flush();

            Scanner s = new Scanner();
            for (CompilationUnitTree t: trees)
                s.scan(t, null);
        }
    }

    private static class Scanner extends TreeScanner<Void,Void> {
        public Void visitPrimitiveType(PrimitiveTypeTree node, Void ignore) {
            // The following call of getPrimitiveTypeKind should not throw an AssertionError
            System.out.println(node + " " + node.getPrimitiveTypeKind());
            return null;
        }
    }

    boolean b;
    byte by;
    char c;
    double d;
    float f;
    int i;
    long l;
    short s;
    void f() { }
}
