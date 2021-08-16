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
 * @summary Tests Compact String. This one is for String.compareToIgnoreCase.
 * @run testng/othervm -XX:+CompactStrings CompareToIgnoreCase
 * @run testng/othervm -XX:-CompactStrings CompareToIgnoreCase
 */

public class CompareToIgnoreCase extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] {
                new Object[] { STRING_EMPTY, "A", -1 },
                new Object[] { STRING_L1, "a", 0 },
                new Object[] { STRING_L1, "A", 0 },
                new Object[] { STRING_L1, "\uFF21", -65248 },
                new Object[] { STRING_L1, "B", -1 },
                new Object[] { STRING_L2, "AB", 0 },
                new Object[] { STRING_L2, "aB", 0 },
                new Object[] { STRING_L2, "\uFF21", -65248 },
                new Object[] { STRING_L2, "A\uFF21", -65247 },
                new Object[] { STRING_L4, "ABCD", 0 },
                new Object[] { STRING_L4, "abcd", 0 },
                new Object[] { STRING_L4, "ABc\uFF21", -65245 },
                new Object[] { STRING_LLONG, "ABCDEFGH", 0 },
                new Object[] { STRING_LLONG, "abcdefgh", 0 },
                new Object[] { STRING_LLONG, "ABCDEFG\uFF21", -65241 },
                new Object[] { STRING_LLONG, "abcdefg\uFF21", -65241 },
                new Object[] { STRING_U1, "\uFF41", 0 },
                new Object[] { STRING_U1,
                        "\uFF41\uFF42\uFF43\uFF44\uFF45\uFF46\uFF47\uFF48", -7 },
                new Object[] { STRING_U1, "A", 65248 },
                new Object[] { STRING_U2, "\uFF41", 1 },
                new Object[] { STRING_U2, "\uFF41\uFF42", 0 },
                new Object[] { STRING_U2,
                        "\uFF41\uFF42\uFF43\uFF44\uFF45\uFF46\uFF47\uFF48", -6 },
                new Object[] { STRING_M12, "\uFF41a", 0 },
                new Object[] { STRING_M12, "\uFF41\uFF42", -65249 },
                new Object[] { STRING_M11, "a\uFF41", 0 },
                new Object[] { STRING_M11, "a\uFF42", -1 },
                new Object[] { STRING_SUPPLEMENTARY, STRING_SUPPLEMENTARY_LOWERCASE, 0 },
                new Object[] { STRING_SUPPLEMENTARY, "\uD801\uDC28\uD801\uDC27\uFF41a", -38 },
                new Object[] { STRING_SUPPLEMENTARY, "\uD802\uDC00\uD801\uDC01\uFF21A", -984 },
        };
    }

    @Test(dataProvider = "provider")
    public void testCompareToIgnoreCase(String str, String anotherString,
            int expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.compareToIgnoreCase(anotherString),
                                    expected,
                                    String.format(
                                            "testing String(%s).compareToIgnoreCase(%s), source : %s, ",
                                            escapeNonASCIIs(data),
                                            escapeNonASCIIs(anotherString),
                                            source));
                        });
    }
}
