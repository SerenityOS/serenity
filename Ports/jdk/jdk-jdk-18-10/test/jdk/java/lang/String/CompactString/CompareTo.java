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
 * @bug 8077559 8137326
 * @summary Tests Compact String. Verifies the compareTo method for String,
 * StringBuilder and StringBuffer.
 * @run testng/othervm -XX:+CompactStrings CompareTo
 * @run testng/othervm -XX:-CompactStrings CompareTo
 */

public class CompareTo extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] {

        new Object[] { STRING_EMPTY, "A", -1 },
                new Object[] { STRING_EMPTY, "\uFF21", -1 },
                new Object[] { STRING_L1, "AB", -1 },
                new Object[] { STRING_L1, "A", 0 },
                new Object[] { STRING_L1, "a", -32 },
                new Object[] { STRING_L1, "\uFF21", -65248 },
                new Object[] { STRING_L2, "AB", 0 },
                new Object[] { STRING_L2, "Ab", -32 },
                new Object[] { STRING_L2, "AA", 1 },
                new Object[] { STRING_L2, "\uFF21", -65248 },
                new Object[] { STRING_L2, "A\uFF21", -65247 },
                new Object[] { STRING_L4, "ABC", 1 },
                new Object[] { STRING_L4, "AB", 2 },
                new Object[] { STRING_L4, "ABcD", -32 },
                new Object[] { STRING_L4, "ABCD\uFF21\uFF21", -2 },
                new Object[] { STRING_L4, "ABCD\uFF21", -1 },
                new Object[] { STRING_LLONG, "ABCDEFG\uFF21", -65241 },
                new Object[] { STRING_LLONG, "AB", 6 },
                new Object[] { STRING_LLONG, "ABCD", 4 },
                new Object[] { STRING_LLONG, "ABCDEFGH\uFF21\uFF21", -2 },
                new Object[] { STRING_U1, "\uFF21", 0 },
                new Object[] { STRING_U1, "\uFF22", -1 },
                new Object[] { STRING_U1, "\uFF21\uFF22", -1 },
                new Object[] { STRING_U1, "A", 65248 },
                new Object[] { STRING_U2, "\uFF21\uFF22", 0 },
                new Object[] { STRING_U2, "\uFF22", -1 },
                new Object[] { STRING_U2, "\uFF21\uFF21", 1 },
                new Object[] { STRING_U2, "A", 65248 },
                new Object[] { STRING_M12, "\uFF21A", 0 },
                new Object[] { STRING_M12, "A\uFF21", 65248 },
                new Object[] { STRING_M12, "\uFF21\uFF21", -65248 },
                new Object[] { STRING_M11, "A\uFF21", 0 },
                new Object[] { STRING_M11, "\uFF21A", -65248 },
                new Object[] { STRING_M11, "AA", 65248 }, };
    }

    @Test(dataProvider = "provider")
    public void testCompareTo(String str, String anotherString, int expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.compareTo(anotherString),
                                    expected,
                                    String.format(
                                            "testing String(%s).compareTo(%s), source : %s, ",
                                            escapeNonASCIIs(data),
                                            escapeNonASCIIs(anotherString),
                                            source));
                        });
    }

    /*
     * Runs the same test with StringBuilder
    */
    @Test(dataProvider = "provider")
    public void testStringBuilder(String str, String anotherString, int expected) {
        StringBuilder another = new StringBuilder(anotherString);
        map.get(str)
                .forEach(
                        (source, data) -> {
                            StringBuilder sb = new StringBuilder(data);
                            assertEquals(
                                    sb.compareTo(another),
                                    expected,
                                    String.format(
                                            "testing StringBuilder(%s).compareTo(%s), source : %s, ",
                                            escapeNonASCIIs(data),
                                            escapeNonASCIIs(anotherString),
                                            source));
                        });
    }

    /*
     * Runs the same test with StringBuffer
    */
    @Test(dataProvider = "provider")
    public void testStringBuffer(String str, String anotherString, int expected) {
        StringBuffer another = new StringBuffer(anotherString);
        map.get(str)
                .forEach(
                        (source, data) -> {
                            StringBuffer sb = new StringBuffer(data);
                            assertEquals(
                                    sb.compareTo(another),
                                    expected,
                                    String.format(
                                            "testing StringBuffer(%s).compareTo(%s), source : %s, ",
                                            escapeNonASCIIs(data),
                                            escapeNonASCIIs(anotherString),
                                            source));
                        });
    }
}
