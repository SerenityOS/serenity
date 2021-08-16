/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
/*
 * @test
 * @bug 8222756
 * @summary Tests plurals support in CompactNumberFormat
 * @run testng/othervm TestPlurals
 */

import java.text.CompactNumberFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestPlurals {

    private final static DecimalFormatSymbols DFS = DecimalFormatSymbols.getInstance(Locale.ROOT);
    private final static String[] PATTERN = {
            "{zero:0->zero one:0->one two:0->two few:0->few many:0->many other:0->other}"};
    private final static String RULE_1 = "zero:n = 0; one:n = 1; two:n = 2; few:n = 3..4; many:n = 5..6,8";
    private final static String RULE_2 = "one:n   %   2   =    1   or   n   /   3   =   2;";
    private final static String RULE_3 = "one:n%2=0andn/3=2;";


    @DataProvider
    Object[][] pluralRules() {
        return new Object[][]{
            // rules, number, expected
            {RULE_1, 0, "0->zero"},
            {RULE_1, 1, "1->one"},
            {RULE_1, 2, "2->two"},
            {RULE_1, 3, "3->few"},
            {RULE_1, 4, "4->few"},
            {RULE_1, 5, "5->many"},
            {RULE_1, 6, "6->many"},
            {RULE_1, 7, "7->other"},
            {RULE_1, 8, "8->many"},
            {RULE_1, 9, "9->other"},

            {RULE_2, 0, "0->other"},
            {RULE_2, 1, "1->one"},
            {RULE_2, 2, "2->other"},
            {RULE_2, 3, "3->one"},
            {RULE_2, 4, "4->other"},
            {RULE_2, 5, "5->one"},
            {RULE_2, 6, "6->one"},

            {RULE_3, 0, "0->other"},
            {RULE_3, 1, "1->other"},
            {RULE_3, 2, "2->other"},
            {RULE_3, 3, "3->other"},
            {RULE_3, 4, "4->other"},
            {RULE_3, 5, "5->other"},
            {RULE_3, 6, "6->one"},
        };
    }

    @DataProvider
    Object[][] invalidRules() {
        return new Object [][] {
            {"one:a = 1"},
            {"on:n = 1"},
            {"one:n = 1...2"},
            {"one:n = 1.2"},
            {"one:n = 1..2,"},
            {"one:n = 1;one:n = 2"},
            {"foo:n = 1"},
            {"one:n = 1..2 andor v % 10 != 0"},
        };
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullPluralRules() {
        String[] pattern = {""};
        new CompactNumberFormat("#", DFS, PATTERN, null);
    }

    @Test(dataProvider = "pluralRules")
    public void testPluralRules(String rules, Number n, String expected) {
        var cnp = new CompactNumberFormat("#", DFS, PATTERN, rules);
        assertEquals(cnp.format(n), expected);
    }

    @Test(dataProvider = "invalidRules", expectedExceptions = IllegalArgumentException.class)
    public void testInvalidRules(String rules) {
        new CompactNumberFormat("#", DFS, PATTERN, rules);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testLimitExceedingRules() {
        String andCond = " and n = 1";
        String invalid = "one: n = 1" + andCond.repeat(2_048 / andCond.length());
        new CompactNumberFormat("#", DFS, PATTERN, invalid);
    }
}
