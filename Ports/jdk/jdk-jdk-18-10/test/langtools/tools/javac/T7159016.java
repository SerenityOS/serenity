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
 * @bug 7159016
 * @summary Static import of member in processor-generated class fails in JDK 7
 * @library lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor
 * @run main T7159016
 * @author Jessie Glick
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collections;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

public class T7159016 {
    public static void main(String[] args) throws Exception {
        File src = new File("C.java");
        Writer w = new FileWriter(src);
        try {
            w.write("import static p.Generated.m;\nclass C { {m(); } }\n");
            w.flush();
        } finally {
            w.close();
        }
        JavaCompiler jc = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = jc.getStandardFileManager(null, null, null)) {
            JavaCompiler.CompilationTask task = jc.getTask(null, fm, null, null, null,
                                                           fm.getJavaFileObjects(src));
            task.setProcessors(Collections.singleton(new Proc()));
            if (!task.call()) {
                throw new Error("Test failed");
            }
        }
    }

    private static class Proc extends JavacTestingAbstractProcessor {
        int written;
        @Override public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (roundEnv.processingOver() || written++ > 0) {
                return false;
            }
            messager.printMessage(Diagnostic.Kind.NOTE, "writing Generated.java");
            try {
                Writer w = processingEnv.getFiler().createSourceFile("p.Generated").openWriter();
                try {
                    w.write("package p; public class Generated { public static void m() { } }");
                } finally {
                    w.close();
                }
            } catch (IOException x) {
                messager.printMessage(Diagnostic.Kind.ERROR, x.toString());
            }
            return true;
        }
    }
}
