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
 * @summary Tests Compact String. This one is for String.subString.
 * @run testng/othervm -XX:+CompactStrings SubString
 * @run testng/othervm -XX:-CompactStrings SubString
 */

public class SubString extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] {

                new Object[] { STRING_EMPTY, 0, 0, "" },
                new Object[] { STRING_L1, 0, 1, "A" },
                new Object[] { STRING_L1, 1, 1, "" },
                new Object[] { STRING_L2, 0, 2, "AB" },
                new Object[] { STRING_L2, 1, 2, "B" },
                new Object[] { STRING_LLONG, 0, 8, "ABCDEFGH" },
                new Object[] { STRING_LLONG, 7, 8, "H" },
                new Object[] { STRING_LLONG, 8, 8, "" },
                new Object[] { STRING_LLONG, 3, 7, "DEFG" },
                new Object[] { STRING_U1, 0, 1, "\uFF21" },
                new Object[] { STRING_U1, 1, 1, "" },
                new Object[] { STRING_U1, 0, 0, "" },
                new Object[] { STRING_U2, 0, 2, "\uFF21\uFF22" },
                new Object[] { STRING_U2, 1, 2, "\uFF22" },
                new Object[] { STRING_U2, 2, 2, "" },
                new Object[] { STRING_U2, 0, 2, "\uFF21\uFF22" },
                new Object[] { STRING_U2, 1, 2, "\uFF22" },
                new Object[] { STRING_M12, 1, 2, "A" },
                new Object[] { STRING_M11, 0, 1, "A" },
                new Object[] { STRING_M11, 1, 2, "\uFF21" },
                new Object[] { STRING_UDUPLICATE, 1, 5,
                        "\uFF22\uFF21\uFF22\uFF21" },
                new Object[] { STRING_MDUPLICATE1, 9, 10, "A" },
                new Object[] { STRING_MDUPLICATE1, 7, 8, "A" }, };
    }

    @Test(dataProvider = "provider")
    public void testSubstring(String str, int beginIndex, int endIndex,
            String expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.substring(beginIndex, endIndex),
                                    expected,
                                    String.format(
                                            "testing String(%s).substring(%d, %d), source : %s, ",
                                            escapeNonASCIIs(data), beginIndex,
                                            endIndex, source));
                        });
    }
}
