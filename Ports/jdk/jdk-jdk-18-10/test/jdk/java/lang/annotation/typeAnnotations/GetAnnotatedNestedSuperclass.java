/*
 * Copyright (c) 2018, Google LLC. All rights reserved.
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
 * @bug 8066967 8198526
 * @summary Class.getAnnotatedSuperclass() does not correctly extract annotations
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.util.Arrays;
import java.util.Objects;

public class GetAnnotatedNestedSuperclass {

    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface A {}

    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface B {}

    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface C {}

    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface D {}

    @Target(ElementType.TYPE_USE)
    @Retention(RetentionPolicy.RUNTIME)
    @interface E {}

    static class X<P1, P2, P3> {}

    static class Y<P1, P2> extends @A X<@B P1, @C P2, @D Class<@E P1>> {}

    public static void main(String[] args) throws Exception {
        AnnotatedType x = Y.class.getAnnotatedSuperclass();
        assertEquals(Arrays.toString(x.getAnnotations()), "[@GetAnnotatedNestedSuperclass$A()]");
        AnnotatedParameterizedType xpt = (AnnotatedParameterizedType) x;
        {
            AnnotatedType arg = xpt.getAnnotatedActualTypeArguments()[0];
            assertEquals(
                    Arrays.toString(arg.getAnnotations()), "[@GetAnnotatedNestedSuperclass$B()]");
        }
        {
            AnnotatedType arg = xpt.getAnnotatedActualTypeArguments()[1];
            assertEquals(
                    Arrays.toString(arg.getAnnotations()), "[@GetAnnotatedNestedSuperclass$C()]");
        }
        {
            AnnotatedType arg = xpt.getAnnotatedActualTypeArguments()[2];
            assertEquals(
                    Arrays.toString(arg.getAnnotations()), "[@GetAnnotatedNestedSuperclass$D()]");
            AnnotatedType nestedArg =
                    ((AnnotatedParameterizedType) arg).getAnnotatedActualTypeArguments()[0];
            assertEquals(
                    Arrays.toString(nestedArg.getAnnotations()),
                    "[@GetAnnotatedNestedSuperclass$E()]");
        }
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("expected: " + expected + "; actual=" + actual);
        }
    }
}
