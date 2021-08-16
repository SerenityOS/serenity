/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6412669 6997958
 * @summary Should be able to get SourcePositions from 269 world
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.file
 */

import java.io.*;
import java.net.*;
import java.util.*;
import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.tools.*;
import com.sun.source.util.*;
import com.sun.tools.javac.api.*;

@SupportedAnnotationTypes("*")
public class T6412669 extends AbstractProcessor {
    public static void main(String... args) throws Exception {
        File testSrc = new File(System.getProperty("test.src", "."));
        File testClasses = new File(System.getProperty("test.classes", "."));

        JavacTool tool = JavacTool.create();
        try (StandardJavaFileManager fm = tool.getStandardFileManager(null, null, null)) {
            fm.setLocation(StandardLocation.CLASS_PATH, Arrays.asList(testClasses));
            Iterable<? extends JavaFileObject> files =
                fm.getJavaFileObjectsFromFiles(Arrays.asList(new File(testSrc, T6412669.class.getName()+".java")));
            String[] opts = {
                "-proc:only",
                "-processor", T6412669.class.getName(),
                "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED"
            };
            StringWriter sw = new StringWriter();
            JavacTask task = tool.getTask(sw, fm, null, Arrays.asList(opts), null, files);
            boolean ok = task.call();
            String out = sw.toString();
            if (!out.isEmpty())
                System.err.println(out);
            if (!ok)
                throw new AssertionError("compilation of test program failed");
            // verify we found an annotated element to exercise the SourcePositions API
            if (!out.contains("processing element"))
                throw new AssertionError("expected text not found in compilation output");
        }
    }

    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        Trees trees = Trees.instance(processingEnv);
        SourcePositions sp = trees.getSourcePositions();
        Messager m = processingEnv.getMessager();
        m.printMessage(Diagnostic.Kind.NOTE, "processing annotations");
        int count = 0;
        for (TypeElement anno: annotations) {
            count++;
            m.printMessage(Diagnostic.Kind.NOTE, "  processing annotation " + anno);
            for (Element e: roundEnv.getElementsAnnotatedWith(anno)) {
                m.printMessage(Diagnostic.Kind.NOTE, "    processing element " + e);
                TreePath p = trees.getPath(e);
                long start = sp.getStartPosition(p.getCompilationUnit(), p.getLeaf());
                long end = sp.getEndPosition(p.getCompilationUnit(), p.getLeaf());
                Diagnostic.Kind k = (start > 0 && end > 0 && start < end
                                     ? Diagnostic.Kind.NOTE : Diagnostic.Kind.ERROR);
                m.printMessage(k, "test [" + start + "," + end + "]", e);
            }
        }
        if (count == 0)
            m.printMessage(Diagnostic.Kind.NOTE, "no annotations found");
        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}
