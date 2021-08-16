/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7142672
 * @summary Problems with the value passed to the 'classes' param of JavaCompiler.CompilationTask.getTask(...)
 * @author holmlund
 * @modules java.compiler
 *          jdk.compiler
 * @compile AnnoProcessor.java Bug.java Test3.java
 * @run main Bug Test2.java
 * @run main Bug Test2.foo
 * @run main Bug Test3.java
 */
import java.io.*;
import java.util.*;
import javax.tools.*;

// Each run should output the 'could not find class file' message, and not throw an AssertError.
public class Bug {
    public static void main(String... arg) throws Throwable {
        String name = arg[0];
        final String expectedMsg = "error: Could not find class file for '" + name + "'.";
        JavaCompiler javac = ToolProvider.getSystemJavaCompiler();
        JavaCompiler.CompilationTask task2;
        StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);


        DiagnosticListener<? super javax.tools.JavaFileObject> dl =
            new DiagnosticListener<javax.tools.JavaFileObject>() {
            public void report(Diagnostic message) {
                pw.print("Diagnostic:\n"+ message.toString()+"\n");
                if (!message.toString().equals(expectedMsg)){
                    System.err.println("Diagnostic:\n"+ message.toString()+"\n");
                    System.err.println("--Failed: Unexpected diagnostic");
                    System.exit(1);
                }
            }
        };

        try (StandardJavaFileManager sjfm = javac.getStandardFileManager(dl,null,null)) {

            List<String> opts = new ArrayList<String>();
            opts.add("-proc:only");
            opts.add("-processor");
            opts.add("AnnoProcessor");

            boolean xxx;

            System.err.println("\n-- " + name);
            task2 = javac.getTask(pw, sjfm, dl, opts, Arrays.asList(name), null);
            xxx = task2.call();

            String out = sw.toString();
            System.err.println(out);
            if (out.contains("Assert")) {
                System.err.println("--Failed: Assertion failure");
                System.exit(1);
            }
            if (!out.contains(expectedMsg)) {
                System.err.println("--Failed: Expected diagnostic not found");
                System.exit(1);
            }
        }
    }
}
