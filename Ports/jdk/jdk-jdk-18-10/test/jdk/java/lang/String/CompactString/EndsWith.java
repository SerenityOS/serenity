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
 * @summary Tests Compact String. This one is for String.endsWith.
 * @run testng/othervm -XX:+CompactStrings EndsWith
 * @run testng/othervm -XX:-CompactStrings EndsWith
 */

public class EndsWith extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] { new Object[] { STRING_EMPTY, "", true },
                new Object[] { STRING_EMPTY, "A", false },
                new Object[] { STRING_L1, "A", true },
                new Object[] { STRING_L1, "", true },
                new Object[] { STRING_L1, " ", false },
                new Object[] { STRING_L2, "AB", true },
                new Object[] { STRING_L2, "B", true },
                new Object[] { STRING_L2, "", true },
                new Object[] { STRING_L2, "A", false },
                new Object[] { STRING_L4, "ABCD", true },
                new Object[] { STRING_L4, "CD", true },
                new Object[] { STRING_L4, "D", true },
                new Object[] { STRING_L4, "", true },
                new Object[] { STRING_L4, "BC", false },
                new Object[] { STRING_LLONG, "ABCDEFGH", true },
                new Object[] { STRING_LLONG, "EFGH", true },
                new Object[] { STRING_LLONG, "", true },
                new Object[] { STRING_LLONG, "CDEF", false },
                new Object[] { STRING_LLONG, "\uFF28", false },
                new Object[] { STRING_U1, "\uFF21", true },
                new Object[] { STRING_U1, "", true },
                new Object[] { STRING_U1, "\uFF22", false },
                new Object[] { STRING_U1, "B", false },
                new Object[] { STRING_U2, "\uFF21\uFF22", true },
                new Object[] { STRING_U2, "\uFF22", true },
                new Object[] { STRING_U2, "", true },
                new Object[] { STRING_U2, "\uFF21", false },
                new Object[] { STRING_M12, "\uFF21A", true },
                new Object[] { STRING_M12, "A", true },
                new Object[] { STRING_M12, "", true },
                new Object[] { STRING_M12, "AA", false },
                new Object[] { STRING_M11, "A\uFF21", true },
                new Object[] { STRING_M11, "\uFF21", true },
                new Object[] { STRING_M11, "", true },
                new Object[] { STRING_M11, "\uFF21\uFF21", false }, };
    }

    @Test(dataProvider = "provider")
    public void testEndsWith(String str, String suffix, boolean expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.endsWith(suffix),
                                    expected,
                                    String.format(
                                            "testing String(%s).endsWith(%s), source : %s, ",
                                            escapeNonASCIIs(data),
                                            escapeNonASCIIs(suffix), source));
                        });
    }
}
