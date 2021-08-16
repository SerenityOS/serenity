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
 * @bug 6863465
 * @summary  javac doesn't detect circular subclass dependencies via qualified names
 * @modules jdk.compiler
 * @run main TestCircularClassfile
 */

import java.io.*;
import java.net.URI;
import java.util.Arrays;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import java.util.EnumSet;

public class TestCircularClassfile {

    enum ClassName {
        A("A"),
        B("B"),
        C("C"),
        OBJECT("Object");

        String name;

        ClassName(String name) {
            this.name = name;
        }
    }

    static class JavaSource extends SimpleJavaFileObject {

        final static String sourceStub = "class #C extends #S {}";

        String source;

        public JavaSource(ClassName clazz, ClassName sup) {
            super(URI.create("myfo:/Test.java"), JavaFileObject.Kind.SOURCE);
            source = sourceStub.replace("#C", clazz.name).replace("#S", sup.name);
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }

    public static void main(String... args) throws Exception {
        int count = 0;
        for (ClassName clazz : EnumSet.of(ClassName.A, ClassName.B, ClassName.C)) {
            for (ClassName sup : EnumSet.of(ClassName.A, ClassName.B, ClassName.C)) {
                if (sup.ordinal() < clazz.ordinal()) continue;
                check("sub_"+count++, clazz, sup);
            }
        }
    }

    static JavaSource[] initialSources = new JavaSource[] {
            new JavaSource(ClassName.A, ClassName.OBJECT),
            new JavaSource(ClassName.B, ClassName.A),
            new JavaSource(ClassName.C, ClassName.B)
        };

    static String workDir = System.getProperty("user.dir");

    static void check(String destPath, ClassName clazz, ClassName sup) throws Exception {
        File destDir = new File(workDir, destPath); destDir.mkdir();
        final JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        JavacTask ct = (JavacTask)tool.getTask(null, null, null,
                Arrays.asList("-d", destPath), null, Arrays.asList(initialSources));
        ct.generate();
        File fileToRemove = new File(destPath, clazz.name + ".class");
        fileToRemove.delete();
        JavaSource newSource = new JavaSource(clazz, sup);
        DiagnosticChecker checker = new DiagnosticChecker();
        ct = (JavacTask)tool.getTask(null, null, checker,
                Arrays.asList("-cp", destPath), null, Arrays.asList(newSource));
        ct.analyze();
        if (!checker.errorFound) {
            throw new AssertionError(newSource.source);
        }
    }

    static class DiagnosticChecker implements DiagnosticListener<JavaFileObject> {

        boolean errorFound = false;

        public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR &&
                    diagnostic.getCode().equals("compiler.err.cyclic.inheritance")) {
                errorFound = true;
            }
        }
    }
}
