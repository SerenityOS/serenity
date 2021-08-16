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
 * @bug 6431879
 * @summary TreePathSCanner(CompilationUnitTree tree, P p) overloading forces use of most specific type
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 */

import java.io.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;
import com.sun.tools.javac.api.*;

public class T6431879 {
    public static void main(String... args) throws IOException {
        String testSrc = System.getProperty("test.src", ".");
        String testClasses = System.getProperty("test.classes", ".");
        JavacTool tool = JavacTool.create();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> files =
                fm.getJavaFileObjectsFromFiles(Arrays.asList(new File(testSrc, T6431879.class.getName()+".java")));
            JavacTask task = tool.getTask(null, fm, null, null, null, files);
            Iterable<? extends CompilationUnitTree> trees = task.parse();
            TreeScanner<Void,Trees> dependencyScanner = new DependencyScanner();
            Trees treeUtil = Trees.instance(task);
            for (CompilationUnitTree unit : trees) {
                //System.err.println("scan " + unit);
                dependencyScanner.scan(unit, treeUtil);
            }
        }
    }

    private static class DependencyScanner<R,P> extends TreePathScanner<R,P> {
        public R visitIdentifier(IdentifierTree tree, P p) {
            //System.err.println(tree);
            return null;
        }
    }
}
