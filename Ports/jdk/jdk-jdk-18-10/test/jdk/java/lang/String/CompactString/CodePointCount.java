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
 * @summary Tests Compact String. This one is for String.codePointCount.
 * @run testng/othervm -XX:+CompactStrings CodePointCount
 * @run testng/othervm -XX:-CompactStrings CodePointCount
 */

public class CodePointCount extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] { new Object[] { STRING_EMPTY, 0, 0, 0 },
                new Object[] { STRING_L1, 0, 1, 1 },
                new Object[] { STRING_L1, 1, 1, 0 },
                new Object[] { STRING_L2, 0, 2, 2 },
                new Object[] { STRING_L2, 0, 1, 1 },
                new Object[] { STRING_L2, 1, 2, 1 },
                new Object[] { STRING_L4, 0, 4, 4 },
                new Object[] { STRING_L4, 0, 1, 1 },
                new Object[] { STRING_L4, 2, 4, 2 },
                new Object[] { STRING_LLONG, 0, 8, 8 },
                new Object[] { STRING_LLONG, 0, 5, 5 },
                new Object[] { STRING_LLONG, 4, 8, 4 },
                new Object[] { STRING_LLONG, 0, 7, 7 },
                new Object[] { STRING_U1, 0, 1, 1 },
                new Object[] { STRING_U2, 0, 2, 2 },
                new Object[] { STRING_U2, 0, 1, 1 },
                new Object[] { STRING_U2, 1, 2, 1 },
                new Object[] { STRING_M12, 0, 2, 2 },
                new Object[] { STRING_M12, 0, 1, 1 },
                new Object[] { STRING_M12, 1, 2, 1 },
                new Object[] { STRING_M11, 0, 2, 2 },
                new Object[] { STRING_M11, 0, 1, 1 },
                new Object[] { STRING_M11, 1, 2, 1 },
                new Object[] { STRING_SUPPLEMENTARY, 0, 1, 1 },
                new Object[] { STRING_SUPPLEMENTARY, 0, 2, 1 },
                new Object[] { STRING_SUPPLEMENTARY, 0, 3, 2 },
                new Object[] { STRING_SUPPLEMENTARY, 0, 5, 3 },
                new Object[] { STRING_SUPPLEMENTARY, 0, 6, 4 },
                new Object[] { STRING_SUPPLEMENTARY, 1, 4, 2 },
                new Object[] { STRING_SUPPLEMENTARY, 1, 6, 4 },
                new Object[] { STRING_SUPPLEMENTARY, 2, 4, 1 },};
    }

    @Test(dataProvider = "provider")
    public void testCodePointCount(String str, int beginIndex, int endIndex,
            int expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.codePointCount(beginIndex, endIndex),
                                    expected,
                                    String.format(
                                            "testing String(%s).codePointCount(%d, %d), source : %s, ",
                                            escapeNonASCIIs(data), beginIndex,
                                            endIndex, source));
                        });
    }
}
