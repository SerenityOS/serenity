/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4910483 8183961
 * @summary Javadoc renders the string ".*\\.pdf" as ".\*\.pdf"
 * @modules jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @run main T4910483
 */

import java.io.File;

import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.util.Context;

import javax.tools.JavaFileObject;

// Test the original issue ("\\") as well as other appearances of '\',
// including a vanilla Unicode escape (U+0021, '!'), and a sequence
// which is not a Unicode escape

/**Test comment abc*\\def\
 *xyz\u0021\\u0021*/
public class T4910483 {
    public static void main(String... args) {
        JavaCompiler compiler = JavaCompiler.instance(new Context());
        compiler.keepComments = true;

        String testSrc = System.getProperty("test.src");
        JavacFileManager fm = new JavacFileManager(new Context(), false, null);
        JavaFileObject f = fm.getJavaFileObject(testSrc + File.separatorChar + "T4910483.java");

        JCTree.JCCompilationUnit cu = compiler.parse(f);
        JCTree classDef = cu.getTypeDecls().head;
        String commentText = cu.docComments.getCommentText(classDef);

        String expected = "Test comment abc*\\\\def\\\nxyz!\\\\u0021"; // 4 '\' escapes to 2 in a string literal
        if (!expected.equals(commentText)) {
            throw new AssertionError("Incorrect comment text: [" + commentText + "], expected [" + expected + "]");
        }
    }
}
