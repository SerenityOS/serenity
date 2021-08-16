/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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

package compiler.conversions;

import jdk.test.lib.Asserts;

/*
 * @test
 * @bug 8234617
 * @summary Test implicit narrowing conversion of primivite values at putfield.
 * @library /test/lib /
 * @compile Conversion.jasm
 * @run main/othervm -Xbatch -XX:CompileCommand=dontinline,compiler.conversions.Conversion::*
 *                   compiler.conversions.TestPrimitiveConversions
 */
public class TestPrimitiveConversions {

    public static void main(String[] args) {
        Conversion conv = new Conversion();
        for (int i = 0; i < 100_000; ++i) {
            int res = conv.testBooleanConst();
            Asserts.assertEquals(res, 0);
            res = conv.testBoolean(2); // 2^1 (maximum boolean value is 1)
            Asserts.assertEquals(res, 0);
            res = conv.testByteConst();
            Asserts.assertEquals(res, 0);
            res = conv.testByte(256); // 2^8 (maximum byte value is 2^7-1)
            Asserts.assertEquals(res, 0);
            res = conv.testCharConst();
            Asserts.assertEquals(res, 0);
            res = conv.testChar(131072); // 2^17 (maximum char value is 2^16-1)
            Asserts.assertEquals(res, 0);
            res = conv.testShortConst();
            Asserts.assertEquals(res, 0);
            res = conv.testShort(65536); // 2^16 (maximum short value is 2^15-1)
            Asserts.assertEquals(res, 0);
        }
    }
}
