/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8263763
 * @summary Check that annotations on an enum constructor are indexed correctly.
 */

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.util.Arrays;

public class EnumConstructorAnnotation {

    public static void main(String[] args) {
        Constructor<?> c = SampleEnum.class.getDeclaredConstructors()[0];
        Annotation[] a1 = c.getParameters()[2].getAnnotations(), a2 = c.getParameterAnnotations()[2];
        for (Annotation[] a : Arrays.asList(a1, a2)) {
            if (a.length != 1) {
                throw new RuntimeException("Unexpected length " + a.length);
            } else if (a[0].annotationType() != SampleAnnotation.class) {
                throw new RuntimeException("Unexpected type " + a[0]);
            }
        }
    }

    enum SampleEnum {
        INSTANCE("foo");
        SampleEnum(@SampleAnnotation String value) { }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface SampleAnnotation { }
}
