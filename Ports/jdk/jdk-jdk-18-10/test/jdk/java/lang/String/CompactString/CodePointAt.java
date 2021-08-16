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

import java.util.stream.IntStream;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/*
 * @test
 * @bug 8077559
 * @summary Tests Compact String. This one is for String.codePointAt.
 * @run testng/othervm -XX:+CompactStrings CodePointAt
 * @run testng/othervm -XX:-CompactStrings CodePointAt
 */

public class CodePointAt extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] {

                new Object[] { STRING_L1, new int[] { 'A' } },
                new Object[] { STRING_L2, new int[] { 'A', 'B' } },
                new Object[] { STRING_L4, new int[] { 'A', 'B', 'C', 'D' } },
                new Object[] { STRING_LLONG,
                        new int[] { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H' } },
                new Object[] { STRING_U1, new int[] { '\uFF21' } },
                new Object[] { STRING_U2, new int[] { '\uFF21', '\uFF22' } },
                new Object[] { STRING_M12, new int[] { '\uFF21', 'A' } },
                new Object[] { STRING_M11, new int[] { 'A', '\uFF21' } },
                new Object[] {
                        STRING_SUPPLEMENTARY,
                        new int[] { Character.toCodePoint('\uD801', '\uDC00'),
                                '\uDC00',
                                Character.toCodePoint('\uD801', '\uDC01'),
                                '\uDC01', '\uFF21', 'A' }, } };
    }

    @Test(dataProvider = "provider")
    public void testCodePointAt(String str, int[] expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            IntStream
                                    .range(0, str.length())
                                    .forEach(
                                            i -> assertEquals(
                                                    str.codePointAt(i),
                                                    expected[i],
                                                    String.format(
                                                            "testing String(%s).codePointAt(%d), source : %s, ",
                                                            escapeNonASCIIs(data),
                                                            i, source)));
                        });
    }
}
