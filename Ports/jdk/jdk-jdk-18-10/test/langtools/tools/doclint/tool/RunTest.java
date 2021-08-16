/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8006263
 * @summary Supplementary test cases needed for doclint
 * @modules jdk.javadoc/jdk.javadoc.internal.doclint
 *          jdk.compiler/com.sun.tools.javac.api
 * @run main/othervm -Djava.security.manager=allow RunTest
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.security.Permission;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.JavacTool;

import jdk.javadoc.internal.doclint.DocLint;
import jdk.javadoc.internal.doclint.DocLint.BadArgs;

public class RunTest {
    static class SimpleSecurityManager extends SecurityManager {
        boolean allowExit = false;

        @Override
        public void checkExit(int status) {
            if (!allowExit)
                throw new SecurityException("System.exit(" + status + ")");
        }
        @Override
        public void checkPermission(Permission perm) { }

    }

    public static void main(String... args) throws Exception {
        // if no security manager already installed, install one to
        // prevent System.exit
        SimpleSecurityManager secmgr = null;
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(secmgr = new SimpleSecurityManager() { });
        }

        try {
            new RunTest().run();
        } finally {
            if (secmgr != null)
                secmgr.allowExit = true;
        }
    }

    void run() throws Exception {
        testMain();
        testRun();
        testInit();
        testArgsNoFiles();

        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    void testMain() {
        System.err.println("test main(String[])");
        testMain(true, "-help");
        testMain(false, "-unknownOption");
    }

    void testMain(boolean expectOK, String... args) {
        try {
            DocLint.main(args);
            if (!expectOK)
                error("expected SecurityException (from System.exit) not thrown");
        } catch (SecurityException e) {
            System.err.println(e);
            if (expectOK)
                error("unexpected SecurityException caught");
        }
    }

    void testRun() throws BadArgs, IOException {
        System.err.println("test run(String[])");
        DocLint dl = new DocLint();
        String[] args = { "-help" };
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream prev = System.out;
        try {
            System.setOut(ps);
            dl.run(args);
        } finally {
            System.setOut(prev);
        }
        ps.close();
        String stdout = baos.toString();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        dl.run(pw, args);
        pw.close();
        String direct = sw.toString();

        if (!stdout.equals(direct)) {
            error("unexpected output");
            System.err.println("EXPECT>>" + direct + "<<");
            System.err.println("FOUND>>" + stdout + "<<");
        }
    }

    void testInit() {
        System.err.println("test init");
        DocLint dl = new DocLint();
        String name = dl.getName();
        if (!Objects.equals(name, "doclint"))
            error("unexpected result for DocLint.getName()");

        List<? extends JavaFileObject> files =
                Arrays.asList(createFile("Test.java", "/** &0; */ class Test{ }"));
        String[] goodArgs = { "-Xmsgs" };
        testInit(true, goodArgs, files);

        String[] badArgs = { "-unknown" };
        testInit(false, badArgs, files);
    }

    void testInit(boolean expectOK, String[] args, List<? extends JavaFileObject> files) {
        JavacTool javac = JavacTool.create();
        JavacTask task = javac.getTask(null, null, null, null, null, files);
        try {
            DocLint dl = new DocLint();
            dl.init(task, args, true);
            if (!expectOK)
                error("expected IllegalArgumentException not thrown");
            task.call();
        } catch (IllegalArgumentException e) {
            System.err.println(e);
            if (expectOK)
                error("unexpected IllegalArgumentException caught");
        }
    }

    void testArgsNoFiles() throws BadArgs, IOException {
        System.err.println("test args, no files");
        DocLint dl = new DocLint();

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        dl.run(pw, "-Xmsgs");
        pw.close();
        String out = sw.toString();

        String expect = "No files given";
        if (!Objects.equals(out.trim(), expect)) {
            error("unexpected output");
            System.err.println("EXPECT>>" + expect + "<<");
            System.err.println("FOUND>>" + out + "<<");
        }

    }

    JavaFileObject createFile(String name, final String body) {
        return new SimpleJavaFileObject(URI.create(name), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
                return body;
            }
        };
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int errors;
}
