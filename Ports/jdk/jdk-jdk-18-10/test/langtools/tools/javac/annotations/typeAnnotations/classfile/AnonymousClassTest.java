/*
 * Copyright (c) 2018 Google LLC. All rights reserved.
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
 * @bug 8198945 8207018
 * @summary Invalid RuntimeVisibleTypeAnnotations for annotation on anonymous class type parameter
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavapTask
 * @run compile -g AnonymousClassTest.java
 * @run main AnonymousClassTest
 */

import static java.util.stream.Collectors.toSet;

import com.sun.tools.classfile.Annotation;
import com.sun.tools.classfile.Annotation.Annotation_element_value;
import com.sun.tools.classfile.Annotation.Array_element_value;
import com.sun.tools.classfile.Annotation.Class_element_value;
import com.sun.tools.classfile.Annotation.Enum_element_value;
import com.sun.tools.classfile.Annotation.Primitive_element_value;
import com.sun.tools.classfile.Annotation.element_value;
import com.sun.tools.classfile.Annotation.element_value.Visitor;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.Code_attribute;
import com.sun.tools.classfile.ConstantPool.CONSTANT_Integer_info;
import com.sun.tools.classfile.ConstantPool.InvalidIndex;
import com.sun.tools.classfile.ConstantPoolException;
import com.sun.tools.classfile.Method;
import com.sun.tools.classfile.RuntimeVisibleTypeAnnotations_attribute;
import com.sun.tools.classfile.TypeAnnotation;
import com.sun.tools.classfile.TypeAnnotation.Position;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Callable;
import toolbox.ToolBox;

public class AnonymousClassTest {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    public @interface TA {
        int value() default 0;
    }

    private void f() {
        new @TA(0) Callable<@TA(1) Object>() {
            public Object call() {
                return null;
            }
        };
    }

    class Inner {
        private void g() {
            // The annotation is propagated from the top-level class Object to NEW expression for
            // the anonymous class' synthetic class declaration, which is an inner class of an inner
            // class.
            new @TA(2) Object() {};
        }
    }

    private void g() {
        new @TA(3) AnonymousClassTest.@TA(4) Inner() {};
    }

    public static void main(String args[]) throws Exception {
        testAnonymousClassDeclaration();
        testTopLevelMethod();
        testInnerClassMethod();
        testQualifiedSuperType();
    }

    static void testAnonymousClassDeclaration() throws Exception {
        ClassFile cf = ClassFile.read(Paths.get(ToolBox.testClasses, "AnonymousClassTest$1.class"));
        RuntimeVisibleTypeAnnotations_attribute rvta =
                (RuntimeVisibleTypeAnnotations_attribute)
                        cf.attributes.get(Attribute.RuntimeVisibleTypeAnnotations);
        assertEquals(
                Set.of(
                        "@LAnonymousClassTest$TA;(1) CLASS_EXTENDS, offset=-1, location=[TYPE_ARGUMENT(0)]",
                        "@LAnonymousClassTest$TA;(0) CLASS_EXTENDS, offset=-1, location=[]"),
                Arrays.stream(rvta.annotations)
                        .map(a -> annotationDebugString(cf, a))
                        .collect(toSet()));
    }

    static void testTopLevelMethod() throws Exception {
        ClassFile cf = ClassFile.read(Paths.get(ToolBox.testClasses, "AnonymousClassTest.class"));
        Method method = findMethod(cf, "f");
        Set<TypeAnnotation> annotations = getRuntimeVisibleTypeAnnotations(method);
        assertEquals(
                Set.of("@LAnonymousClassTest$TA;(0) NEW, offset=0, location=[INNER_TYPE]"),
                annotations.stream().map(a -> annotationDebugString(cf, a)).collect(toSet()));
    }

    static void testInnerClassMethod() throws Exception {
        ClassFile cf =
                ClassFile.read(Paths.get(ToolBox.testClasses, "AnonymousClassTest$Inner.class"));
        Method method = findMethod(cf, "g");
        Set<TypeAnnotation> annotations = getRuntimeVisibleTypeAnnotations(method);
        // The annotation needs two INNER_TYPE type path entries to apply to
        // AnonymousClassTest$Inner$1.
        assertEquals(
                Set.of(
                        "@LAnonymousClassTest$TA;(2) NEW, offset=0, location=[INNER_TYPE, INNER_TYPE]"),
                annotations.stream().map(a -> annotationDebugString(cf, a)).collect(toSet()));
    }

