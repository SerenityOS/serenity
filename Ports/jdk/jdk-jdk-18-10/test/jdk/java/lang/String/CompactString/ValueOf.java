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
 * @summary Tests Compact String. This one is for String.valueOf.
 *          valueOf(char[] data) is not tested here.
 * @run testng/othervm -XX:+CompactStrings ValueOf
 * @run testng/othervm -XX:-CompactStrings ValueOf
 */

public class ValueOf {

    /*
     * Data provider for testValueOf
     *
     * @return input parameter for testValueOf
     */
    @DataProvider
    public Object[][] valueOfs() {
        return new Object[][] { { String.valueOf(true), "true" },
                { String.valueOf(false), "false" },
                { String.valueOf(1.0f), "1.0" },
                { String.valueOf(0.0f), "0.0" },
                { String.valueOf(Float.MAX_VALUE), "3.4028235E38" },
                { String.valueOf(Float.MIN_VALUE), "1.4E-45" },
                { String.valueOf(1.0d), "1.0" },
                { String.valueOf(0.0d), "0.0" },
                { String.valueOf(Double.MAX_VALUE), "1.7976931348623157E308" },
                { String.valueOf(Double.MIN_VALUE), "4.9E-324" },
                { String.valueOf(1), "1" }, { String.valueOf(0), "0" },
                { String.valueOf(Integer.MAX_VALUE), "2147483647" },
                { String.valueOf(Integer.MIN_VALUE), "-2147483648" },
                { String.valueOf(1L), "1" }, { String.valueOf(0L), "0" },
                { String.valueOf(Long.MAX_VALUE), "9223372036854775807" },
                { String.valueOf(Long.MIN_VALUE), "-9223372036854775808" } };
    }

    /*
     * test String.valueOf(xxx).
     *
     * @param res
     *            real result
     * @param expected
     *            expected result
     */
    @Test(dataProvider = "valueOfs")
    public void testValueOf(String res, String expected) {
        assertEquals(res, expected);
    }

}
