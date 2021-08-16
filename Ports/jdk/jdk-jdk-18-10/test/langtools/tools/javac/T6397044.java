/*
 * Copyright (c) 2006, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6397044
 * @summary JCModifiers.getModifiers() returns incorrect Modifiers set.
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 */

import java.io.*;
import java.util.*;
import javax.tools.*;
import javax.lang.model.element.Modifier;
import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.tree.JCTree.JCModifiers;

public abstract class T6397044 {
    public static void main(String[] args) throws Exception {
        String srcDir = System.getProperty("test.src", ".");
        String self = T6397044.class.getName();
        JavacTool tool = JavacTool.create();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> files
                = fm.getJavaFileObjectsFromFiles(Arrays.asList(new File(srcDir, self + ".java")));
            JavacTask task = tool.getTask(null, fm, null, List.of("--release", "16"), null, files);
            Iterable<? extends CompilationUnitTree> trees = task.parse();
            Checker checker = new Checker();
            for (CompilationUnitTree tree: trees)
                checker.check(tree);
        }
    }

    public int x_public;
    protected int x_protected;
    private int x_private;
    abstract int x_abstract();
    static int x_static;
    final int x_final = 1;
    transient int x_transient;
    volatile int x_volatile;
    synchronized void x_synchronized() { }
    native int x_native();
    strictfp void x_strictfp() { }

    static class Checker extends TreeScanner<Void,Void> {
        void check(Tree tree) {
            if (tree != null)
                tree.accept(this, null);
        }

        void check(List<? extends Tree> trees) {
            if (trees == null)
                return;
            for (Tree tree: trees)
                check(tree);
        }

        public Void visitCompilationUnit(CompilationUnitTree tree, Void ignore) {
            check(tree.getTypeDecls());
            return null;
        }

        public Void visitClass(ClassTree tree, Void ignore) {
            check(tree.getMembers());
            return null;
        }

        public Void visitMethod(MethodTree tree, Void ignore) {
            check(tree.getName(), tree.getModifiers());
            return null;
        }

        public Void visitVariable(VariableTree tree, Void ignore) {
            check(tree.getName(), tree.getModifiers());
            return null;
        }

        private void check(CharSequence name, ModifiersTree modifiers) {
            long sysflags = ((JCModifiers) modifiers).flags;
            System.err.println(name + ": " + modifiers.getFlags() + " | " + Flags.toString(sysflags));
            if (name.toString().startsWith("x_")) {
                String expected = "[" + name.toString().substring(2) + "]";
                String found = modifiers.getFlags().toString();
                if (!found.equals(expected))
                    throw new AssertionError("expected: " + expected + "; found: " + found);
            }
        }
    }
}
