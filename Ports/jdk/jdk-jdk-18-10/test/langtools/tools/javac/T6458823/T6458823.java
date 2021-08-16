/*
 * Copyright (c) 2010, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6458823
 * @summary Messager messages on TypeParamterElements to not include position information.
 *
 * @modules java.compiler
 *          jdk.compiler
 * @compile MyProcessor.java T6458823.java
 * @run main T6458823
 */

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class T6458823 {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new RuntimeException("can't get javax.tools.JavaCompiler!");
        }
        DiagnosticCollector<JavaFileObject> diagColl =
            new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            List<String> options = new ArrayList<String>();
            options.add("-processor");
            options.add("MyProcessor");
            options.add("-proc:only");
            List<File> files = new ArrayList<File>();
            files.add(new File(T6458823.class.getResource("TestClass.java").toURI()));
            final CompilationTask task = compiler.getTask(null, fm, diagColl,
                options, null, fm.getJavaFileObjectsFromFiles(files));
            task.call();
            int diagCount = 0;
            for (Diagnostic<? extends JavaFileObject> diag : diagColl.getDiagnostics()) {
                if (diag.getKind() != Diagnostic.Kind.WARNING) {
                    throw new AssertionError("Only warnings expected");
                }
                System.out.println(diag);
                if (diag.getPosition() == Diagnostic.NOPOS) {
                    throw new AssertionError("No position info in message");
                }
                if (diag.getSource() == null) {
                    throw new AssertionError("No source info in message");
                }
                diagCount++;
            }
            if (diagCount != 2) {
                throw new AssertionError("unexpected number of warnings: " +
                    diagCount + ", expected: 2");
            }
        }
    }
}
