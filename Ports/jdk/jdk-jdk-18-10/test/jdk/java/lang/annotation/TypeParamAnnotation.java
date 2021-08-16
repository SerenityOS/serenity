/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8004698 8007278
 * @summary Unit test for annotations on TypeVariables
 */

import java.util.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.io.Serializable;

public class TypeParamAnnotation {
    public static void main(String[] args) throws Exception {
        testOnClass();
        testOnMethod();
        testGetAnno();
        testGetAnnos();
    }

    private static void check(boolean b) {
        if (!b)
            throw new RuntimeException();
    }

    private static void testOnClass() {
        TypeVariable<?>[] ts = TypeParam.class.getTypeParameters();
        check(ts.length == 3);

        Annotation[] as;

        as = ts[0].getAnnotations();
        check(as.length == 2);
        check(((ParamAnno)as[0]).value().equals("t"));
        check(((ParamAnno2)as[1]).value() == 1);

        as = ts[1].getAnnotations();
        check(as.length == 0);

        as = ts[2].getAnnotations();
        check(as.length == 2);
        check(((ParamAnno)as[0]).value().equals("v"));
        check(((ParamAnno2)as[1]).value() == 2);
    }
    private static void testOnMethod() throws Exception {
        TypeVariable<?>[] ts = TypeParam.class.getDeclaredMethod("foo").getTypeParameters();
        check(ts.length == 3);

        Annotation[] as;

        as = ts[0].getAnnotations();
        check(as.length == 2);
        check(((ParamAnno)as[0]).value().equals("x"));
        check(((ParamAnno2)as[1]).value() == 3);

        as = ts[1].getAnnotations();
        check(as.length == 0);

        as = ts[2].getAnnotations();
        check(as.length == 2);
        check(((ParamAnno)as[0]).value().equals("z"));
        check(((ParamAnno2)as[1]).value() == 4);
    }

    private static void testGetAnno() {
        TypeVariable<?>[] ts = TypeParam.class.getTypeParameters();
        ParamAnno a;
        a = ts[0].getAnnotation(ParamAnno.class);
        check(a.value().equals("t"));
    }
    private static void testGetAnnos() throws Exception {
        TypeVariable<?>[] ts = TypeParam.class.getDeclaredMethod("foo").getTypeParameters();
        ParamAnno2[] as;
        as = ts[0].getAnnotationsByType(ParamAnno2.class);
        check(as.length == 1);
        check(as[0].value() == 3);
    }
}

class TypeParam <@ParamAnno("t") @ParamAnno2(1) T,
                 U,
                 @ParamAnno("v") @ParamAnno2(2) V extends Runnable> {
    public <@ParamAnno("x") @ParamAnno2(3) X,
            Y,
            @ParamAnno("z") @ParamAnno2(4) Z extends Cloneable> void foo() {}
}

@Target(ElementType.TYPE_PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@interface ParamAnno {
    String value();
}

@Target(ElementType.TYPE_PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@interface ParamAnno2 {
    int value();
}
