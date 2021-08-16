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
 * Verify behaviour of Unsafe.get/putAddress and Unsafe.addressSize
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main GetPutAddress
 */

import jdk.test.lib.Platform;
import jdk.internal.misc.Unsafe;
import static jdk.test.lib.Asserts.*;

public class GetPutAddress {
    public static void main(String args[]) throws Exception {
        Unsafe unsafe = Unsafe.getUnsafe();
        int addressSize = unsafe.addressSize();
        // Ensure the size returned from Unsafe.addressSize is correct
        assertEquals(unsafe.addressSize(), Platform.is32bit() ? 4 : 8);

        // Write the address, read it back and make sure it's the same value
        long address = unsafe.allocateMemory(addressSize);
        unsafe.putAddress(address, address);
        long readAddress = unsafe.getAddress(address);
        if (addressSize == 4) {
          readAddress &= 0x00000000FFFFFFFFL;
        }
        assertEquals(address, readAddress);
        unsafe.freeMemory(address);
    }
}
