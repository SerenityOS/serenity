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
 * @bug 8006033
 * @summary bug in Pretty.toSimpleString
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 */

import java.io.File;

import javax.tools.StandardJavaFileManager;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.Pretty;

public class PrettySimpleStringTest {
    public static void main(String... args) throws Exception {
        new PrettySimpleStringTest().run();
    }

    void run() throws Exception {
        File testSrc = new File(System.getProperty("test.src"));
        File thisFile = new File(testSrc, getClass().getName() + ".java");
        JavacTool tool = JavacTool.create();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            JavacTask task = tool.getTask(null, fm, null, null, null,
                    fm.getJavaFileObjects(thisFile));
            Iterable<? extends CompilationUnitTree> trees = task.parse();
            CompilationUnitTree thisTree = trees.iterator().next();

            {   // test default
                String thisSrc = Pretty.toSimpleString((JCTree) thisTree);
                System.err.println(thisSrc);
                String expect = "import jav[...]} } }";
                if (!thisSrc.equals(expect)) {
                    throw new Exception("unexpected result");
                }
            }

            {   // test explicit length
                String thisSrc = Pretty.toSimpleString((JCTree) thisTree, 32);
                System.err.println(thisSrc);
                String expect = "import java.io.Fil[...]} } } } }";
                if (!thisSrc.equals(expect)) {
                    throw new Exception("unexpected result");
                }
            }
        }
    }
}
