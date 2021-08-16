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
 * @bug 4962341 6832557
 * @summary Check getEnclosingMethod method
 * @author Joseph D. Darcy
 */

import java.lang.reflect.Constructor;
import java.lang.annotation.*;

public class EnclosingConstructorTests {
    static Class<?> anonymousClass;
    static Class<?> localClass;
    static Class<?> anotherLocalClass;

    static {
        Cloneable c = new Cloneable() {}; // anonymous cloneable
        anonymousClass = c.getClass();
    }

    @ConstructorDescriptor("EnclosingConstructorTests()")
    EnclosingConstructorTests() {
        class Local {};
        Local l = new Local();
        localClass = l.getClass();
    }


    @ConstructorDescriptor("private EnclosingConstructorTests(int)")
    private EnclosingConstructorTests(int i) {
        class Local {};
        Local l = new Local();
        anotherLocalClass = l.getClass();
    }


    static int examine(Class<?> enclosedClass, String constructorSig) {
        Constructor<?> c = enclosedClass.getEnclosingConstructor();
        if (c == null && constructorSig == null)
            return 0;

        if (c != null &&
            c.getAnnotation(ConstructorDescriptor.class).value().equals(constructorSig))
            return 0; // everything is okay
        else {
            System.err.println("\nUnexpected constructor value; expected:\t" + constructorSig +
                               "\ngot:\t" + c);
            return 1;
        }
    }


    public static void main(String argv[]) {
        int failures = 0;
        class StaticLocal {};
        EnclosingConstructorTests ect = new EnclosingConstructorTests();
        ect = new EnclosingConstructorTests(5);

        failures += examine(StaticLocal.class,
                            null);

        failures += examine(localClass,
                             "EnclosingConstructorTests()");

        failures += examine(anotherLocalClass,
                            "private EnclosingConstructorTests(int)");

        failures += examine(anonymousClass,
                            null);

        if (failures > 0)
            throw new RuntimeException("Test failed.");
    }
}

@Retention(RetentionPolicy.RUNTIME)
@interface ConstructorDescriptor {
    String value();
}
