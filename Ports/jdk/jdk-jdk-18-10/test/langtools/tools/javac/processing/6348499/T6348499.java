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
 * @summary javac crashes at com.sun.tools.javac.jvm.ClassReader$BadClassFile
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 * @build JavacTestingAbstractProcessor A
 * @run main T6348499
 */

// Note that 6441871 is an interim partial fix for 6348499 that just removes the javac
// crash message and stacktrace

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.*;
import com.sun.source.util.*;
import com.sun.tools.javac.api.*;


public class T6348499 {
    public static void main(String... args) throws IOException {
        String testSrc = System.getProperty("test.src", ".");
        String testClasses = System.getProperty("test.classes");
        String testClassPath = System.getProperty("test.class.path", testClasses);
        String A_java = new File(testSrc, "A.java").getPath();
        JavacTool tool = JavacTool.create();
        MyDiagListener dl = new MyDiagListener();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(dl, null, null)) {
            Iterable<? extends JavaFileObject> files =
                fm.getJavaFileObjectsFromFiles(Arrays.asList(new File(testSrc, "A.java")));
            Iterable<String> opts = Arrays.asList("-proc:only",
                                                  "-processor", "A",
                                                  "-processorpath", testClassPath);
            StringWriter out = new StringWriter();
            JavacTask task = tool.getTask(out, fm, dl, opts, null, files);
            task.call();
            String s = out.toString();
            System.err.print(s);
            // Expect the following 1 multi-line diagnostic, and no output to log
            //     error: cannot access A_0
            //     bad class file: A_0.class
            //     illegal start of class file
            //     Please remove or make sure it appears in the correct subdirectory of the classpath.
            System.err.println(dl.count + " diagnostics; " + s.length() + " characters");
            if (dl.count != 1 || s.length() != 0)
                throw new AssertionError("unexpected output from compiler");
        }
    }

    static class MyDiagListener implements DiagnosticListener<JavaFileObject> {
        public void report(Diagnostic d) {
            System.err.println(d);
            count++;
        }

        public int count;
    }

    private static String self = T6348499.class.getName();
}
