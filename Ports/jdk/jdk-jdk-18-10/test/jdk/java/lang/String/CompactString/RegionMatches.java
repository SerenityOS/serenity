/*
 * Copyright (c) 2015, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8077559 8248655 8264544
 * @summary Tests Compact String. This one is for String.regionMatches.
 * @run testng/othervm -XX:+CompactStrings RegionMatches
 * @run testng/othervm -XX:-CompactStrings RegionMatches
 */

public class RegionMatches extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] {

                new Object[] { STRING_EMPTY, true, 0, "", 0, 0, true },
                new Object[] { STRING_EMPTY, true, 0, "", 0, 1, false },
                new Object[] { STRING_EMPTY, true, 0, "A", 0, 0, true },
                new Object[] { STRING_EMPTY, true, 0, "", 0, 0, true },
                new Object[] { STRING_EMPTY, true, 0, "", 0, 1, false },
                new Object[] { STRING_L1, false, 0, "a", 0, 1, false },
                new Object[] { STRING_L1, false, 0, "BA", 1, 1, true },
                new Object[] { STRING_L1, false, 0, "Ba", 1, 1, false },
                new Object[] { STRING_L1, true, 0, "a", 0, 1, true },
                new Object[] { STRING_L1, true, 0, "BA", 1, 1, true },
                new Object[] { STRING_L1, true, 0, "Ba", 1, 1, true },
                new Object[] { STRING_L2, true, 1, "b", 0, 1, true },
                new Object[] { STRING_L2, true, 1, "B", 0, 1, true },
                new Object[] { STRING_L2, true, 0, "xaBc", 1, 2, true },
                new Object[] { STRING_L2, false, 0, "AB", 0, 2, true },
                new Object[] { STRING_L2, false, 0, "Ab", 0, 2, false },
                new Object[] { STRING_L2, false, 1, "BAB", 2, 1, true },
                new Object[] { STRING_LLONG, true, 1, "bCdEF", 0, 5, true },
                new Object[] { STRING_LLONG, false, 2, "CDEFG", 0, 5, true },
                new Object[] { STRING_LLONG, true, 2, "CDEFg", 0, 5, true },
                new Object[] { STRING_U1, true, 0, "\uFF41", 0, 1, true },
                new Object[] { STRING_U1, false, 0, "\uFF41", 0, 1, false },
                new Object[] { STRING_MDUPLICATE1, true, 0, "\uFF41a\uFF41", 0,
                        3, true },
                new Object[] { STRING_MDUPLICATE1, false, 0, "\uFF21a\uFF21",
                        0, 3, false },
                new Object[] { STRING_SUPPLEMENTARY, true, 0, "\uD801\uDC28\uD801\uDC29",
                        0, 4, true },
                new Object[] { STRING_SUPPLEMENTARY, true, 1, "\uDC00\uD801",
                        0, 2, true },
                new Object[] { STRING_SUPPLEMENTARY, true, 1, "\uDC28",
                        0, 1, false },
                new Object[] { STRING_SUPPLEMENTARY, true, 4, "\uFF21", 0, 1,
                        true },
                new Object[] { STRING_SUPPLEMENTARY, true, 5, "A", 0, 1, true },
                new Object[] { STRING_SUPPLEMENTARY, true, 0, "\uD802\uDC00\uD801\uDC01\uFF21A", 0, 2, false },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE, false, 0,
                        "\uD801\uDC28\uD801\uDC29", 0, 4, true },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE, true, 0,
                        "\uD801\uDC00\uD801\uDC01", 0, 4, true },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE, true, 1,
                        "\uDC28\uD801", 0, 2, true },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE, true, 1,
                        "\uDC00", 0, 1, false },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE, true, 1,
                        "\uDC00\uD801", 0, 2, false },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE, true, 4,
                        "\uFF21", 0, 1, true },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE, false, 4,
                        "\uFF21", 0, 1, false },
                new Object[] { STRING_SUPPLEMENTARY_LOWERCASE, false, 4,
                        "\uFF41", 0, 1, true },
        };
    }

    @Test(dataProvider = "provider")
    public void testRegionMatches(String str, boolean ignoreCase, int toffset,
            String other, int ooffset, int len, boolean expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.regionMatches(ignoreCase, toffset,
                                            other, ooffset, len),
                                    expected,
                                    String.format(
                                            "testing String(%s).regionMatches(%b, %d, %s, %d, %d), source : %s, ",
                                            escapeNonASCIIs(data), ignoreCase,
                                            toffset, escapeNonASCIIs(other),
                                            ooffset, len, source));
                        });
    }
}
