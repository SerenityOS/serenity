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

import java.util.Arrays;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/*
 * @test
 * @bug 8077559
 * @summary Tests Compact String. This one is for String.split.
 * @run testng/othervm -XX:+CompactStrings Split
 * @run testng/othervm -XX:-CompactStrings Split
 */

public class Split extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] {
                new Object[] { STRING_L1, "", 0, new String[] { "A" } },
                new Object[] { STRING_L1, "", 1, new String[] { "A" } },
                new Object[] { STRING_L1, "", 2, new String[] { "A", "" } },
                new Object[] { STRING_L1, "A", 0, new String[] {} },
                new Object[] { STRING_L2, "A", 0, new String[] { "", "B" } },
                new Object[] { STRING_L2, "B", 0, new String[] { "A" } },
                new Object[] { STRING_LLONG, "D", 0,
                        new String[] { "ABC", "EFGH" } },
                new Object[] { STRING_LLONG, "[D]", 0,
                        new String[] { "ABC", "EFGH" } },
                new Object[] { STRING_LLONG, "CD", 0,
                        new String[] { "AB", "EFGH" } },
                new Object[] { STRING_LLONG, "DC", 0,
                        new String[] { "ABCDEFGH" } },
                new Object[] { STRING_LLONG, "[CF]", 0,
                        new String[] { "AB", "DE", "GH" } },
                new Object[] { STRING_LLONG, "[CF]", 1,
                        new String[] { "ABCDEFGH" } },
                new Object[] { STRING_LLONG, "[CF]", 2,
                        new String[] { "AB", "DEFGH" } },
                new Object[] { STRING_LLONG, "[FC]", 0,
                        new String[] { "AB", "DE", "GH" } },
                new Object[] { STRING_LLONG, "[FC]", 1,
                        new String[] { "ABCDEFGH" } },
                new Object[] { STRING_LLONG, "[FC]", 2,
                        new String[] { "AB", "DEFGH" } },
                new Object[] { STRING_U1, "", 0, new String[] { "\uFF21" } },
                new Object[] { STRING_U1, "", 1, new String[] { "\uFF21" } },
                new Object[] { STRING_U1, "", 2, new String[] { "\uFF21", "" } },
                new Object[] { STRING_U1, "\uFF21", 0, new String[] {} },
                new Object[] { STRING_M12, "\uFF21", 0,
                        new String[] { "", "A" } },
                new Object[] { STRING_M12, "A", 0, new String[] { "\uFF21" } },
                new Object[] {
                        STRING_UDUPLICATE,
                        "\uFF21",
                        0,
                        new String[] { "", "\uFF22", "\uFF22", "\uFF22",
                                "\uFF22", "\uFF22" } },
                new Object[] {
                        STRING_UDUPLICATE,
                        "\uFF21",
                        2,
                        new String[] { "",
                                "\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22" } },
                new Object[] {
                        STRING_UDUPLICATE,
                        "\uFF21",
                        4,
                        new String[] { "", "\uFF22", "\uFF22",
                                "\uFF22\uFF21\uFF22\uFF21\uFF22" } },
                new Object[] {
                        STRING_UDUPLICATE,
                        "\uFF22",
                        0,
                        new String[] { "\uFF21", "\uFF21", "\uFF21", "\uFF21",
                                "\uFF21" } },
                new Object[] {
                        STRING_UDUPLICATE,
                        "\uFF22",
                        3,
                        new String[] { "\uFF21", "\uFF21",
                                "\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22" } },

                new Object[] { STRING_MDUPLICATE1, "\uFF21", 0,
                        new String[] { "", "A", "A", "A", "A", "A" } },
                new Object[] { STRING_MDUPLICATE1, "\uFF21", 3,
                        new String[] { "", "A", "A\uFF21A\uFF21A\uFF21A" } },
                new Object[] {
                        STRING_MDUPLICATE1,
                        "A",
                        0,
                        new String[] { "\uFF21", "\uFF21", "\uFF21", "\uFF21",
                                "\uFF21" } },
                new Object[] {
                        STRING_MDUPLICATE1,
                        "A",
                        4,
                        new String[] { "\uFF21", "\uFF21", "\uFF21",
                                "\uFF21A\uFF21A" } },
                new Object[] { STRING_SUPPLEMENTARY, "\uD801\uDC01", 0,
                        new String[] { "\uD801\uDC00", "\uFF21A" } },
                new Object[] { STRING_SUPPLEMENTARY, "\uDC01", 0,
                        new String[] { "\uD801\uDC00\uD801\uDC01\uFF21A" } },
                new Object[] { STRING_SUPPLEMENTARY, "\uD801\uDC01", 0,
                        new String[] { "\uD801\uDC00", "\uFF21A" } },
                new Object[] { STRING_SUPPLEMENTARY, "[\uD801\uDC01\uFF21]", 0,
                        new String[] { "\uD801\uDC00", "", "A" } },
                new Object[] { STRING_SUPPLEMENTARY, "[\uD801\uDC01\uFF21]", 1,
                        new String[] { "\uD801\uDC00\uD801\uDC01\uFF21A" } },
                new Object[] { STRING_SUPPLEMENTARY, "[\uD801\uDC01\uFF21]", 2,
                        new String[] { "\uD801\uDC00", "\uFF21A" } },
                new Object[] { STRING_SUPPLEMENTARY, "[\uFF21\uD801\uDC01]", 0,
                        new String[] { "\uD801\uDC00", "", "A" } },
                new Object[] { STRING_SUPPLEMENTARY, "[\uFF21\uD801\uDC01]", 1,
                        new String[] { "\uD801\uDC00\uD801\uDC01\uFF21A" } },
                new Object[] { STRING_SUPPLEMENTARY, "[\uFF21\uD801\uDC01]", 2,
                        new String[] { "\uD801\uDC00", "\uFF21A" } },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE, "\uDC01", 0,
                        new String[] { "\uD801\uDC28\uD801\uDC29\uFF41a" } },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE, "\uD801\uDC29",
                        0, new String[] { "\uD801\uDC28", "\uFF41a" } },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE,
                        "[\uD801\uDC29\uFF41]", 0,
                        new String[] { "\uD801\uDC28", "", "a" } },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE,
                        "[\uD801\uDC29\uFF41]", 1,
                        new String[] { "\uD801\uDC28\uD801\uDC29\uFF41a" } },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE,
                        "[\uD801\uDC29\uFF41]", 2,
                        new String[] { "\uD801\uDC28", "\uFF41a" } },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE,
                        "[\uFF41\uD801\uDC29]", 0,
                        new String[] { "\uD801\uDC28", "", "a" } },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE,
                        "[\uFF41\uD801\uDC29]", 1,
                        new String[] { "\uD801\uDC28\uD801\uDC29\uFF41a" } },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE,
                        "[\uFF41\uD801\uDC29]", 2,
                        new String[] { "\uD801\uDC28", "\uFF41a" } }, };
    }

    @Test(dataProvider = "provider")
    public void testSplit(String str, String regex, int limit, String[] expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertTrue(
                                    Arrays.equals(data.split(regex, limit),
                                            expected),
                                    String.format(
                                            "testing String(%s).split(%s, %d), source : %s, ",
                                            escapeNonASCIIs(data),
                                            escapeNonASCIIs(regex), limit,
                                            source));
                        });
    }
}
