/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @requires vm.compMode != "Xcomp"
 * @summary Verifies behaviour of Unsafe.allocateMemory
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:MallocMaxTestWords=100m AllocateMemory
 */

import jdk.internal.misc.Unsafe;
import static jdk.test.lib.Asserts.*;

public class AllocateMemory {
    public static void main(String args[]) throws Exception {
        Unsafe unsafe = Unsafe.getUnsafe();

        // Allocate a byte, write to the location and read back the value
        long address = unsafe.allocateMemory(1);
        assertNotEquals(address, 0L);

        unsafe.putByte(address, Byte.MAX_VALUE);
        assertEquals(Byte.MAX_VALUE, unsafe.getByte(address));
        unsafe.freeMemory(address);

        // Call to allocateMemory() with a negative value should result in an IllegalArgumentException
        try {
            address = unsafe.allocateMemory(-1);
            throw new RuntimeException("Did not get expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
            assertNotEquals(address, 0L);
        }

        // allocateMemory() should throw an OutOfMemoryError when the underlying malloc fails,
        // we test this by limiting the malloc using -XX:MallocMaxTestWords
        try {
            address = unsafe.allocateMemory(100 * 1024 * 1024 * 8);
            throw new RuntimeException("Did not get expected OutOfMemoryError");
        } catch (OutOfMemoryError e) {
            // Expected
        }

        // Allocation should fail on a 32-bit system if the aligned-up
        // size overflows a size_t
        if (Unsafe.ADDRESS_SIZE == 4) {
            try {
                address = unsafe.allocateMemory((long)Integer.MAX_VALUE * 2);
                throw new RuntimeException("Did not get expected IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                // Expected
            }
        }

        // Allocation should fail if the aligned-up size overflows a
        // Java long
        try {
            address = unsafe.allocateMemory((long)Long.MAX_VALUE);
            throw new RuntimeException("Did not get expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
}
