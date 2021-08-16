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

/* @test
 * @bug 8022853
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 * @run main GetUncompressedObject
 */

import static jdk.test.lib.Asserts.*;

import jdk.internal.misc.Unsafe;

public class GetUncompressedObject {

    public static void main(String args[]) throws Exception {
        Unsafe unsafe = Unsafe.getUnsafe();

        // Allocate some memory and fill it with non-zero values.
        final int size = 32;
        final long address = unsafe.allocateMemory(size);
        unsafe.setMemory(address, size, (byte) 0x23);

        // The only thing we can do is check for null-ness.
        // So, store a null somewhere.
        unsafe.putAddress(address + 16, 0);

        Object nullObj = unsafe.getUncompressedObject(address + 16);
        if (nullObj != null) {
            throw new InternalError("should be null");
        }
    }

}
