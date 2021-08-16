/*
 * Copyright (c) 2019, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4406815 8222969 8266784
 * @summary RuleBasedCollatorTest uses very limited but selected test data
 *  to test basic functionalities provided by RuleBasedCollator.
 * @run testng/othervm RuleBasedCollatorTest
 */

import java.text.CollationElementIterator;
import java.text.CollationKey;
import java.text.RuleBasedCollator;
import java.text.Collator;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Locale;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.testng.SkipException;
import static org.testng.Assert.*;

public class RuleBasedCollatorTest {

    static RuleBasedCollator USC;
    static String US_RULES;

    @BeforeClass
    public void setup() {
        Collator c = Collator.getInstance(Locale.US);
        if (!(c instanceof RuleBasedCollator)) {
            throw new SkipException("skip tests.");
        }
        USC = (RuleBasedCollator) c;
        US_RULES = USC.getRules();
    }


    @DataProvider(name = "rulesData")
    Object[][] rulesData() {
        //Basic Tailor
        String BASIC_TAILOR_RULES = "< b=c<\u00e6;A,a";
        String[] BASIC_TAILOR_DATA = {"\u00e6", "b", "a", "c", "A"};
        String[] BASIC_TAILOR_EXPECTED = {"b", "c", "\u00e6", "A", "a"};

        //Contraction
        String CONTRACTION_RULES = US_RULES + "& b < ch ,cH, Ch, CH < c ";
        String[] CONTRACTION_DATA = {"b", "c", "ch", "CH", "Ch", "cH"};
        String[] CONTRACTION_EXPECTED = {"b", "ch", "cH", "Ch", "CH", "c"};

        //Expansion
        String EXPANSION_RULES = US_RULES + "& ae = \u00e4 < b";
        String[] EXPANSION_DATA = {"ad", "af", "\u00e4"};
        String[] EXPANSION_EXPECTED = {"ad", "\u00e4", "af"};

        //Punctuation
        String PUNCTUATION_RULES = US_RULES + "< ' ' < '-'";
        String[] PUNCTUATION_DATA = {"b-w", "b-W", "B-w", "B-W", "bW", "bw",
                "Bw", "BW", "b w", "b W", "B w", "B W"};
        String[] PUNCTUATION_EXPECTED = {"bw", "bW", "Bw", "BW", "b w", "b W",
                "B w", "B W", "b-w", "b-W", "B-w", "B-W"};

        return new Object[][] {
                {BASIC_TAILOR_RULES, BASIC_TAILOR_DATA, BASIC_TAILOR_EXPECTED},
                {CONTRACTION_RULES, CONTRACTION_DATA, CONTRACTION_EXPECTED},
                {EXPANSION_RULES, EXPANSION_DATA, EXPANSION_EXPECTED},
                {PUNCTUATION_RULES, PUNCTUATION_DATA, PUNCTUATION_EXPECTED}
        };
    }

    @Test(dataProvider = "rulesData")
    public void testRules(String rules, String[] testData, String[] expected)
            throws ParseException {
        Arrays.sort(testData, new RuleBasedCollator(rules));
        assertEquals(testData, expected);

    }

    @DataProvider(name = "FrenchSecondarySort")
    Object[][] FrenchSecondarySort() {
        return new Object[][] {
                { "\u0061\u00e1\u0061", "\u00e1\u0061\u0061", 1 },
                //{"\u0061\u00e1", "\u00e1\u0041", 1},  //JDK-4406815
                //{"\u00e1\u0041", "\u0061\u00e1", -1}, //JDK-4406815
                {"\u1ea0a", "\u1ea2A", -1}, //case ignore
                { "\u1ea0b", "\u1ea2A", 1 },  //primary overwrite
                { "\u1e15", "\u1e1d", -1 },   //ignore sec diacritic
                { "a", "\u1ea1", -1 } };
    }

    @Test(dataProvider = "FrenchSecondarySort")
    public void testFrenchSecondarySort(String sData, String tData,
            int expected) throws ParseException {
        String french_rule = "@";
        String rules = US_RULES + french_rule;
        RuleBasedCollator rc = new RuleBasedCollator(rules);
        int result = rc.compare(sData, tData);
        assertEquals(expected, result);
    }

