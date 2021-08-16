/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 8016846 8024341 8071479 8145006
 * @summary Unit tests stream and lambda-based methods on Pattern and Matcher
 * @library /lib/testlibrary/bootlib
 * @build java.base/java.util.stream.OpTestCase
 * @run testng/othervm PatternStreamTest
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.LambdaTestHelpers;
import java.util.stream.OpTestCase;
import java.util.stream.Stream;
import java.util.stream.TestData;

import static org.testng.Assert.*;

@Test
public class PatternStreamTest extends OpTestCase {

    @DataProvider(name = "Patterns")
    public static Object[][] makeStreamTestData() {
        // Each item must match the type signature of the consumer of this data
        // String, String, Pattern
        List<Object[]> data = new ArrayList<>();

        String description = "All matches";
        String input = "XXXXXX";
        Pattern pattern = Pattern.compile("X");
        data.add(new Object[]{description, input, pattern});

        description = "Bounded every other match";
        input = "XYXYXYYXYX";
        pattern = Pattern.compile("X");
        data.add(new Object[]{description, input, pattern});

        description = "Every other match";
        input = "YXYXYXYYXYXY";
        pattern = Pattern.compile("X");
        data.add(new Object[]{description, input, pattern});

        description = "";
        input = "awgqwefg1fefw4vssv1vvv1";
        pattern = Pattern.compile("4");
        data.add(new Object[]{description, input, pattern});

        input = "afbfq\u00a3abgwgb\u00a3awngnwggw\u00a3a\u00a3ahjrnhneerh";
        pattern = Pattern.compile("\u00a3a");
        data.add(new Object[]{description, input, pattern});

        input = "awgqwefg1fefw4vssv1vvv1";
        pattern = Pattern.compile("1");
        data.add(new Object[]{description, input, pattern});

        input = "a\u4ebafg1fefw\u4eba4\u9f9cvssv\u9f9c1v\u672c\u672cvv";
        pattern = Pattern.compile("1");
        data.add(new Object[]{description, input, pattern});

        input = "1\u56da23\u56da456\u56da7890";
        pattern = Pattern.compile("\u56da");
        data.add(new Object[]{description, input, pattern});

        input = "1\u56da23\u9f9c\u672c\u672c\u56da456\u56da\u9f9c\u672c7890";
        pattern = Pattern.compile("\u56da");
        data.add(new Object[]{description, input, pattern});

        description = "Empty input";
        input = "";
        pattern = Pattern.compile("\u56da");
        data.add(new Object[]{description, input, pattern});

        description = "Empty input with empty pattern";
        input = "";
        pattern = Pattern.compile("");
        data.add(new Object[]{description, input, pattern});

        description = "Multiple separators";
        input = "This is,testing: with\tdifferent separators.";
        pattern = Pattern.compile("[ \t,:.]");
        data.add(new Object[]{description, input, pattern});

        description = "Repeated separators within and at end";
        input = "boo:and:foo";
        pattern = Pattern.compile("o");
        data.add(new Object[]{description, input, pattern});

        description = "Many repeated separators within and at end";
        input = "booooo:and:fooooo";
        pattern = Pattern.compile("o");
        data.add(new Object[]{description, input, pattern});

        description = "Many repeated separators before last match";
        input = "fooooo:";
        pattern = Pattern.compile("o");
        data.add(new Object[] {description, input, pattern});

        return data.toArray(new Object[0][]);
    }

    @Test(dataProvider = "Patterns")
    public void testPatternSplitAsStream(String description, String input, Pattern pattern) {
        // Derive expected result from pattern.split
        List<String> expected = Arrays.asList(pattern.split(input));

        Supplier<Stream<String>> ss =  () -> pattern.splitAsStream(input);
        withData(TestData.Factory.ofSupplier(description, ss))
                .stream(LambdaTestHelpers.identity())
                .expectedResult(expected)
                .exercise();
    }

    @Test(dataProvider = "Patterns")
    public void testReplaceFirst(String description, String input, Pattern pattern) {
        // Derive expected result from Matcher.replaceFirst(String )
        String expected = pattern.matcher(input).replaceFirst("R");
        String actual = pattern.matcher(input).replaceFirst(r -> "R");
        assertEquals(actual, expected);
    }

    @Test(dataProvider = "Patterns")
    public void testReplaceAll(String description, String input, Pattern pattern) {
        // Derive expected result from Matcher.replaceAll(String )
        String expected = pattern.matcher(input).replaceAll("R");
        String actual = pattern.matcher(input).replaceAll(r -> "R");
        assertEquals(actual, expected);

        // Derive expected result from Matcher.find
        Matcher m = pattern.matcher(input);
        int expectedMatches = 0;
        while (m.find()) {
            expectedMatches++;
        }
        AtomicInteger actualMatches = new AtomicInteger();
        pattern.matcher(input).replaceAll(r -> "R" + actualMatches.incrementAndGet());
        assertEquals(expectedMatches, actualMatches.get());
    }

