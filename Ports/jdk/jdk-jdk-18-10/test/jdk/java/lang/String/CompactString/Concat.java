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
 * @summary Tests Compact String. This one is for String.concat.
 * @run testng/othervm -XX:+CompactStrings Concat
 * @run testng/othervm -XX:-CompactStrings Concat
 */

public class Concat extends CompactString {

    @DataProvider
    public Object[][] provider() {
        return new Object[][] {
                new Object[] { STRING_EMPTY, "ABC", "ABC" },
                new Object[] { STRING_EMPTY,
                        "ABC".concat("\uFF21\uFF22\uFF23").concat("DEF"),
                        "ABC\uFF21\uFF22\uFF23DEF" },
                new Object[] {
                        STRING_EMPTY,
                        "\uFF21\uFF22\uFF23".concat("ABC").concat(
                                "\uFF24\uFF25\uFF26"),
                        "\uFF21\uFF22\uFF23ABC\uFF24\uFF25\uFF26" },
                new Object[] { STRING_L1,
                        "ABC".concat("\uFF21\uFF22\uFF23").concat("DEF"),
                        "AABC\uFF21\uFF22\uFF23DEF" },
                new Object[] {
                        STRING_L1,
                        "\uFF21\uFF22\uFF23".concat("ABC").concat(
                                "\uFF24\uFF25\uFF26"),
                        "A\uFF21\uFF22\uFF23ABC\uFF24\uFF25\uFF26" },
                new Object[] { STRING_L2,
                        "ABC".concat("\uFF21\uFF22\uFF23").concat("DEF"),
                        "ABABC\uFF21\uFF22\uFF23DEF" },
                new Object[] {
                        STRING_L2,
                        "\uFF21\uFF22\uFF23".concat("ABC").concat(
                                "\uFF24\uFF25\uFF26"),
                        "AB\uFF21\uFF22\uFF23ABC\uFF24\uFF25\uFF26" },
                new Object[] { STRING_L4,
                        "ABC".concat("\uFF21\uFF22\uFF23").concat("DEF"),
                        "ABCDABC\uFF21\uFF22\uFF23DEF" },
                new Object[] {
                        STRING_L4,
                        "\uFF21\uFF22\uFF23".concat("ABC").concat(
                                "\uFF24\uFF25\uFF26"),
                        "ABCD\uFF21\uFF22\uFF23ABC\uFF24\uFF25\uFF26" },
                new Object[] { STRING_LLONG,
                        "ABC".concat("\uFF21\uFF22\uFF23").concat("DEF"),
                        "ABCDEFGHABC\uFF21\uFF22\uFF23DEF" },
                new Object[] {
                        STRING_LLONG,
                        "\uFF21\uFF22\uFF23".concat("ABC").concat(
                                "\uFF24\uFF25\uFF26"),
                        "ABCDEFGH\uFF21\uFF22\uFF23ABC\uFF24\uFF25\uFF26" },
                new Object[] { STRING_U1,
                        "ABC".concat("\uFF21\uFF22\uFF23").concat("DEF"),
                        "\uFF21ABC\uFF21\uFF22\uFF23DEF" },
                new Object[] {
                        STRING_U1,
                        "\uFF21\uFF22\uFF23".concat("ABC").concat(
                                "\uFF24\uFF25\uFF26"),
                        "\uFF21\uFF21\uFF22\uFF23ABC\uFF24\uFF25\uFF26" },
                new Object[] { STRING_U2,
                        "ABC".concat("\uFF21\uFF22\uFF23").concat("DEF"),
                        "\uFF21\uFF22ABC\uFF21\uFF22\uFF23DEF" },
                new Object[] {
                        STRING_U2,
                        "\uFF21\uFF22\uFF23".concat("ABC").concat(
                                "\uFF24\uFF25\uFF26"),
                        "\uFF21\uFF22\uFF21\uFF22\uFF23ABC\uFF24\uFF25\uFF26" },
                new Object[] { STRING_M12,
                        "ABC".concat("\uFF21\uFF22\uFF23").concat("DEF"),
                        "\uFF21AABC\uFF21\uFF22\uFF23DEF" },
                new Object[] {
                        STRING_M12,
                        "\uFF21\uFF22\uFF23".concat("ABC").concat(
                                "\uFF24\uFF25\uFF26"),
                        "\uFF21A\uFF21\uFF22\uFF23ABC\uFF24\uFF25\uFF26" },
                new Object[] { STRING_M11,
                        "ABC".concat("\uFF21\uFF22\uFF23").concat("DEF"),
                        "A\uFF21ABC\uFF21\uFF22\uFF23DEF" },
                new Object[] {
                        STRING_M11,
                        "\uFF21\uFF22\uFF23".concat("ABC").concat(
                                "\uFF24\uFF25\uFF26"),
                        "A\uFF21\uFF21\uFF22\uFF23ABC\uFF24\uFF25\uFF26" }, };
    }

    @Test(dataProvider = "provider")
    public void testConcat(String str, String anotherString, String expected) {
        map.get(str)
                .forEach(
                        (source, data) -> {
                            assertEquals(
                                    data.concat(anotherString),
                                    expected,
                                    String.format(
                                            "testing String(%s).concat(%s), source : %s, ",
                                            escapeNonASCIIs(data),
                                            escapeNonASCIIs(anotherString),
                                            source));
                        });
    }
}
