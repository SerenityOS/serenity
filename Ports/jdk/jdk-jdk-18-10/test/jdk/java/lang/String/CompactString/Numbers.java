/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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


import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/*
 * @test
 * @bug 8077559
 * @summary Tests Compact String. This one is testing
 *          Integer/Long's methods related to String.
 * @run testng/othervm -XX:+CompactStrings Numbers
 * @run testng/othervm -XX:-CompactStrings Numbers
 */

public class Numbers {

    /*
     * Data provider for testIntegerLong
     *
     * @return input parameter for testIntegerLong
     */
    @DataProvider
    public Object[][] numbers() {
        return new Object[][] {
                { Integer.toBinaryString(Integer.MAX_VALUE),
                        "1111111111111111111111111111111" },
                { Integer.toBinaryString(Integer.MIN_VALUE),
                        "10000000000000000000000000000000" },
                { Integer.toBinaryString(7), "111" },
                { Integer.toBinaryString(0), "0" },
                { Integer.toOctalString(Integer.MAX_VALUE), "17777777777" },
                { Integer.toOctalString(Integer.MIN_VALUE), "20000000000" },
                { Integer.toOctalString(9), "11" },
                { Integer.toOctalString(0), "0" },
                { Integer.toHexString(Integer.MAX_VALUE), "7fffffff" },
                { Integer.toHexString(Integer.MIN_VALUE), "80000000" },
                { Integer.toHexString(17), "11" },
                { Integer.toHexString(0), "0" },
                { Integer.toString(Integer.MAX_VALUE, 2),
                        "1111111111111111111111111111111" },
                { Integer.toString(Integer.MIN_VALUE, 2),
                        "-10000000000000000000000000000000" },
                { Integer.toString(7, 2), "111" },
                { Integer.toString(0, 2), "0" },
                { Integer.toString(Integer.MAX_VALUE, 8), "17777777777" },
                { Integer.toString(Integer.MIN_VALUE, 8), "-20000000000" },
                { Integer.toString(9, 8), "11" },
                { Integer.toString(Integer.MAX_VALUE, 16), "7fffffff" },
                { Integer.toString(Integer.MIN_VALUE, 16), "-80000000" },
                { Integer.toString(17, 16), "11" },
                { Long.toBinaryString(Long.MAX_VALUE),
                        "111111111111111111111111111111111111111111111111111111111111111" },
                { Long.toBinaryString(Long.MIN_VALUE),
                        "1000000000000000000000000000000000000000000000000000000000000000" },
                { Long.toOctalString(Long.MAX_VALUE), "777777777777777777777" },
                { Long.toOctalString(Long.MIN_VALUE), "1000000000000000000000" },
                { Long.toHexString(Long.MAX_VALUE), "7fffffffffffffff" },
                { Long.toHexString(Long.MIN_VALUE), "8000000000000000" },
                { Long.toString(Long.MAX_VALUE, 2),
                        "111111111111111111111111111111111111111111111111111111111111111" },
                { Long.toString(Long.MIN_VALUE, 2),
                        "-1000000000000000000000000000000000000000000000000000000000000000" },
                { Long.toString(Long.MAX_VALUE, 8), "777777777777777777777" },
                { Long.toString(Long.MIN_VALUE, 8), "-1000000000000000000000" },
                { Long.toString(Long.MAX_VALUE, 16), "7fffffffffffffff" },
                { Long.toString(Long.MIN_VALUE, 16), "-8000000000000000" } };
    }

    /*
     * test Integer/Long's methods related to String.
     *
     * @param res
     *            real result
     * @param expected
     *            expected result
     */
    @Test(dataProvider = "numbers")
    public void testIntegerLong(String res, String expected) {
        assertEquals(res, expected);
    }

}
