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
 * @bug 7031108
 * @summary NPE in javac.jvm.ClassReader.findMethod in PackageElement.enclosedElements from AP in incr build
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor T7031108
 * @run main T7031108
 */

import java.io.*;
import java.net.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.tools.*;
import javax.tools.JavaCompiler.CompilationTask;

public class T7031108 extends JavacTestingAbstractProcessor {
    public static void main(String... args) throws Exception {
        new T7031108().run();
    }

    /* Class containing a local class definition;
     * compiled class file will have an EnclosedMethod attribute.
     */
    static final JavaSource pC =
            new JavaSource("p/C.java",
                  "package p;\n"
                + "class C {\n"
                + "    void m() {\n"
                + "        new Runnable() {\n"
                + "            public void run() {\n"
                + "                new Runnable() {\n"
                + "                    public void run() { }\n"
                + "                };\n"
                + "            }\n"
                + "        };\n"
                + "    }\n"
                + "}");

    private static final String PACKAGE_CONTENT_ERROR = "package does not contain C";

    /* Dummy source file to compile while running anno processor. */
    static final JavaSource dummy =
            new JavaSource("Dummy.java",
                "class Dummy { }");

    void run() throws Exception {
        JavaCompiler comp = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = comp.getStandardFileManager(null, null, null)) {

            // step 1: compile test classes
            File cwd = new File(".");
            fm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(cwd));
            compile(comp, fm, null, null, pC);

            // step 2: verify functioning of processor
            fm.setLocation(StandardLocation.ANNOTATION_PROCESSOR_PATH,
                    fm.getLocation(StandardLocation.CLASS_PATH));
            fm.setLocation(StandardLocation.CLASS_PATH, Arrays.asList(cwd));
            compile(comp, fm, null, getClass().getName(), dummy);

            File pC_class = new File(new File("p"), "C.class");
            pC_class.delete();

            DiagnosticCollector<JavaFileObject> dc = new DiagnosticCollector<JavaFileObject>();
            compile(comp, fm, dc, getClass().getName(), dummy);
            List<Diagnostic<? extends JavaFileObject>> diags =dc.getDiagnostics();

            System.err.println(diags);
            switch (diags.size()) {
                case 0:
                    throw new Exception("no diagnostics received");
                case 1:
                    String code = diags.get(0).getCode();
                    String expect = "compiler.err.proc.messager";
                    if (!expect.equals(code))
                        throw new Exception("unexpected diag code: " + code
                                + ", expected: " + expect);
                    String message = diags.get(0).getMessage(null);
                    if (!PACKAGE_CONTENT_ERROR.equals(message)) {
                        throw new Exception("unexpected diag message: " + code
                                + ", expected: " + PACKAGE_CONTENT_ERROR);
                    }
                    break;
                default:
                    throw new Exception("unexpected diags received");
            }
        }
    }

    void compile(JavaCompiler comp, JavaFileManager fm,
            DiagnosticListener<JavaFileObject> dl,
            String processor, JavaFileObject... files) throws Exception {
        System.err.println("compile processor:" + processor + ", files:" + Arrays.asList(files));
        List<String> opts = new ArrayList<String>();
        if (processor != null) {
            // opts.add("-verbose");
            opts.addAll(Arrays.asList("-processor", processor));
        }
        CompilationTask task = comp.getTask(null, fm, dl, opts, null, Arrays.asList(files));
        boolean ok = task.call();
        if (dl == null && !ok)
            throw new Exception("compilation failed");
    }

    static class JavaSource extends SimpleJavaFileObject {
        JavaSource(String name, String text) {
            super(URI.create("js://" + name), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }
        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
        final String text;
    }

    // annotation processor method

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            PackageElement p = elements.getPackageElement("p");
            List<? extends Element> elems = p.getEnclosedElements();
            System.err.println("contents of package p: " + elems);
            if (elems.size() != 1 || !elems.get(0).getSimpleName().contentEquals("C")) {
                messager.printMessage(Diagnostic.Kind.ERROR, PACKAGE_CONTENT_ERROR);
            }
        }
        return true;
    }
}

