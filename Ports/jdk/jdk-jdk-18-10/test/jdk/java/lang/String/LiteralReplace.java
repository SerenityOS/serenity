/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* @test
 * @bug 8058779 8054307 8222955
 * @library /test/lib
 * @build jdk.test.lib.RandomFactory
 * @run testng LiteralReplace
 * @summary Basic tests of String.replace(CharSequence, CharSequence)
 * @key randomness
 */

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Random;

import jdk.test.lib.RandomFactory;

import org.testng.annotations.Test;
import org.testng.annotations.DataProvider;
import static org.testng.Assert.fail;

public class LiteralReplace {

    @Test(dataProvider="sourceTargetReplacementExpected")
    public void testExpected(String source, String target,
             String replacement, String expected)
    {
        String canonical = canonicalReplace(source, target, replacement);
        if (!canonical.equals(expected)) {
            fail("Canonical: " + canonical + " != " + expected);
        }
        test0(source, target, replacement, expected);
    }

    @Test(dataProvider="sourceTargetReplacement")
    public void testCanonical(String source, String target,
            String replacement)
    {
        String canonical = canonicalReplace(source, target, replacement);
        test0(source, target, replacement, canonical);
    }

    private void test0(String source, String target, String replacement,
            String expected)
    {
        String result = source.replace(target, replacement);
        if (!result.equals(expected)) {
            fail(result + " != " + expected);
        }
    }

    @Test(dataProvider="sourceTargetReplacementWithNull",
            expectedExceptions = {NullPointerException.class})
    public void testNPE(String source, String target, String replacement) {
        source.replace(target, replacement);
    }

    @Test(expectedExceptions = {OutOfMemoryError.class})
    public void testOOM() {
        "1".repeat(65537).replace("1", "2".repeat(65537));
    }

