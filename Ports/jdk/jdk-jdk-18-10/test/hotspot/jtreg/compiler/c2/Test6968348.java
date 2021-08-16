/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6968348
 * @summary Byteswapped memory access can point to wrong location after JIT
 * @modules java.base/jdk.internal.misc
 *
 * @run main compiler.c2.Test6968348
 */

package compiler.c2;

import jdk.internal.misc.Unsafe;

import java.lang.reflect.Field;

public class Test6968348 {
    static Unsafe unsafe = Unsafe.getUnsafe();
    static final long[] buffer = new long[4096];
    static int array_long_base_offset;

    public static void main(String[] args) throws Exception {
        array_long_base_offset = unsafe.arrayBaseOffset(long[].class);

        for (int n = 0; n < 100000; n++) {
            test();
        }
    }

    public static void test() {
        for (long i = array_long_base_offset; i < 4096; i += 8) {
            unsafe.putLong(buffer, i, Long.reverseBytes(i));
        }
    }
}
