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
 * @test 8242529
 * @summary javac defines type annotations incorrectly for record members (constructor and property accessor)
 * @modules jdk.compiler/com.sun.tools.javac.util
 */

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.*;
import java.util.concurrent.Callable;
import com.sun.tools.javac.util.Assert;

public record RecordNotPreservingNestedTypeAnnotationsTest(@RegularAnnotation @TypeAnnotation Callable<@TypeAnnotation ?> foo) {
    public static void main(String[] args) throws Exception {
        RecordComponent recordComponent = RecordNotPreservingNestedTypeAnnotationsTest.class.getRecordComponents()[0];
        checkAnnotations(recordComponent.getAnnotations(), recordComponent.getAnnotatedType());

        Method accessor = recordComponent.getAccessor();
        checkAnnotations(accessor.getAnnotations(), accessor.getAnnotatedReturnType());

        Constructor<?> constructor = RecordNotPreservingNestedTypeAnnotationsTest.class.getConstructor(Callable.class);
        checkAnnotations(constructor.getParameterAnnotations()[0], constructor.getAnnotatedParameterTypes()[0]);

        Field field = RecordNotPreservingNestedTypeAnnotationsTest.class.getDeclaredField(recordComponent.getName());
        checkAnnotations(field.getAnnotations(), field.getAnnotatedType());
    }

    static void checkAnnotations(Annotation[] decAnnotations, AnnotatedType annoType) {
        Assert.check(decAnnotations.length == 1);
        Assert.check(decAnnotations[0] instanceof RegularAnnotation);

        Assert.check(annoType.getAnnotations()[0] instanceof TypeAnnotation);
        var annoTypeArgs = ((AnnotatedParameterizedType) annoType).getAnnotatedActualTypeArguments();
        Assert.check(annoTypeArgs.length == 1);
        Assert.check(annoTypeArgs[0].getAnnotations()[0] instanceof TypeAnnotation);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE_USE})
    @interface TypeAnnotation {}

    @Retention(RetentionPolicy.RUNTIME)
    @interface RegularAnnotation {}
}
