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
 * @summary Verify behaviour of Unsafe.get/putBoolean
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 * @run main GetPutBoolean
 */

import java.lang.reflect.Field;
import jdk.internal.misc.Unsafe;
import static jdk.test.lib.Asserts.*;

public class GetPutBoolean {
    public static void main(String args[]) throws Exception {
        Unsafe unsafe = Unsafe.getUnsafe();
        Test t = new Test();
        Field field = Test.class.getField("b1");

        long offset = unsafe.objectFieldOffset(field);
        assertEquals(false, unsafe.getBoolean(t, offset));
        unsafe.putBoolean(t, offset, true);
        assertEquals(true, unsafe.getBoolean(t, offset));

        boolean arrayBoolean[] = { true, false, false, true };
        int scale = unsafe.arrayIndexScale(arrayBoolean.getClass());
        offset = unsafe.arrayBaseOffset(arrayBoolean.getClass());
        for (int i = 0; i < arrayBoolean.length; i++) {
            assertEquals(unsafe.getBoolean(arrayBoolean, offset), arrayBoolean[i]);
            offset += scale;
        }
    }

    static class Test {
        public boolean b1 = false;
    }
}
