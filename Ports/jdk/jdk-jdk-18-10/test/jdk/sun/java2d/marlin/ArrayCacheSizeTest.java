/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import sun.java2d.marlin.ArrayCacheConst;

/*
 * @test
 * @bug 8144445
 * @summary Check the ArrayCache getNewLargeSize() method
 * @run main ArrayCacheSizeTest
 * @modules java.desktop/sun.java2d.marlin
 */
public class ArrayCacheSizeTest {

    public static void main(String[] args) {
        testNewSize();
        testNewLargeSize();
    }

    private static void testNewSize() {
        testNewSize(0, 1);
        testNewSize(0, 100000);

        testNewSize(4096, 4097);
        testNewSize(4096 * 16, 4096 * 16 + 1);

        testNewSize(4096 * 4096 * 4, 4096 * 4096 * 4 + 1);

        testNewSize(4096 * 4096 * 4, Integer.MAX_VALUE);

        testNewSize(Integer.MAX_VALUE - 1000, Integer.MAX_VALUE);

        testNewSizeExpectAIOB(Integer.MAX_VALUE - 1000, Integer.MAX_VALUE + 1);
        testNewSizeExpectAIOB(1, -1);
        testNewSizeExpectAIOB(Integer.MAX_VALUE, -1);
    }

    private static void testNewSizeExpectAIOB(final int curSize,
                                              final int needSize) {
        try {
            testNewSize(curSize, needSize);
            throw new RuntimeException("ArrayIndexOutOfBoundsException not thrown");
        } catch (ArrayIndexOutOfBoundsException aiobe) {
            System.out.println("ArrayIndexOutOfBoundsException expected.");
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable th) {
            throw new RuntimeException("Unexpected exception", th);
        }
    }

    private static void testNewSize(final int curSize,
                                    final int needSize) {

        int size = ArrayCacheConst.getNewSize(curSize, needSize);

        System.out.println("getNewSize(" + curSize + ", " + needSize
            + ") = " + size);

        if (size < 0 || size < needSize) {
            throw new IllegalStateException("Invalid getNewSize("
                + curSize + ", " + needSize + ") = " + size + " !");
        }
    }

    private static void testNewLargeSize() {
        testNewLargeSize(0, 1);
        testNewLargeSize(0, 100000);

        testNewLargeSize(4096, 4097);
        testNewLargeSize(4096 * 16, 4096 * 16 + 1);

        testNewLargeSize(4096 * 4096 * 4, 4096 * 4096 * 4 + 1);

        testNewLargeSize(4096 * 4096 * 4, Integer.MAX_VALUE);

        testNewLargeSize(Integer.MAX_VALUE - 1000, Integer.MAX_VALUE);

        testNewLargeSizeExpectAIOB(Integer.MAX_VALUE - 1000, Integer.MAX_VALUE + 1L);
        testNewLargeSizeExpectAIOB(1, -1L);
        testNewLargeSizeExpectAIOB(Integer.MAX_VALUE, -1L);
    }

    private static void testNewLargeSizeExpectAIOB(final long curSize,
                                                   final long needSize) {
        try {
            testNewLargeSize(curSize, needSize);
            throw new RuntimeException("ArrayIndexOutOfBoundsException not thrown");
        } catch (ArrayIndexOutOfBoundsException aiobe) {
            System.out.println("ArrayIndexOutOfBoundsException expected.");
        } catch (RuntimeException re) {
            throw re;
        } catch (Throwable th) {
            throw new RuntimeException("Unexpected exception", th);
        }
    }

    private static void testNewLargeSize(final long curSize,
                                         final long needSize) {

        long size = ArrayCacheConst.getNewLargeSize(curSize, needSize);

        System.out.println("getNewLargeSize(" + curSize + ", " + needSize
            + ") = " + size);

        if (size < 0 || size < needSize || size > Integer.MAX_VALUE) {
            throw new IllegalStateException("Invalid getNewLargeSize("
                + curSize + ", " + needSize + ") = " + size + " !");
        }
    }

}
