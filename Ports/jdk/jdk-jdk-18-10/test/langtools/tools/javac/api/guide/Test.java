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
 * @bug     6427274 6347778 6469079
 * @summary Various bugs fixed while writing Compiler API Guide
 * @author  Peter von der Ah\u0081
 * @library ../lib
 * @modules java.compiler
 *          jdk.compiler
 * @build ToolTester
 * @compile Test.java
 * @run main Test
 */

import javax.tools.*;
import java.io.File;
import java.util.Collections;

public class Test extends ToolTester {

    public boolean success = false;

    class DiagnosticTester implements DiagnosticListener<JavaFileObject> {
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.NOTE) {
                try {
                    // 6427274: FileObject.openReader throws exception
                    // 6347778: getSource() returns null for notes
                    diagnostic.getSource().openReader(true).getClass();
                } catch (Exception ex) {
                    throw new AssertionError(ex);
                }
                success = true;
            }
        }
    }

    void test(String... args) throws Exception {
        final Iterable<? extends JavaFileObject> compilationUnits =
            fm.getJavaFileObjects(new File(test_src, "TestMe.java"));
        task = tool.getTask(null, fm, new DiagnosticTester(), null, null, compilationUnits);
        if (!task.call())
            throw new AssertionError("Compilation failed");
        if (!success)
            throw new AssertionError("Did not see a NOTE");
        // 6427274: openReader throws exception
        fm.getFileForInput(StandardLocation.PLATFORM_CLASS_PATH,
                           "java.lang",
                           "Object.class").openReader(true).getClass();
        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<JavaFileObject>();
        task = tool.getTask(null, fm, diags, Collections.singleton("-Xlint:all"),
                            null, compilationUnits);
        if (!task.call())
            throw new AssertionError("Compilation failed");
        String msg = diags.getDiagnostics().get(0).getMessage(null);
        long lineno = diags.getDiagnostics().get(0).getLineNumber();
        if (msg.contains(":"+lineno+":"))
            // 6469079: Diagnostic.getMessage(Locale) includes line numbers
            throw new AssertionError(msg);
    }

    public static void main(String... args) throws Exception {
        try (Test t = new Test()) {
            t.test(args);
        }
    }

}
