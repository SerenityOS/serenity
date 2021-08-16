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
 * @bug 6361619 6392118
 * @summary AssertionError from ClassReader; mismatch between JavacTaskImpl.context and JSR 269
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 */

import java.io.*;
import java.net.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.tools.*;
import com.sun.source.util.*;
import com.sun.tools.javac.api.*;
import com.sun.source.util.Trees;

@SupportedAnnotationTypes("*")
public class T6361619 extends AbstractProcessor {
    public static void main(String... args) throws Throwable {
        String testSrcDir = System.getProperty("test.src");
        String testClassDir = System.getProperty("test.classes");
        String self = T6361619.class.getName();

        JavacTool tool = JavacTool.create();

        final PrintWriter out = new PrintWriter(System.err, true);

        Iterable<String> flags = Arrays.asList("-processorpath", testClassDir,
                                               "-processor", self,
                                               "-d", ".");
        DiagnosticListener<JavaFileObject> dl = new DiagnosticListener<JavaFileObject>() {
            public void report(Diagnostic<? extends JavaFileObject> m) {
                out.println(m);
            }
        };

        try (StandardJavaFileManager fm = tool.getStandardFileManager(dl, null, null)) {
            Iterable<? extends JavaFileObject> f =
                fm.getJavaFileObjectsFromFiles(Arrays.asList(new File(testSrcDir,
                                                                      self + ".java")));

            JavacTask task = tool.getTask(out, fm, dl, flags, null, f);
            MyTaskListener tl = new MyTaskListener(task);
            task.setTaskListener(tl);

            // should complete, without exceptions
            task.call();
        }
    }

    public boolean process(Set<? extends TypeElement> elems, RoundEnvironment renv) {
        return true;
    }


    static class MyTaskListener implements TaskListener {
        public MyTaskListener(JavacTask task) {
            this.task = task;
        }

        public void started(TaskEvent e) {
            System.err.println("Started: " + e);
            Trees t = Trees.instance(task);
        }
        public void finished(TaskEvent e) {
            System.err.println("Finished: " + e);
            Trees t = Trees.instance(task);
        }

        JavacTask task;
    }
}