    @DataProvider(name = "ThaiLaoVowelConsonantSwapping")
    Object[][] ThaiLaoVowelConsonantSwapping() {
        return new Object[][] {{"\u0e44\u0e01", "\u0e40\u0e2e", -1},//swap
                {"\u0e2e\u0e40", "\u0e01\u0e44", 1},//no swap
                {"\u0e44\u0061", "\u0e40\u0081", 1}//no swap
        };
    }

    @Test(dataProvider = "ThaiLaoVowelConsonantSwapping")
    public void testThaiLaoVowelConsonantSwapping(String sData, String tData,
            int expected) throws ParseException {
        String thai_rule = "& Z < \u0e01 < \u0e2e <\u0e40 < \u0e44!";
        String rules = US_RULES + thai_rule;
        RuleBasedCollator rc = new RuleBasedCollator(rules);
        int result = rc.compare(sData, tData);
        assertEquals(expected, result);
    }

    @Test
    public void testIgnorableCharacter() throws ParseException {
        String rule = "=f<a<c";
        RuleBasedCollator rc = new RuleBasedCollator(rule);
        CollationElementIterator iter = rc.getCollationElementIterator("f");
        int element = iter.next();
        int primary = iter.primaryOrder(element);
        assertEquals(primary, 0);
    }

    @DataProvider(name = "Normalization")
    Object[][] Normalization() {
        return new Object[][] {
                //micro sign has no canonical decomp mapping
                // 0:NO_Decomposition;
                // 1:CANONICAL_Decomposition;
                // 2:FULL_Decomposition
                {"\u00b5", "\u03BC", 0, -1},
                {"\u00b5", "\u03BC", 1, -1},
                {"\u00b5", "\u03BC", 2, 0}
        };
    }

    @Test(dataProvider = "Normalization")
    public void testNormalization(String sData, String tData, int decomp,
            int result) {
        RuleBasedCollator rc = (RuleBasedCollator)USC.clone();
        rc.setDecomposition(decomp);
        assertEquals(rc.compare(sData, tData), result);
    }

    @Test
    public void testEquality() throws ParseException {
        String rule1 = "<a=b";
        RuleBasedCollator rc1= new RuleBasedCollator(rule1);
        //test equals()
        assertTrue(rc1.equals(new RuleBasedCollator(rule1)));

        //test semantic equality
        String[] array1 = {"b", "c", "a"};
        String[] array2 = Arrays.copyOf(array1, array1.length);
        String[] expected = {"b", "a", "c"};
        String rule2 = "<b=a";
        RuleBasedCollator rc2= new RuleBasedCollator(rule2);

        Arrays.sort(array1, rc1);
        Arrays.sort(array2, rc2);
        assertEquals(array1, array2);
        assertEquals(array1, expected);
    }

    @Test
    public void testBasicParsingOrder() throws ParseException {
        String rule1 = "< a < b & a < c";
        String rule2 = "< a < c & a < b";
        String rule3 = "< a < b < c";
        String s = "abc";
        RuleBasedCollator c1 = new RuleBasedCollator(rule1);
        RuleBasedCollator c2 = new RuleBasedCollator(rule2);
        RuleBasedCollator c3 = new RuleBasedCollator(rule3);
        CollationKey k1 = c1.getCollationKey(s);
        CollationKey k2 = c2.getCollationKey(s);
        CollationKey k3 = c3.getCollationKey(s);
        //rule1 should not equals to rule2
        assertEquals(k1.compareTo(k2) == 0, false);

        //rule2 should equals to rule3
        assertEquals(k2.compareTo(k3) == 0, true);
    }

    @DataProvider(name = "ParseData")
    Object[][] ParseData() {
        return new Object[][] {
                {""},
                {"a < b"},
                {"< a-b < c"},
                {"< ,a"},
                {"< a < b & c < d"}
        };
    }

    @Test(dataProvider = "ParseData",
            expectedExceptions = ParseException.class)
    public void testParseException(String rule) throws ParseException{
        new RuleBasedCollator(rule);
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullParseException() throws ParseException{
        new RuleBasedCollator(null);
    }
}
