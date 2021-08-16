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
 * @bug 8055063
 * @summary javac should generate method parameters correctly.
 * @clean InnerClassToString
 * @compile -parameters InnerClassToString.java
 * @run main InnerClassToString
 * @clean InnerClassToString
 * @compile InnerClassToString.java
 * @run main InnerClassToString
 */

import java.lang.reflect.Constructor;
import java.lang.reflect.Parameter;
import java.util.Set;

// Test copied and expanded from webbug group report.
public class InnerClassToString {
    private static final Class<?>[] genericParamClasses = new Class<?>[] {
        InnerClassToString.class, Set.class
    };

    private static final Class<?>[] nongenericParamClasses = new Class<?>[] {
        InnerClassToString.class, String.class
    };

    private int errors = 0;

    private void test(Constructor<MyEntity> constructor,
                     Class<?>[] paramClasses) {
        final Parameter[] params = constructor.getParameters();

        for (int i = 0; i < params.length; i++) {
            final Parameter parameter = params[i];
            System.out.println(parameter.toString());

            if (!parameter.getType().equals(paramClasses[i])) {
                errors++;
                System.err.println("Expected type " + paramClasses[i] +
                                   " but got " + parameter.getType());
            }

            System.out.println(parameter.getParameterizedType());
            System.out.println(parameter.getAnnotatedType());
        }

    }

    private void run() throws Exception {
        final Constructor<MyEntity> genericConstructor =
            MyEntity.class.getConstructor(InnerClassToString.class, Set.class);

        test(genericConstructor, genericParamClasses);

        final Constructor<MyEntity> nongenericConstructor =
            MyEntity.class.getConstructor(InnerClassToString.class, String.class);

        test(nongenericConstructor, nongenericParamClasses);

        if (errors != 0)
            throw new RuntimeException(errors + " errors in test");
    }

    public static void main(String[] args) throws Exception {
        new InnerClassToString().run();
    }

    public class MyEntity {
        public MyEntity(Set<?> names) {}
        public MyEntity(String names) {}
    }
}
