/*
 * Copyright (c) 2013, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5015163 7172553 8249258
 * @summary tests StringJoinerTest
 * @modules java.base/jdk.internal.util
 * @requires vm.bits == "64" & os.maxMemory > 4G
 * @run testng/othervm -Xmx4g -XX:+CompactStrings StringJoinerTest
 * @author Jim Gish
 */
import java.util.ArrayList;
import java.util.StringJoiner;
import org.testng.annotations.Test;
import static jdk.internal.util.ArraysSupport.SOFT_MAX_ARRAY_LENGTH;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;


@Test(groups = {"unit","string","util","libs"})
public class StringJoinerTest {

    private static final String EMPTY = "EMPTY";
    private static final String ONE = "One";
    private static final int ONE_LEN = ONE.length();
    private static final String TWO = "Two";
    private static final int TWO_LEN = TWO.length();
    private static final String THREE = "Three";
    private static final String FOUR = "Four";
    private static final String FIVE = "Five";
    private static final String DASH = "-";
    private static final String MAX_STRING = "*".repeat(SOFT_MAX_ARRAY_LENGTH);

    public void addAddAll() {
        StringJoiner sj = new StringJoiner(DASH, "{", "}");
        sj.add(ONE);

        ArrayList<String> nextOne = new ArrayList<>();
        nextOne.add(TWO);
        nextOne.add(THREE);
        nextOne.stream().forEachOrdered(sj::add);

        String expected = "{"+ONE+DASH+TWO+DASH+THREE+"}";
        assertEquals(sj.toString(), expected);
    }

    void addAlladd() {
        StringJoiner sj = new StringJoiner(DASH, "{", "}");

        ArrayList<String> firstOne = new ArrayList<>();
        firstOne.add(ONE);
        firstOne.add(TWO);
        firstOne.stream().forEachOrdered(sj::add);

        sj.add(THREE);

        String expected = "{"+ONE+DASH+TWO+DASH+THREE+"}";
        assertEquals(sj.toString(), expected);
    }

    // The following tests do two successive adds of different types
    public void addAlladdAll() {
        StringJoiner sj = new StringJoiner(DASH, "{", "}");
        ArrayList<String> firstOne = new ArrayList<>();
        firstOne.add(ONE);
        firstOne.add(TWO);
        firstOne.add(THREE);
        firstOne.stream().forEachOrdered(sj::add);

        ArrayList<String> nextOne = new ArrayList<>();
        nextOne.add(FOUR);
        nextOne.add(FIVE);
        nextOne.stream().forEachOrdered(sj::add);

        String expected = "{"+ONE+DASH+TWO+DASH+THREE+DASH+FOUR+DASH+FIVE+"}";
        assertEquals(sj.toString(), expected);
    }

    public void addCharSequence() {
        StringJoiner sj = new StringJoiner(",");
        CharSequence cs_one = ONE;
        CharSequence cs_two = TWO;

        sj.add(cs_one);
        sj.add(cs_two);

        assertEquals(sj.toString(), ONE + "," + TWO);

        sj = new StringJoiner(DASH, "{", "}");
        sj.add(cs_one);
        sj.add(cs_two);

        assertEquals(sj.toString(), "{" + ONE + DASH + TWO + "}");

        StringBuilder builder = new StringBuilder(ONE);
        StringBuffer buffer = new StringBuffer(THREE);
        sj = new StringJoiner(", ", "{ ", " }");
        sj.add(builder).add(buffer);
        builder.append(TWO);
        buffer.append(FOUR);
        assertEquals(sj.toString(), "{ " + ONE + ", " + THREE + " }",
                "CharSequence is copied when add");
        sj.add(builder);
        assertEquals(sj.toString(), "{ " + ONE + ", " + THREE + ", " + ONE +
                TWO + " }");
    }

