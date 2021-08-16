/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6993305
 * @summary starting position of a method without modifiers and with type parameters is incorrect
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 */

import java.io.File;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.SourcePositions;
import com.sun.source.util.TreeScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTool;
import java.io.IOException;

/*
 * Test verifies the starting position of all methods by computing the start position
 * of each method as the first non-white character on the first line containing
 * (" " + methodName + "("), and then comparing this value against the reported
 * value in the SourcePositions table.
 */
public class T6993305 {
    <T> void test1(T t) { }  // this is the primary case to be tested
    public <T> void test2(T t) { }
    @Deprecated <T> void test3(T t) { }

    public static void main(String... args) throws Exception {
        new T6993305().run();
    }

    void run() throws Exception {
        File testSrc = new File(System.getProperty("test.src"));

        JavacTool tool = JavacTool.create();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {

            File f = new File(testSrc, T6993305.class.getSimpleName() + ".java");
            Iterable<? extends JavaFileObject> fos = fm.getJavaFileObjects(f);
            JavacTask task = tool.getTask(null, fm, null, null, null, fos);
            Iterable<? extends CompilationUnitTree> cus = task.parse();

            TestScanner s = new TestScanner();
            s.scan(cus, task);

            if (errors > 0)
                throw new Exception(errors + " errors occurred");
        }
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;

    class TestScanner extends TreeScanner<Void, JavacTask> {
        CompilationUnitTree cu;
        SourcePositions sourcePositions;
        String source;

        void show(String label, int pos) {
            System.err.println(label + ": " +
                    source.substring(pos, Math.min(source.length(), pos + 10)));
        }

        @Override public Void visitCompilationUnit(CompilationUnitTree tree, JavacTask task) {
            cu = tree;
            Trees trees = Trees.instance(task);
            sourcePositions = trees.getSourcePositions();
            try {
                source = String.valueOf(tree.getSourceFile().getCharContent(true));
            } catch (IOException e) {
                throw new Error(e);
            }
            return super.visitCompilationUnit(tree, task);
        }

        // this is the core of the test
        @Override public Void visitMethod(MethodTree tree, JavacTask task) {
            String name = String.valueOf(tree.getName());
            int pos = source.indexOf(" " + name + "(");
            while (source.charAt(pos - 1) != '\n') pos--;
            while (source.charAt(pos) == ' ') pos++;
            int expectedStart = pos;
            int reportedStart = (int) sourcePositions.getStartPosition(cu, tree);
            System.err.println("Method " + name
                    + " expectedStart:" + expectedStart
                    + " reportedStart:" + reportedStart);
            if (expectedStart != reportedStart) {
                error("Unexpected value for " + name);
                show("expected", expectedStart);
                show("reported", reportedStart);
            }
            return super.visitMethod(tree, task);
        }
    }
}
