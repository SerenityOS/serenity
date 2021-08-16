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
 * @summary Tests Compact String. This one is for String.contains.
 * @run testng/othervm -XX:+CompactStrings Contains
 * @run testng/othervm -XX:-CompactStrings Contains
 */

public class Contains extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] {

        new Object[] { STRING_EMPTY, "", true },
                new Object[] { STRING_EMPTY, "A", false },
                new Object[] { STRING_EMPTY, "\uFF21", false },
                new Object[] { STRING_L1, "", true },
                new Object[] { STRING_L1, "A", true },
                new Object[] { STRING_L1, "\uFF21", false },
                new Object[] { STRING_L2, "", true },
                new Object[] { STRING_L2, "A", true },
                new Object[] { STRING_L2, "AB", true },
                new Object[] { STRING_L2, "B", true },
                new Object[] { STRING_L2, "ABC", false },
                new Object[] { STRING_L2, "ab", false },
                new Object[] { STRING_L4, "ABCD", true },
                new Object[] { STRING_L4, "BC", true },
                new Object[] { STRING_LLONG, "ABCDEFGH", true },
                new Object[] { STRING_LLONG, "BCDEFGH", true },
                new Object[] { STRING_LLONG, "EF", true },
                new Object[] { STRING_U1, "", true },
                new Object[] { STRING_U1, "\uFF21", true },
                new Object[] { STRING_U1, "a", false },
                new Object[] { STRING_U1, "\uFF21B", false },
                new Object[] { STRING_U2, "", true },
                new Object[] { STRING_U2, "\uFF21\uFF22", true },
                new Object[] { STRING_U2, "a", false },
                new Object[] { STRING_U2, "\uFF21B", false },
                new Object[] { STRING_M12, "\uFF21A", true },
                new Object[] { STRING_M12, "\uFF21", true },
                new Object[] { STRING_M12, "A", true },
                new Object[] { STRING_M12, "A\uFF21", false },
                new Object[] { STRING_M11, "A\uFF21", true },
                new Object[] { STRING_M11, "Ab", false }, };
    }

    @Test(dataProvider = "provider")
    public void testContains(String str, String anotherString, boolean expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.contains(anotherString),
                                    expected,
                                    String.format(
                                            "testing String(%s).contains(%s), source : %s, ",
                                            escapeNonASCIIs(data),
                                            escapeNonASCIIs(anotherString),
                                            source));
                        });
    }
}
