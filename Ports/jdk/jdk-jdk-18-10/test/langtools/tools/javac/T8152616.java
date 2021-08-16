/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8152616
 * @summary Unit test for corner case of PrettyPrinting when SourceOutput is false
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.tree
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import javax.tools.StandardJavaFileManager;
import javax.tools.DiagnosticListener;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.Pretty;
import java.io.IOException;

public class T8152616 {

    public String PrettyPrint(JCTree tree){
        StringWriter s = new StringWriter();
        try {
            new Pretty(s, false).printExpr(tree);
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
        return s.toString();
    }

    public static void main(String[] args) throws Exception {
        T8152616 obj = new T8152616();
        JavacTool javac = JavacTool.create();
        StandardJavaFileManager jfm = javac.getStandardFileManager(null,null,null);
        File file = File.createTempFile("test", ".java");
        OutputStream outputStream = new FileOutputStream(file);
        outputStream.write("enum Foo {AA(10), BB, CC { void m() {} }; void m() {};}".getBytes());
        JavacTask task = javac.getTask(null, jfm, null, null, null,
                   jfm.getJavaFileObjects(file.getAbsolutePath()));
        Iterable<? extends CompilationUnitTree> trees = task.parse();
        CompilationUnitTree thisTree = trees.iterator().next();
        file.delete();
        outputStream = new FileOutputStream(file);
        outputStream.write((obj.PrettyPrint((JCTree)thisTree)).getBytes());
        task = javac.getTask(null, jfm, null, null, null,
                   jfm.getJavaFileObjects(file.getAbsolutePath()));
        if(task.parse().toString().contains("ERROR")){
             throw new AssertionError("parsing temp file failed with errors");
        }else{
             System.out.println("parsing successfull");
        }
        file.delete();
    }
}
