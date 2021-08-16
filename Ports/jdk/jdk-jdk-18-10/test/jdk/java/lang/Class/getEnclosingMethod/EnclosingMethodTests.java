/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4962341
 * @summary Check getEnclosingMethod method
 * @author Joseph D. Darcy
 */

import java.lang.reflect.Method;
import java.lang.annotation.*;

public class EnclosingMethodTests {
    static Class<?> anonymousClass;

    static {
        Cloneable c = new Cloneable() {}; // anonymous cloneable
        anonymousClass = c.getClass();
    }

    EnclosingMethodTests() {}

    @MethodDescriptor("java.lang.Class EnclosingMethodTests.getLocalClass(Object o)")
    Class getLocalClass(Object o) {
        class Local {};
        Local l = new Local();
        return l.getClass();
    }

    static int examine(Class enclosedClass, String methodSig) {
        Method m = enclosedClass.getEnclosingMethod();
        if (m == null && methodSig == null)
            return 0;

        if (m != null &&
            m.getAnnotation(MethodDescriptor.class).value().equals(methodSig))
            return 0; // everything is okay
        else {
            System.err.println("\nUnexpected method value; expected:\t" + methodSig +
                               "\ngot:\t" + m);
            return 1;
        }
    }

    @MethodDescriptor("public static void EnclosingMethodTests.main(java.lang.String[])")
    public static void main(String argv[]) {
        int failures = 0;
        class StaticLocal {};

        failures += examine(StaticLocal.class,
                            "public static void EnclosingMethodTests.main(java.lang.String[])");

        failures += examine( (new EnclosingMethodTests()).getLocalClass(null),
                             "java.lang.Class EnclosingMethodTests.getLocalClass(Object o)");

        failures += examine(EnclosingMethodTests.class, null);

        failures += examine(anonymousClass, null);

        if (failures > 0)
            throw new RuntimeException("Test failed.");
    }
}

@Retention(RetentionPolicy.RUNTIME)
@interface MethodDescriptor {
    String value();
}
