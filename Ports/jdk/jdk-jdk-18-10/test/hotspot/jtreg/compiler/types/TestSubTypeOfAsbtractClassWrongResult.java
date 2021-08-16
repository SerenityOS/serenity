/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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

/**
 * @test
 * @bug 8240195
 * @summary subtype check with abstract class that has no children classes can sometimes be constant folded
 * @requires vm.compiler2.enabled
 *
 * @run main/othervm -XX:-BackgroundCompilation TestSubTypeOfAsbtractClassWrongResult
 *
 */


public class TestSubTypeOfAsbtractClassWrongResult {
    public static void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            if (!test1(A.class)) {
                throw new RuntimeException("Wrong result");
            }
            test2(new Object());
            test3(new Exception());
        }
    }

    private static boolean test1(Class c) {
        return A.class.isAssignableFrom(c);
    }

    private static boolean test2(Object o) {
        return o instanceof A;
    }

    private static void test3(Exception e) {
        try {
            throw e;
        } catch (A ex1) {
        } catch (Exception ex2) {
        }
    }

    static abstract class A extends Exception {
    }
}
