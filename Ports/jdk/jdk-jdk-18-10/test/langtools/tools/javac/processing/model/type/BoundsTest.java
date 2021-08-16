/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6499673
 * @library /tools/javac/lib
 * @modules jdk.compiler
 * @build JavacTestingAbstractProcessor BoundsTest
 * @run main BoundsTest
 * @summary Assertion check for TypeVariable.getUpperBound() fails
 */

import com.sun.source.util.*;
import javax.annotation.processing.*;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.element.*;
import javax.tools.*;
import java.util.*;
import java.io.*;

public class BoundsTest {

    private int errors = 0;
    private static final String Intersection_name = "IntersectionTest.java";
    private static final String Intersection_contents =
        "import java.util.List;\n" +
        "import java.io.Serializable;\t" +
        "public class IntersectionTest<S extends List & Serializable> {\n" +
        "  void method(S s) { }\n" +
        "}";
    private static final String[] Intersection_bounds = {
        "java.util.List",
        "java.io.Serializable"
    };
    private static final String[] Intersection_supers = {
        "java.util.List",
        "java.io.Serializable"
    };

    private static final String Single_name = "SingleTest.java";
    private static final String Single_contents =
        "import java.util.List;\n" +
        "public class SingleTest<S extends List> {\n" +
        "  void method(S s) { }\n" +
        "}";
    private static final String[] Single_bounds = {
        "java.util.List",
    };
    private static final String[] Single_supers = {
        "java.lang.Object",
        "java.util.Collection"
    };

    private static final String NoBounds_name = "NoBoundsTest.java";
    private static final String NoBounds_contents =
        "public class NoBoundsTest<S> {\n" +
        "  void method(S s) { }\n" +
        "}";
    private static final String[] NoBounds_bounds = {
        "java.lang.Object",
    };
    private static final String[] NoBounds_supers = {};

    private HashSet<String> expected_bounds;
    private HashSet<String> expected_supers;

    private static final File classesdir = new File("intersectionproperties");
    private static final JavaCompiler comp =
        ToolProvider.getSystemJavaCompiler();
    private static final StandardJavaFileManager fm =
        comp.getStandardFileManager(null, null, null);

    public void runOne(final String Test_name, final String Test_contents,
                       final String[] Test_bounds, final String[] Test_supers)
        throws IOException {
        System.err.println("Testing " + Test_name);
        expected_bounds = new HashSet<>(Arrays.asList(Test_bounds));
        expected_supers = new HashSet<>(Arrays.asList(Test_supers));
        final Iterable<? extends JavaFileObject> files =
            fm.getJavaFileObjectsFromFiles(Collections.singleton(writeFile(classesdir, Test_name, Test_contents)));
        final JavacTask ct =
            (JavacTask)comp.getTask(null, fm, null, null, null, files);
        ct.setProcessors(Collections.singleton(new TestProcessor()));

        if (!ct.call()) {
            System.err.println("Compilation unexpectedly failed");
            errors++;
        }
    }

    public void run() throws IOException {
        try {
            runOne(Intersection_name, Intersection_contents,
                   Intersection_bounds, Intersection_supers);
            runOne(Single_name, Single_contents,
                   Single_bounds, Single_supers);
            runOne(NoBounds_name, NoBounds_contents,
                   NoBounds_bounds, NoBounds_supers);

            if (0 != errors)
                throw new RuntimeException(errors + " errors occurred");
        } finally {
            fm.close();
        }
    }

    public static void main(String... args) throws IOException {
        new BoundsTest().run();
    }

    private static File writeFile(File dir, String path, String body)
        throws IOException {
        File f = new File(dir, path);
        f.getParentFile().mkdirs();
        try (FileWriter out = new FileWriter(f)) {
            out.write(body);
        }
        return f;
    }

    private class TestProcessor extends JavacTestingAbstractProcessor {

        private Set<? extends Element> rootElements;

        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            rootElements = roundEnv.getRootElements();
            if (!rootElements.isEmpty()) {
                performCheck();
            }
            return true;
        }

        private void performCheck() {
            TypeElement typeElement = (TypeElement) rootElements.iterator().next();
            ExecutableElement method = ElementFilter.methodsIn(typeElement.getEnclosedElements()).get(0);
            TypeVariable typeVariable = (TypeVariable) method.getParameters().get(0).asType();

            final TypeMirror upperBound = typeVariable.getUpperBound();

            TypeParameterElement typeParameterElement = ((TypeParameterElement) typeVariable.asElement());
            final List<? extends TypeMirror> bounds = typeParameterElement.getBounds();
            final List<? extends TypeMirror> supers = processingEnv.getTypeUtils().directSupertypes(upperBound);

            final HashSet<String> actual_bounds = new HashSet<>();
            final HashSet<String> actual_supers = new HashSet<>();

            for(TypeMirror ty : bounds) {
                actual_bounds.add(((TypeElement)((DeclaredType)ty).asElement()).getQualifiedName().toString());
            }

            for(TypeMirror ty : supers) {
                actual_supers.add(((TypeElement)((DeclaredType)ty).asElement()).getQualifiedName().toString());
            }

            if (!expected_bounds.equals(actual_bounds)) {
                System.err.println("Mismatched expected and actual bounds.");
                System.err.println("Expected:");
                for(CharSequence tm : expected_bounds)
                    System.err.println("  " + tm);
                System.err.println("Actual:");
                for(CharSequence tm : actual_bounds)
                    System.err.println("  " + tm);
                errors++;
            }

            if (!expected_supers.equals(actual_supers)) {
                System.err.println("Mismatched expected and actual supers.");
                System.err.println("Expected:");
                for(CharSequence tm : expected_supers)
                    System.err.println("  " + tm);
                System.err.println("Actual:");
                for(CharSequence tm : actual_supers)
                    System.err.println("  " + tm);
                errors++;
            }
        }
    }

}
