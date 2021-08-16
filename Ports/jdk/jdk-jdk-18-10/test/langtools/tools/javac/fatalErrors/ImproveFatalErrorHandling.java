/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8069116 8216400
 * @summary Improve handling of IOExceptions and FatalError in JavaCompiler.close()
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox
 * @run main ImproveFatalErrorHandling
 */

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.main.JavaCompiler;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.FatalError;
import static com.sun.tools.javac.main.JavaCompiler.compilerKey;

import toolbox.TestRunner;
import toolbox.ToolBox;
import toolbox.Task;
import static toolbox.ToolBox.lineSeparator;

public class ImproveFatalErrorHandling extends TestRunner {
    ToolBox tb;

    String testCode = "public class Test { }";

    public ImproveFatalErrorHandling() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String[] args) throws Exception {
        ImproveFatalErrorHandling handling = new ImproveFatalErrorHandling();
        handling.runTests();
    }

    @Test
    public void testIOExceptionInMethodClose() throws Exception {
        List<? extends JavaFileObject> files = Arrays.asList(new MemFile("Test.java", testCode));
        JavacTaskImpl task = (JavacTaskImpl) ToolProvider
                .getSystemJavaCompiler()
                .getTask(null, null, null, null, null, files);
        Context context = task.getContext();
        task.call();
        JavaCompiler compiler = context.get(compilerKey);
        compiler.closeables = com.sun.tools.javac.util.List.of(
                new CloseException1(), new CloseException2(),
                new CloseSuccess(), new CloseException3());

        try {
            compiler.close();
        } catch (FatalError fatalError) {
            // Do the check.
            List<String> expectedMsg = Arrays.asList(
                    "Fatal Error: Cannot close compiler resources",
                    "exception 1",
                    "exception 2",
                    "exception 3");
            ArrayList<String> actualMsg = new ArrayList();
            actualMsg.add(fatalError.getMessage());
            actualMsg.add(fatalError.getCause().getMessage());
            for (Throwable t : fatalError.getSuppressed()) {
                actualMsg.add(t.getMessage());
            }
            tb.checkEqual(expectedMsg, actualMsg);
        }
    }

    class CloseException1 implements Closeable {
        public void close() throws IOException {
            throw new IOException("exception 1");
        }
    }

    class CloseException2 implements Closeable {
        public void close() throws IOException {
            throw new IOException("exception 2");
        }
    }

    class CloseException3 implements Closeable {
        public void close() throws IOException {
            throw new IOException("exception 3");
        }
    }

    class CloseSuccess implements Closeable {
        public void close() throws IOException { }
    }

    class MemFile extends SimpleJavaFileObject {
        public final String text;

        MemFile(String name, String text) {
            super(URI.create(name), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }

        @Override
        public String getName() {
            return uri.toString();
        }

        @Override
        public String getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }
}
