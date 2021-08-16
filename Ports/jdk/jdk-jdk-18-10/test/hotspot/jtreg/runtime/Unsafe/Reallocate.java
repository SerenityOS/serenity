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
 * @requires vm.compMode != "Xcomp"
 * @bug 8058897
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main/othervm -XX:+UnlockDiagnosticVMOptions -XX:MallocMaxTestWords=100m Reallocate
 */

import jdk.internal.misc.Unsafe;
import static jdk.test.lib.Asserts.*;

public class Reallocate {
    public static void main(String args[]) throws Exception {
        Unsafe unsafe = Unsafe.getUnsafe();

        long address = unsafe.allocateMemory(1);
        assertNotEquals(address, 0L);

        // Make sure we reallocate correctly
        unsafe.putByte(address, Byte.MAX_VALUE);
        address = unsafe.reallocateMemory(address, 2);
        assertNotEquals(address, 0L);
        assertEquals(unsafe.getByte(address), Byte.MAX_VALUE);

        // Reallocating with a 0 size should return a null pointer
        address = unsafe.reallocateMemory(address, 0);
        assertEquals(address, 0L);

        // Reallocating with a null pointer should result in a normal allocation
        address = unsafe.reallocateMemory(0L, 1);
        assertNotEquals(address, 0L);
        unsafe.putByte(address, Byte.MAX_VALUE);
        assertEquals(unsafe.getByte(address), Byte.MAX_VALUE);

        // Make sure we can throw an OOME when we fail to reallocate due to OOM
        try {
            unsafe.reallocateMemory(address, 100 * 1024 * 1024 * 8);
        } catch (OutOfMemoryError e) {
            // Expected
            return;
        }
        throw new RuntimeException("Did not get expected OOM");
    }
}
