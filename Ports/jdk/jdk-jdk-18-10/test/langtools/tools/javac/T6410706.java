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
 * @bug 6410706
 * @summary CONFORMANCE Mandatory warnings in Tree API
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 */

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;

public class T6410706 {
    public static void main(String... args) throws IOException {
        String testSrc = System.getProperty("test.src", ".");
        String testClasses = System.getProperty("test.classes", ".");
        JavacTool tool = JavacTool.create();
        MyDiagListener dl = new MyDiagListener();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(dl, null, null)) {
            fm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(new File(testClasses)));
            Iterable<? extends JavaFileObject> files =
                fm.getJavaFileObjectsFromFiles(Arrays.asList(new File(testSrc, T6410706.class.getName()+".java")));
            JavacTask task = tool.getTask(null, fm, dl, null, null, files);
            task.parse();
            task.analyze();
            task.generate();

            // expect 2 notes:
            // Note: T6410706.java uses or overrides a deprecated API.
            // Note: Recompile with -Xlint:deprecation for details.

            if (dl.notes != 2)
                throw new AssertionError(dl.notes + " notes given");
        }
    }

    private static class MyDiagListener implements DiagnosticListener<JavaFileObject>
    {
        public void report(Diagnostic d) {
            System.err.println(d);
            if (d.getKind() == Diagnostic.Kind.NOTE)
                notes++;
        }

        int notes;
    }

    Foo foo; // should generate deprecation warning
}

@Deprecated class Foo { }
