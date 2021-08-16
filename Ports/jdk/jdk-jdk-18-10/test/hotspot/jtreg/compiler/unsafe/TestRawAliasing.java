/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8178047
 * @run main/othervm -XX:CompileCommand=exclude,*.main -XX:-TieredCompilation -XX:-BackgroundCompilation compiler.unsafe.TestRawAliasing
 * @modules java.base/jdk.internal.misc:+open
 */

package compiler.unsafe;

import java.lang.reflect.Field;

public class TestRawAliasing {
    static private final jdk.internal.misc.Unsafe UNSAFE;
    static {
        try {
            Field f = jdk.internal.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (jdk.internal.misc.Unsafe) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException("Unable to get Unsafe instance.", e);
        }
    }

    static private final int OFFSET_X = 50;
    static private final int OFFSET_Y = 100;

    private static int test(long base_plus_offset_x, long base_plus_offset_y, int magic_value) {
        // write 0 to a location
        UNSAFE.putByte(base_plus_offset_x - OFFSET_X, (byte)0);
        // write unfoldable value to really the same location with another base
        UNSAFE.putByte(base_plus_offset_y - OFFSET_Y, (byte)magic_value);
        // read the value back, should be equal to "unfoldable_value"
        return UNSAFE.getByte(base_plus_offset_x - OFFSET_X);
    }

    private static final int OFF_HEAP_AREA_SIZE = 128;
    private static final byte MAGIC = 123;

    // main is excluded from compilation since we don't want the test method to inline and make base values fold
    public static void main(String... args) {
        long base = UNSAFE.allocateMemory(OFF_HEAP_AREA_SIZE);
        for (int i = 0; i < 100_000; i++) {
            if (test(base + OFFSET_X, base + OFFSET_Y, MAGIC) != MAGIC) {
                throw new RuntimeException("Unexpected magic value");
            }
        }
    }
}
