/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8054987
 * @summary Test sharing of annotations between Executable/Field instances.
 *          Sharing should not be noticeable when performing mutating
 *          operations.
 * @run testng AnnotationSharing
 */

import java.lang.annotation.*;
import java.lang.reflect.*;

import org.testng.annotations.Test;

public class AnnotationSharing {
    @Test
    public void testMethodSharing() throws Exception {
        Method[] m1 = AnnotationSharing.class.getMethods();
        Method[] m2 = AnnotationSharing.class.getMethods();
        validateSharingSafelyObservable(m1, m2);
    }

    @Test
    public void testDeclaredMethodSharing() throws Exception {
        Method[] m3 = AnnotationSharing.class.getDeclaredMethods();
        Method[] m4 = AnnotationSharing.class.getDeclaredMethods();
        validateSharingSafelyObservable(m3, m4);
    }

    @Test
    public void testFieldSharing() throws Exception {
        Field[] f1 = AnnotationSharing.class.getFields();
        Field[] f2 = AnnotationSharing.class.getFields();
        validateSharingSafelyObservable(f1, f2);
    }

    @Test
    public void testDeclaredFieldsSharing() throws Exception {
        Field[] f3 = AnnotationSharing.class.getDeclaredFields();
        Field[] f4 = AnnotationSharing.class.getDeclaredFields();
        validateSharingSafelyObservable(f3, f4);
    }

    @Test
    public void testMethodSharingOccurs() throws Exception {
        Method mm1 = AnnotationSharing.class.getDeclaredMethod("m", (Class<?>[])null);
        Method mm2 = AnnotationSharing.class.getDeclaredMethod("m", (Class<?>[])null);
        validateAnnotationSharing(mm1, mm2);
    }

    @Test
    public void testMethodSharingIsSafe() throws Exception {
        Method mm1 = AnnotationSharing.class.getDeclaredMethod("m", (Class<?>[])null);
        Method mm2 = AnnotationSharing.class.getDeclaredMethod("m", (Class<?>[])null);
        validateAnnotationSharingIsSafe(mm1, mm2);
        validateArrayValues(mm1.getAnnotation(Baz.class), mm2.getAnnotation(Baz.class));
    }

    @Test
    public void testFieldSharingOccurs() throws Exception {
        Field ff1 = AnnotationSharing.class.getDeclaredField("f");
        Field ff2 = AnnotationSharing.class.getDeclaredField("f");
        validateAnnotationSharing(ff1, ff2);
    }

    @Test
    public void testFieldSharingIsSafe() throws Exception {
        Field ff1 = AnnotationSharing.class.getDeclaredField("f");
        Field ff2 = AnnotationSharing.class.getDeclaredField("f");
        validateAnnotationSharingIsSafe(ff1, ff2);
        validateArrayValues(ff1.getAnnotation(Baz.class), ff2.getAnnotation(Baz.class));
    }

    // Validate that AccessibleObject instances are not shared
    private static void validateSharingSafelyObservable(AccessibleObject[] m1, AccessibleObject[] m2)
            throws Exception {

        // Validate that setAccessible works
        for (AccessibleObject m : m1)
            m.setAccessible(false);

        for (AccessibleObject m : m2)
            m.setAccessible(true);

        for (AccessibleObject m : m1)
            if (m.isAccessible())
                throw new RuntimeException(m + " should not be accessible");

        for (AccessibleObject m : m2)
            if (!m.isAccessible())
                throw new RuntimeException(m + " should be accessible");

        // Validate that methods are still equal()
        for (int i = 0; i < m1.length; i++)
            if (!m1[i].equals(m2[i]))
                throw new RuntimeException(m1[i] + " and " + m2[i] + " should be equal()");

        // Validate that the arrays aren't shared
        for (int i = 0; i < m1.length; i++)
            m1[i] = null;

        for (int i = 0; i < m2.length; i++)
            if (m2[i] == null)
                throw new RuntimeException("Detected sharing of AccessibleObject arrays");
    }

    // Validate that annotations are shared
    private static void validateAnnotationSharing(AccessibleObject m1, AccessibleObject m2) {
        Bar b1 = m1.getAnnotation(Bar.class);
        Bar b2 = m2.getAnnotation(Bar.class);

        if (b1 != b2)
            throw new RuntimeException(b1 + " and " + b2 + " should be ==");

    }

    // Validate that Method instances representing the annotation elements
    // behave as intended
    private static void validateAnnotationSharingIsSafe(AccessibleObject m1, AccessibleObject m2)
            throws Exception {
        Bar b1 = m1.getAnnotation(Bar.class);
        Bar b2 = m2.getAnnotation(Bar.class);

        Method mm1 = b1.annotationType().getMethod("value", (Class<?>[]) null);
        Method mm2 = b2.annotationType().getMethod("value", (Class<?>[]) null);
        inner(mm1, mm2);

        mm1 = b1.getClass().getMethod("value", (Class<?>[]) null);
        mm2 = b2.getClass().getMethod("value", (Class<?>[]) null);
        inner(mm1, mm2);

    }
    private static void inner(Method mm1, Method mm2)
            throws Exception {
        if (!mm1.equals(mm2))
            throw new RuntimeException(mm1 + " and " + mm2 + " should be equal()");

        mm1.setAccessible(false);
        mm2.setAccessible(true);

        if (mm1.isAccessible())
            throw new RuntimeException(mm1 + " should not be accessible");

        if (!mm2.isAccessible())
            throw new RuntimeException(mm2 + " should be accessible");
    }

    // Validate that array element values are not shared
    private static void validateArrayValues(Baz a, Baz b) {
        String[] s1 = a.value();
        String[] s2 = b.value();

        s1[0] = "22";

        if (!s2[0].equals("1"))
            throw new RuntimeException("Mutation of array elements should not be detectable");
    }

    @Foo @Bar("val") @Baz({"1", "2"})
    public void m() {
        return ;
    }

    @Foo @Bar("someValue") @Baz({"1", "22", "33"})
    public Object f = new Object();
}

@Retention(RetentionPolicy.RUNTIME)
@interface Foo {}

@Retention(RetentionPolicy.RUNTIME)
@interface Bar {
    String value();
}

@Retention(RetentionPolicy.RUNTIME)
@interface Baz {
    String [] value();
}