    public void addCharSequenceWithEmptyValue() {
        StringJoiner sj = new StringJoiner(",").setEmptyValue(EMPTY);
        CharSequence cs_one = ONE;
        CharSequence cs_two = TWO;

        sj.add(cs_one);
        sj.add(cs_two);

        assertEquals(sj.toString(), ONE + "," + TWO);

        sj = new StringJoiner(DASH, "{", "}");
        sj.add(cs_one);
        sj.add(cs_two);
        assertEquals(sj.toString(), "{" + ONE + DASH + TWO + "}");

        sj = new StringJoiner(DASH, "{", "}");
        assertEquals(sj.toString(), "{}");

        sj = new StringJoiner("=", "{", "}").setEmptyValue("");
        assertEquals(sj.toString(), "");

        sj = new StringJoiner(DASH, "{", "}").setEmptyValue(EMPTY);
        assertEquals(sj.toString(), EMPTY);

        sj.add(cs_one);
        sj.add(cs_two);
        assertEquals(sj.toString(), "{" + ONE + DASH + TWO + "}");
    }

    public void addString() {
        StringJoiner sj = new StringJoiner(DASH);
        sj.add(ONE);
        assertEquals(sj.toString(), ONE);

        sj = new StringJoiner(DASH, "{", "}");
        sj.add(ONE);
        assertEquals(sj.toString(), "{" + ONE + "}");

        sj.add(TWO);
        assertEquals(sj.toString(), "{" + ONE + DASH + TWO + "}");
    }

    public void lengthWithCustomEmptyValue() {
        StringJoiner sj = new StringJoiner(DASH, "<", ">").setEmptyValue(EMPTY);
        assertEquals(sj.length(), EMPTY.length());
        sj.add("");
        assertEquals(sj.length(), "<>".length());
        sj.add("");
        assertEquals(sj.length(), "<->".length());
        sj.add(ONE);
        assertEquals(sj.length(), 4 + ONE_LEN);
        assertEquals(sj.toString().length(), sj.length());
        sj.add(TWO);
        assertEquals(sj.length(), 5 + ONE_LEN + TWO_LEN);
        assertEquals(sj.toString().length(), sj.length());
        sj = new StringJoiner("||", "<", "-->");
        assertEquals(sj.length(), 4);
        assertEquals(sj.toString().length(), sj.length());
        sj.add("abcdef");
        assertEquals(sj.length(), 10);
        assertEquals(sj.toString().length(), sj.length());
        sj.add("xyz");
        assertEquals(sj.length(), 15);
        assertEquals(sj.toString().length(), sj.length());
    }

