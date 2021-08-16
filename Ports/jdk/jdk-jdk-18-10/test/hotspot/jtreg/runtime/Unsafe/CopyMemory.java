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

/*
 * @test
 * @summary Verifies behaviour of Unsafe.copyMemory
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main CopyMemory
 */

import jdk.internal.misc.Unsafe;
import static jdk.test.lib.Asserts.*;

public class CopyMemory {
    final static int LENGTH = 8;
    public static void main(String args[]) throws Exception {
        Unsafe unsafe = Unsafe.getUnsafe();
        long src = unsafe.allocateMemory(LENGTH);
        long dst = unsafe.allocateMemory(LENGTH);
        assertNotEquals(src, 0L);
        assertNotEquals(dst, 0L);

        // call copyMemory() with different lengths and verify the contents of
        // the destination array
        for (int i = 0; i < LENGTH; i++) {
            unsafe.putByte(src + i, (byte)i);
            unsafe.copyMemory(src, dst, i);
            for (int j = 0; j < i; j++) {
                assertEquals(unsafe.getByte(src + j), unsafe.getByte(src + j));
            }
        }
        unsafe.freeMemory(src);
        unsafe.freeMemory(dst);
    }
}
