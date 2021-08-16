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
 * @bug 8253566
 * @summary clazz.isAssignableFrom will return false for interface implementors
 * @requires vm.compiler2.enabled
 *
 * @run main/othervm -XX:-BackgroundCompilation TestSubTypeCheckMacroTrichotomy
 *
 */

public class TestSubTypeCheckMacroTrichotomy {
    public static void main(String[] args) {
        for (int i = 0; i < 20_000; i++) {
            final int res1 = test(A.class, B.class);
            final int res2 = test(B.class, A.class);
            final int res3 = test(A.class, C.class);
            if (res1 != 0 || res2 != 1 || res3 != 0) {
                throw new RuntimeException("test(A, B) = " + res1 + " test(B, A) = " + res2 + " test(A, C) = " + res3);
            }
        }
    }

    private static int test(Class<?> c1, Class<?> c2) {
        if (c1 == null) {
        }
        if (c2 == null) {
        }
        int res = 0;
        if (!c1.isAssignableFrom(c2)) {
            if (c2.isAssignableFrom(c1)) {
                res = 1;
            }
        }
        return res;
    }

    private static class A {
    }

    private static class B extends A {
    }

    private static class C {
    }
}
