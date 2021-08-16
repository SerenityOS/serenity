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
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/*
 * @test
 * @bug 7183985
 * @summary getAnnotation() should throw NoClassDefFoundError when an annotation class is not
 *          present at runtime
 * @compile MissingAnnotationArrayElementTest.java MissingAnnotation.java
 * @clean MissingAnnotation
 * @run main MissingAnnotationArrayElementTest
 */
public class MissingAnnotationArrayElementTest {

    @Retention(RUNTIME)
    @interface AnnotationAnnotation {
        MissingAnnotation[] value();
    }

    @AnnotationAnnotation({@MissingAnnotation, @MissingAnnotation})
    static class Test {
        void f(@AnnotationAnnotation({@MissingAnnotation, @MissingAnnotation}) int x) {}

        Test(@AnnotationAnnotation({@MissingAnnotation, @MissingAnnotation}) int x) {}
    }

    public static void main(String[] args) throws Exception {
        // MissingAnnotation will be absent from the runtime classpath, causing a
        // NoClassDefFoundError when AnnotationAnnotation is read (since the type of its value array
        // references cannot be completed).
        assertThrowsNoClassDefFoundError(
                () -> Test.class.getAnnotation(AnnotationAnnotation.class));
        Method method = Test.class.getDeclaredMethod("f", int.class);
        assertThrowsNoClassDefFoundError(method::getParameterAnnotations);
        Constructor constructor = Test.class.getDeclaredConstructor(int.class);
        assertThrowsNoClassDefFoundError(constructor::getParameterAnnotations);
    }

    interface ThrowingRunnable {
        void run() throws Exception;
    }

    static void assertThrowsNoClassDefFoundError(ThrowingRunnable throwingRunnable)
            throws Exception {
        try {
            throwingRunnable.run();
            throw new AssertionError("expected exception");
        } catch (NoClassDefFoundError expected) {
            if (!expected.getMessage().contains("MissingAnnotation")) {
                throw expected;
            }
        }
    }
}
