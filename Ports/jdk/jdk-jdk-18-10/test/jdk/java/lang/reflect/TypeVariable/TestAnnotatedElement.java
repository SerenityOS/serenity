/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7086192
 * @summary Verify functionality of AnnotatedElement methods on type variables
 * @author Joseph D. Darcy
 */

import java.lang.reflect.*;
import java.lang.annotation.*;

public class TestAnnotatedElement<A> {
    // Type variable on a method
    private static <B> B m(B b) {return null;}

    // Type variable on a construtor
    private <C> TestAnnotatedElement(){super();}

    public static void main(String... argv) throws ReflectiveOperationException {
        int errors = 0;

        Class<?> clazz = TestAnnotatedElement.class;
        errors += testTypeVariable(clazz.getTypeParameters());
        errors += testTypeVariable(clazz.getDeclaredConstructor().getTypeParameters());
        errors += testTypeVariable(clazz.getDeclaredMethod("m", Object.class).getTypeParameters());

        if (errors > 0)
            throw new RuntimeException(errors + " failures");
    }


    private static int testTypeVariable(TypeVariable<?>[] typeVars) {
        int errors = 0;
        if (typeVars.length == 0)
            return ++errors;

        for(TypeVariable<?> typeVar : typeVars) {
            try {
                typeVar.getAnnotation(null);
                errors++;
            } catch(NullPointerException npe) {
                ; // Expected
            }

            if (typeVar.getAnnotation(SuppressWarnings.class) != null)
                errors++;

            try {
                typeVar.isAnnotationPresent(null);
                errors++;
            } catch(NullPointerException npe) {
                ; // Expected
            }

            if (typeVar.isAnnotationPresent(SuppressWarnings.class))
                errors++;

            if(typeVar.getAnnotations().length != 0)
                errors++;

            if(typeVar.getDeclaredAnnotations().length != 0)
                errors++;
        }
        return errors;
    }
}
