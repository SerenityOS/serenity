/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8142931
 * @summary java compiler: type erasure doesn't work since 9-b28
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 */

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.DeclaredType;
import javax.tools.*;
import com.sun.source.util.*;
import com.sun.tools.javac.api.*;

@SupportedAnnotationTypes("*")
public class T8142931 extends AbstractProcessor {

    public java.util.List<? extends javax.xml.namespace.QName> f0;

    public static void main(String... args) throws IOException {
        String testSrc = System.getProperty("test.src", ".");
        String testClasses = System.getProperty("test.classes");
        JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        MyDiagListener dl = new MyDiagListener();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(dl, null, null)) {
            Iterable<? extends JavaFileObject> files =
                fm.getJavaFileObjectsFromFiles(Arrays.asList(new File(testSrc, T8142931.class.getName()+".java")));
            Iterable<String> opts = Arrays.asList(
                "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
                "-XDaccessInternalAPI",
                "-proc:only",
                "-processor", "T8142931",
                "-processorpath", testClasses);
            StringWriter out = new StringWriter();
            JavacTask task = (JavacTask)tool.getTask(out, fm, dl, opts, null, files);
            task.call();
            String s = out.toString();
            System.err.print(s);
            System.err.println(dl.count + " diagnostics; " + s.length() + " characters");
            if (dl.count != 0 || s.length() != 0)
                throw new AssertionError("unexpected output from compiler");
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Set<? extends Element> set = roundEnv.getRootElements();
        for (Element element : set) {
            Collection<VariableElement> fields = ElementFilter.fieldsIn(((TypeElement) element).getEnclosedElements());
            for (VariableElement field : fields) {
                TypeMirror listType = field.asType();
                List<? extends TypeMirror> typeArgs = ((DeclaredType) listType).getTypeArguments();
                TypeMirror arg = typeArgs.get(0);
                String erasure = processingEnv.getTypeUtils().erasure(arg).toString();
                if (!erasure.equals("javax.xml.namespace.QName"))
                    throw new AssertionError("Wrong Erasure: " + erasure);
            }
        }
        return false;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    static class MyDiagListener implements DiagnosticListener {
        public void report(Diagnostic d) {
            System.err.println(d);
            count++;
        }

        public int count;
    }
}