    @DataProvider
    public static Object[][] sourceTargetReplacementExpected() {
        return new Object[][] {
            {"aaa", "aa", "b", "ba"},
            {"abcdefgh", "def", "DEF", "abcDEFgh"},
            {"abcdefgh", "123", "DEF", "abcdefgh"},
            {"abcdefgh", "abcdefghi", "DEF", "abcdefgh"},
            {"abcdefghabc", "abc", "DEF", "DEFdefghDEF"},
            {"abcdefghdef", "def", "", "abcgh"},
            {"abcdefgh", "", "_", "_a_b_c_d_e_f_g_h_"},
            {"", "", "", ""},
            {"", "a", "b", ""},
            {"", "", "abc", "abc"},
            {"abcdefgh", "abcdefgh", "abcdefgh", "abcdefgh"},
            {"abcdefgh", "abcdefgh", "abcdefghi", "abcdefghi"},
            {"abcdefgh", "abcdefgh", "", ""},
            {"abcdabcd", "abcd", "", ""},
            {"aaaaaaaaa", "aa", "_X_", "_X__X__X__X_a"},
            {"aaaaaaaaa", "aa", "aaa", "aaaaaaaaaaaaa"},
            {"aaaaaaaaa", "aa", "aa", "aaaaaaaaa"},
            {"a.c.e.g.", ".", "-", "a-c-e-g-"},
            {"abcdefgh", "[a-h]", "X", "abcdefgh"},
            {"aa+", "a+", "", "a"},
            {"^abc$", "abc", "x", "^x$"},
            {"abc", "b", "_", "a_c"},
            {"abc", "bc", "_", "a_"},
            {"abc".repeat(65537) + "end", "b", "_XYZ_", "a_XYZ_c".repeat(65537) + "end"},
            {"abc".repeat(65537) + "end", "a", "_", "_bc".repeat(65537) + "end"},
            {"a".repeat(65537), "a", "", ""},
            {"ab".repeat(65537), "a", "", "b".repeat(65537)},
            {"ab".repeat(65537), "ab", "", ""},
            {"b" + "ab".repeat(65537), "ab", "", "b"},

            // more with non-latin1 characters
            {"\u4e00\u4e00\u4e00",
             "\u4e00\u4e00",
             "\u4e01",
             "\u4e01\u4e00"},

            {"\u4e00\u4e01\u4e02\u4e03\u4e04\u4e05\u4e06\u4e07\u4e08",
             "\u4e03\u4e04\u4e05",
             "\u4e10\u4e11\u4e12",
             "\u4e00\u4e01\u4e02\u4e10\u4e11\u4e12\u4e06\u4e07\u4e08"},

            {"\u4e00\u4e01\u4e02\u4e03\u4e04\u4e05\u4e06\u4e07\u4e08",
             "ABC",
             "\u4e10\u4e11\u4e12",
             "\u4e00\u4e01\u4e02\u4e03\u4e04\u4e05\u4e06\u4e07\u4e08"},

            {"\u4e00\u4e01\u4e02\u4e03\u4e04\u4e02\u4e03\u4e07\u4e08",
             "\u4e02\u4e03",
             "\u4e12\u4e13",
             "\u4e00\u4e01\u4e12\u4e13\u4e04\u4e12\u4e13\u4e07\u4e08"},

            {"\u4e00\u4e01\u4e02\u4e03\u4e04\u4e02\u4e03\u4e07\u4e08",
             "\u4e02\u4e03",
             "ab",
             "\u4e00\u4e01ab\u4e04ab\u4e07\u4e08"},

            {"\u4e00\u4e01\u4e02\u4e03\u4e04\u4e05\u4e06\u4e07",
             "",
             "_",
             "_\u4e00_\u4e01_\u4e02_\u4e03_\u4e04_\u4e05_\u4e06_\u4e07_"},
            {"^\u4e00\u4e01\u4e02$",
             "\u4e00\u4e01\u4e02",
             "\u4e03",
             "^\u4e03$"},

            {"", "\u4e00", "\u4e01", ""},
            {"", "", "\u4e00\u4e01\u4e02", "\u4e00\u4e01\u4e02"},

            {"^\u4e00\u4e01\u4e02$",
             "\u4e00\u4e01\u4e02",
             "X",
             "^X$"},

            {"abcdefgh",
             "def",
             "\u4e01",
             "abc\u4e01gh"},

            {"abcdefgh",
             "def",
             "\u4e01\u4e02",
             "abc\u4e01\u4e02gh"},

            {"abcdefabcgh",
             "abc",
             "\u4e01\u4e02",
             "\u4e01\u4e02def\u4e01\u4e02gh"},

            {"abcdefabcghabc",
             "abc",
             "\u4e01\u4e02",
             "\u4e01\u4e02def\u4e01\u4e02gh\u4e01\u4e02"},

            {"\u4e00\u4e01\u4e02\u4e03\u4e04\u4e05",
             "\u4e00\u4e01\u4e02\u4e03\u4e04\u4e05",
             "abcd",
             "abcd"},

            {"\u4e00\u4e01",
             "\u4e00\u4e01",
             "abcdefg",
             "abcdefg"},

            {"\u4e00\u4e01xyz",
             "\u4e00\u4e01",
             "abcdefg",
             "abcdefgxyz"},

            {"\u4e00\u4e00\u4e00\u4e00\u4e00\u4e00",
             "\u4e00\u4e00",
             "\u4e00\u4e00\u4e00",
             "\u4e00\u4e00\u4e00\u4e00\u4e00\u4e00\u4e00\u4e00\u4e00"},

            {"\u4e00\u4e00\u4e00\u4e00\u4e00\u4e00",
             "\u4e00\u4e00\u4e00",
             "\u4e00\u4e00",
             "\u4e00\u4e00\u4e00\u4e00"},

            {"\u4e00.\u4e01.\u4e02.\u4e03.\u4e04.",
             ".",
             "-",
             "\u4e00-\u4e01-\u4e02-\u4e03-\u4e04-"},

            {"\u4e00\u4e00\u4e00\u4e00\u4e00\u4e00",
             "\u4e00",
             "",
             ""},

            {"\u4e00\u4e01\u4e02\u4e03\u4e04\u4e05",
             "\u4e00\u4e01\u4e02\u4e03\u4e04\u4e05",
             "",
             ""},

            {"\u4e01\u4e02\u4e03", "\u4e02", "\u4e02", "\u4e01\u4e02\u4e03"},
            {"\u4e01\u4e02\u4e03", "\u4e02", "\u4e04", "\u4e01\u4e04\u4e03"},
            {"\u4e01\u4e02\u4e03", "\u4e02", "_", "\u4e01_\u4e03"},
            {"a\u4e02c", "\u4e02", "_", "a_c"},
            {"\u4e01@\u4e03", "@", "_", "\u4e01_\u4e03"},
            {"\u4e01@\u4e03", "@", "\u4e02", "\u4e01\u4e02\u4e03"},
            {"\u4e01\u4e02\u4e03", "\u4e02\u4e03", "\u4e02\u4e03", "\u4e01\u4e02\u4e03"},
            {"\u4e01\u4e02\u4e03", "\u4e02\u4e03", "\u4e04\u4e05", "\u4e01\u4e04\u4e05"},
            {"\u4e01\u4e02\u4e03", "\u4e02\u4e03", "\u4e06", "\u4e01\u4e06"},
            {"\u4e01\u4e02\u4e03", "\u4e02\u4e03", "<>", "\u4e01<>"},
            {"@\u4e02\u4e03", "\u4e02\u4e03", "<>", "@<>"},
            {"\u4e01@@", "\u4e01@", "", "@"},
            {"\u4e01@@", "\u4e01@", "#", "#@"},
            {"\u4e01@@", "\u4e01@", "\u4e09", "\u4e09@"},
            {"\u4e01@@", "\u4e01@", "#\u4e09", "#\u4e09@"},
            {"\u4e01\u4e02\u4e03".repeat(32771) + "end", "\u4e02", "\u4e02", "\u4e01\u4e02\u4e03".repeat(32771) + "end"},
            {"\u4e01\u4e02\u4e03".repeat(32771) + "end", "\u4e02", "\u4e04", "\u4e01\u4e04\u4e03".repeat(32771) + "end"},
            {"\u4e01\u4e02\u4e03".repeat(32771) + "end", "\u4e02", "\u4e04\u4e05", "\u4e01\u4e04\u4e05\u4e03".repeat(32771) + "end"},
            {"\u4e01\u4e02\u4e03".repeat(32771) + "end", "\u4e02", "_", "\u4e01_\u4e03".repeat(32771) + "end"},
            {"\u4e01_\u4e03".repeat(32771) + "end", "_", "_", "\u4e01_\u4e03".repeat(32771) + "end"},
            {"\u4e01_\u4e03".repeat(32771) + "end", "_", "\u4e06", "\u4e01\u4e06\u4e03".repeat(32771) + "end"},
            {"\u4e01_\u4e03".repeat(32771) + "end", "_", "\u4e06\u4e06", "\u4e01\u4e06\u4e06\u4e03".repeat(32771) + "end"},
            {"X_Y".repeat(32771) + "end", "_", "\u4e07", "X\u4e07Y".repeat(32771) + "end"},
            {"X_Y".repeat(32771) + "end", "_", "\u4e07\u4e08", "X\u4e07\u4e08Y".repeat(32771) + "end"},
            {"X_Y".repeat(32771) + "end", "_", ".\u4e08.", "X.\u4e08.Y".repeat(32771) + "end"},
            {"\u4e0a".repeat(32771), "\u4e0a", "", ""},
            {"\u4e0a\u4e0b".repeat(32771), "\u4e0a", "", "\u4e0b".repeat(32771)},
            {"\u4e0a\u4e0b".repeat(32771), "\u4e0b", "", "\u4e0a".repeat(32771)},
            {"\u4e0a\u4e0b".repeat(32771), "\u4e0a\u4e0b", "", ""},
            {"\u4e0b" + "\u4e0a\u4e0b".repeat(32771), "\u4e0a\u4e0b", "", "\u4e0b"},
            {"\u4e0a\u4e0b".repeat(32771) + "\u4e0a", "\u4e0a\u4e0b", "", "\u4e0a"},
            {"\u4e0b" + "\u4e0a\u4e0b".repeat(32771) + "\u4e0a", "\u4e0a\u4e0b", "", "\u4e0b\u4e0a"},
            {"b" + "\u4e0a\u4e0b".repeat(32771), "\u4e0a\u4e0b", "", "b"},
            {"\u4e0a\u4e0b".repeat(32771) + "a", "\u4e0a\u4e0b", "", "a"},
            {"b" + "\u4e0a\u4e0b".repeat(32771) + "a", "\u4e0a\u4e0b", "", "ba"},
        };
    }