    static void testQualifiedSuperType() throws Exception {
        {
            ClassFile cf =
                    ClassFile.read(Paths.get(ToolBox.testClasses, "AnonymousClassTest.class"));
            Method method = findMethod(cf, "g");
            Set<TypeAnnotation> annotations = getRuntimeVisibleTypeAnnotations(method);
            // Only @TA(4) is propagated to the anonymous class declaration.
            assertEquals(
                    Set.of("@LAnonymousClassTest$TA;(4) NEW, offset=0, location=[INNER_TYPE]"),
                    annotations.stream().map(a -> annotationDebugString(cf, a)).collect(toSet()));
        }

        {
            ClassFile cf =
                    ClassFile.read(Paths.get(ToolBox.testClasses, "AnonymousClassTest$2.class"));
            RuntimeVisibleTypeAnnotations_attribute rvta =
                    (RuntimeVisibleTypeAnnotations_attribute)
                            cf.attributes.get(Attribute.RuntimeVisibleTypeAnnotations);
            assertEquals(
                    Set.of(
                            "@LAnonymousClassTest$TA;(3) CLASS_EXTENDS, offset=-1, location=[]",
                            "@LAnonymousClassTest$TA;(4) CLASS_EXTENDS, offset=-1, location=[INNER_TYPE]"),
                    Arrays.stream(rvta.annotations)
                            .map(a -> annotationDebugString(cf, a))
                            .collect(toSet()));
        }
    }

    // Returns the Method's RuntimeVisibleTypeAnnotations, and asserts that there are no RVTIs
    // erroneously associated with the Method instead of its Code attribute.
    private static Set<TypeAnnotation> getRuntimeVisibleTypeAnnotations(Method method) {
        if (method.attributes.get(Attribute.RuntimeVisibleTypeAnnotations) != null) {
            throw new AssertionError(
                    "expected no RuntimeVisibleTypeAnnotations attribute on enclosing method");
        }
        Code_attribute code = (Code_attribute) method.attributes.get(Attribute.Code);
        RuntimeVisibleTypeAnnotations_attribute rvta =
                (RuntimeVisibleTypeAnnotations_attribute)
                        code.attributes.get(Attribute.RuntimeVisibleTypeAnnotations);
        return Set.of(rvta.annotations);
    }

    private static Method findMethod(ClassFile cf, String name) {
        return Arrays.stream(cf.methods)
                .filter(
                        m -> {
                            try {
                                return m.getName(cf.constant_pool).contentEquals(name);
                            } catch (ConstantPoolException e) {
                                throw new AssertionError(e);
                            }
                        })
                .findFirst()
                .get();
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!actual.equals(expected)) {
            throw new AssertionError(String.format("expected: %s, saw: %s", expected, actual));
        }
    }

    private static String annotationDebugString(ClassFile cf, TypeAnnotation annotation) {
        Position pos = annotation.position;
        String name;
        try {
            name = cf.constant_pool.getUTF8Info(annotation.annotation.type_index).value;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        return String.format(
                "@%s(%s) %s, offset=%d, location=%s",
                name,
                annotationValueoDebugString(cf, annotation.annotation),
                pos.type,
                pos.offset,
                pos.location);
    }

    private static String annotationValueoDebugString(ClassFile cf, Annotation annotation) {
        if (annotation.element_value_pairs.length != 1) {
            throw new UnsupportedOperationException();
        }
        try {
            return elementValueDebugString(cf, annotation.element_value_pairs[0].value);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    private static String elementValueDebugString(ClassFile cf, element_value value) {
        class Viz implements Visitor<String, Void> {
            @Override
            public String visitPrimitive(Primitive_element_value ev, Void aVoid) {
                try {
                    switch (ev.tag) {
                        case 'I':
                            return Integer.toString(
                                    ((CONSTANT_Integer_info)
                                                    cf.constant_pool.get(ev.const_value_index))
                                            .value);
                        default:
                            throw new UnsupportedOperationException(String.format("%c", ev.tag));
                    }
                } catch (InvalidIndex e) {
                    throw new AssertionError(e);
                }
            }

            @Override
            public String visitEnum(Enum_element_value ev, Void aVoid) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String visitClass(Class_element_value ev, Void aVoid) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String visitAnnotation(Annotation_element_value ev, Void aVoid) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String visitArray(Array_element_value ev, Void aVoid) {
                throw new UnsupportedOperationException();
            }
        }
        return value.accept(new Viz(), null);
    }
}
