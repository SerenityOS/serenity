/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8202471
 * @summary A constructor's parameterized receiver type's type variables can be type annotated
 */

import java.lang.annotation.Target;
import java.lang.annotation.*;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.Constructor;

public class TestReceiverTypeParameterizedConstructor<T> {

    public static void main(String[] args) throws NoSuchMethodException {
        doAssert(TestReceiverTypeParameterizedConstructor.Inner.class);
        doAssert(TestReceiverTypeParameterizedConstructor.Inner.Inner2.class);
    }

    private static void doAssert(Class<?> c) throws NoSuchMethodException {
        Constructor<?> constructor = c.getDeclaredConstructor(c.getDeclaringClass());
        AnnotatedType receiverType = constructor.getAnnotatedReceiverType();
        AnnotatedParameterizedType parameterizedType = (AnnotatedParameterizedType) receiverType;
        int count = 0;
        do {
            AnnotatedType[] arguments = parameterizedType.getAnnotatedActualTypeArguments();
            Annotation[] annotations = arguments[0].getAnnotations();
            if (annotations.length != 1
                    || !(annotations[0] instanceof TypeAnnotation)
                    || ((TypeAnnotation) annotations[0]).value() != count++) {
                throw new AssertionError();
            }
            parameterizedType = (AnnotatedParameterizedType) parameterizedType.getAnnotatedOwnerType();
        } while (parameterizedType != null);
    }

    class Inner<S> {
        Inner(TestReceiverTypeParameterizedConstructor<@TypeAnnotation(0) T> TestReceiverTypeParameterizedConstructor.this) { }

        class Inner2 {
            Inner2(TestReceiverTypeParameterizedConstructor<@TypeAnnotation(1) T>.Inner<@TypeAnnotation(0) S> TestReceiverTypeParameterizedConstructor.Inner.this) { }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    @interface TypeAnnotation {
        int value();
    }
}
