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
 * @bug 6357331
 * @summary NPE from JavacTask.getElements() after calling CompilationTask.run
 * @modules jdk.compiler
 */
import java.io.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.util.*;

public class T6357331
{
    public static void main(String... args) throws IOException {
        JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        PrintWriter out = new PrintWriter(new StringWriter());
        List<String> opts = Arrays.asList("-d", ".");
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            File thisFile = new File(System.getProperty("test.src"), "T6357331.java");
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(thisFile);
            final JavacTask task = (JavacTask) (tool.getTask(out, fm, null, opts, null, files));

            // set a listener to verify that IllegalStateException is not thrown
            // during the compilation
            task.setTaskListener(new TaskListener() {
                    public void started(TaskEvent e) {
                        task.getElements();
                        task.getTypes();
                    }
                    public void finished(TaskEvent e) { }
                });

            task.call();

            // now the compilation is over, we expect IllegalStateException (not NPE)
            try {
                task.getElements();
                throw new AssertionError("IllegalStateException not thrown");
            }
            catch (IllegalStateException e) {
                // expected
            }

            try {
                task.getTypes();
                throw new AssertionError("IllegalStateException not thrown");
            }
            catch (IllegalStateException e) {
                // expected
            }
        }
    }
}
