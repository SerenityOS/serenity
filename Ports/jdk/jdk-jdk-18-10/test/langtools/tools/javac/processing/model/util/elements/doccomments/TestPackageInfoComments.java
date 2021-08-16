/*
 * Copyright (c) 2014, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8042345
 * @summary getDocComment() fails for doc comments on PackageElement found in package-info.java
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build JavacTestingAbstractProcessor TestPackageInfoComments
 * @run main TestPackageInfoComments
 */
import com.sun.source.util.JavacTask;

import java.io.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import javax.tools.*;

public class TestPackageInfoComments extends JavacTestingAbstractProcessor {

    public static void main(String... args) throws Exception {
        String[] opts = {
            "-implicit:none",
            "-processor", TestPackageInfoComments.class.getName(),
            "-processorpath", System.getProperty("test.class.path")
        };
        File[] files = {
            new File(System.getProperty("test.src"), "p/package-info.java")
        };
        run_test(opts, files);
    }

    static void run_test(String[] opts, File[] files) throws IOException {
        DiagnosticListener<JavaFileObject> dl = new DiagnosticListener<JavaFileObject>() {
            public void report(Diagnostic diagnostic) {
                throw new Error(diagnostic.toString());
            }
        };
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            Iterable<? extends JavaFileObject> units = fm.getJavaFileObjects(files);
            JavacTask t = (JavacTask) c.getTask(null, fm, dl, Arrays.asList(opts), null, units);
            t.parse();
            t.analyze();
        }
    }

    // -- Annotation processor: Check all PackageDecl's have a doc comment

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e: roundEnv.getRootElements())
            new TestElementScanner().scan(e);
        return true;
    }

    class TestElementScanner extends ElementScanner<Void, Void> {
        @Override
        public Void visitPackage(PackageElement e, Void v) {
            if (elements.getDocComment(e) == null)
                messager.printMessage(Diagnostic.Kind.ERROR, "doc comment is null", e);
            return v;
        }
    }
}
