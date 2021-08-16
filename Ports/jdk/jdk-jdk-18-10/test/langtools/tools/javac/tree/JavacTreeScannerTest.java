/*
 * Copyright (c) 2010, 2019, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Utility and test program to check javac's internal TreeScanner class.
 * The program can be run standalone, or as a jtreg test.  For info on
 * command line args, run program with no args.
 *
 * <p>
 * jtreg: Note that by using the -r switch in the test description below, this test
 * will process all java files in the langtools/test directory, thus implicitly
 * covering any new language features that may be tested in this test suite.
 */

/*
 * @test
 * @bug 6923080
 * @summary TreeScanner.visitNewClass should scan tree.typeargs
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @build AbstractTreeScannerTest JavacTreeScannerTest
 * @run main JavacTreeScannerTest -q -r .
 */

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import javax.tools.*;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Pair;

public class JavacTreeScannerTest extends AbstractTreeScannerTest {
    /**
     * Main entry point.
     * If test.src is set, program runs in jtreg mode, and will throw an Error
     * if any errors arise, otherwise System.exit will be used. In jtreg mode,
     * the default base directory for file args is the value of ${test.src}.
     * In jtreg mode, the -r option can be given to change the default base
     * directory to the root test directory.
     */
    public static void main(String... args) {
        String testSrc = System.getProperty("test.src");
        File baseDir = (testSrc == null) ? null : new File(testSrc);
        boolean ok = new JavacTreeScannerTest().run(baseDir, args);
        if (!ok) {
            if (testSrc != null)  // jtreg mode
                throw new Error("failed");
            else
                System.exit(1);
        }
    }

    int test(Pair<JavacTask, JCCompilationUnit> taskAndTree) {
        return new ScanTester().test(taskAndTree.snd);
    }

    /**
     * Main class for testing operation of tree scanner.
     * The set of nodes found by the scanner are compared
     * against the set of nodes found by reflection.
     */
    private class ScanTester extends TreeScanner {
        /** Main entry method for the class. */
        int test(JCCompilationUnit tree) {
            sourcefile = tree.sourcefile;
            found = new HashSet<JCTree>();
            scan(tree);
            expect = new HashSet<JCTree>();
            reflectiveScan(tree);

            if (found.equals(expect)) {
                //System.err.println(sourcefile.getName() + ": trees compared OK");
                return found.size();
            }

            error(sourcefile, "differences found");

            if (found.size() != expect.size()) {
                error("Size mismatch; found: " + found.size() + ", expected: " + expect.size());
                Set<JCTree> notFound = new HashSet<>(expect);
                notFound.removeAll(found);
                if (!notFound.isEmpty()) {
                    System.err.println("found by reflective access to the AST, but not found in the scanner API:");
                    notFound.forEach(t -> System.err.println(trim(t, 64)));
                }
                Set<JCTree> notExpected = new HashSet<>(found);
                notExpected.removeAll(expect);
                if (!notExpected.isEmpty()) {
                    System.err.println("found in the scanner API, but not found by reflective access to the AST:");
                    notExpected.forEach(t -> System.err.println(trim(t, 64)));
                }
            }

            Set<JCTree> missing = new HashSet<JCTree>();
            missing.addAll(expect);
            missing.removeAll(found);
            for (JCTree t: missing)
                error(sourcefile, t, "missing");

            Set<JCTree> excess = new HashSet<JCTree>();
            excess.addAll(found);
            excess.removeAll(expect);
            for (JCTree t: excess)
                error(sourcefile, t, "unexpected");

            return 0;
        }

        /** Record all tree nodes found by scanner. */
        @Override
        public void scan(JCTree tree) {
            if (tree == null)
                return;
            //System.err.println("FOUND: " + tree.getTag() + " " + trim(tree, 64));
            found.add(tree);
            super.scan(tree);
        }

        /** record all tree nodes found by reflection. */
        public void reflectiveScan(Object o) {
            if (o == null)
                return;
            if (o instanceof JCTree) {
                JCTree tree = (JCTree) o;
                //System.err.println("EXPECT: " + tree.getTag() + " " + trim(tree, 64));
                expect.add(tree);
                for (Field f: getFields(tree)) {
                    try {
                        //System.err.println("FIELD: " + f.getName());
                        reflectiveScan(f.get(tree));
                    } catch (IllegalAccessException e) {
                        error(e.toString());
                    }
                }
            } else if (o instanceof List) {
                List<?> list = (List<?>) o;
                for (Object item: list)
                    reflectiveScan(item);
            } else if (o instanceof Pair) {
                return;
            } else
                error("unexpected item: " + o);
        }

        JavaFileObject sourcefile;
        Set<JCTree> found;
        Set<JCTree> expect;
    }
}
