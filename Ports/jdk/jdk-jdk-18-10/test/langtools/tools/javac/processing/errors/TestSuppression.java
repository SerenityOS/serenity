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
 * @bug 6403465
 * @summary javac should defer diagnostics until it can be determined they are persistent
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.TypeElement;
import javax.tools.*;

import com.sun.source.util.JavacTask;
import com.sun.tools.javac.api.ClientCodeWrapper;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.util.JCDiagnostic;

import static com.sun.tools.javac.util.JCDiagnostic.DiagnosticFlag.*;


public class TestSuppression {
    public static void main(String... args) throws Exception {
        new TestSuppression().run(args);
    }

    enum WarningKind { NO, YES };

    String[] cases = {
        // missing class C
        "class X { C c; }",
        "class X { C foo() { return null; } }",
        "class X { void foo(C c) { } }",
        "class X extends C { }",
        "class X<T extends C> { }",
        // missing interface I
        "class X implements I { }",
        "interface X extends I { }",
        // missing exception E
        "class X { void m() throws E { } }",
        // missing method m
        "class X extends C { int i = m(); }",
        // missing field f
        "class X extends C { int i = f; }"
    };

    void run(String... args) throws Exception {
        for (String c: cases) {
            for (WarningKind wk: WarningKind.values()) {
                for (int g = 1; g <= 3; g++) {
                    try {
                        test(c, wk, g);
                    } catch (Throwable t) {
                        error("caught: " + t);
                    }
                    if (errors > 0) throw new AssertionError();
                }
            }
        }

        System.err.println(count + " test cases");

        if (errors > 0)
            throw new Exception(errors + " errors occurred");
    }

    void test(String src, WarningKind wk, int gen) throws Exception {
        count++;
        System.err.println("Test " + count + ": wk:" + wk + " gen:" + gen + " src:" +src);

        File testDir = new File("test" + count);
        File srcDir = createDir(testDir, "src");
        File gensrcDir = createDir(testDir, "gensrc");
        File classesDir = createDir(testDir, "classes");

        File x = writeFile(new File(srcDir, "X.java"), src);

        DiagListener dl = new DiagListener();
        JavacTool tool = JavacTool.create();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(dl, null, null)) {
            fm.setLocation(StandardLocation.CLASS_PATH,
                    Arrays.asList(classesDir, new File(System.getProperty("test.classes"))));
            fm.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singleton(classesDir));
            fm.setLocation(StandardLocation.SOURCE_OUTPUT, Collections.singleton(gensrcDir));
            List<String> args = new ArrayList<String>();
    //        args.add("-XprintProcessorInfo");
            args.add("-XprintRounds");
            args.add("-Agen=" + gen);
            if (wk == WarningKind.YES)
                args.add("-Xlint:serial");
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(x);

            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            JavacTask task = tool.getTask(pw, fm, dl, args, null, files);
            task.setProcessors(Arrays.asList(new AnnoProc()));
            boolean ok = task.call();
            pw.close();

            System.err.println("ok:" + ok + " diags:" + dl.counts);
            if (sw.toString().length() > 0) {
                System.err.println("output:\n" + sw.toString());
            }

            for (Diagnostic.Kind dk: Diagnostic.Kind.values()) {
                Integer v = dl.counts.get(dk);
                int found = (v == null) ? 0 : v;
                int expect = (dk == Diagnostic.Kind.WARNING && wk == WarningKind.YES) ? gen : 0;
                if (found != expect) {
                    error("Unexpected value for " + dk + ": expected: " + expect + " found: " + found);
                }
            }

            System.err.println();
        }
    }

    File createDir(File parent, String name) {
        File dir = new File(parent, name);
        dir.mkdirs();
        return dir;
    }

    File writeFile(File f, String content) throws IOException {
        FileWriter out = new FileWriter(f);
        try {
            out.write(content);
        } finally {
            out.close();
        }
        return f;
    }

    <T> void add(List<T> list, T... values) {
        for (T v: values)
            list.add(v);
    }

    void error(String msg) {
        System.err.println("Error: " + msg);
        errors++;
    }

    int count;
    int errors;

    static class DiagListener implements DiagnosticListener<JavaFileObject> {
        int total;
        Map<Diagnostic.Kind,Integer> counts = new TreeMap<Diagnostic.Kind,Integer>();

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            System.err.println((++total) + ": "
                    + "resolveError:" + isResolveError(unwrap(diagnostic)) + "\n"
                    + diagnostic);
            Diagnostic.Kind dk = diagnostic.getKind();
            Integer c = counts.get(dk);
            counts.put(dk, (c == null ? 1 : c + 1));
        }

        private static boolean isResolveError(JCDiagnostic d) {
            return d.isFlagSet(RESOLVE_ERROR);
        }

        private JCDiagnostic unwrap(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic instanceof JCDiagnostic)
                return (JCDiagnostic) diagnostic;
            if (diagnostic instanceof ClientCodeWrapper.DiagnosticSourceUnwrapper)
                return ((ClientCodeWrapper.DiagnosticSourceUnwrapper)diagnostic).d;
            throw new IllegalArgumentException();
        }
    }

    @SupportedAnnotationTypes("*")
    @SupportedOptions("gen")
    public static class AnnoProc extends AbstractProcessor {
        Filer f;
        Messager m;
        int gen;

        @Override
        public void init(ProcessingEnvironment processingEnv) {
            f = processingEnv.getFiler();
            m = processingEnv.getMessager();
            Map<String,String> options = processingEnv.getOptions();
            gen = Integer.parseInt(options.get("gen"));
        }

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            round++;
            if (round < gen)
                writeSource("Dummy" + round, "class Dummy" + round + " extends java.util.ArrayList{ }");
            else if (round == gen) {
                writeSource("C", "class C { int f; int m() { return 0; } }");
                writeSource("I", "interface I { }");
                writeSource("E", "class E extends Exception { }");
            }
            return true;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

        private void writeSource(String name, String text) {
            try {
                JavaFileObject fo = f.createSourceFile(name);
                Writer out = fo.openWriter();
                out.write(text);
                out.close();
            } catch (IOException e) {
                m.printMessage(Diagnostic.Kind.ERROR, e.toString());
            }
        }

        int round = 0;
    }
}
