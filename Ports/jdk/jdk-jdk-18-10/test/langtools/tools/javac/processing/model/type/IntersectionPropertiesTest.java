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
 * @bug 6499673 6557966
 * @library /tools/javac/lib
 * @modules jdk.compiler/com.sun.tools.javac.util
 * @build JavacTestingAbstractProcessor IntersectionPropertiesTest
 * @run main IntersectionPropertiesTest
 * @summary Assertion check for TypeVariable.getUpperBound() fails
 */

import com.sun.source.util.*;
import com.sun.tools.javac.util.Assert;
import javax.annotation.processing.*;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.element.*;
import javax.tools.*;
import java.util.*;
import java.io.*;
import javax.lang.model.util.Types;

public class IntersectionPropertiesTest {

    private int errors = 0;
    private static final String Intersection_name = "IntersectionTest.java";
    private static final String Intersection_contents =
        "import java.util.AbstractList;\n" +
        "import java.util.List;\n" +
        "import java.io.Serializable;\t" +
        "public class IntersectionTest<S extends List & Serializable, One extends AbstractList & Runnable & Serializable, Two extends AbstractList & Serializable & Runnable> {\n" +
        "  void method(S s, One o, Two t) { }\n" +
        "  public static abstract class SubType extends AbstractList implements Runnable, Serializable { } \n" +
        "}";

    private static final File classesdir = new File("intersectionproperties");
    private static final JavaCompiler comp =
        ToolProvider.getSystemJavaCompiler();
    private static final StandardJavaFileManager fm =
        comp.getStandardFileManager(null, null, null);

    public void runOne(final String Test_name, final String Test_contents)
        throws IOException {
        System.err.println("Testing " + Test_name);
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
            runOne(Intersection_name, Intersection_contents);

            if (0 != errors)
                throw new RuntimeException(errors + " errors occurred");
        } finally {
            fm.close();
        }
    }

    public static void main(String... args) throws IOException {
        new IntersectionPropertiesTest().run();
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
            Types types = processingEnv.getTypeUtils();
            final HashSet<TypeMirror> actual = new HashSet<TypeMirror>(types.directSupertypes(upperBound));
            final HashSet<TypeMirror> expected = new HashSet<TypeMirror>(bounds);
            if (!expected.equals(actual)) {
                System.err.println("Mismatched expected and actual bounds.");
                System.err.println("Expected:");
                for(TypeMirror tm : expected)
                    System.err.println("  " + tm);
                System.err.println("Actual:");
                for(TypeMirror tm : actual)
                    System.err.println("  " + tm);
                errors++;
            }

            TypeVariable oneTypeVariable = (TypeVariable) method.getParameters().get(1).asType();
            TypeMirror oneUpperBound = oneTypeVariable.getUpperBound();
            TypeVariable twoTypeVariable = (TypeVariable) method.getParameters().get(2).asType();
            TypeMirror twoUpperBound = twoTypeVariable.getUpperBound();
            TypeElement oneUpperBoundElement = (TypeElement) types.asElement(oneUpperBound);

            Assert.checkNonNull(oneUpperBoundElement);

            Assert.check("java.util.AbstractList".equals(oneUpperBoundElement.getSuperclass().toString()),
                         oneUpperBoundElement.getSuperclass().toString());

            List<String> superInterfaces = new java.util.ArrayList<>();

            for (TypeMirror tm : oneUpperBoundElement.getInterfaces()) {
                superInterfaces.add(tm.toString());
            }

            Assert.check(java.util.Arrays.asList("java.lang.Runnable",
                                                 "java.io.Serializable").equals(superInterfaces),
                         superInterfaces);

            Assert.check(types.isSameType(upperBound, types.capture(upperBound)));
            Assert.check(types.isSameType(types.erasure(typeVariable), types.erasure(upperBound)));

            TypeElement subTypeClass = processingEnv.getElementUtils().getTypeElement("IntersectionTest.SubType");

            Assert.checkNonNull(subTypeClass);

            Assert.check(types.isAssignable(subTypeClass.asType(), oneUpperBound));
            Assert.check(types.isSameType(oneUpperBound, twoUpperBound));
            Assert.check(!types.isSameType(upperBound, twoUpperBound));
            Assert.check(types.isSubtype(subTypeClass.asType(), oneUpperBound));
            Assert.check(types.isSubtype(oneUpperBound, upperBound));
        }

    }

}
