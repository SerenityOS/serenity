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

/* @test
 * @bug 6985202
 * @summary no access to doc comments from Tree API
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 */

import java.io.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.tools.javac.api.JavacTool;

/**
 * class-TestDocComments.
 */
public class TestDocComments {
    /**
     * method-main.
     */
    public static void main(String... args) throws Exception {
        new TestDocComments().run();
    }

    /**
     * method-run.
     */
    void run() throws Exception {
        File testSrc = new File(System.getProperty("test.src"));
        File file = new File(testSrc, "TestDocComments.java");

        JavacTool tool = JavacTool.create();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            Iterable<? extends JavaFileObject> fileObjects = fm.getJavaFileObjects(file);
            JavacTask task = tool.getTask(pw, fm, null, null, null, fileObjects);
            Iterable<? extends CompilationUnitTree> units = task.parse();
            Trees trees = Trees.instance(task);

            CommentScanner s = new CommentScanner();
            int n = s.scan(units, trees);

            if (n != 12)
                error("Unexpected number of doc comments found: " + n);

            if (errors > 0)
                throw new Exception(errors + " errors occurred");
        }
    }

    /**
     * class-CommentScanner.
     */
    class CommentScanner extends TreePathScanner<Integer,Trees> {

        /**
         * method-visitClass.
         */
        @Override
        public Integer visitClass(ClassTree t, Trees trees) {
            return reduce(super.visitClass(t, trees),
                    check(trees, "class-" + t.getSimpleName() + "."));
        }

        /**
         * method-visitMethod.
         */
        @Override
        public Integer visitMethod(MethodTree t, Trees trees) {
            return reduce(super.visitMethod(t, trees),
                    check(trees, "method-" + t.getName() + "."));
        }

        /**
         * method-visitVariable.
         */
        @Override
        public Integer visitVariable(VariableTree t, Trees trees) {
            // for simplicity, only check fields, not parameters or local decls
            int n = (getCurrentPath().getParentPath().getLeaf().getKind() == Tree.Kind.CLASS)
                    ? check(trees, "field-" + t.getName() + ".")
                    : 0;
            return reduce(super.visitVariable(t, trees), n);
        }

        /**
         * method-reduce.
         */
        @Override
        public Integer reduce(Integer i1, Integer i2) {
            return (i1 == null) ? i2 : (i2 == null) ? i1 : Integer.valueOf(i1 + i2);
        }

        /**
         * method-check.
         */
        int check(Trees trees, String expect) {
            TreePath p = getCurrentPath();
            String dc = trees.getDocComment(p);

            if (dc != null && dc.trim().equals(expect))
                return 1;

            Tree.Kind k = p.getLeaf().getKind();
            if (dc == null)
                error("no doc comment for " + k);
            else
                error("unexpected doc comment for " + k + "\nexpect: " + expect + "\nfound:  " + dc);

            return 0;
        }
    }

    /**
     * method-nullCheck.
     */
    int nullCheck(Integer i) {
        return (i == null) ? 0 : i;
    }

    /**
     * method-error.
     */
    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    /**
     * field-errors.
     */
    int errors;
}

