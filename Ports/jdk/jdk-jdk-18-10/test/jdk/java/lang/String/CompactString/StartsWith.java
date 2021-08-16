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
 * @summary Tests Compact String. This one is for String.startsWith.
 * @run testng/othervm -XX:+CompactStrings StartsWith
 * @run testng/othervm -XX:-CompactStrings StartsWith
 */

public class StartsWith extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] {
                new Object[] {STRING_EMPTY, "", 0, true},
                new Object[] {STRING_EMPTY, "A", 0, false},
                new Object[] {STRING_EMPTY, "", 0, true},
                new Object[] {STRING_EMPTY, "", -1, false},
                new Object[] {STRING_L1, "A", 0, true},
                new Object[] {STRING_L1, "A", -1, false},
                new Object[] {STRING_L1, "A", 1, false},
                new Object[] {STRING_L2, "B", 1, true},
                new Object[] {STRING_L2, "B", 0, false},
                new Object[] {STRING_L2, "A", 0, true},
                new Object[] {STRING_L2, "AB", 1, false},
                new Object[] {STRING_L4, "ABC", 0, true},
                new Object[] {STRING_LLONG, "ABCDEFGH", 0, true},
                new Object[] {STRING_LLONG, "ABCDE", 0, true},
                new Object[] {STRING_LLONG, "CDE", 0, false},
                new Object[] {STRING_LLONG, "FG", 5, true},
                new Object[] {STRING_U1, "\uFF21", 0, true},
                new Object[] {STRING_U1, "", 1, true},
                new Object[] {STRING_U1, "\uFF21", 0, true},
                new Object[] {STRING_U1, "A", 0, false},
                new Object[] {STRING_U2, "\uFF21\uFF22", 0, true},
                new Object[] {STRING_U2, "\uFF21", 0, true},
                new Object[] {STRING_U2, "\uFF22", 0, false},
                new Object[] {STRING_U2, "", 0, true},
                new Object[] {STRING_M12, "\uFF21", 0, true},
                new Object[] {STRING_M12, "\uFF21A", 0, true},
                new Object[] {STRING_M12, "A", 0, false},
                new Object[] {STRING_M12, "\uFF21A", 0, true},
                new Object[] {STRING_M12, "A", 1, true},
                new Object[] {STRING_M11, "A", 0, true},
                new Object[] {STRING_M11, "A\uFF21", 0, true},
                new Object[] {STRING_M11, "A\uFF21", 0, true},
                new Object[] {STRING_M11, "\uFF21", 1, true},
                new Object[] {STRING_UDUPLICATE,
                        "\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22",
                        0, true},
                new Object[] {STRING_UDUPLICATE,
                        "\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22", 0, true},
                new Object[] {STRING_UDUPLICATE,
                        "\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22", 2, true},
                new Object[] {STRING_UDUPLICATE,
                        "\uFF21\uFF22\uFF21\uFF22\uFF21\uFF22", 5, false},
                new Object[] {STRING_MDUPLICATE1,
                        "\uFF21A\uFF21A\uFF21A\uFF21A\uFF21A", 0, true},
                new Object[] {STRING_MDUPLICATE1,
                        "\uFF21A\uFF21A\uFF21A\uFF21A\uFF21", 0, true},
                new Object[] {STRING_MDUPLICATE1, "A\uFF21A\uFF21A\uFF21A", 1, true},
                new Object[] {STRING_SUPPLEMENTARY, "\uDC01\uFF21", 3, true},
        };
    }

    @Test(dataProvider = "provider")
    public void testStartsWith(String str, String prefix, int toffset,
            boolean expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.startsWith(prefix, toffset),
                                    expected,
                                    String.format(
                                            "testing String(%s).startsWith(%s, %d), source : %s, ",
                                            escapeNonASCIIs(data),
                                            escapeNonASCIIs(prefix), toffset,
                                            source));
                        });
    }
}
