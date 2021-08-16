/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8202414
 * @summary Unsafe write after primitive array creation may result in array length change
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run main/othervm compiler.c2.Test8202414
 */

package compiler.c2;

import sun.misc.Unsafe;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import jtreg.SkippedException;

public class Test8202414 {

    public static void main(String[] args) {
        // Some CPUs (for example, ARM) does not support unaligned
        // memory accesses. This test may cause JVM crash due to
        // alignment check failure on such CPUs.
        if (!jdk.internal.misc.Unsafe.getUnsafe().unalignedAccess()) {
          throw new SkippedException(
            "Platform is missing unaligned memory accesses support.");
        }
        System.err.close();
        int count = 0;
        while (count++ < 120000) {
          test();
        }
    }

    public static void test() {
        byte[] newBufb = serByte(397);
        short[] newBufs = serShort(397);
        int[] newBufi = serInt(397);
        long[] newBufl = serLong(397);
        if (newBufb.length != 397 || newBufs.length != 397
            || newBufi.length != 397 || newBufl.length != 397) {
            System.out.println("array length internal error");
            throw new RuntimeException("Test failed");
        }

    }

    public static byte[] serByte(int bufLen) {
        byte[] buf = new byte[bufLen];
        THE_UNSAFE.putByte(buf, BYTE_ARRAY_BASE_OFFSET + 1, (byte) buf.length);
        System.err.println("ref " + buf);
        return buf;
    }

    public static short[] serShort(int bufLen) {
        short[] buf = new short[bufLen];
        THE_UNSAFE.putShort(buf, SHORT_ARRAY_BASE_OFFSET + 1, (short) buf.length);
        System.err.println("ref " + buf);
        return buf;
    }

    public static int[] serInt(int bufLen) {
        int[] buf = new int[bufLen];
        THE_UNSAFE.putInt(buf, INT_ARRAY_BASE_OFFSET + 1, buf.length);
        System.err.println("ref " + buf);
        return buf;
    }

    public static long[] serLong(int bufLen) {
        long[] buf = new long[bufLen];
        THE_UNSAFE.putLong(buf, LONG_ARRAY_BASE_OFFSET + 1, buf.length);
        System.err.println("ref " + buf);
        return buf;
    }

    /* Unsafe fields and initialization
     */
    static final Unsafe THE_UNSAFE;
    static final long BYTE_ARRAY_BASE_OFFSET;
    static final long SHORT_ARRAY_BASE_OFFSET;
    static final long INT_ARRAY_BASE_OFFSET;
    static final long LONG_ARRAY_BASE_OFFSET;
    static {
        THE_UNSAFE = (Unsafe) AccessController.doPrivileged (
            new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    try {
                        Field f = Unsafe.class.getDeclaredField("theUnsafe");
                        f.setAccessible(true);
                        return f.get(null);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        throw new Error();
                    }
                }
            }
        );
        BYTE_ARRAY_BASE_OFFSET = THE_UNSAFE.arrayBaseOffset(byte[].class);
        SHORT_ARRAY_BASE_OFFSET = THE_UNSAFE.arrayBaseOffset(short[].class);
        INT_ARRAY_BASE_OFFSET = THE_UNSAFE.arrayBaseOffset(int[].class);
        LONG_ARRAY_BASE_OFFSET = THE_UNSAFE.arrayBaseOffset(long[].class);
    }
}
