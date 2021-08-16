/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.LambdaTestHelpers;
import java.util.stream.OpTestCase;
import java.util.stream.Stream;
import java.util.stream.TestData;

import static org.testng.Assert.*;

/**
 * @test
 * @bug 8072722 8150488
 * @summary Tests of stream support in java.util.Scanner
 * @library /lib/testlibrary/bootlib
 * @build java.base/java.util.stream.OpTestCase
 * @run testng/othervm ScannerStreamTest
 */

@Test
public class ScannerStreamTest extends OpTestCase {

    static File inputFile = new File(System.getProperty("test.src", "."), "input.txt");

    @DataProvider(name = "Tokens")
    public static Object[][] makeTokensTestData() {
        // each inner array is [String description, String input, String delimiter]
        // delimiter may be null
        List<Object[]> data = new ArrayList<>();

        data.add(new Object[] { "default delimiter", "abc def ghi",           null });
        data.add(new Object[] { "fixed delimiter",   "abc,def,,ghi",          "," });
        data.add(new Object[] { "regex delimiter",   "###abc##def###ghi###j", "#+" });

        return data.toArray(new Object[0][]);
    }

    /*
     * Creates a scanner over the input, applying a delimiter if non-null.
     */
    Scanner makeScanner(String input, String delimiter) {
        Scanner sc = new Scanner(input);
        if (delimiter != null) {
            sc.useDelimiter(delimiter);
        }
        return sc;
    }

    /*
     * Given input and a delimiter, tests that tokens() returns the same
     * results that would be provided by a Scanner hasNext/next loop.
     */
    @Test(dataProvider = "Tokens")
    public void tokensTest(String description, String input, String delimiter) {
        // derive expected result by using conventional loop
        Scanner sc = makeScanner(input, delimiter);
        List<String> expected = new ArrayList<>();
        while (sc.hasNext()) {
            expected.add(sc.next());
        }

        Supplier<Stream<String>> ss = () -> makeScanner(input, delimiter).tokens();
        withData(TestData.Factory.ofSupplier(description, ss))
                .stream(LambdaTestHelpers.identity())
                .expectedResult(expected)
                .exercise();
    }

    /*
     * Creates a Scanner over the given input file.
     */
    Scanner makeFileScanner(File file) {
        try {
            return new Scanner(file, "UTF-8");
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /*
     * Tests that the matches produced by findAll(pat) are the same
     * as what are returned by findWithinHorizon(pat, 0). This tests
     * a single pattern against a single input file.
     */
    public void findAllFileTest() {
        // derive expected result by using conventional loop
        Pattern pat = Pattern.compile("[A-Z]{7,}");
        List<String> expected = new ArrayList<>();

        try (Scanner sc = makeFileScanner(inputFile)) {
            String match;
            while ((match = sc.findWithinHorizon(pat, 0)) != null) {
                expected.add(match);
            }
        }

        Supplier<Stream<String>> ss =
            () -> makeFileScanner(inputFile).findAll(pat).map(MatchResult::group);

        withData(TestData.Factory.ofSupplier("findAllFileTest", ss))
                .stream(LambdaTestHelpers.identity())
                .expectedResult(expected)
                .exercise();
    }

    @DataProvider(name = "FindAllZero")
    public static Object[][] makeFindAllZeroTestData() {
        // each inner array is [String input, String patternString]
        List<Object[]> data = new ArrayList<>();

        data.add(new Object[] { "aaaaa",        "a*" });
        data.add(new Object[] { "aaaaab",       "a*" });
        data.add(new Object[] { "aaaaabb",      "a*" });
        data.add(new Object[] { "aaaaabbb",     "a*" });
        data.add(new Object[] { "aaabbaaaa",    "a*" });
        data.add(new Object[] { "aaabbaaaab",   "a*" });
        data.add(new Object[] { "aaabbaaaabb",  "a*" });
        data.add(new Object[] { "aaabbaaaabbb", "a*" });
        data.add(new Object[] { "aaabbaaaa",    "a*|b*" });
        data.add(new Object[] { "aaabbaaaab",   "a*|b*" });
        data.add(new Object[] { "aaabbaaaabb",  "a*|b*" });
        data.add(new Object[] { "aaabbaaaabbb", "a*|b*" });

        return data.toArray(new Object[0][]);
    }

    /*
     * Tests findAll() using a pattern against an input string.
     * The results from findAll() should equal the results obtained
     * using a loop around Matcher.find().
     *
     * The provided regexes should allow zero-length matches.
     * This primarily tests the auto-advance feature of findAll() that
     * occurs if the regex match is of zero length to see if it has the
     * same behavior as Matcher.find()'s auto-advance (JDK-8150488).
     * Without auto-advance, findAll() would return an infinite stream
     * of zero-length matches. Apply a limit to the stream so
     * that an infinite stream will be truncated. The limit must be
     * high enough that the resulting truncated stream won't be
     * mistaken for a correct expected result.
     */
    @Test(dataProvider = "FindAllZero")
    public void findAllZeroTest(String input, String patternString) {
        Pattern pattern = Pattern.compile(patternString);

        // generate expected result using Matcher.find()
        Matcher m = pattern.matcher(input);
        List<String> expected = new ArrayList<>();
        while (m.find()) {
            expected.add(m.group());
        }

        Supplier<Stream<String>> ss = () -> new Scanner(input).findAll(pattern)
                                                              .limit(100)
                                                              .map(MatchResult::group);

        withData(TestData.Factory.ofSupplier("findAllZeroTest", ss))
                .stream(LambdaTestHelpers.identity())
                .expectedResult(expected)
                .exercise();
    }
}
