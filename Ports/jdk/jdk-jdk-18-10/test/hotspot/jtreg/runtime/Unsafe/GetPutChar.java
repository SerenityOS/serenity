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
 * @summary Verify behaviour of Unsafe.get/putChar
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main GetPutChar
 */

import java.lang.reflect.Field;
import jdk.internal.misc.Unsafe;
import static jdk.test.lib.Asserts.*;

public class GetPutChar {
    public static void main(String args[]) throws Exception {
        Unsafe unsafe = Unsafe.getUnsafe();
        Test t = new Test();
        Field field = Test.class.getField("c");

        long offset = unsafe.objectFieldOffset(field);
        assertEquals('\u0000', unsafe.getChar(t, offset));
        unsafe.putChar(t, offset, '\u0001');
        assertEquals('\u0001', unsafe.getChar(t, offset));

        long address = unsafe.allocateMemory(8);
        unsafe.putChar(address, '\u0002');
        assertEquals('\u0002', unsafe.getChar(address));
        unsafe.freeMemory(address);

        char arrayChar[] = { '\uabcd', '\u00ff', '\uff00', };
        int scale = unsafe.arrayIndexScale(arrayChar.getClass());
        offset = unsafe.arrayBaseOffset(arrayChar.getClass());
        for (int i = 0; i < arrayChar.length; i++) {
            assertEquals(unsafe.getChar(arrayChar, offset), arrayChar[i]);
            offset += scale;
        }
    }

    static class Test {
        public char c = '\u0000';
    }
}
