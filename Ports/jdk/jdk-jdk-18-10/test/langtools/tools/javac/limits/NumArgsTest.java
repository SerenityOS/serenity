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

import com.sun.tools.javac.api.*;
import com.sun.tools.javac.file.*;
import java.io.*;
import java.util.*;
import javax.tools.*;

// More general parameter limit testing framework, and designed so
// that it could be expanded into a general limits-testing framework
// in the future.
public class NumArgsTest {

    private static final NumArgsTest.NestingDef[] NO_NESTING = {};

    // threshold is named as such because "threshold" args is expected
    // to pass, and "threshold" + 1 args is expected to fail.
    private final int threshold;
    private final boolean isStaticMethod;
    private final String result;
    private final String testName;
    private final String methodName;
    private final NestingDef[] nesting;
    private final File testdir;
    private final JavacTool tool = JavacTool.create();
    private final JavacFileManager fm =
        tool.getStandardFileManager(null, null, null);
    private int errors = 0;

    public NumArgsTest(final int threshold,
                       final boolean isStaticMethod,
                       final String result,
                       final String methodName,
                       final String testName,
                       final NestingDef[] nesting) {
        this.threshold = threshold;
        this.isStaticMethod = isStaticMethod;
        this.result = result;
        this.methodName = methodName;
        this.testName = testName;
        this.nesting = nesting;
        testdir = new File(testName);
        testdir.mkdir();
    }

    public NumArgsTest(final int threshold,
                       final boolean isStaticMethod,
                       final String result,
                       final String methodName,
                       final String testName) {
        this(threshold, isStaticMethod, result, methodName,
             testName, NO_NESTING);
    }

    public NumArgsTest(final int threshold,
                       final String result,
                       final String methodName,
                       final String testName,
                       final NestingDef[] nesting) {
        this(threshold, false, result, methodName, testName, nesting);
    }

    public NumArgsTest(final int threshold,
                       final String result,
                       final String methodName,
                       final String testName) {
        this(threshold, false, result, methodName, testName, NO_NESTING);
    }

    public NumArgsTest(final int threshold,
                       final String testName,
                       final NestingDef[] nesting) {
        this(threshold, null, null, testName, nesting);
    }

    public NumArgsTest(final int threshold,
                       final String testName) {
        this(threshold, null, null, testName, NO_NESTING);
    }

    public NumArgsTest(final int threshold,
                       final String testName,
                       final String constructorName,
                       final NestingDef[] nesting) {
        this(threshold, null, constructorName, testName, nesting);
    }

    protected void writeArgs(final int num, final PrintWriter stream)
        throws IOException {
        stream.print("int x1");
        for(int i = 1; i < num; i++)
            stream.print(", int x" + (i + 1));
    }

    protected void writeMethod(final int num,
                               final String name,
                               final PrintWriter stream)
        throws IOException {
        stream.write("public ");
        if (isStaticMethod) stream.write("static ");
        if (result == null)
            stream.write("");
        else {
            stream.write(result);
            stream.write(" ");
        }
        stream.write(name);
        stream.write("(");
        writeArgs(num, stream);
        stream.write(") {}\n");
    }

    protected void writeJavaFile(final int num,
                                 final boolean pass,
                                 final PrintWriter stream)
        throws IOException {
        final String fullName = testName + (pass ? "Pass" : "Fail");
        stream.write("public class ");
        stream.write(fullName);
        stream.write(" {\n");
        for(int i = 0; i < nesting.length; i++)
            nesting[i].writeBefore(stream);
        if (null == methodName)
            writeMethod(num, fullName, stream);
        else
            writeMethod(num, methodName, stream);
        for(int i = nesting.length - 1; i >= 0; i--)
            nesting[i].writeAfter(stream);
        stream.write("}\n");
    }

    public void runTest() throws Exception {
        // Run the pass test
        final String passTestName = testName + "Pass.java";
        final StringWriter passBody = new StringWriter();
        final PrintWriter passStream = new PrintWriter(passBody);
        final File passFile = new File(testdir, passTestName);
        final FileWriter passWriter = new FileWriter(passFile);

        writeJavaFile(threshold, true, passStream);
        passStream.close();
        passWriter.write(passBody.toString());
        passWriter.close();

        final StringWriter passSW = new StringWriter();
        final String[] passArgs = { passFile.toString() };
        final Iterable<? extends JavaFileObject> passFiles =
            fm.getJavaFileObjectsFromFiles(Arrays.asList(passFile));
        final JavaCompiler.CompilationTask passTask =
            tool.getTask(passSW, fm, null, null, null, passFiles);

        if (!passTask.call()) {
            errors++;
            System.err.println("Compilation unexpectedly failed. Body:\n" +
                               passBody);
            System.err.println("Output:\n" + passSW.toString());
        }

        // Run the fail test
        final String failTestName = testName + "Fail.java";
        final StringWriter failBody = new StringWriter();
        final PrintWriter failStream = new PrintWriter(failBody);
        final File failFile = new File(testdir, failTestName);
        final FileWriter failWriter = new FileWriter(failFile);

        writeJavaFile(threshold + 1, false, failStream);
        failStream.close();
        failWriter.write(failBody.toString());
        failWriter.close();

        final StringWriter failSW = new StringWriter();
        final TestDiagnosticHandler failDiag =
            new TestDiagnosticHandler("compiler.err.limit.parameters");
        final Iterable<? extends JavaFileObject> failFiles =
            fm.getJavaFileObjectsFromFiles(Arrays.asList(failFile));
        final JavaCompiler.CompilationTask failTask =
            tool.getTask(failSW,
                         tool.getStandardFileManager(null, null, null),
                         failDiag,
                         null,
                         null,
                         failFiles);

        if (failTask.call()) {
            errors++;
            System.err.println("Compilation unexpectedly succeeded.");
            System.err.println("Input:\n" + failBody);
        }

        if (!failDiag.sawError) {
            errors++;
            System.err.println("Did not see expected compile error.");
        }

        if (errors != 0)
            throw new RuntimeException("Test failed with " +
                                       errors + " errors");
    }

    public static NestingDef classNesting(final String name) {
        return new NestedClassBuilder(name, false);
    }

    public static NestingDef classNesting(final String name,
                                          final boolean isStatic) {
        return new NestedClassBuilder(name, isStatic);
    }

    protected interface NestingDef {
        public abstract void writeBefore(final PrintWriter stream);
        public abstract void writeAfter(final PrintWriter stream);
    }

    private static class NestedClassBuilder implements NestingDef {
        private final String name;
        private final boolean isStatic;
        public NestedClassBuilder(final String name, final boolean isStatic) {
            this.name = name;
            this.isStatic = isStatic;
        }
        public void writeBefore(final PrintWriter stream) {
            stream.write("public ");
            if (isStatic) stream.write("static");
            stream.write(" class ");
            stream.write(name);
            stream.write(" {\n");
        }
        public void writeAfter(final PrintWriter stream) {
            stream.write("}\n");
        }
    }

    public class TestDiagnosticHandler<T> implements DiagnosticListener<T> {
        public boolean sawError;
        public final String target;

        public TestDiagnosticHandler(final String target) {
            this.target = target;
        }

        public void report(final Diagnostic<? extends T> diag) {
            if (diag.getCode().equals(target))
                sawError = true;
        }
    }

}
