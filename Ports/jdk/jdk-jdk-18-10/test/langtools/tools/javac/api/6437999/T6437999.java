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
 * @bug     6437999
 * @summary Unit test for encoding argument to standard file manager
 * @author  Peter von der Ah\u00e9
 * @library ../lib
 * @modules java.compiler
 *          jdk.compiler
 * @build ToolTester
 * @compile T6437999.java
 * @run main T6437999
 */

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.tools.*;
import static java.nio.file.StandardOpenOption.*;

public class T6437999 extends ToolTester {
    final File testFile = new File("Utf8.java");
    T6437999() throws IOException {
        createTestFile();
    }
    final void createTestFile() throws IOException {
        List<String> scratch = new ArrayList<>();
        scratch.add("// @author Peter von der Ah" + (char) 0xe9);
        scratch.add("class Utf8{}");
        Files.write(testFile.toPath(), scratch, Charset.forName("UTF-8"),
                CREATE, TRUNCATE_EXISTING);
    }

    static class MyDiagnosticListener implements DiagnosticListener<JavaFileObject> {
        boolean error = false;
        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            error |= diagnostic.getKind() == Diagnostic.Kind.ERROR;
            System.out.println(diagnostic);
        }
    }

    void test(String... args) {
        Iterable<String> sourceLevel = Collections.singleton("6");
        MyDiagnosticListener dl = new MyDiagnosticListener();
        StandardJavaFileManager fm;
        Iterable<? extends JavaFileObject> files;

        dl.error = false;
        fm = getFileManager(tool, dl, Charset.forName("ASCII"));
        fm.handleOption("-source", sourceLevel.iterator());
        files = fm.getJavaFileObjects(testFile);
        tool.getTask(null, fm, null, null, null, files).call();
        if (!dl.error)
            throw new AssertionError("No error in ASCII mode");

        dl.error = false;
        fm = getFileManager(tool, dl, Charset.forName("UTF-8"));
        fm.handleOption("-source", sourceLevel.iterator());
        files = fm.getJavaFileObjects(testFile);
        task = tool.getTask(null, fm, null, null, null, files);
        if (dl.error)
            throw new AssertionError("Error in UTF-8 mode");
    }
    public static void main(String... args) throws IOException {
        try (T6437999 t = new T6437999()) {
            t.test(args);
        }
    }
}