    @DataProvider
    public static Iterator<Object[]> sourceTargetReplacement() {
        ArrayList<Object[]> list = new ArrayList<>();
        for (int maxSrcLen = 1; maxSrcLen <= (1 << 10); maxSrcLen <<= 1) {
            for (int maxTrgLen = 1; maxTrgLen <= (1 << 10); maxTrgLen <<= 1) {
                for (int maxPrlLen = 1; maxPrlLen <= (1 << 10); maxPrlLen <<= 1) {
                    list.add(makeArray(makeRandomString(maxSrcLen),
                                       makeRandomString(maxTrgLen),
                                       makeRandomString(maxPrlLen)));

                    String source = makeRandomString(maxSrcLen);
                    list.add(makeArray(source,
                                       mekeRandomSubstring(source, maxTrgLen),
                                       makeRandomString(maxPrlLen)));
                }
            }
        }
        return list.iterator();
    }

    @DataProvider
    public static Iterator<Object[]> sourceTargetReplacementWithNull() {
        ArrayList<Object[]> list = new ArrayList<>();
        Object[] arr = {null, "", "a", "b", "string", "str", "ababstrstr"};
        for (int i = 0; i < arr.length; ++i) {
            for (int j = 0; j < arr.length; ++j) {
                for (int k = 0; k < arr.length; ++k) {
                    if (arr[i] != null && (arr[j] == null || arr[k] == null)) {
                        list.add(makeArray(arr[i], arr[j], arr[k]));
                    }
                }
            }
        }
        return list.iterator();
    }

