/*
 * Copyright (c) 2012, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7021614
 * @summary extend com.sun.source API to support parsing javadoc comments
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
import com.sun.source.util.DocTreeScanner;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SimpleDocTreeVisitor;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.api.JavacTool;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Name;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

public class SimpleDocTreeVisitorTest {
    public static void main(String... args) throws Exception {
        SimpleDocTreeVisitorTest t = new SimpleDocTreeVisitorTest();
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

            Set<DocTree.Kind> found = EnumSet.noneOf(DocTree.Kind.class);
            DeclScanner ds = new DeclScanner(trees, found);
            for (CompilationUnitTree unit: units) {
                ds.scan(unit, null);
            }

            for (DocTree.Kind k: DocTree.Kind.values()) {
                if (!found.contains(k) && k != DocTree.Kind.OTHER && k != DocTree.Kind.DOC_TYPE)
                    error("not found: " + k);
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

    static class DeclScanner extends TreePathScanner<Void, Void> {
        DocTrees trees;
        DocTreeScanner<Void,Void> cs;

        DeclScanner(DocTrees trees, final Set<DocTree.Kind> found) {
            this.trees = trees;
            cs = new CommentScanner(found);
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
                cs.scan(dc, null);
        }
    }

    static class CommentScanner extends DocTreeScanner<Void, Void> {
        DocTreeVisitor<Void, Void> visitor;

        CommentScanner(Set<DocTree.Kind> found) {
            visitor = new Visitor(found);
        }

        @Override
        public Void scan(DocTree tree, Void ignore) {
            if (tree != null)
                tree.accept(visitor, ignore);
            return super.scan(tree, ignore);
        }
    }

    static class Visitor extends SimpleDocTreeVisitor<Void, Void> {
        Set<DocTree.Kind> found;

        Visitor(Set<DocTree.Kind> found) {
            this.found = found;
        }

        @Override
        public Void defaultAction(DocTree tree, Void ignore) {
            found.add(tree.getKind());
            return null;
        }
    }
}
