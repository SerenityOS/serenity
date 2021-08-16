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
 * @bug 6441871
 * @summary spurious compiler error elicited by packageElement.getEnclosedElements()
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 * @build JavacTestingAbstractProcessor b6341534
 * @run main T6430209
 */

// Note that 6441871 is an interim partial fix for 6430209 that just removes the javac
// crash message and stacktrace

import java.io.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.util.*;
import com.sun.tools.javac.api.*;


public class T6430209 {
    public static void main(String... args) throws IOException {
        // set up dir1/test0.java
        File dir1 = new File("dir1");
        dir1.mkdir();
        BufferedWriter fout = new BufferedWriter(new FileWriter(new File(dir1, "test0.java")));
        fout.write("public class test0 { }");
        fout.close();

        // run annotation processor b6341534 so we can check diagnostics
        // -proc:only -processor b6341534 -cp . ./src/*.java
        String testSrc = System.getProperty("test.src", ".");
        String testClassPath = System.getProperty("test.class.path");
        JavacTool tool = JavacTool.create();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_PATH, Arrays.asList(new File(".")));
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjectsFromFiles(Arrays.asList(
                new File(testSrc, "test0.java"), new File(testSrc, "test1.java")));
            Iterable<String> opts = Arrays.asList("-XDrawDiagnostics",
                                                  "-proc:only",
                                                  "-processor", "b6341534",
                                                  "-processorpath", testClassPath);
            StringWriter out = new StringWriter();
            JavacTask task = tool.getTask(out, fm, null, opts, null, files);
            task.call();
            String s = out.toString();
            System.err.print(s);
            s = s.replace(System.getProperty("line.separator"), "\n");
            String expected = "test0.java:1:8: compiler.err.duplicate.class: test0\n" +
                              "1 error\n";
            if (!expected.equals(s))
                throw new AssertionError("unexpected text in output");
        }
    }

}
