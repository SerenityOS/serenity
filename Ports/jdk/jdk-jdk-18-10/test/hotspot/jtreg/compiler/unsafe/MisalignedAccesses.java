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
 * @bug 8235385
 * @summary Crash on aarch64 JDK due to long offset
 *
 * @modules jdk.unsupported/sun.misc
 * @run testng/othervm -Diters=20000 -XX:-TieredCompilation  compiler.unsafe.MisalignedAccesses
 */

package compiler.unsafe;

import org.testng.annotations.Test;

import java.lang.reflect.Field;

import static org.testng.Assert.*;

public class MisalignedAccesses {
    static final int ITERS = Integer.getInteger("iters", 1);

    static final sun.misc.Unsafe UNSAFE;

    static final long BYTE_ARRAY_OFFSET;

    static final byte[] byteArray = new byte[4096];

    static {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get Unsafe instance.", e);
        }
        BYTE_ARRAY_OFFSET = UNSAFE.arrayBaseOffset(byte[].class);
    }

    @Test
    static long testBytes() {
        long sum = 0;
        sum += UNSAFE.getByte(byteArray, 1 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 38 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 75 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 112 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 149 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 186 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 223 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 260 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 297 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 334 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 371 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 408 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 445 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 482 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 519 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 556 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 593 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 630 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 667 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 704 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 741 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 778 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 815 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 852 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 889 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 926 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 963 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1000 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1037 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1074 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1111 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1148 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1185 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1222 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1259 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1296 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1333 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1370 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1407 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1444 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1481 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1518 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1555 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1592 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1629 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1666 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1703 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1740 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1777 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1814 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1851 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1888 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1925 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1962 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 1999 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2036 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2073 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2110 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2147 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2184 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2221 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2258 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2295 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2332 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2369 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2406 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2443 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2480 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2517 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2554 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2591 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2628 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2665 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2702 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2739 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2776 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2813 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2850 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2887 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2924 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2961 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 2998 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3035 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3072 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3109 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3146 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3183 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3220 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3257 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3294 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3331 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3368 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3405 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3442 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3479 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3516 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3553 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3590 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3627 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3664 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3701 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3738 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3775 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3812 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3849 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3886 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3923 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3960 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getByte(byteArray, 3997 + BYTE_ARRAY_OFFSET);
        return sum;
    }

    @Test
    static long testShorts() {
        long sum = 0;
        sum += UNSAFE.getShort(byteArray, 1 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 38 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 75 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 112 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 149 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 186 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 223 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 260 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 297 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 334 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 371 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 408 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 445 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 482 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 519 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 556 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 593 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 630 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 667 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 704 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 741 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 778 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 815 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 852 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 889 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 926 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 963 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1000 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1037 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1074 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1111 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1148 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1185 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1222 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1259 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1296 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1333 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1370 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1407 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1444 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1481 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1518 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1555 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1592 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1629 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1666 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1703 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1740 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1777 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1814 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1851 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1888 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1925 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1962 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 1999 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2036 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2073 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2110 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2147 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2184 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2221 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2258 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2295 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2332 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2369 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2406 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2443 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2480 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2517 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2554 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2591 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2628 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2665 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2702 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2739 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2776 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2813 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2850 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2887 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2924 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2961 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 2998 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3035 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3072 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3109 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3146 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3183 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3220 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3257 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3294 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3331 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3368 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3405 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3442 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3479 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3516 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3553 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3590 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3627 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3664 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3701 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3738 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3775 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3812 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3849 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3886 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3923 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3960 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getShort(byteArray, 3997 + BYTE_ARRAY_OFFSET);
        return sum;
    }

    @Test
    static long testInts() {
        long sum = 0;
        sum += UNSAFE.getInt(byteArray, 1 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 38 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 75 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 112 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 149 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 186 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 223 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 260 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 297 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 334 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 371 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 408 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 445 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 482 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 519 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 556 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 593 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 630 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 667 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 704 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 741 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 778 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 815 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 852 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 889 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 926 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 963 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1000 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1037 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1074 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1111 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1148 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1185 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1222 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1259 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1296 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1333 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1370 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1407 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1444 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1481 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1518 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1555 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1592 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1629 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1666 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1703 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1740 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1777 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1814 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1851 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1888 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1925 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1962 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 1999 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2036 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2073 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2110 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2147 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2184 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2221 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2258 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2295 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2332 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2369 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2406 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2443 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2480 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2517 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2554 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2591 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2628 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2665 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2702 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2739 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2776 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2813 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2850 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2887 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2924 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2961 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 2998 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3035 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3072 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3109 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3146 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3183 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3220 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3257 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3294 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3331 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3368 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3405 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3442 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3479 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3516 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3553 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3590 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3627 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3664 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3701 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3738 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3775 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3812 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3849 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3886 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3923 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3960 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getInt(byteArray, 3997 + BYTE_ARRAY_OFFSET);
        return sum;
    }

    @Test
    static long testLongs() {
        long sum = 0;
        sum += UNSAFE.getLong(byteArray, 1 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 38 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 75 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 112 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 149 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 186 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 223 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 260 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 297 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 334 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 371 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 408 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 445 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 482 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 519 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 556 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 593 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 630 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 667 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 704 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 741 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 778 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 815 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 852 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 889 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 926 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 963 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1000 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1037 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1074 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1111 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1148 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1185 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1222 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1259 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1296 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1333 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1370 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1407 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1444 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1481 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1518 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1555 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1592 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1629 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1666 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1703 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1740 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1777 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1814 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1851 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1888 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1925 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1962 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 1999 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2036 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2073 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2110 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2147 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2184 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2221 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2258 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2295 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2332 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2369 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2406 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2443 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2480 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2517 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2554 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2591 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2628 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2665 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2702 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2739 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2776 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2813 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2850 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2887 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2924 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2961 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 2998 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3035 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3072 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3109 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3146 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3183 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3220 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3257 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3294 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3331 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3368 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3405 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3442 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3479 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3516 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3553 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3590 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3627 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3664 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3701 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3738 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3775 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3812 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3849 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3886 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3923 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3960 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getLong(byteArray, 3997 + BYTE_ARRAY_OFFSET);
        return sum;
    }

    @Test
    static long testFloats() {
        long sum = 0;
        sum += UNSAFE.getFloat(byteArray, 1 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 38 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 75 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 112 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 149 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 186 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 223 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 260 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 297 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 334 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 371 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 408 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 445 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 482 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 519 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 556 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 593 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 630 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 667 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 704 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 741 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 778 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 815 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 852 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 889 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 926 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 963 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1000 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1037 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1074 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1111 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1148 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1185 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1222 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1259 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1296 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1333 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1370 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1407 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1444 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1481 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1518 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1555 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1592 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1629 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1666 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1703 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1740 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1777 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1814 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1851 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1888 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1925 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1962 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 1999 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2036 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2073 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2110 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2147 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2184 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2221 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2258 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2295 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2332 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2369 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2406 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2443 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2480 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2517 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2554 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2591 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2628 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2665 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2702 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2739 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2776 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2813 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2850 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2887 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2924 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2961 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 2998 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3035 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3072 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3109 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3146 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3183 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3220 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3257 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3294 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3331 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3368 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3405 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3442 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3479 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3516 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3553 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3590 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3627 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3664 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3701 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3738 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3775 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3812 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3849 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3886 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3923 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3960 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getFloat(byteArray, 3997 + BYTE_ARRAY_OFFSET);
        return sum;
    }

    @Test
    static long testDoubles() {
        long sum = 0;
        sum += UNSAFE.getDouble(byteArray, 1 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 38 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 75 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 112 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 149 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 186 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 223 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 260 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 297 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 334 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 371 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 408 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 445 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 482 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 519 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 556 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 593 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 630 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 667 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 704 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 741 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 778 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 815 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 852 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 889 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 926 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 963 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1000 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1037 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1074 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1111 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1148 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1185 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1222 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1259 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1296 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1333 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1370 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1407 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1444 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1481 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1518 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1555 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1592 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1629 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1666 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1703 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1740 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1777 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1814 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1851 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1888 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1925 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1962 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 1999 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2036 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2073 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2110 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2147 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2184 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2221 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2258 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2295 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2332 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2369 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2406 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2443 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2480 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2517 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2554 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2591 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2628 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2665 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2702 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2739 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2776 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2813 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2850 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2887 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2924 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2961 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 2998 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3035 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3072 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3109 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3146 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3183 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3220 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3257 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3294 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3331 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3368 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3405 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3442 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3479 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3516 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3553 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3590 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3627 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3664 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3701 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3738 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3775 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3812 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3849 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3886 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3923 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3960 + BYTE_ARRAY_OFFSET);
        sum += UNSAFE.getDouble(byteArray, 3997 + BYTE_ARRAY_OFFSET);
        return sum;
    }


    static volatile long result;

    public static void main(String[] args) {
        for (int i = 0; i < ITERS; i++) {
            result += testBytes();
            result += testShorts();
            result += testInts();
            result += testLongs();
            result += testFloats();
            result += testDoubles();
        }
    }
}


