/*
 * Copyright (c) 2011, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7068451
 * @summary Regression: javac compiles fixed sources against previous,
 *              not current, version of generated sources
 * @modules java.compiler
 *          jdk.compiler
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

public class T7068451 {
    public static void main(String[] args) throws Exception {
        new T7068451().run();
    }

    void run() throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        System.err.println("using " + compiler.getClass() + " from " + compiler.getClass().getProtectionDomain().getCodeSource());

        File tmp = new File("tmp");
        tmp.mkdir();
        for (File f: tmp.listFiles())
            f.delete();

        File input = writeFile(tmp, "X.java", "package p; class X { { p.C.first(); } }");

        List<String> opts = Arrays.asList(
                "-s", tmp.getPath(),
                "-d", tmp.getPath(),
                "-XprintRounds");

        System.err.println();
        System.err.println("FIRST compilation");
        System.err.println();

        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            CompilationTask task = compiler.getTask(null, fm, null, opts, null,
                    fm.getJavaFileObjects(input));
            task.setProcessors(Collections.singleton(new Proc("first")));
            check("compilation", task.call());

            writeFile(tmp, "X.java", "package p; class X { { p.C.second(); } }");

            //Thread.sleep(2000);

            System.err.println();
            System.err.println("SECOND compilation");
            System.err.println();

            task = compiler.getTask(null, fm, null, opts, null,
                    fm.getJavaFileObjects(input));
            task.setProcessors(Collections.singleton(new Proc("second")));
            check("compilation", task.call());

            //Thread.sleep(2000);

            System.err.println();
            System.err.println("SECOND compilation, REPEATED");
            System.err.println();

            task = compiler.getTask(null, fm, null, opts, null,
                    fm.getJavaFileObjects(input));
            task.setProcessors(Collections.singleton(new Proc("second")));
            check("compilation", task.call());
        }
    }

    void check(String msg, boolean ok) {
        System.err.println(msg + ": " + (ok ? "ok" : "failed"));
        if (!ok)
            throw new AssertionError(msg);
    }

    static File writeFile(File base, String path, String body) throws IOException {
        File f = new File(base, path);
        FileWriter out = new FileWriter(f);
        out.write(body);
        out.close();
        System.err.println("wrote " + path + ": " + body);
        return f;
    }

    @SupportedAnnotationTypes("*")
    private static class Proc extends AbstractProcessor {
        final String m;
        Proc(String m) {
            this.m = m;
        }

        int count;
        @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (roundEnv.processingOver() || count++ > 0) {
                return false;
            }

            Filer filer = processingEnv.getFiler();
            Messager messager = processingEnv.getMessager();

            System.err.println("running Proc");
            try {
                int len = filer.getResource(StandardLocation.SOURCE_OUTPUT, "p", "C.java").getCharContent(false).length();
                messager.printMessage(Kind.NOTE, "C.java: found previous content of length " + len);
            } catch (FileNotFoundException | NoSuchFileException x) {
                messager.printMessage(Kind.NOTE, "C.java: not previously there");
            } catch (IOException x) {
                messager.printMessage(Kind.ERROR, "while reading: " + x);
            }

            try {
                String body = "package p; public class C { public static void " + m + "() {} }";
                Writer w = filer.createSourceFile("p.C").openWriter();
                w.write(body);
                w.close();
                messager.printMessage(Kind.NOTE, "C.java: wrote new content: " + body);
            } catch (IOException x) {
                messager.printMessage(Kind.ERROR, "while writing: " + x);
            }

            return true;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }
    }
}

