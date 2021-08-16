/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 * @bug 8147585
 * @summary Check Annotation with Lambda, with or without parameter
 * @run testng AnnotationWithLambda
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.function.Consumer;

import org.testng.annotations.*;
import static org.testng.Assert.*;

public class AnnotationWithLambda {

    @Test
    public void testAnnotationWithLambda() {
        Method[] methods = AnnotationWithLambda.MethodsWithAnnotations.class.getDeclaredMethods();
        for (Method method : methods) {
            assertTrue((method.isAnnotationPresent(LambdaWithParameter.class)) &&
                       (method.isAnnotationPresent(LambdaWithoutParameter.class)));

        }
    }

    static class MethodsWithAnnotations {

        @LambdaWithParameter
        @LambdaWithoutParameter
        public void testAnnotationLambda() {
        }
    }
}

@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface LambdaWithParameter {
    Consumer<Integer> f1 = a -> {
        System.out.println("lambda has parameter");
    };
}

@Target(value = ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@interface LambdaWithoutParameter {
    Runnable r = () -> System.out.println("lambda without parameter");
}

