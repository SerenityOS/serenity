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
 * @summary Tests Compact String. This one is for String.toCharArray.
 * @run testng/othervm -XX:+CompactStrings ToCharArray
 * @run testng/othervm -XX:-CompactStrings ToCharArray
 */

public class ToCharArray extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] {
                new Object[] { STRING_EMPTY, new char[] {} },
                new Object[] { STRING_L1, new char[] { 'A' } },
                new Object[] { STRING_L2, new char[] { 'A', 'B' } },
                new Object[] { STRING_LLONG,
                        new char[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H' } },
                new Object[] { STRING_U1, new char[] { '\uFF21' } },
                new Object[] { STRING_U2, new char[] { '\uFF21', '\uFF22' } },
                new Object[] { STRING_M12, new char[] { '\uFF21', 'A' } },
                new Object[] { STRING_M11, new char[] { 'A', '\uFF21' } }, };
    }

    @Test(dataProvider = "provider")
    public void testToCharArray(String str, char[] expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertTrue(
                                    Arrays.equals(data.toCharArray(), expected),
                                    String.format(
                                            "testing String(%s).toCharArray(), source : %s, ",
                                            escapeNonASCIIs(data), source));
                        });
    }
}
