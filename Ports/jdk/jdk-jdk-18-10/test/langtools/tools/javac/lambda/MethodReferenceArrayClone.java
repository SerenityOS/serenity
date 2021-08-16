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
 * @bug 8056051
 * @summary int[]::clone causes "java.lang.NoClassDefFoundError: Array"
 * @run main MethodReferenceArrayClone
 */

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Supplier;

public class MethodReferenceArrayClone {
    public static void main(String[] args) {
        int[] intArgs = new int[] {1, 2, 3, 4, 5};
        checkInt("int[]::clone", int[]::clone, intArgs);
        checkInt("a -> a.clone()", a -> a.clone(), intArgs);
        checkInt("intArgs::clone", intArgs::clone, intArgs);

        String[] stringArgs = new String[] {"hi", "de", "ho"};
        checkString("String[]::clone", String[]::clone, stringArgs);
        checkString("a -> a.clone()", a -> a.clone(), stringArgs);
        checkString("args::clone", stringArgs::clone, stringArgs);
    }

    private static void checkInt(String label, Supplier<int[]> s, int[] expected) {
        if (!Arrays.equals(s.get(), expected)) {
            throw new RuntimeException("Unexpected value " + label + ": " + Arrays.toString(s.get()));
        }
    }

    private static void checkInt(String label, Function<int[], int[]> f, int[] a) {
        checkInt(label, () -> f.apply(a), a);
    }

    private static void checkString(String label, Supplier<String[]> s, String[] expected) {
        if (!Arrays.equals(s.get(), expected)) {
            throw new RuntimeException("Unexpected value " + label + ": " + Arrays.toString(s.get()));
        }
    }

    private static void checkString(String label, Function<String[], String[]> f, String[] a) {
        checkString(label, () -> f.apply(a), a);
    }
}
