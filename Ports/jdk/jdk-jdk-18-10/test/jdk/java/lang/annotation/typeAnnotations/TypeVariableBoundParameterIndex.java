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
 * @bug     8202473
 * @summary Annotations on type variables with multiple bounds should be placed on their respective bound
 * @compile  TypeVariableBoundParameterIndex.java
 * @run main TypeVariableBoundParameterIndex
 */

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.TypeVariable;
import java.util.concurrent.Callable;

/*
 * A class might have multiple bounds as parameterized types with type annotations on these bounds.
 * This test assures that these bound annotations are resolved correctly.
 */
public class TypeVariableBoundParameterIndex {

    public static void main(String[] args) throws Exception {
        TypeVariable<?>[] variables = Sample.class.getTypeParameters();

        for (int i = 0; i < 2; i++) {
            TypeVariable<?> variable = variables[i];
            AnnotatedType[] bounds = variable.getAnnotatedBounds();
            AnnotatedType bound = bounds[0];
            AnnotatedParameterizedType parameterizedType = (AnnotatedParameterizedType) bound;
            AnnotatedType[] actualTypeArguments = parameterizedType.getAnnotatedActualTypeArguments();
            Annotation[] annotations = actualTypeArguments[0].getAnnotations();
            if (annotations.length != 1 || annotations[0].annotationType() != TypeAnnotation.class) {
                throw new AssertionError();
            }
        }

        TypeVariable<?> variable = variables[2];
        AnnotatedType[] bounds = variable.getAnnotatedBounds();
        AnnotatedType bound = bounds[0];
        AnnotatedParameterizedType parameterizedType = (AnnotatedParameterizedType) bound;
        AnnotatedType[] actualTypeArguments = parameterizedType.getAnnotatedActualTypeArguments();
        Annotation[] annotations = actualTypeArguments[0].getAnnotations();
        if (annotations.length != 0) {
            throw new AssertionError();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE_USE)
    @interface TypeAnnotation { }

    static class Sample<T extends Callable<@TypeAnnotation ?>, S extends Callable<@TypeAnnotation ?>, U extends Callable<?>> { }
}
