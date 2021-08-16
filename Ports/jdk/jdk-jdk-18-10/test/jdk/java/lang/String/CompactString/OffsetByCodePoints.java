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
 * @summary Tests Compact String. This one is for String.offsetByCodePoints.
 * @run testng/othervm -XX:+CompactStrings OffsetByCodePoints
 * @run testng/othervm -XX:-CompactStrings OffsetByCodePoints
 */

public class OffsetByCodePoints extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] {

        new Object[] { STRING_SUPPLEMENTARY, 0, 1, 2 },
                new Object[] { STRING_SUPPLEMENTARY, 0, 3, 5 },
                new Object[] { STRING_SUPPLEMENTARY, 1, 1, 2 },
                new Object[] { STRING_SUPPLEMENTARY, 1, 3, 5 },
                new Object[] { STRING_SUPPLEMENTARY, 2, 1, 4 },
                new Object[] { STRING_SUPPLEMENTARY, 2, 2, 5 },
                new Object[] { STRING_SUPPLEMENTARY, 2, 3, 6 },
                new Object[] { STRING_SUPPLEMENTARY, 3, 1, 4 },
                new Object[] { STRING_SUPPLEMENTARY, 3, 2, 5 },
                new Object[] { STRING_SUPPLEMENTARY, 3, 3, 6 }, };
    }

    @Test(dataProvider = "provider")
    public void testOffsetByCodePoints(String str, int index,
            int codePointOffset, int expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.offsetByCodePoints(index,
                                            codePointOffset),
                                    expected,
                                    String.format(
                                            "testing String(%s).offsetByCodePoints(%d, %d), source : %s, ",
                                            escapeNonASCIIs(data), index,
                                            codePointOffset, source));
                        });
    }
}
