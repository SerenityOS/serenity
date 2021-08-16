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
 * @summary Tests Compact String. This one is for String.getChars.
 * @run testng/othervm -XX:+CompactStrings GetChars
 * @run testng/othervm -XX:-CompactStrings GetChars
 */

public class GetChars extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] {

                new Object[] { STRING_EMPTY, 0, STRING_EMPTY.length(),
                        new char[STRING_EMPTY.length()], 0, CHAR_ARRAY_EMPTY },
                new Object[] { STRING_L1, 0, STRING_L1.length(),
                        new char[STRING_L1.length()], 0, CHAR_ARRAY_L1 },
                new Object[] { STRING_L2, 0, STRING_L2.length(),
                        new char[STRING_L2.length()], 0, CHAR_ARRAY_L2 },
                new Object[] { STRING_L4, 0, STRING_L4.length(),
                        new char[STRING_L4.length()], 0, CHAR_ARRAY_L4 },
                new Object[] { STRING_LLONG, 0, STRING_LLONG.length(),
                        new char[STRING_LLONG.length()], 0, CHAR_ARRAY_LLONG },
                new Object[] { STRING_U1, 0, STRING_U1.length(),
                        new char[STRING_U1.length()], 0, CHAR_ARRAY_U1 },
                new Object[] { STRING_U2, 0, STRING_U2.length(),
                        new char[STRING_U2.length()], 0, CHAR_ARRAY_U2 },
                new Object[] { STRING_M12, 0, STRING_M12.length(),
                        new char[STRING_M12.length()], 0, CHAR_ARRAY_M12 },
                new Object[] { STRING_M11, 0, STRING_M11.length(),
                        new char[STRING_M11.length()], 0, CHAR_ARRAY_M11 },
                new Object[] { STRING_UDUPLICATE, 0,
                        STRING_UDUPLICATE.length(),
                        new char[STRING_UDUPLICATE.length()], 0,
                        CHAR_ARRAY_UDUPLICATE },
                new Object[] { STRING_MDUPLICATE1, 0,
                        STRING_MDUPLICATE1.length(),
                        new char[STRING_MDUPLICATE1.length()], 0,
                        CHAR_ARRAY_MDUPLICATE1 }, };
    }

    @Test(dataProvider = "provider")
    public void testGetChars(String str, int srcBegin, int srcEnd, char[] dst,
            int dstBegin, char[] expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            data.getChars(srcBegin, srcEnd, dst, dstBegin);
                            assertTrue(
                                    Arrays.equals(dst, expected),
                                    String.format(
                                            "testing String(%s).getChars(%d, %d, %s, %d), source : %s, ",
                                            escapeNonASCIIs(data), srcBegin,
                                            srcEnd, escapeNonASCIIs(Arrays
                                                    .toString(dst)), dstBegin,
                                            source));
                        });
    }

}
