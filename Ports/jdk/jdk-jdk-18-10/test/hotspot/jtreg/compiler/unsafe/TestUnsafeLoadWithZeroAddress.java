/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8224658
 * @summary Test compilation of unsafe access with zero address.
 * @modules java.base/jdk.internal.misc:+open
 * @run main/othervm -Xcomp
 *                   -XX:CompileCommand=compileonly,compiler.unsafe.TestUnsafeLoadWithZeroAddress::*
 *                   compiler.unsafe.TestUnsafeLoadWithZeroAddress
 * @run main/othervm -Xcomp -XX:+IgnoreUnrecognizedVMOptions -XX:+AlwaysIncrementalInline
 *                   -XX:CompileCommand=compileonly,compiler.unsafe.TestUnsafeLoadWithZeroAddress::*
 *                   compiler.unsafe.TestUnsafeLoadWithZeroAddress
 */

package compiler.unsafe;

import jdk.internal.misc.Unsafe;

public class TestUnsafeLoadWithZeroAddress {
    static final Unsafe UNSAFE = Unsafe.getUnsafe();
    static boolean f;

    public static void test1() {
        if (f) {
            // This branch is never executed but compiled due to -Xcomp
            UNSAFE.getInt(0);
        }
    }

    public static void test2() {
        if (f) {
            // This branch is never executed but compiled due to -Xcomp
            UNSAFE.putInt(0, 0);
        }
    }

    private static int getAddress() {
        return 0;
    }

    public static void test3() {
        if (f) {
            // This branch is never executed but compiled due to -Xcomp
            UNSAFE.getInt(getAddress());
        }
    }

    public static void test4() {
        if (f) {
            // This branch is never executed but compiled due to -Xcomp
            UNSAFE.putInt(getAddress(), 0);
        }
    }

    static public void main(String[] args) {
        test1();
        test2();
        test3();
        test4();
    }
}