    public void noAddAndEmptyValue() {
        StringJoiner sj = new StringJoiner(DASH, "", "").setEmptyValue(EMPTY);
        assertEquals(sj.toString(), EMPTY);

        sj = new StringJoiner(DASH, "<..", "");
        assertEquals(sj.toString(), "<..");

        sj = new StringJoiner(DASH, "<..", "");
        assertEquals(sj.toString(), "<..");

        sj = new StringJoiner(DASH, "", "==>");
        assertEquals(sj.toString(), "==>");

        sj = new StringJoiner(DASH, "{", "}");
        assertEquals(sj.toString(), "{}");
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void setEmptyValueNull() {
        new StringJoiner(DASH, "{", "}").setEmptyValue(null);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void setDelimiterNull() {
        new StringJoiner(null);
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void setPrefixNull() {
        new StringJoiner(DASH, null, "}");
    }

    @Test(expectedExceptions = {NullPointerException.class})
    public void setSuffixNull() {
        new StringJoiner(DASH, "{", null);
    }

    public void stringFromtoString() {
        StringJoiner sj = new StringJoiner(", ");
        assertEquals(sj.toString(), "");
        sj = new StringJoiner(",", "{", "}");
        assertEquals(sj.toString(), "{}");

        sj = new StringJoiner(",");
        sj.add(ONE);
        assertEquals(sj.toString(), ONE);

        sj.add(TWO);
        assertEquals(sj.toString(), ONE + "," + TWO);

        sj = new StringJoiner(",", "{--", "--}");
        sj.add(ONE);
        sj.add(TWO);
        assertEquals(sj.toString(), "{--" + ONE + "," + TWO + "--}");

    }

    public void stringFromtoStringWithEmptyValue() {
        StringJoiner sj = new StringJoiner(" ", "", "");
        assertEquals(sj.toString(), "");
        sj = new StringJoiner(", ");
        assertEquals(sj.toString(), "");
        sj = new StringJoiner(",", "{", "}");
        assertEquals(sj.toString(), "{}");

        sj = new StringJoiner(",", "{", "}").setEmptyValue("");
        assertEquals(sj.toString(), "");

        sj = new StringJoiner(",");
        sj.add(ONE);
        assertEquals(sj.toString(), ONE);

        sj.add(TWO);
        assertEquals(sj.toString(), ONE + "," + TWO);

        sj = new StringJoiner(",", "{--", "--}");
        sj.add(ONE);
        assertEquals(sj.toString(), "{--" + ONE + "--}" );

        sj.add(TWO);
        assertEquals(sj.toString(), "{--" + ONE + "," + TWO + "--}");

    }

    public void toStringWithCustomEmptyValue() {
        StringJoiner sj = new StringJoiner(DASH, "<", ">").setEmptyValue(EMPTY);
        assertEquals(sj.toString(), EMPTY);
        sj.add("");
        assertEquals(sj.toString(), "<>");
        sj.add("");
        assertEquals(sj.toString(), "<->");
    }

    private void testCombos(String infix, String prefix, String suffix) {
        StringJoiner sj = new StringJoiner(infix, prefix, suffix);
        assertEquals(sj.toString(), prefix + suffix);
        assertEquals(sj.toString().length(), sj.length());
        // EmptyValue
        sj = new StringJoiner(infix, prefix, suffix).setEmptyValue("<NONE>");
        assertEquals(sj.toString(), "<NONE>");
        assertEquals(sj.toString().length(), sj.length());

        // empty in front
        sj.add("");
        assertEquals(sj.toString(), prefix + suffix);
        // empty in middle
        sj.add("");
        assertEquals(sj.toString(), prefix + infix + suffix);
        sj.add("1");
        assertEquals(sj.toString(), prefix + infix + infix + "1" + suffix);
        // empty at end
        sj.add("");
        assertEquals(sj.toString(), prefix + infix + infix + "1" + infix + suffix);

        sj = new StringJoiner(infix, prefix, suffix).setEmptyValue("<NONE>");
        sj.add("1");
        assertEquals(sj.toString(), prefix + "1" + suffix);
        sj.add("2");
        assertEquals(sj.toString(), prefix + "1" + infix + "2" + suffix);
        sj.add("");
        assertEquals(sj.toString(), prefix + "1" + infix + "2" + infix + suffix);
        sj.add("3");
        assertEquals(sj.toString(), prefix + "1" + infix + "2" + infix + infix + "3" + suffix);
    }

    public void testDelimiterCombinations() {
        testCombos("", "", "");
        testCombos("", "<", "");
        testCombos("", "", ">");
        testCombos("", "<", ">");
        testCombos(",", "", "");
        testCombos(",", "<", "");
        testCombos(",", "", ">");
        testCombos(",", "<", ">");
    }

    public void OOM1() {
        try {
            new StringJoiner(MAX_STRING, MAX_STRING, MAX_STRING).toString();
            fail("Should have thrown OutOfMemoryError");
        } catch (OutOfMemoryError ex) {
            // okay
        }
    }

    public void OOM2() {
        try {
            new StringJoiner(MAX_STRING, MAX_STRING, "").toString();
            fail("Should have thrown OutOfMemoryError");
        } catch (OutOfMemoryError ex) {
            // okay
        }
    }

    public void OOM3() {
        try {
            new StringJoiner(MAX_STRING, "", MAX_STRING).toString();
            fail("Should have thrown OutOfMemoryError");
        } catch (OutOfMemoryError ex) {
            // okay
        }
    }

    public void OOM4() {
        try {
            new StringJoiner("", MAX_STRING, MAX_STRING).toString();
            fail("Should have thrown OutOfMemoryError");
        } catch (OutOfMemoryError ex) {
            // okay
        }
    }
}

