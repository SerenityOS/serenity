/*
 * Copyright (c) 2004, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5033583 8161500
 * @summary Check toGenericString() method
 * @author Joseph D. Darcy
 */

import java.lang.reflect.*;
import java.lang.annotation.*;
import java.util.*;

public class GenericStringTest {
    public static void main(String argv[]) throws Exception {
        int failures = 0;

        for(Class clazz: List.of(TestClass1.class, TestClass2.class))
            for(Field field: clazz.getDeclaredFields()) {
                ExpectedString es = field.getAnnotation(ExpectedString.class);
                String genericString = field.toGenericString();
                System.out.println(genericString);
                if (! es.value().equals(genericString)) {
                    failures ++;
                    System.err.printf("ERROR: Expected ''%s''; got ''%s''.\n",
                                      es.value(), genericString);
                }
            }

        if (failures > 0) {
            System.err.println("Test failed.");
            throw new RuntimeException();
        }
    }
}

class TestClass1 {
    @ExpectedString("int TestClass1.field1")
    int field1;

    @ExpectedString("private static java.lang.String TestClass1.field2")
    private static String field2;
}

class TestClass2<E> {
    @ExpectedString("public E TestClass2.field1")
    public E field1;
}

@Retention(RetentionPolicy.RUNTIME)
@interface ExpectedString {
    String value();
}
