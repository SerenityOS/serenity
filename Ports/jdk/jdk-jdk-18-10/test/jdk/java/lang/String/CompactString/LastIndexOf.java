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
 * @summary Tests Compact String. This one is for String.lastIndexOf.
 * @run testng/othervm -XX:+CompactStrings LastIndexOf
 * @run testng/othervm -XX:-CompactStrings LastIndexOf
 */

public class LastIndexOf extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] {

                new Object[] { STRING_EMPTY, (int) 'A', -1 },
                new Object[] { STRING_L1, (int) 'A', 0 },
                new Object[] { STRING_L2, (int) 'A', 0 },
                new Object[] { STRING_L2, (int) 'B', 1 },
                new Object[] { STRING_L4, (int) 'A', 0 },
                new Object[] { STRING_L4, (int) 'D', 3 },
                new Object[] { STRING_LLONG, (int) 'A', 0 },
                new Object[] { STRING_LLONG, (int) 'H', 7 },
                new Object[] { STRING_U1, (int) '\uFF21', 0 },
                new Object[] { STRING_U1, (int) 'B', -1 },
                new Object[] { STRING_U2, (int) '\uFF21', 0 },
                new Object[] { STRING_U2, (int) '\uFF22', 1 },
                new Object[] { STRING_M12, (int) '\uFF21', 0 },
                new Object[] { STRING_M12, (int) 'A', 1 },
                new Object[] { STRING_M11, (int) 'A', 0 },
                new Object[] { STRING_M11, (int) '\uFF21', 1 },
                new Object[] { STRING_UDUPLICATE, (int) '\uFF22', 9 },
                new Object[] { STRING_UDUPLICATE, (int) '\uFF21', 8 },
                new Object[] { STRING_SUPPLEMENTARY,
                        Character.toCodePoint('\uD801', '\uDC01'), 2 }, };
    }

    @Test(dataProvider = "provider")
    public void testLastIndexOf(String str, int ch, int expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.lastIndexOf(ch),
                                    expected,
                                    String.format(
                                            "testing String(%s).lastIndexOf(%d), source : %s, ",
                                            escapeNonASCIIs(data), ch, source));
                        });
    }

    @DataProvider
    public Object[][] provider2() {
        return new Object[][] {

        new Object[] { STRING_EMPTY, (int) 'A', 0, -1 },
                new Object[] { STRING_L1, (int) 'A', 0, 0 },
                new Object[] { STRING_L1, (int) 'A', 1, 0 },
                new Object[] { STRING_L2, (int) 'A', 0, 0 },
                new Object[] { STRING_L2, (int) 'B', 1, 1 },
                new Object[] { STRING_L2, (int) 'B', 2, 1 },
                new Object[] { STRING_L4, (int) 'A', 0, 0 },
                new Object[] { STRING_L4, (int) 'C', 2, 2 },
                new Object[] { STRING_L4, (int) 'C', 1, -1 },
                new Object[] { STRING_LLONG, (int) 'A', 0, 0 },
                new Object[] { STRING_LLONG, (int) 'H', 7, 7 },
                new Object[] { STRING_LLONG, (int) 'H', 6, -1 },
                new Object[] { STRING_U1, (int) '\uFF21', 0, 0 },
                new Object[] { STRING_U1, (int) '\uFF21', 7, 0 },
                new Object[] { STRING_U2, (int) '\uFF21', 0, 0 },
                new Object[] { STRING_U2, (int) '\uFF22', 0, -1 },
                new Object[] { STRING_M12, (int) '\uFF21', 0, 0 },
                new Object[] { STRING_M12, (int) 'A', 1, 1 },
                new Object[] { STRING_M12, (int) 'A', 0, -1 },
                new Object[] { STRING_M11, (int) 'A', 0, 0 },
                new Object[] { STRING_M11, (int) '\uFF21', 1, 1 },
                new Object[] { STRING_M11, (int) '\uFF21', 0, -1 },
                new Object[] { STRING_UDUPLICATE, (int) '\uFF21', 5, 4 },
                new Object[] { STRING_UDUPLICATE, (int) '\uFF21', 6, 6 },
                new Object[] { STRING_UDUPLICATE, (int) '\uFF22', 5, 5 },
                new Object[] { STRING_UDUPLICATE, (int) '\uFF22', 6, 5 }, };
    }

    @Test(dataProvider = "provider2")
    public void testLastIndexOf(String str, int ch, int fromIndex, int expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.lastIndexOf(ch, fromIndex),
                                    expected,
                                    String.format(
                                            "testing String(%s).lastIndexOf(%d, %d), source : %s, ",
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

                new Object[] { STRING_L2, "AB", 0 },
                new Object[] { STRING_L2, "B", 1 },
                new Object[] { STRING_L4, "ABCD", 0 },
                new Object[] { STRING_L4, "B", 1 },
                new Object[] { STRING_LLONG, "ABCD", 0 },
                new Object[] { STRING_LLONG, "GH", 6 },
                new Object[] { STRING_U1, "\uFF21", 0 },
                new Object[] { STRING_U1, "\uFF22", -1 },
                new Object[] { STRING_U2, "\uFF21\uFF22", 0 },
                new Object[] { STRING_U2, "\uFF22", 1 },
                new Object[] { STRING_M12, "\uFF21A", 0 },
                new Object[] { STRING_M12, "A", 1 },
                new Object[] { STRING_M11, "A\uFF21", 0 },
                new Object[] { STRING_M11, "\uFF21", 1 },
                new Object[] { STRING_UDUPLICATE, "\uFF21\uFF22\uFF21\uFF22", 6 },
                new Object[] { STRING_UDUPLICATE, "\uFF21\uFF22", 8 }, };
    }

    @Test(dataProvider = "provider3")
    public void testLastIndexOf(String str, String anotherString, int expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.lastIndexOf(anotherString),
                                    expected,
                                    String.format(
                                            "testing String(%s).lastIndexOf(%s), source : %s, ",
                                            escapeNonASCIIs(data),
                                            escapeNonASCIIs(anotherString),
                                            source));
                        });
    }

    @DataProvider
    public Object[][] provider4() {
        return new Object[][] {

                new Object[] { STRING_EMPTY, "A", 0, -1 },
                new Object[] { STRING_L2, "AB", 0, 0 },

                new Object[] { STRING_L1, "AB", -1, -1 },

                new Object[] { STRING_L2, "B", 1, 1 },
                new Object[] { STRING_L2, "B", 0, -1 },
                new Object[] { STRING_L4, "ABC", 3, 0 },
                new Object[] { STRING_L4, "ABC", 0, 0 },
                new Object[] { STRING_L4, "ABC", 1, 0 },
                new Object[] { STRING_L4, "BC", 1, 1 },
                new Object[] { STRING_L4, "BC", 0, -1 },
                new Object[] { STRING_LLONG, "ABCDEFGH", 0, 0 },
                new Object[] { STRING_LLONG, "EFGH", 7, 4 },
                new Object[] { STRING_LLONG, "EFGH", 3, -1 },
                new Object[] { STRING_U1, "\uFF21", 0, 0 },
                new Object[] { STRING_U1, "\uFF21", 7, 0 },
                new Object[] { STRING_U2, "\uFF21\uFF22", 0, 0 },
                new Object[] { STRING_U2, "\uFF21\uFF22", 1, 0 },
                new Object[] { STRING_M12, "\uFF21A", 0, 0 },
                new Object[] { STRING_M12, "A", 1, 1 },
                new Object[] { STRING_M12, "A", 0, -1 },
                new Object[] { STRING_M11, "A\uFF21", 0, 0 },
                new Object[] { STRING_M11, "A\uFF21", 1, 0 },
                new Object[] { STRING_M11, "\uFF21", 0, -1 },
                new Object[] {
                        STRING_UDUPLICATE,
                        "\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22",
                        9, 0 },
                new Object[] {
                        STRING_UDUPLICATE,
                        "\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22",
                        0, 0 },
                new Object[] { STRING_UDUPLICATE, "\uFF21\uFF22", 6, 6 },
                new Object[] { STRING_UDUPLICATE, "\uFF21\uFF22\uFF21", 6, 6 }, };
    }

    @Test(dataProvider = "provider4")
    public void testLastIndexOf(String str, String anotherString,
            int fromIndex, int expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.lastIndexOf(anotherString, fromIndex),
                                    expected,
                                    String.format(
                                            "testing String(%s).lastIndexOf(%s, %d), source : %s, ",
                                            escapeNonASCIIs(data),
                                            escapeNonASCIIs(anotherString),
                                            fromIndex, source));
                        });
    }
}
