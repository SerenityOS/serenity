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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.lang.model.element.Element;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;

/**
 * @test
 * @bug 6956638
 * @summary JavacTask.generate does not generate all required files
 * @modules jdk.compiler
 */
public class T6956638 {
    public static void main(String[] args) throws Exception {
        new T6956638().run();
    }

    void run() throws Exception {
        File srcDir = new File("src");

        File[] files = {
            writeFile(new File(srcDir, "T1.java"),
                "public class T1 extends T2 {}\n"),
            writeFile(new File(srcDir, "T2.java"),
                "public class T2 extends T3 {}\n"),
            writeFile(new File(srcDir, "T3.java"),
                "public class T3 { public static final int C = 1; }\n"),
            writeFile(new File(srcDir, "Test.java"),
                "public class Test { public static final int D = T1.C; }\n")
        };

        for (File f1: files) {
            for (File f2: files) {
                if (f2 == f1) continue;
                for (File f3: files) {
                    if (f3 == f2 || f3 == f1) continue;
                    for (File f4: files) {
                        if (f4 == f3 || f4 == f2 || f4 == f1) continue;
                        try {
                            test(f1, f2, f3, f4);
                        } catch (Exception e) {
                            error(e);
                        }
                    }
                }
            }
        }

        if (errors > 0)
            throw new Exception(errors + " tests failed");
    }

    void test(File... sourceFiles) throws Exception {
        System.err.println("Test " + (++count) + ": " + Arrays.asList(sourceFiles));

        File classesDir = new File("classes" + count);
        classesDir.mkdirs();

        StringWriter compilerOutputStream = new StringWriter();

        List<String> compileOptions = Arrays.asList("-d", classesDir.getPath());
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<JavaFileObject>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnosticCollector, null, null)) {
            Iterable<? extends JavaFileObject> sourceFileObjects = fileManager.getJavaFileObjects(sourceFiles);
            System.err.println("1- javac given java source JavaFileObjects " + sourceFileObjects);
            JavaCompiler.CompilationTask task = compiler.getTask(compilerOutputStream, fileManager, null, compileOptions, null, sourceFileObjects);
            JavacTask javacTask = (JavacTask) task;

            Iterable<? extends CompilationUnitTree> parsedTrees = javacTask.parse();
            Iterable<? extends Element> analyzedTrees = javacTask.analyze();
            Iterable<? extends JavaFileObject> generatedFiles = javacTask.generate();

            System.err.println("2- parsed:" + size(parsedTrees) + " analysed:" + size(analyzedTrees) + " generated:" + size(generatedFiles));

            System.err.print("3-");
            for (JavaFileObject f : generatedFiles)
                System.err.print(" " + f);
            System.err.println("");

            System.err.print("5-");
            for (File f : classesDir.listFiles())
                System.err.print(" " + f);
            System.err.println("");

            System.err.println("----");
            System.err.println(compilerOutputStream.toString());

            if (size(generatedFiles) != size(parsedTrees)) {
                throw new Exception("wrong number of files generated: " + size(generatedFiles)
                        + " expected: " + size(parsedTrees));
            }
        }
    }

    private void error(Throwable t) {
        t.printStackTrace();
        errors++;
    }

    int count;
    int errors;

    private static <E> int size(Iterable<E> x) {
        int n = 0;
        for (Iterator<E> iter = x.iterator(); iter.hasNext(); ++n)
            iter.next();
        return n;
    }

    private static File writeFile(File f, String str) throws IOException {
        f.getParentFile().mkdirs();
        BufferedWriter fout = new BufferedWriter(new FileWriter(f));
        try {
            fout.write(str);
            fout.flush();
        } finally {
            fout.close();
        }
        return f;
    }
}
