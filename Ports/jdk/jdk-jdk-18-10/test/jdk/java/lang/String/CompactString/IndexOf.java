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
 * @summary Tests Compact String. This one is for String.indexOf.
 * @run testng/othervm -XX:+CompactStrings IndexOf
 * @run testng/othervm -XX:-CompactStrings IndexOf
 */

public class IndexOf extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] {

                new Object[] { STRING_EMPTY, (int) 'A', -1 },
                new Object[] { STRING_L1, (int) 'A', 0 },
                new Object[] { STRING_L2, (int) 'A', 0 },
                new Object[] { STRING_L2, (int) 'B', 1 },
                new Object[] { STRING_L4, (int) 'A', 0 },
                new Object[] { STRING_L4, (int) 'D', 3 },
                new Object[] { STRING_L4, (int) 'E', -1 },
                new Object[] { STRING_LLONG, (int) 'A', 0 },
                new Object[] { STRING_LLONG, (int) 'H', 7 },
                new Object[] { STRING_U1, (int) '\uFF21', 0 },
                new Object[] { STRING_U1, (int) 'A', -1 },
                new Object[] { STRING_U2, (int) '\uFF21', 0 },
                new Object[] { STRING_U2, (int) '\uFF22', 1 },
                new Object[] { STRING_M12, (int) '\uFF21', 0 },
                new Object[] { STRING_M12, (int) 'A', 1 },
                new Object[] { STRING_M11, (int) 'A', 0 },
                new Object[] { STRING_M11, (int) '\uFF21', 1 },
                new Object[] { STRING_UDUPLICATE, (int) '\uFF21', 0 },
                new Object[] { STRING_UDUPLICATE, (int) '\uFF22', 1 },
                new Object[] { STRING_SUPPLEMENTARY, 'A', 5 },
                new Object[] { STRING_SUPPLEMENTARY, '\uFF21', 4 },
                new Object[] { STRING_SUPPLEMENTARY,
                        Character.toCodePoint('\uD801', '\uDC00'), 0 },
                new Object[] { STRING_SUPPLEMENTARY,
                        Character.toCodePoint('\uD801', '\uDC01'), 2 }, };
    }

    @Test(dataProvider = "provider")
    public void testIndexOf(String str, int ch, int expected) {
        map.get(str).forEach(
                (source, data) -> {
                    assertEquals(data.indexOf(ch), expected, String.format(
                            "testing String(%s).indexOf(%d), source : %s, ",
                            escapeNonASCIIs(data), ch, source));
                });
    }

    @DataProvider
    public Object[][] provider2() {
        return new Object[][] {

        new Object[] { STRING_EMPTY, (int) 'A', 0, -1 },
                new Object[] { STRING_L1, (int) 'A', 0, 0 },
                new Object[] { STRING_L1, (int) 'A', 1, -1 },
                new Object[] { STRING_L1, (int) 'B', 0, -1 },
                new Object[] { STRING_L2, (int) 'A', 0, 0 },
                new Object[] { STRING_L2, (int) 'A', 1, -1 },
                new Object[] { STRING_L2, (int) 'B', 0, 1 },
                new Object[] { STRING_L2, (int) 'B', 1, 1 },
                new Object[] { STRING_L4, (int) 'A', 0, 0 },
                new Object[] { STRING_L4, (int) 'D', 2, 3 },
                new Object[] { STRING_L4, (int) 'B', 2, -1 },
                new Object[] { STRING_LLONG, (int) 'A', 0, 0 },
                new Object[] { STRING_LLONG, (int) 'H', 5, 7 },
                new Object[] { STRING_U1, (int) '\uFF21', 0, 0 },
                new Object[] { STRING_U1, (int) 'A', 0, -1 },
                new Object[] { STRING_U2, (int) '\uFF21', 0, 0 },
                new Object[] { STRING_U2, (int) '\uFF22', 0, 1 },
                new Object[] { STRING_M12, (int) '\uFF21', 0, 0 },
                new Object[] { STRING_M12, (int) 'A', 1, 1 },
                new Object[] { STRING_M11, (int) 'A', 0, 0 },
                new Object[] { STRING_M11, (int) '\uFF21', 1, 1 },
                new Object[] { STRING_UDUPLICATE, (int) '\uFF21', 1, 2 },
                new Object[] { STRING_UDUPLICATE, (int) '\uFF22', 1, 1 }, };
    }

    @Test(dataProvider = "provider2")
    public void testIndexOf(String str, int ch, int fromIndex, int expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.indexOf(ch, fromIndex),
                                    expected,
                                    String.format(
                                            "testing String(%s).indexOf(%d, %d), source : %s, ",
                                            escapeNonASCIIs(data), ch,
                                            fromIndex, source));
                        });
    }

    @DataProvider
    public Object[][] provider3() {
        return new Object[][] {

                new Object[] { STRING_EMPTY, "A", -1 },
                new Object[] { STRING_L1, "A", 0 },
                new Object[] { STRING_L1, "AB", -1 },
                new Object[] { STRING_L2, "A", 0 },
                new Object[] { STRING_L2, "B", 1 },
                new Object[] { STRING_L2, "AB", 0 },
                new Object[] { STRING_L2, "AC", -1 },
                new Object[] { STRING_L2, "ABC", -1 },
                new Object[] { STRING_L4, "ABCD", 0 },
                new Object[] { STRING_L4, "D", 3 },
                new Object[] { STRING_LLONG, "ABCDEFGH", 0 },
                new Object[] { STRING_LLONG, "EFGH", 4 },
                new Object[] { STRING_LLONG, "EFGHI", -1 },
                new Object[] { STRING_U1, "\uFF21", 0 },
                new Object[] { STRING_U1, "\uFF21A", -1 },
                new Object[] { STRING_U2, "\uFF21\uFF22", 0 },
                new Object[] { STRING_U2, "\uFF22", 1 },
                new Object[] { STRING_U2, "A\uFF22", -1 },
                new Object[] { STRING_M12, "\uFF21A", 0 },
                new Object[] { STRING_M12, "A", 1 },
                new Object[] { STRING_M12, "\uFF21\uFF21", -1 },
                new Object[] { STRING_M11, "A\uFF21", 0 },
                new Object[] { STRING_M11, "\uFF21", 1 },
                new Object[] { STRING_M11, "A", 0 },
                new Object[] {
                        STRING_UDUPLICATE,
                        "\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22",
                        0 },
                new Object[] { STRING_UDUPLICATE,
                        "\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21", 1 },
                new Object[] {
                        STRING_UDUPLICATE,
                        "\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21",
                        -1 }, };
    }

    @Test(dataProvider = "provider3")
    public void testIndexOf(String str, String anotherString, int expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.indexOf(anotherString),
                                    expected,
                                    String.format(
                                            "testing String(%s).indexOf(%s), source : %s, ",
                                            escapeNonASCIIs(data),
                                            escapeNonASCIIs(anotherString),
                                            source));
                        });
    }

    @DataProvider
    public Object[][] provider4() {
        return new Object[][] {

                new Object[] { STRING_EMPTY, "A", 0, -1 },
                new Object[] { STRING_L1, "A", 0, 0 },
                new Object[] { STRING_L1, "A", 1, -1 },
                new Object[] { STRING_L1, "AB", 0, -1 },
                new Object[] { STRING_L2, "A", 0, 0 },
                new Object[] { STRING_L2, "B", 0, 1 },
                new Object[] { STRING_L2, "AB", 0, 0 },
                new Object[] { STRING_L2, "AB", 1, -1 },
                new Object[] { STRING_L4, "ABCD", 0, 0 },
                new Object[] { STRING_L4, "BC", 0, 1 },
                new Object[] { STRING_L4, "A", 0, 0 },
                new Object[] { STRING_L4, "CD", 0, 2 },
                new Object[] { STRING_L4, "A", 2, -1 },
                new Object[] { STRING_L4, "ABCDE", 0, -1 },
                new Object[] { STRING_LLONG, "ABCDEFGH", 0, 0 },
                new Object[] { STRING_LLONG, "DEFGH", 0, 3 },
                new Object[] { STRING_LLONG, "A", 0, 0 },
                new Object[] { STRING_LLONG, "GHI", 0, -1 },
                new Object[] { STRING_U1, "\uFF21", 0, 0 },
                new Object[] { STRING_U1, "\uFF21A", 0, -1 },
                new Object[] { STRING_U2, "\uFF21\uFF22", 0, 0 },
                new Object[] { STRING_U2, "\uFF22", 0, 1 },
                new Object[] { STRING_U2, "\uFF21", 1, -1 },
                new Object[] { STRING_M12, "\uFF21A", 0, 0 },
                new Object[] { STRING_M12, "A", 1, 1 },
                new Object[] { STRING_M12, "\uFF21A", 1, -1 },
                new Object[] { STRING_M12, "\uFF21", 0, 0 },
                new Object[] { STRING_M11, "A\uFF21", 0, 0 },
                new Object[] { STRING_M11, "\uFF21", 1, 1 },
                new Object[] { STRING_M11, "A\uFF21", 1, -1 },
                new Object[] { STRING_M11, "A\uFF21A", 0, -1 },
                new Object[] {
                        STRING_UDUPLICATE,
                        "\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22",
                        0, 0 },
                new Object[] {
                        STRING_UDUPLICATE,
                        "\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22",
                        1, -1 },
                new Object[] {
                        STRING_UDUPLICATE,
                        "\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22",
                        1, 1 },
                new Object[] { STRING_UDUPLICATE, "\uFF21\uFF22\uFF21\uFF22",
                        4, 4 },
                new Object[] { STRING_UDUPLICATE, "\uFF21\uFF22\uFF21\uFF22",
                        7, -1 }, };
    }

    @Test(dataProvider = "provider4")
    public void testIndexOf(String str, String anotherString, int fromIndex,
            int expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.indexOf(anotherString, fromIndex),
                                    expected,
                                    String.format(
                                            "testing String(%s).indexOf(%s), source : %s, ",
                                            escapeNonASCIIs(data),
                                            escapeNonASCIIs(anotherString),
                                            fromIndex, source));
                        });
    }
}