    @Test(dataProvider = "Patterns")
    public void testMatchResults(String description, String input, Pattern pattern) {
        // Derive expected result from Matcher.find
        Matcher m = pattern.matcher(input);
        List<MatchResultHolder> expected = new ArrayList<>();
        while (m.find()) {
            expected.add(new MatchResultHolder(m));
        }

        Supplier<Stream<MatchResult>> ss =  () -> pattern.matcher(input).results();
        withData(TestData.Factory.ofSupplier(description, ss))
                .stream(s -> s.map(MatchResultHolder::new))
                .expectedResult(expected)
                .exercise();
    }

    @Test
    public void testLateBinding() {
        Pattern pattern = Pattern.compile(",");

        StringBuilder sb = new StringBuilder("a,b,c,d,e");
        Stream<String> stream = pattern.splitAsStream(sb);
        sb.setLength(3);
        assertEquals(Arrays.asList("a", "b"), stream.collect(Collectors.toList()));

        stream = pattern.splitAsStream(sb);
        sb.append(",f,g");
        assertEquals(Arrays.asList("a", "b", "f", "g"), stream.collect(Collectors.toList()));
    }

    public void testFailfastMatchResults() {
        Pattern p = Pattern.compile("X");
        Matcher m = p.matcher("XX");

        Stream<MatchResult> s = m.results();
        m.find();
        // Should start on the second match
        assertEquals(s.count(), 1);

        // Fail fast without short-circuit
        // Exercises Iterator.forEachRemaining
        m.reset();
        try {
            m.results().peek(mr -> m.reset()).count();
            fail();
        } catch (ConcurrentModificationException e) {
            // Should reach here
        }

        m.reset();
        try {
            m.results().peek(mr -> m.find()).count();
            fail();
        } catch (ConcurrentModificationException e) {
            // Should reach here
        }

        // Fail fast with short-circuit
        // Exercises Iterator.hasNext/next
        m.reset();
        try {
            m.results().peek(mr -> m.reset()).limit(2).count();
            fail();
        } catch (ConcurrentModificationException e) {
            // Should reach here
        }

        m.reset();
        try {
            m.results().peek(mr -> m.find()).limit(2).count();
            fail();
        } catch (ConcurrentModificationException e) {
            // Should reach here
        }
    }

    public void testFailfastReplace() {
        Pattern p = Pattern.compile("X");
        Matcher m = p.matcher("XX");

        // Fail fast without short-circuit
        // Exercises Iterator.forEachRemaining
        m.reset();
        try {
            m.replaceFirst(mr -> { m.reset(); return "Y"; });
            fail();
        } catch (ConcurrentModificationException e) {
            // Should reach here
        }

        m.reset();
        try {
            m.replaceAll(mr -> { m.reset(); return "Y"; });
            fail();
        } catch (ConcurrentModificationException e) {
            // Should reach here
        }
    }

    // A holder of MatchResult that can compare
    static class MatchResultHolder implements Comparable<MatchResultHolder> {
        final MatchResult mr;

        MatchResultHolder(Matcher m) {
            this(m.toMatchResult());
        }

        MatchResultHolder(MatchResult mr) {
            this.mr = mr;
        }

        @Override
        public int compareTo(MatchResultHolder that) {
            int c = that.mr.group().compareTo(this.mr.group());
            if (c != 0)
                return c;

            c = Integer.compare(that.mr.start(), this.mr.start());
            if (c != 0)
                return c;

            c = Integer.compare(that.mr.end(), this.mr.end());
            if (c != 0)
                return c;

            c = Integer.compare(that.mr.groupCount(), this.mr.groupCount());
            if (c != 0)
                return c;

            for (int g = 0; g < this.mr.groupCount(); g++) {
                c = that.mr.group(g).compareTo(this.mr.group(g));
                if (c != 0)
                    return c;

                c = Integer.compare(that.mr.start(g), this.mr.start(g));
                if (c != 0)
                    return c;

                c = Integer.compare(that.mr.end(g), this.mr.end(g));
                if (c != 0)
                    return c;
            }
            return 0;
        }

        @Override
        public boolean equals(Object that) {
            if (this == that) return true;
            if (that == null || getClass() != that.getClass()) return false;

            return this.compareTo((MatchResultHolder) that) == 0;
        }

        @Override
        public int hashCode() {
            return mr.group().hashCode();
        }
    }
}
