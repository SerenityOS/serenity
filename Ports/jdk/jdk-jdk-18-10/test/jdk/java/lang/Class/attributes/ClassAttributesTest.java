/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8170595
 * @summary Checks Class.isAnonymousClass, isMemberClass and isLocalClass
 *          attributes
 */

public class ClassAttributesTest {

    class NestedClass {}

    static int test(Class<?> clazz, boolean anonymous, boolean local, boolean member) {
        if (clazz.isAnonymousClass() != anonymous) {
            System.err.println("Unexpected isAnonymousClass value for " +
                               clazz.getName() + " expected: " + anonymous +
                               " got: " + (!anonymous));
            return 1;
        }
        if (clazz.isLocalClass() != local) {
            System.err.println("Unexpected isLocalClass value for " +
                               clazz.getName() + " expected: " + local +
                               " got: " + (!local));
            return 1;
        }
        if (clazz.isMemberClass() != member) {
            System.err.println("Unexpected isMemberClass status for " +
                               clazz.getName() + " expected: " + member +
                               " got: " + (!member));
            return 1;
        }
        return 0;
    }

    public static void main(String argv[]) {
        int failures = 0;

        class LocalClass {}
        Cloneable clone = new Cloneable() {};
        Runnable lambda = () -> System.out.println("run");

        failures += test(ClassAttributesTest.class,       false, false, false);
        failures += test(NestedClass.class,               false, false, true);
        failures += test(LocalClass.class,                false, true,  false);
        failures += test(clone.getClass(),                true,  false, false);

        // Lambdas may be hidden classes, but are named, non-local classes
        // in this sense
        failures += test(lambda.getClass(),               false, false, false);

        if (failures != 0)
            throw new RuntimeException("Test failed with " + failures  + " failures.");
    }
}
