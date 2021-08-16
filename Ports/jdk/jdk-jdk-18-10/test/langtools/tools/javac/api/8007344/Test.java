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
 * @bug 8007344
 * @summary javac may not make tree end positions and/or doc comments
 *          available to processors and listeners
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build JavacTestingAbstractProcessor
 * @run main Test
 */

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import com.sun.source.doctree.DocCommentTree;
import com.sun.source.tree.*;
import com.sun.source.util.DocTrees;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.util.Position;

/** Doc comment: Test */
public class Test {
    public static final int EXPECT_DOC_COMMENTS = 3;

    /** Doc comment: main */
    public static void main(String... args) throws Exception {
        PrintWriter out = new PrintWriter(System.err);
        try {
            new Test(out).run();
        } finally {
            out.flush();
        }
    }

    PrintWriter out;
    int errors;

    Test(PrintWriter out) {
        this.out = out;
    }

    /** Doc comment: run */
    void run() throws Exception {
        File testSrc = new File(System.getProperty("test.src"));
        File thisFile = new File(testSrc, getClass().getName() + ".java");
        JavacTool javac = JavacTool.create();
        try (StandardJavaFileManager fm = javac.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(new File(".")));
            Iterable<? extends JavaFileObject> fos = fm.getJavaFileObjects(thisFile);
            testAnnoProcessor(javac, fm, fos, out, EXPECT_DOC_COMMENTS);
            testTaskListener(javac, fm, fos, out, EXPECT_DOC_COMMENTS);

            if (errors > 0)
                throw new Exception(errors + " errors occurred");
        }
    }

    void testAnnoProcessor(JavacTool javac, StandardJavaFileManager fm,
            Iterable<? extends JavaFileObject> files, PrintWriter out,
            int expectedDocComments) {
        out.println("Test annotation processor");
        JavacTask task = javac.getTask(out, fm, null, null, null, files);
        AnnoProc ap = new AnnoProc(DocTrees.instance(task));
        task.setProcessors(Arrays.asList(ap));
        task.call();
        ap.checker.checkDocComments(expectedDocComments);
    }

    void testTaskListener(JavacTool javac, StandardJavaFileManager fm,
            Iterable<? extends JavaFileObject> files, PrintWriter out,
            int expectedDocComments) {
        out.println("Test task listener");
        JavacTask task = javac.getTask(out, fm, null, null, null, files);
        TaskListnr tl = new TaskListnr(DocTrees.instance(task));
        task.addTaskListener(tl);
        task.call();
        tl.checker.checkDocComments(expectedDocComments);
    }

    void error(String msg) {
        out.println("Error: " + msg);
        errors++;
    }

    class AnnoProc extends JavacTestingAbstractProcessor {
        Checker checker;

        AnnoProc(DocTrees trees) {
            checker = new Checker(trees);
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            for (Element e : roundEnv.getRootElements()) {
                checker.scan(checker.trees.getPath(e), null);
            }
            return true;
        }
    }

    class TaskListnr implements TaskListener {
        Checker checker;

        TaskListnr(DocTrees trees) {
            checker = new Checker(trees);
        }

        public void started(TaskEvent e) {
            if (e.getKind() == TaskEvent.Kind.ANALYZE)
                checker.scan(new TreePath(e.getCompilationUnit()), null);
        }

        public void finished(TaskEvent e) {
        }
    }

    class Checker extends TreePathScanner<Void,Void> {
        DocTrees trees;
        SourcePositions srcPosns;

        int docComments = 0;

        Checker(DocTrees trees) {
            this.trees = trees;
            srcPosns = trees.getSourcePositions();
        }

        @Override
        public Void scan(Tree tree, Void ignore) {
            if (tree != null) {
                switch (tree.getKind()) {
                    // HACK: Workaround 8007350
                    // Some tree nodes do not have endpos set
                    case ASSIGNMENT:
                    case BLOCK:
                    case IDENTIFIER:
                    case METHOD_INVOCATION:
                        break;

                    default:
                        checkEndPos(getCurrentPath().getCompilationUnit(), tree);
                }
            }
            return super.scan(tree, ignore);
        }

        @Override
        public Void visitClass(ClassTree tree, Void ignore) {
            checkComment();
            return super.visitClass(tree, ignore);
        }

        @Override
        public Void visitMethod(MethodTree tree, Void ignore) {
            checkComment();
            return super.visitMethod(tree, ignore);
        }

        @Override
        public Void visitVariable(VariableTree tree, Void ignore) {
            checkComment();
            return super.visitVariable(tree, ignore);
        }

        void checkComment() {
            DocCommentTree dc = trees.getDocCommentTree(getCurrentPath());
            if (dc != null) {
                out.println("comment: " + dc.toString().replaceAll("\\s+", " "));
                docComments++;
            }
        }

        void checkEndPos(CompilationUnitTree unit, Tree tree) {
            long sp = srcPosns.getStartPosition(unit, tree);
            long ep = srcPosns.getEndPosition(unit, tree);
            if (sp >= 0 && ep == Position.NOPOS) {
                error("endpos not set for " + tree.getKind()
                        + " " + Pretty.toSimpleString(((JCTree) tree))
                        +", start:" + sp);
            }
        }

        void checkDocComments(int expected) {
            if (docComments != expected) {
                error("Unexpected number of doc comments received: "
                        + docComments + ", expected: " + expected);
            }
        }

    }
}
