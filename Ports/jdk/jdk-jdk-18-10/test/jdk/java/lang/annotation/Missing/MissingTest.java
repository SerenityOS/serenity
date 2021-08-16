/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6322301 5041778
 * @summary Verify when missing annotation classes cause exceptions
 * @author Joseph D. Darcy
 * @compile MissingTest.java A.java B.java C.java D.java Marker.java Missing.java MissingWrapper.java MissingDefault.java
 * @clean Missing
 * @run main MissingTest
 */

import java.lang.reflect.*;
import java.lang.annotation.*;

/**
 * This test verifies that a missing annotation class leads to the
 * expected exceptional behavior; a missing directly applied
 * annotation is currently ignored but a missing annotation value
 * inside another annotation throws an exception.
 *
 * To be run as intended, the annotation type Missing should *not* be
 * on the classpath when the test is run; with jtreg, it is deleted by
 * the @clean directive.
 */
public class MissingTest {
    /**
     * For the annotated element argument, get all its annotations and
     * see whether or not an exception is throw upon reading the
     * annotations.  Additionally, verify at least one annotation is
     * present.
     */
    private static void testAnnotation(AnnotatedElement element,
                                boolean exceptionExpected) {
        java.lang.annotation.Annotation[] annotations;
        try {
            annotations = element.getAnnotations();
            if (exceptionExpected) {
                System.err.println("Error: Did not get an exception reading annotations on "
                                   + element);
                System.err.println("Annotations found: "
                                   + java.util.Arrays.toString(annotations));
                throw new RuntimeException();
            }
            if (annotations.length == 0) {
                System.err.println("Error: no annotations found on " + element);
                throw new RuntimeException();
            }
        } catch (Throwable t) {
            if (!exceptionExpected) {
                System.err.println("Error: Got an unexpected exception reading annotations on "
                                   + element);
                throw new RuntimeException(t);
            }
        }
    }

    /**
     * For the annotated element argument, get all its annotations and
     * see whether or not an exception is throw upon reading the
     * annotations.  Additionally, verify at least one annotation is
     * present.
     */
    private static void testParameterAnnotation(Method m,
                                                boolean exceptionExpected) {
        java.lang.annotation.Annotation[][] annotationsArray;
        try {
            annotationsArray = m.getParameterAnnotations();
            if (exceptionExpected) {
                System.err.println("Error: Did not get an exception reading annotations on method"
                                   + m);
                System.err.println("Annotations found: "
                                   + java.util.Arrays.toString(annotationsArray));
                throw new RuntimeException();
            }
            if (annotationsArray.length == 0 ) {
                System.err.println("Error: no parameters for " + m);
                throw new RuntimeException();
            } else {
                java.lang.annotation.Annotation[] annotations = annotationsArray[0];
                if (annotations.length == 0) {
                    System.err.println("Error: no annotations on " + m);
                    throw new RuntimeException();
                }
            }
        } catch (Throwable t) {
            if (!exceptionExpected) {
                System.err.println("Error: Got an unexpected exception reading annotations on "
                                   + m);
                throw new RuntimeException(t);
            }
        }
    }

    private static void testMethodGetDefaultValue(Class<?> clazz) throws Exception{
        Method m = clazz.getMethod("value", (Class<?>[])null);

        try {
            System.out.println(m.getDefaultValue());
            throw new RuntimeException("Expected exception not thrown");
        } catch (TypeNotPresentException tnpe) {
            ; // Expected
        } catch (AnnotationFormatError afe) {
            throw new RuntimeException(afe);
        }
    }

    public static void main(String... args) throws Exception {
        // Class A has a directly applied annotation whose class is
        // missing.
        testAnnotation(A.class, false);

        // Class B has a directly applied annotation whose value
        // includes to an annotation class that is missing.
        testAnnotation(B.class, true);


        // Class C has a directly applied parameter annotation whose
        // class is missing.
        testParameterAnnotation(C.class.getDeclaredMethod("method1", Object.class),
                                false);

        // Class D has a directly applied parameter annotation whose value
        // includes to an annotation class that is missing.
        testParameterAnnotation(D.class.getDeclaredMethod("method1", Object.class),
                                true);
        // The MissingDefault annotation type has a default value of the Missing class.
        testMethodGetDefaultValue(MissingDefault.class);
    }
}
