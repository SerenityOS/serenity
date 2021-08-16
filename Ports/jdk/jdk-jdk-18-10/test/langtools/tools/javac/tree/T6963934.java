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
 * @bug     6963934
 * @summary JCCompilationUnit.getImports does not report all imports
 * @modules jdk.compiler
 */

import java.io.File;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;; // NOTE: extra semicolon for test

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.util.JavacTask;
; // NOTE: extra semicolon for test

public class T6963934 {
    public static void main(String[] args) throws Exception {
        File testSrc = new File(System.getProperty("test.src"));
        File thisSrc = new File(testSrc, T6963934.class.getSimpleName() + ".java");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, fileManager, null, null, null,
                    fileManager.getJavaFileObjects(thisSrc));
            CompilationUnitTree tree = task.parse().iterator().next();
            int count = 0;
            for (ImportTree importTree : tree.getImports()) {
                System.out.println(importTree);
                count++;
            }
            int expected = 7;
            if (count != expected)
                throw new Exception("unexpected number of imports found: " + count + ", expected: " + expected);
        }
    }
}
