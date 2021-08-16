/*
 * Copyright (c) 2018, Google Inc. All rights reserved.
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

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.util.Arrays;

/*
 * @test
 * @bug 7183985
 * @summary getAnnotation() throws an ArrayStoreException when the annotation class not present
 * @compile MissingClassArrayElementTest.java MissingClass.java MissingClass2.java
 * @clean MissingClass MissingClass2
 * @run main MissingClassArrayElementTest
 */
public class MissingClassArrayElementTest {

    @Retention(RUNTIME)
    @interface AnnotationAnnotation {
        ClassArrayAnnotation[] value();
    }

    @Retention(RUNTIME)
    @interface ClassArrayAnnotation {
        Class<?>[] value();
    }

    @AnnotationAnnotation({
        @ClassArrayAnnotation({MissingClass.class}),
        @ClassArrayAnnotation({MissingClass.class, String.class}),
        @ClassArrayAnnotation({String.class, MissingClass.class}),
        @ClassArrayAnnotation({MissingClass.class, MissingClass2.class}),
        @ClassArrayAnnotation({String.class})
    })
    static class Test {
        void f(
                @AnnotationAnnotation({
                            @ClassArrayAnnotation({MissingClass.class, MissingClass.class}),
                            @ClassArrayAnnotation({Float.class})
                        })
                        int x,
                @AnnotationAnnotation({@ClassArrayAnnotation({Double.class})}) int y) {}

        Test(
                @AnnotationAnnotation({
                            @ClassArrayAnnotation({MissingClass.class, MissingClass.class}),
                            @ClassArrayAnnotation({Short.class}),
                        })
                        int x,
                @AnnotationAnnotation({@ClassArrayAnnotation({Character.class})}) int y) {}
    }

    public static void main(String[] args) throws Exception {
        classAnnotationTest();
        methodParameterAnnotationsTest();
        constructorParameterAnnotationsTest();
    }

    static void classAnnotationTest() throws Exception {
        ClassArrayAnnotation[] outer = Test.class.getAnnotation(AnnotationAnnotation.class).value();
        assertMissing(outer[0]);
        assertMissing(outer[1]);
        assertMissing(outer[2]);
        assertMissing(outer[3]);
        assertArrayEquals(outer[4].value(), new Class<?>[] {String.class});
    }

    static void methodParameterAnnotationsTest() throws Exception {
        AnnotationAnnotation[] methodParameterAnnotations =
                Arrays.stream(
                                Test.class
                                        .getDeclaredMethod("f", int.class, int.class)
                                        .getParameterAnnotations())
                        .map(x -> ((AnnotationAnnotation) x[0]))
                        .toArray(AnnotationAnnotation[]::new);
        // The first parameter's annotation contains some well-formed values, and the second
        // parameter's
        // annotation is well-formed
        assertArrayEquals(
                methodParameterAnnotations[0].value()[1].value(), new Class<?>[] {Float.class});
        assertArrayEquals(
                methodParameterAnnotations[1].value()[0].value(), new Class<?>[] {Double.class});
        // The first parameter's annotation contains a missing value
        assertMissing(methodParameterAnnotations[0].value()[0]);
    }

    static void constructorParameterAnnotationsTest() throws Exception {
        AnnotationAnnotation[] constructorParameterAnnotations =
                Arrays.stream(
                                Test.class
                                        .getDeclaredConstructor(int.class, int.class)
                                        .getParameterAnnotations())
                        .map(x -> ((AnnotationAnnotation) x[0]))
                        .toArray(AnnotationAnnotation[]::new);
        // The first parameter's annotation contains some well-formed values, and the second
        // parameter's
        // annotation is well-formed
        assertArrayEquals(
                constructorParameterAnnotations[0].value()[1].value(),
                new Class<?>[] {Short.class});
        assertArrayEquals(
                constructorParameterAnnotations[1].value()[0].value(),
                new Class<?>[] {Character.class});
        // The first parameter's annotation contains a missing value
        assertMissing(constructorParameterAnnotations[0].value()[0]);
    }

    static void assertArrayEquals(Object[] actual, Object[] expected) {
        if (!Arrays.equals(actual, expected)) {
            throw new AssertionError(
                    "expected: " + Arrays.toString(expected) + ", was: " + Arrays.toString(actual));
        }
    }

    static void assertMissing(ClassArrayAnnotation missing) {
        try {
            missing.value();
            throw new AssertionError("expected exception");
        } catch (TypeNotPresentException expected) {
            if (!expected.typeName().equals("MissingClass")) {
                throw new AssertionError(
                        "expected TypeNotPresentException: MissingClass", expected);
            }
        }
    }
}
