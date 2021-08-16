/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8009724
 * @summary adding DocTreePath and DocTreePathScanner
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 */

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.doctree.DocTree;
import com.sun.source.doctree.DocTree.Kind;
import com.sun.source.doctree.DocTreeVisitor;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.DocTreePath;
import com.sun.source.util.DocTreePathScanner;
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.api.JavacTool;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Name;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

public class DocTreePathScannerTest {
    public static void main(String... args) throws Exception {
        DocTreePathScannerTest t = new DocTreePathScannerTest();
        t.run();
    }

    void run() throws Exception {
        List<File> files = new ArrayList<File>();
        File testSrc = new File(System.getProperty("test.src"));
        for (File f: testSrc.listFiles()) {
            if (f.isFile() && f.getName().endsWith(".java"))
                files.add(f);
        }

        JavacTool javac = JavacTool.create();
        try (StandardJavaFileManager fm = javac.getStandardFileManager(null, null, null)) {

            Iterable<? extends JavaFileObject> fos = fm.getJavaFileObjectsFromFiles(files);

            JavacTask t = javac.getTask(null, fm, null, null, null, fos);
            DocTrees trees = DocTrees.instance(t);

            Iterable<? extends CompilationUnitTree> units = t.parse();

            DeclScanner ds = new DeclScanner(trees);
            for (CompilationUnitTree unit: units) {
                ds.scan(unit, null);
            }

            if (errors > 0)
                throw new Exception(errors + " errors occurred");
        }
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;

    class DeclScanner extends TreePathScanner<Void, Void> {
        DocTrees trees;
        DocTreePathScanner<Void,Void> cs;

        DeclScanner(DocTrees trees) {
            this.trees = trees;
            cs = new CommentPathScanner();
        }

        @Override
        public Void visitClass(ClassTree tree, Void ignore) {
            super.visitClass(tree, ignore);
            visitDecl(tree, tree.getSimpleName());
            return null;
        }

        @Override
        public Void visitMethod(MethodTree tree, Void ignore) {
            super.visitMethod(tree, ignore);
            visitDecl(tree, tree.getName());
            return null;
        }

        @Override
        public Void visitVariable(VariableTree tree, Void ignore) {
            super.visitVariable(tree, ignore);
            visitDecl(tree, tree.getName());
            return null;
        }

        void visitDecl(Tree tree, Name name) {
            TreePath path = getCurrentPath();
            DocCommentTree dc = trees.getDocCommentTree(path);
            if (dc != null)
                cs.scan(new DocTreePath(path, dc), null);
        }
    }

    class CommentPathScanner extends DocTreePathScanner<Void, Void> {
        CommentPathScanner() {}

        @Override
        public Void scan(final DocTree tree, Void ignore) {
            if (tree != null) {
                DocTree previous = null;
                for (DocTree current : getCurrentPath()) {
                    if (previous != null) {
                        final List<DocTree> children = new ArrayList<>();
                        current.accept(new DocTreeScanner<Void, Void>() {
                            @Override public Void scan(DocTree node, Void p) {
                                children.add(node);
                                return null;
                            }
                        }, null);

                        if (!children.contains(previous)) {
                            error("Invalid DocTreePath for: " + tree);
                        }
                    }

                    previous = current;
                }
            }
            return super.scan(tree, ignore);
        }
    }

}
