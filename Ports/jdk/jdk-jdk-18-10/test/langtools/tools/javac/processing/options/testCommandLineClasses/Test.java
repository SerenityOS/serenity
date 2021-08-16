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
 * @bug 6930508
 * @summary Passing nested class names on javac command line interfere with subsequent name -> class lookup
 * @library /tools/javac/lib
 * @modules java.compiler
 *          jdk.compiler
 * @build JavacTestingAbstractProcessor p.NestedExamples Test
 * @run main Test
 */

import java.io.*;
import java.util.*;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.*;

import p.NestedExamples;

public class Test extends JavacTestingAbstractProcessor {
    public static void main(String... args) throws Exception {
        new Test().run();
    }

    void run() throws Exception {
        NestedExamples e = new NestedExamples();
        List<String> names = getNames(e.getClasses());
        test(names);
        test(reverse(names));
        names = Arrays.asList(e.getClassNames());
        test(names);
        test(reverse(names));

        if (errors > 0)
            throw new RuntimeException(errors + " errors occurred");
    }

    List<String> getNames(Class<?>[] classes) {
        List<String> names = new ArrayList<String>();
        for (Class<?> c: classes)
            names.add(c.getName());
        return names;
    }

    void test(List<String> names) throws Exception {
        System.err.println("test: " + names);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            File testClasses = new File(System.getProperty("test.classes"));
            fm.setLocation(StandardLocation.CLASS_PATH, Arrays.asList(testClasses));
            JavaCompiler.CompilationTask task = compiler.getTask(
                    null, null, null, Arrays.asList("-proc:only"), names, null);
            task.setProcessors(Arrays.asList(new Test()));
            boolean ok = task.call();
            if (!ok)
                error("compilation failed");
            System.err.println();
        }
    }

    <T> List<T> reverse(List<T> list) {
        List<T> newList = new ArrayList<T>(list);
        Collections.reverse(newList);
        return newList;
    }

    int errors = 0;

    void error(String msg) {
        System.out.println("Error: " + msg);
        errors++;
    }

    //----------

    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (!roundEnv.processingOver()) {
            for (TypeElement typeElt : ElementFilter.typesIn(roundEnv.getRootElements())) {
                messager.printMessage(Diagnostic.Kind.NOTE, "processing " + typeElt);
            }
        }
        return true;
    }
}
