/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/*
 * @test
 * @bug 8054307 8077559
 * @summary Tests Compact String. This test is testing StringBuilder
 *          behavior related to Compact String.
 * @run testng/othervm -XX:+CompactStrings CompactStringBuilder
 * @run testng/othervm -XX:-CompactStrings CompactStringBuilder
 */

public class CompactStringBuilder {

    /*
     * Tests for "A"
     */
    @Test
    public void testCompactStringBuilderForLatinA() {
        final String ORIGIN = "A";
        /*
         * Because right now ASCII is the default encoding parameter for source
         * code in JDK build environment, so we escape them. same as below.
         */
        check(new StringBuilder(ORIGIN).append(new char[] { '\uFF21' }),
                "A\uFF21");
        check(new StringBuilder(ORIGIN).append(new StringBuffer("\uFF21")),
                "A\uFF21");
        check(new StringBuilder(ORIGIN).append("\uFF21"), "A\uFF21");
        check(new StringBuilder(ORIGIN).append(new StringBuffer("\uFF21")),
                "A\uFF21");
        check(new StringBuilder(ORIGIN).delete(0, 1), "");
        check(new StringBuilder(ORIGIN).delete(0, 0), "A");
        check(new StringBuilder(ORIGIN).deleteCharAt(0), "");
        assertEquals(new StringBuilder(ORIGIN).indexOf("A", 0), 0);
        assertEquals(new StringBuilder(ORIGIN).indexOf("\uFF21", 0), -1);
        assertEquals(new StringBuilder(ORIGIN).indexOf("", 0), 0);
        assertEquals(new StringBuilder(ORIGIN).insert(1, "\uD801\uDC00")
                .indexOf("A", 0), 0);
        assertEquals(new StringBuilder(ORIGIN).insert(0, "\uD801\uDC00")
                .indexOf("A", 0), 2);
        check(new StringBuilder(ORIGIN).insert(0, new char[] {}), "A");
        check(new StringBuilder(ORIGIN).insert(1, new char[] { '\uFF21' }),
                "A\uFF21");
        check(new StringBuilder(ORIGIN).insert(0, new char[] { '\uFF21' }),
                "\uFF21A");
        check(new StringBuilder(ORIGIN).insert(0, new StringBuffer("\uFF21")),
                "\uFF21A");
        check(new StringBuilder(ORIGIN).insert(1, new StringBuffer("\uFF21")),
                "A\uFF21");
        check(new StringBuilder(ORIGIN).insert(0, ""), "A");
        check(new StringBuilder(ORIGIN).insert(0, "\uFF21"), "\uFF21A");
        check(new StringBuilder(ORIGIN).insert(1, "\uFF21"), "A\uFF21");
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("A"), 0);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("\uFF21"), -1);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf(""), 1);
        check(new StringBuilder(ORIGIN).replace(0, 0, "\uFF21"), "\uFF21A");
        check(new StringBuilder(ORIGIN).replace(0, 1, "\uFF21"), "\uFF21");
        checkSetCharAt(new StringBuilder(ORIGIN), 0, '\uFF21', "\uFF21");
        checkSetLength(new StringBuilder(ORIGIN), 0, "");
        checkSetLength(new StringBuilder(ORIGIN), 1, "A");
        check(new StringBuilder(ORIGIN).substring(0), "A");
        check(new StringBuilder(ORIGIN).substring(1), "");
    }

    /*
     * Tests for "\uFF21"
     */
    @Test
    public void testCompactStringBuilderForNonLatinA() {
        final String ORIGIN = "\uFF21";
        check(new StringBuilder(ORIGIN).append(new char[] { 'A' }), "\uFF21A");
        check(new StringBuilder(ORIGIN).append(new StringBuffer("A")), "\uFF21A");
        check(new StringBuilder(ORIGIN).append("A"), "\uFF21A");
        check(new StringBuilder(ORIGIN).append(new StringBuffer("A")), "\uFF21A");
        check(new StringBuilder(ORIGIN).delete(0, 1), "");
        check(new StringBuilder(ORIGIN).delete(0, 0), "\uFF21");
        check(new StringBuilder(ORIGIN).deleteCharAt(0), "");
        assertEquals(new StringBuilder(ORIGIN).indexOf("A", 0), -1);
        assertEquals(new StringBuilder(ORIGIN).indexOf("\uFF21", 0), 0);
        assertEquals(new StringBuilder(ORIGIN).indexOf("", 0), 0);
        check(new StringBuilder(ORIGIN).insert(0, new char[] {}), "\uFF21");
        check(new StringBuilder(ORIGIN).insert(1, new char[] { 'A' }), "\uFF21A");
        check(new StringBuilder(ORIGIN).insert(0, new char[] { 'A' }), "A\uFF21");
        check(new StringBuilder(ORIGIN).insert(0, new StringBuffer("A")),
                "A\uFF21");
        check(new StringBuilder(ORIGIN).insert(1, new StringBuffer("A")),
                "\uFF21A");
        check(new StringBuilder(ORIGIN).insert(0, ""), "\uFF21");
        check(new StringBuilder(ORIGIN).insert(0, "A"), "A\uFF21");
        check(new StringBuilder(ORIGIN).insert(1, "A"), "\uFF21A");
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("A"), -1);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("\uFF21"), 0);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf(""), 1);
        check(new StringBuilder(ORIGIN).replace(0, 0, "A"), "A\uFF21");
        check(new StringBuilder(ORIGIN).replace(0, 1, "A"), "A");
        checkSetCharAt(new StringBuilder(ORIGIN), 0, 'A', "A");
        checkSetLength(new StringBuilder(ORIGIN), 0, "");
        checkSetLength(new StringBuilder(ORIGIN), 1, "\uFF21");
        check(new StringBuilder(ORIGIN).substring(0), "\uFF21");
        check(new StringBuilder(ORIGIN).substring(1), "");
    }

    /*
     * Tests for "\uFF21A"
     */
    @Test
    public void testCompactStringBuilderForMixedA1() {
        final String ORIGIN = "\uFF21A";
        check(new StringBuilder(ORIGIN).delete(0, 1), "A");
        check(new StringBuilder(ORIGIN).delete(1, 2), "\uFF21");
        check(new StringBuilder(ORIGIN).deleteCharAt(1), "\uFF21");
        check(new StringBuilder(ORIGIN).deleteCharAt(0), "A");
        assertEquals(new StringBuilder(ORIGIN).indexOf("A", 0), 1);
        assertEquals(new StringBuilder(ORIGIN).indexOf("\uFF21", 0), 0);
        assertEquals(new StringBuilder(ORIGIN).indexOf("", 0), 0);
        check(new StringBuilder(ORIGIN).insert(1, new char[] { 'A' }),
                "\uFF21AA");
        check(new StringBuilder(ORIGIN).insert(0, new char[] { '\uFF21' }),
                "\uFF21\uFF21A");
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("A"), 1);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("\uFF21"), 0);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf(""), 2);
        check(new StringBuilder(ORIGIN).replace(0, 0, "A"), "A\uFF21A");
        check(new StringBuilder(ORIGIN).replace(0, 1, "A"), "AA");
        checkSetCharAt(new StringBuilder(ORIGIN), 0, 'A', "AA");
        checkSetLength(new StringBuilder(ORIGIN), 0, "");
        checkSetLength(new StringBuilder(ORIGIN), 1, "\uFF21");
        check(new StringBuilder(ORIGIN).substring(0), "\uFF21A");
        check(new StringBuilder(ORIGIN).substring(1), "A");
    }

    /*
     * Tests for "A\uFF21"
     */
    @Test
    public void testCompactStringBuilderForMixedA2() {
        final String ORIGIN = "A\uFF21";
        check(new StringBuilder(ORIGIN).replace(1, 2, "A"), "AA");
        checkSetLength(new StringBuilder(ORIGIN), 1, "A");
        check(new StringBuilder(ORIGIN).substring(0), "A\uFF21");
        check(new StringBuilder(ORIGIN).substring(1), "\uFF21");
        check(new StringBuilder(ORIGIN).substring(0, 1), "A");
    }

    /*
     * Tests for "\uFF21A\uFF21A\uFF21A\uFF21A\uFF21A"
     */
    @Test
    public void testCompactStringBuilderForDuplicatedMixedA1() {
        final String ORIGIN = "\uFF21A\uFF21A\uFF21A\uFF21A\uFF21A";
        checkSetLength(new StringBuilder(ORIGIN), 1, "\uFF21");
        assertEquals(new StringBuilder(ORIGIN).indexOf("A", 5), 5);
        assertEquals(new StringBuilder(ORIGIN).indexOf("\uFF21", 5), 6);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("A"), 9);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("\uFF21"), 8);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf(""), 10);
        check(new StringBuilder(ORIGIN).substring(9), "A");
        check(new StringBuilder(ORIGIN).substring(8), "\uFF21A");
    }

    /*
     * Tests for "A\uFF21A\uFF21A\uFF21A\uFF21A\uFF21"
     */
    @Test
    public void testCompactStringBuilderForDuplicatedMixedA2() {
        final String ORIGIN = "A\uFF21A\uFF21A\uFF21A\uFF21A\uFF21";
        checkSetLength(new StringBuilder(ORIGIN), 1, "A");
        assertEquals(new StringBuilder(ORIGIN).indexOf("A", 5), 6);
        assertEquals(new StringBuilder(ORIGIN).indexOf("\uFF21", 5), 5);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("A"), 8);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("\uFF21"), 9);
        check(new StringBuilder(ORIGIN).substring(9), "\uFF21");
        check(new StringBuilder(ORIGIN).substring(8), "A\uFF21");
    }

    /*
     * Tests for "\uD801\uDC00\uD801\uDC01"
     */
    @Test
    public void testCompactStringForSupplementaryCodePoint() {
        final String ORIGIN = "\uD801\uDC00\uD801\uDC01";
        check(new StringBuilder(ORIGIN).append("A"), "\uD801\uDC00\uD801\uDC01A");
        check(new StringBuilder(ORIGIN).append("\uFF21"),
                "\uD801\uDC00\uD801\uDC01\uFF21");
        check(new StringBuilder(ORIGIN).appendCodePoint('A'),
                "\uD801\uDC00\uD801\uDC01A");
        check(new StringBuilder(ORIGIN).appendCodePoint('\uFF21'),
                "\uD801\uDC00\uD801\uDC01\uFF21");
        assertEquals(new StringBuilder(ORIGIN).charAt(0), '\uD801');
        assertEquals(new StringBuilder(ORIGIN).codePointAt(0),
                Character.codePointAt(ORIGIN, 0));
        assertEquals(new StringBuilder(ORIGIN).codePointAt(1),
                Character.codePointAt(ORIGIN, 1));
        assertEquals(new StringBuilder(ORIGIN).codePointBefore(2),
                Character.codePointAt(ORIGIN, 0));
        assertEquals(new StringBuilder(ORIGIN).codePointCount(1, 3), 2);
        check(new StringBuilder(ORIGIN).delete(0, 2), "\uD801\uDC01");
        check(new StringBuilder(ORIGIN).delete(0, 3), "\uDC01");
        check(new StringBuilder(ORIGIN).deleteCharAt(1), "\uD801\uD801\uDC01");
        checkGetChars(new StringBuilder(ORIGIN), 0, 3, new char[] { '\uD801',
                '\uDC00', '\uD801' });
        assertEquals(new StringBuilder(ORIGIN).indexOf("\uD801\uDC01"), 2);
        assertEquals(new StringBuilder(ORIGIN).indexOf("\uDC01"), 3);
        assertEquals(new StringBuilder(ORIGIN).indexOf("\uFF21"), -1);
        assertEquals(new StringBuilder(ORIGIN).indexOf("A"), -1);
        check(new StringBuilder(ORIGIN).insert(0, "\uFF21"),
                "\uFF21\uD801\uDC00\uD801\uDC01");
        check(new StringBuilder(ORIGIN).insert(1, "\uFF21"),
                "\uD801\uFF21\uDC00\uD801\uDC01");
        check(new StringBuilder(ORIGIN).insert(1, "A"),
                "\uD801A\uDC00\uD801\uDC01");
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("\uDC00\uD801"), 1);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("\uD801"), 2);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("\uFF21"), -1);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("A"), -1);
        assertEquals(new StringBuilder(ORIGIN).length(), 4);
        assertEquals(new StringBuilder(ORIGIN).offsetByCodePoints(1, 1), 2);
        assertEquals(new StringBuilder(ORIGIN).offsetByCodePoints(0, 1), 2);
        check(new StringBuilder(ORIGIN).replace(0, 2, "A"), "A\uD801\uDC01");
        check(new StringBuilder(ORIGIN).replace(0, 3, "A"), "A\uDC01");
        check(new StringBuilder(ORIGIN).replace(0, 2, "\uFF21"),
                "\uFF21\uD801\uDC01");
        check(new StringBuilder(ORIGIN).replace(0, 3, "\uFF21"), "\uFF21\uDC01");
        check(new StringBuilder(ORIGIN).reverse(), "\uD801\uDC01\uD801\uDC00");
        checkSetCharAt(new StringBuilder(ORIGIN), 1, '\uDC01',
                "\uD801\uDC01\uD801\uDC01");
        checkSetCharAt(new StringBuilder(ORIGIN), 1, 'A', "\uD801A\uD801\uDC01");
        checkSetLength(new StringBuilder(ORIGIN), 2, "\uD801\uDC00");
        checkSetLength(new StringBuilder(ORIGIN), 3, "\uD801\uDC00\uD801");
        check(new StringBuilder(ORIGIN).substring(1, 3), "\uDC00\uD801");
    }

    /*
     * Tests for "A\uD801\uDC00\uFF21"
     */
    @Test
    public void testCompactStringForSupplementaryCodePointMixed1() {
        final String ORIGIN = "A\uD801\uDC00\uFF21";
        assertEquals(new StringBuilder(ORIGIN).codePointBefore(3),
                Character.codePointAt(ORIGIN, 1));
        assertEquals(new StringBuilder(ORIGIN).codePointBefore(2), '\uD801');
        assertEquals(new StringBuilder(ORIGIN).codePointBefore(1), 'A');
        assertEquals(new StringBuilder(ORIGIN).codePointCount(0, 3), 2);
        assertEquals(new StringBuilder(ORIGIN).codePointCount(0, 4), 3);
        check(new StringBuilder(ORIGIN).delete(0, 1), "\uD801\uDC00\uFF21");
        check(new StringBuilder(ORIGIN).delete(0, 1).delete(2, 3),
                "\uD801\uDC00");
        check(new StringBuilder(ORIGIN).deleteCharAt(3).deleteCharAt(0),
                "\uD801\uDC00");
        assertEquals(new StringBuilder(ORIGIN).indexOf("\uFF21"), 3);
        assertEquals(new StringBuilder(ORIGIN).indexOf("A"), 0);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("\uFF21"), 3);
        assertEquals(new StringBuilder(ORIGIN).lastIndexOf("A"), 0);
        assertEquals(new StringBuilder(ORIGIN).offsetByCodePoints(0, 1), 1);
        assertEquals(new StringBuilder(ORIGIN).offsetByCodePoints(1, 1), 3);
        check(new StringBuilder(ORIGIN).replace(1, 3, "A"), "AA\uFF21");
        check(new StringBuilder(ORIGIN).replace(1, 4, "A"), "AA");
        check(new StringBuilder(ORIGIN).replace(1, 4, ""), "A");
        check(new StringBuilder(ORIGIN).reverse(), "\uFF21\uD801\uDC00A");
        checkSetLength(new StringBuilder(ORIGIN), 1, "A");
        check(new StringBuilder(ORIGIN).substring(0, 1), "A");
    }

    /*
     * Tests for "\uD801\uDC00\uFF21A"
     */
    @Test
    public void testCompactStringForSupplementaryCodePointMixed2() {
        final String ORIGIN = "\uD801\uDC00\uFF21A";
        assertEquals(new StringBuilder(ORIGIN).codePointBefore(3),
                Character.codePointAt(ORIGIN, 2));
        assertEquals(new StringBuilder(ORIGIN).codePointBefore(2),
                Character.codePointAt(ORIGIN, 0));
        assertEquals(new StringBuilder(ORIGIN).codePointBefore(1), '\uD801');
        assertEquals(new StringBuilder(ORIGIN).codePointCount(0, 3), 2);
        assertEquals(new StringBuilder(ORIGIN).codePointCount(0, 4), 3);
        check(new StringBuilder(ORIGIN).delete(0, 2), "\uFF21A");
        check(new StringBuilder(ORIGIN).delete(0, 3), "A");
        check(new StringBuilder(ORIGIN).deleteCharAt(0).deleteCharAt(0)
                .deleteCharAt(0), "A");
        assertEquals(new StringBuilder(ORIGIN).indexOf("A"), 3);
        assertEquals(new StringBuilder(ORIGIN).delete(0, 3).indexOf("A"), 0);
        assertEquals(new StringBuilder(ORIGIN).replace(0, 3, "B").indexOf("A"),
                1);
        assertEquals(new StringBuilder(ORIGIN).substring(3, 4).indexOf("A"), 0);
        assertEquals(new StringBuilder(ORIGIN).offsetByCodePoints(1, 1), 2);
        assertEquals(new StringBuilder(ORIGIN).offsetByCodePoints(0, 1), 2);
        assertEquals(new StringBuilder(ORIGIN).offsetByCodePoints(2, 1), 3);
        check(new StringBuilder(ORIGIN).replace(0, 3, "B"), "BA");
        check(new StringBuilder(ORIGIN).reverse(), "A\uFF21\uD801\uDC00");
    }

    /*
     * Tests for "\uD801A\uDC00\uFF21"
     */
    @Test
    public void testCompactStringForSupplementaryCodePointMixed3() {
        final String ORIGIN = "\uD801A\uDC00\uFF21";
        assertEquals(new StringBuilder(ORIGIN).codePointAt(1), 'A');
        assertEquals(new StringBuilder(ORIGIN).codePointAt(3), '\uFF21');
        assertEquals(new StringBuilder(ORIGIN).codePointBefore(1), '\uD801');
        assertEquals(new StringBuilder(ORIGIN).codePointBefore(2), 'A');
        assertEquals(new StringBuilder(ORIGIN).codePointBefore(3), '\uDC00');
        assertEquals(new StringBuilder(ORIGIN).codePointCount(0, 3), 3);
        assertEquals(new StringBuilder(ORIGIN).codePointCount(1, 3), 2);
        assertEquals(new StringBuilder(ORIGIN).delete(0, 1).delete(1, 3)
                .indexOf("A"), 0);
        assertEquals(
                new StringBuilder(ORIGIN).replace(0, 1, "B").replace(2, 4, "C")
                        .indexOf("A"), 1);
        assertEquals(new StringBuilder(ORIGIN).substring(1, 4).substring(0, 1)
                .indexOf("A"), 0);
        assertEquals(new StringBuilder(ORIGIN).offsetByCodePoints(0, 1), 1);
        assertEquals(new StringBuilder(ORIGIN).offsetByCodePoints(1, 1), 2);
        assertEquals(new StringBuilder(ORIGIN).offsetByCodePoints(2, 1), 3);
        check(new StringBuilder(ORIGIN).reverse(), "\uFF21\uDC00A\uD801");
    }

    /*
     * Tests for "A\uDC01\uFF21\uD801"
     */
    @Test
    public void testCompactStringForSupplementaryCodePointMixed4() {
        final String ORIGIN = "A\uDC01\uFF21\uD801";
        assertEquals(new StringBuilder(ORIGIN).codePointAt(1), '\uDC01');
        assertEquals(new StringBuilder(ORIGIN).codePointAt(3), '\uD801');
        assertEquals(new StringBuilder(ORIGIN).codePointBefore(1), 'A');
        assertEquals(new StringBuilder(ORIGIN).codePointBefore(2), '\uDC01');
        assertEquals(new StringBuilder(ORIGIN).codePointBefore(3), '\uFF21');
        assertEquals(new StringBuilder(ORIGIN).codePointCount(0, 3), 3);
        assertEquals(new StringBuilder(ORIGIN).codePointCount(1, 3), 2);
        assertEquals(new StringBuilder(ORIGIN).delete(1, 4).indexOf("A"), 0);
        assertEquals(new StringBuilder(ORIGIN).replace(1, 4, "B").indexOf("A"),
                0);
        assertEquals(new StringBuilder(ORIGIN).substring(0, 1).indexOf("A"), 0);
        assertEquals(new StringBuilder(ORIGIN).offsetByCodePoints(0, 1), 1);
        assertEquals(new StringBuilder(ORIGIN).offsetByCodePoints(1, 1), 2);
        assertEquals(new StringBuilder(ORIGIN).offsetByCodePoints(2, 1), 3);
        check(new StringBuilder(ORIGIN).reverse(), "\uD801\uFF21\uDC01A");
    }

    private void checkGetChars(StringBuilder sb, int srcBegin, int srcEnd,
            char expected[]) {
        char[] dst = new char[srcEnd - srcBegin];
        sb.getChars(srcBegin, srcEnd, dst, 0);
        assertTrue(Arrays.equals(dst, expected));
    }

    private void checkSetCharAt(StringBuilder sb, int index, char ch,
            String expected) {
        sb.setCharAt(index, ch);
        check(sb, expected);
    }

    private void checkSetLength(StringBuilder sb, int newLength, String expected) {
        sb.setLength(newLength);
        check(sb, expected);
    }

    private void check(StringBuilder sb, String expected) {
        check(sb.toString(), expected);
    }

    private void check(String str, String expected) {
        assertTrue(str.equals(expected), String.format(
                "Get (%s) but expect (%s), ", escapeNonASCIIs(str),
                escapeNonASCIIs(expected)));
    }

    /*
     * Escape non-ASCII characters since not all systems support them.
     */
    private String escapeNonASCIIs(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c > 0x7F) {
                sb.append("\\u").append(Integer.toHexString((int) c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
