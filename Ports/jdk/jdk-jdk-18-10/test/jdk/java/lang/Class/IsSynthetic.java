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
 * @bug 5012133
 * @summary Check Class.isSynthetic method
 * @author Joseph D. Darcy
 */

import java.lang.reflect.*;

public class IsSynthetic {

    static class NestedClass {
    }

    static int test(Class<?> clazz, boolean expected) {
        if (clazz.isSynthetic() == expected)
            return 0;
        else {
            System.err.println("Unexpected synthetic status for " +
                               clazz.getName() + " expected: " + expected +
                               " got: " + (!expected));
            return 1;
        }
    }

    public static void main(String argv[]) {
        int failures = 0;
        class LocalClass {}

        Cloneable clone = new Cloneable() {};

        failures += test(IsSynthetic.class,             false);
        failures += test(java.lang.String.class,        false);
        failures += test(LocalClass.class,              false);
        failures += test(NestedClass.class,             false);
        failures += test(clone.getClass(),              false);

        for(Constructor c: Tricky.class.getDeclaredConstructors()) {
            Class<?>[] paramTypes = c.getParameterTypes();
            if (paramTypes.length > 0) {
                System.out.println("Testing class that should be synthetic.");
                for(Class paramType: paramTypes) {
                    failures += test(paramType, true);
                }
            }
        }

        if (failures != 0)
            throw new RuntimeException("Test failed with " + failures  + " failures.");
    }
}

class Tricky {
    private Tricky() {}

    public static class Nested {
        Tricky t = new Tricky();
    }
}
