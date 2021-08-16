/*
 * Copyright (c) 2009, 2015, Oracle and/or its affiliates. All rights reserved.
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

import java.io.*;
import java.net.*;
import javax.tools.*;
import java.util.*;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.Pretty;

/**
 * @test
 * @bug 6902720
 * @summary javac pretty printer does not handle enums correctly
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 */

public class Test {

    public static void main(String[] args) throws Exception {
        Test t = new Test();
        t.run("E1.java", "E2.java");
    }

    void run(String... args) throws Exception {
        File testSrcDir = new File(System.getProperty("test.src"));
        for (String arg: args) {
            test(new File(testSrcDir, arg));
        }
    }

    void test(File test) throws Exception {
        JavacTool tool1 = JavacTool.create();
        try (StandardJavaFileManager fm = tool1.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(test);

            // parse test file into a tree, and write it out to a stringbuffer using Pretty
            JavacTask t1 = tool1.getTask(null, fm, null, null, null, files);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            Iterable<? extends CompilationUnitTree> trees = t1.parse();
            for (CompilationUnitTree tree: trees) {
                new Pretty(pw, true).printExpr((JCTree) tree);
            }
            pw.close();

            final String out = sw.toString();
            System.err.println("generated code:\n" + out + "\n");

            // verify the generated code is valid Java by compiling it
            JavacTool tool2 = JavacTool.create();
            JavaFileObject fo = new SimpleJavaFileObject(URI.create("output"), JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                    return out;
                }
            };
            JavacTask t2 = tool2.getTask(null, fm, null, null, null, Collections.singleton(fo));
            boolean ok = t2.call();
            if (!ok)
                throw new Exception("compilation of generated code failed");

            File expectedClass = new File(test.getName().replace(".java", ".class"));
            if (!expectedClass.exists())
                throw new Exception(expectedClass + " not found");
        }
    }
}

