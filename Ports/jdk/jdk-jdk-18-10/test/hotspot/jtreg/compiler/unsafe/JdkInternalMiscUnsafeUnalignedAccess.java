/*
 * Copyright (c) 2016, 2020 SAP SE. All rights reserved.
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
 * @bug 8158260
 * @summary Test unaligned Unsafe accesses
 * @modules java.base/jdk.internal.misc:+open
 * @library /test/lib
 * @run main/othervm -Diters=20000 -XX:-UseOnStackReplacement -XX:-BackgroundCompilation
 *      compiler.unsafe.JdkInternalMiscUnsafeUnalignedAccess
 * @author volker.simonis@gmail.com
 */

package compiler.unsafe;

import jdk.internal.misc.Unsafe;

import java.lang.reflect.Field;
import java.nio.ByteOrder;
import jtreg.SkippedException;

public class JdkInternalMiscUnsafeUnalignedAccess {
    static final int ITERS = Integer.getInteger("iters", 20_000);
    private static final boolean BIG_ENDIAN = ByteOrder.nativeOrder().equals(ByteOrder.BIG_ENDIAN);
    private static final Unsafe UNSAFE;
    private static final int SIZE = 1024;
    private static long memory;

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to get Unsafe instance.", e);
        }
    }

    static int getInt_0() {
        return UNSAFE.getInt(memory + 0);
    }
    static int getInt_1() {
        return UNSAFE.getInt(memory + 1);
    }
    static int getInt_4() {
        return UNSAFE.getInt(memory + 4);
    }
    static int getInt_17() {
        return UNSAFE.getInt(memory + 17);
    }

    static long getIntAsLong_0() {
        return UNSAFE.getInt(memory + 0);
    }
    static long getIntAsLong_1() {
        return UNSAFE.getInt(memory + 1);
    }
    static long getIntAsLong_4() {
        return UNSAFE.getInt(memory + 4);
    }
    static long getIntAsLong_17() {
        return UNSAFE.getInt(memory + 17);
    }

    static long getLong_0() {
        return UNSAFE.getLong(memory + 0);
    }
    static long getLong_1() {
        return UNSAFE.getLong(memory + 1);
    }
    static long getLong_4() {
        return UNSAFE.getLong(memory + 4);
    }
    static long getLong_8() {
        return UNSAFE.getLong(memory + 8);
    }
    static long getLong_17() {
        return UNSAFE.getLong(memory + 17);
    }

    static void putInt_0(int i) {
        UNSAFE.putInt(memory + 0, i);
    }
    static void putInt_1(int i) {
        UNSAFE.putInt(memory + 1, i);
    }
    static void putInt_4(int i) {
        UNSAFE.putInt(memory + 4, i);
    }
    static void putInt_17(int i) {
        UNSAFE.putInt(memory + 17, i);
    }

    static void putLong_0(long l) {
        UNSAFE.putLong(memory + 0, l);
    }
    static void putLong_1(long l) {
        UNSAFE.putLong(memory + 1, l);
    }
    static void putLong_4(long l) {
        UNSAFE.putLong(memory + 4, l);
    }
    static void putLong_8(long l) {
        UNSAFE.putLong(memory + 8, l);
    }
    static void putLong_17(long l) {
        UNSAFE.putLong(memory + 17, l);
    }

    public static void main(String[] args) throws Exception {

        if (!UNSAFE.unalignedAccess()) {
            throw new SkippedException("Platform is not supporting unaligned access - nothing to test.");
        }

        memory = UNSAFE.allocateMemory(SIZE);

        UNSAFE.putInt(memory +  0, 0x00112233);
        UNSAFE.putInt(memory +  4, 0x44556677);
        UNSAFE.putInt(memory +  8, 0x8899aabb);
        UNSAFE.putInt(memory + 12, 0xccddeeff);
        UNSAFE.putInt(memory + 16, 0x01234567);
        UNSAFE.putInt(memory + 20, 0x89abcdef);
        UNSAFE.putInt(memory + 24, 0x01234567);

        // Unsafe.getInt()
        int res;
        for (int i = 0; i < ITERS; i++) {
            res = getInt_0();
            if (res != 0x00112233) {
                throw new Exception(res + " != 0x00112233");
            }
        }

        for (int i = 0; i < ITERS; i++) {
            res = getInt_1();
            if (res != (BIG_ENDIAN ? 0x11223344 : 0x77001122)) {
                throw new Exception(res + " != " + (BIG_ENDIAN ? 0x11223344 : 0x77001122));
            }
        }

        for (int i = 0; i < ITERS; i++) {
            res = getInt_4();
            if (res != 0x44556677) {
                throw new Exception(res + " != 0x44556677");
            }
        }

        for (int i = 0; i < ITERS; i++) {
            res = getInt_17();
            if (res != (BIG_ENDIAN ? 0x23456789 : 0xef012345)) {
                throw new Exception(res + " != " + (BIG_ENDIAN ? 0x23456789 : 0xef012345));
            }
        }

        // (long)Unsafe.getInt()
        long lres;
        for (int i = 0; i < ITERS; i++) {
            lres = getIntAsLong_0();
            if (lres != (long)0x00112233) {
                throw new Exception(lres + " != 0x00112233");
            }
        }

        for (int i = 0; i < ITERS; i++) {
            lres = getIntAsLong_1();
            if (lres != (BIG_ENDIAN ? (long)0x11223344 : (long)0x77001122)) {
                throw new Exception(lres + " != " + (BIG_ENDIAN ? (long)0x11223344 : (long)0x77001122));
            }
        }

        for (int i = 0; i < ITERS; i++) {
            lres = getIntAsLong_4();
            if (lres != (long)0x44556677) {
                throw new Exception(lres + " != 0x44556677");
            }
        }

        for (int i = 0; i < ITERS; i++) {
            lres = getIntAsLong_17();
            if (lres != (BIG_ENDIAN ? (long)0x23456789 : (long)0xef012345)) {
                throw new Exception(lres + " != " + (BIG_ENDIAN ? (long)0x23456789 : (long)0xef012345));
            }
        }

        // Unsafe.getLong()
        for (int i = 0; i < ITERS; i++) {
            lres = getLong_0();
            if (lres != (BIG_ENDIAN ? 0x0011223344556677L : 0x4455667700112233L)) {
                throw new Exception(lres + " != " + (BIG_ENDIAN ? 0x0011223344556677L : 0x4455667700112233L));
            }
        }

        for (int i = 0; i < ITERS; i++) {
            lres = getLong_1();
            if (lres != (BIG_ENDIAN ? 0x1122334455667788L : 0xbb44556677001122L)) {
                throw new Exception(lres + " != " + (BIG_ENDIAN ? 0x1122334455667788L : 0xbb44556677001122L));
            }
        }

        for (int i = 0; i < ITERS; i++) {
            lres = getLong_4();
            if (lres != (BIG_ENDIAN ? 0x445566778899aabbL : 0x8899aabb44556677L)) {
                throw new Exception(lres + " != " + (BIG_ENDIAN ? 0x445566778899aabbL : 0x8899aabb44556677L));
            }
        }

        for (int i = 0; i < ITERS; i++) {
            lres = getLong_8();
            if (lres != (BIG_ENDIAN ? 0x8899aabbccddeeffL : 0xccddeeff8899aabbL)) {
                throw new Exception(lres + " != " + (BIG_ENDIAN ? 0x8899aabbccddeeffL : 0xccddeeff8899aabbL));
            }
        }

        for (int i = 0; i < ITERS; i++) {
            lres = getLong_17();
            if (lres != (BIG_ENDIAN ? 0x23456789abcdef01L : 0x6789abcdef012345L)) {
                throw new Exception(lres + " != " + (BIG_ENDIAN ? 0x23456789abcdef01L : 0x6789abcdef012345L));
            }
        }

        // Unsafe.putInt()
        for (int i = 0; i < ITERS; i++) {
            putInt_0(0x00112233);
            res = getInt_0();
            if (res != 0x00112233) {
                throw new Exception(res + " != 0x00112233");
            }
        }

        for (int i = 0; i < ITERS; i++) {
            putInt_1(BIG_ENDIAN ? 0x11223344 : 0x77001122);
            res = getInt_1();
            if (res != (BIG_ENDIAN ? 0x11223344 : 0x77001122)) {
                throw new Exception(res + " != " + (BIG_ENDIAN ? 0x11223344 : 0x77001122));
            }
        }

        for (int i = 0; i < ITERS; i++) {
            putInt_4(0x44556677);
            res = getInt_4();
            if (res != 0x44556677) {
                throw new Exception(res + " != 0x44556677");
            }
        }

        for (int i = 0; i < ITERS; i++) {
            putInt_17(BIG_ENDIAN ? 0x23456789 : 0xef012345);
            res = getInt_17();
            if (res != (BIG_ENDIAN ? 0x23456789 : 0xef012345)) {
                throw new Exception(res + " != " + (BIG_ENDIAN ? 0x23456789 : 0xef012345));
            }
        }


        // Unsafe.putLong()
        for (int i = 0; i < ITERS; i++) {
            putLong_0(BIG_ENDIAN ? 0x0011223344556677L : 0x4455667700112233L);
            lres = getLong_0();
            if (lres != (BIG_ENDIAN ? 0x0011223344556677L : 0x4455667700112233L)) {
                throw new Exception(lres + " != " + (BIG_ENDIAN ? 0x0011223344556677L : 0x4455667700112233L));
            }
        }

        for (int i = 0; i < ITERS; i++) {
            putLong_1(BIG_ENDIAN ? 0x1122334455667788L : 0xbb44556677001122L);
            lres = getLong_1();
            if (lres != (BIG_ENDIAN ? 0x1122334455667788L : 0xbb44556677001122L)) {
                throw new Exception(lres + " != " + (BIG_ENDIAN ? 0x1122334455667788L : 0xbb44556677001122L));
            }
        }

        for (int i = 0; i < ITERS; i++) {
            putLong_4(BIG_ENDIAN ? 0x445566778899aabbL : 0x8899aabb44556677L);
            lres = getLong_4();
            if (lres != (BIG_ENDIAN ? 0x445566778899aabbL : 0x8899aabb44556677L)) {
                throw new Exception(lres + " != " + (BIG_ENDIAN ? 0x445566778899aabbL : 0x8899aabb44556677L));
            }
        }

        for (int i = 0; i < ITERS; i++) {
            putLong_8(BIG_ENDIAN ? 0x8899aabbccddeeffL : 0xccddeeff8899aabbL);
            lres = getLong_8();
            if (lres != (BIG_ENDIAN ? 0x8899aabbccddeeffL : 0xccddeeff8899aabbL)) {
                throw new Exception(lres + " != " + (BIG_ENDIAN ? 0x8899aabbccddeeffL : 0xccddeeff8899aabbL));
            }
        }

        for (int i = 0; i < ITERS; i++) {
            putLong_17(BIG_ENDIAN ? 0x23456789abcdef01L : 0x6789abcdef012345L);
            lres = getLong_17();
            if (lres != (BIG_ENDIAN ? 0x23456789abcdef01L : 0x6789abcdef012345L)) {
                throw new Exception(lres + " != " + (BIG_ENDIAN ? 0x23456789abcdef01L : 0x6789abcdef012345L));
            }
        }
    }

}