    // utilities

    /**
     * How the String.replace(CharSequence, CharSequence) used to be implemented
     */
    private static String canonicalReplace(String source, String target, String replacement) {
        return Pattern.compile(target.toString(), Pattern.LITERAL).matcher(
                source).replaceAll(Matcher.quoteReplacement(replacement.toString()));
    }

    private static final Random random = RandomFactory.getRandom();

    private static final char[] CHARS = ("qwertyuiop[]12345678" +
        "90-=\\`asdfghjkl;'zxcvbnm,./~!@#$%^&*()_+|QWERTYUIOP{" +
        "}ASDFGHJKL:\"ZXCVBNM<>?\n\r\t\u0444\u044B\u0432\u0430").toCharArray();

    private static String makeRandomString(int maxLen) {
        int len = random.nextInt(maxLen);
        char[] buf = new char[len];
        for (int i = 0; i < len; ++i) {
            buf[i] = CHARS[random.nextInt(CHARS.length)];
        }
        return new String(buf);
    }

    private static String mekeRandomSubstring(String source, int maxLen) {
        if (source.isEmpty()) {
            return source;
        }
        int pos = random.nextInt(source.length());
        int len = Integer.min(source.length() - pos,
                              random.nextInt(maxLen));
        return source.substring(pos, pos + len);
    }

    private static Object[] makeArray(Object... array) {
         return array;
    }
}
