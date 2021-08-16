/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8177552
 * @summary Checks the functioning of
 *          CompactNumberFormat.formatToCharacterIterator method
 * @modules jdk.localedata
 * @run testng/othervm TestFormatToCharacterIterator
 */
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.AttributedCharacterIterator;
import java.text.CharacterIterator;
import java.text.Format;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Set;
import static org.testng.Assert.assertEquals;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestFormatToCharacterIterator {

    private static final NumberFormat FORMAT_DZ = NumberFormat
            .getCompactNumberInstance(new Locale("dz"),
                    NumberFormat.Style.LONG);

    private static final NumberFormat FORMAT_EN_US = NumberFormat
            .getCompactNumberInstance(Locale.US,
                    NumberFormat.Style.SHORT);

    private static final NumberFormat FORMAT_EN_LONG = NumberFormat
            .getCompactNumberInstance(new Locale("en"),
                    NumberFormat.Style.LONG);

    @DataProvider(name = "fieldPositions")
    Object[][] compactFieldPositionData() {
        return new Object[][]{
            // compact format instance, number, resulted string, attributes/fields, attribute positions
            {FORMAT_DZ, 1000.09, "\u0F66\u0F9F\u0F7C\u0F44\u0F0B\u0F55\u0FB2\u0F42 \u0F21",
                new Format.Field[]{NumberFormat.Field.PREFIX, NumberFormat.Field.INTEGER}, new int[]{0, 9, 9, 10}},
            {FORMAT_DZ, -999.99, "-\u0F66\u0F9F\u0F7C\u0F44\u0F0B\u0F55\u0FB2\u0F42 \u0F21",
                new Format.Field[]{NumberFormat.Field.SIGN, NumberFormat.Field.PREFIX, NumberFormat.Field.INTEGER},
                new int[]{0, 1, 1, 10, 10, 11}},
            {FORMAT_DZ, -0.0, "-\u0F20", new Format.Field[]{NumberFormat.Field.SIGN, NumberFormat.Field.INTEGER}, new int[]{0, 1, 1, 2}},
            {FORMAT_DZ, 3000L, "\u0F66\u0F9F\u0F7C\u0F44\u0F0B\u0F55\u0FB2\u0F42 \u0F23",
                new Format.Field[]{NumberFormat.Field.PREFIX, NumberFormat.Field.INTEGER}, new int[]{0, 9, 9, 10}},
            {FORMAT_DZ, new BigInteger("12345678901234567890"),
                "\u0F51\u0F74\u0F44\u0F0B\u0F55\u0FB1\u0F74\u0F62\u0F0B\u0F66\u0F0B\u0F61\u0F0B \u0F21\u0F22\u0F23\u0F24\u0F25\u0F27",
                new Format.Field[]{NumberFormat.Field.PREFIX, NumberFormat.Field.INTEGER}, new int[]{0, 14, 14, 20}},
            {FORMAT_DZ, new BigDecimal("12345678901234567890.89"),
                "\u0F51\u0F74\u0F44\u0F0B\u0F55\u0FB1\u0F74\u0F62\u0F0B\u0F66\u0F0B\u0F61\u0F0B \u0F21\u0F22\u0F23\u0F24\u0F25\u0F27",
                new Format.Field[]{NumberFormat.Field.PREFIX, NumberFormat.Field.INTEGER}, new int[]{0, 14, 14, 20}},
            // Zeros
            {FORMAT_EN_US, 0, "0", new Format.Field[]{NumberFormat.Field.INTEGER}, new int[]{0, 1}},
            {FORMAT_EN_US, 0.0, "0", new Format.Field[]{NumberFormat.Field.INTEGER}, new int[]{0, 1}},
            {FORMAT_EN_US, -0.0, "-0", new Format.Field[]{NumberFormat.Field.SIGN, NumberFormat.Field.INTEGER}, new int[]{0, 1, 1, 2}},
            // Less than 1000 no suffix
            {FORMAT_EN_US, 499, "499", new Format.Field[]{NumberFormat.Field.INTEGER}, new int[]{0, 3}},
            // Boundary number
            {FORMAT_EN_US, 1000.0, "1K",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 1, 1, 2}},
            // Long
            {FORMAT_EN_US, 3000L, "3K",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 1, 1, 2}},
            {FORMAT_EN_US, 30000L, "30K",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 2, 2, 3}},
            {FORMAT_EN_US, 300000L, "300K",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 3, 3, 4}},
            {FORMAT_EN_US, 3000000L, "3M",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 1, 1, 2}},
            {FORMAT_EN_US, 30000000L, "30M",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 2, 2, 3}},
            {FORMAT_EN_US, 300000000L, "300M",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 3, 3, 4}},
            {FORMAT_EN_US, 3000000000L, "3B",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 1, 1, 2}},
            {FORMAT_EN_US, 30000000000L, "30B",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 2, 2, 3}},
            {FORMAT_EN_US, 300000000000L, "300B",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 3, 3, 4}},
            {FORMAT_EN_US, 3000000000000L, "3T",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 1, 1, 2}},
            {FORMAT_EN_US, 30000000000000L, "30T",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 2, 2, 3}},
            {FORMAT_EN_US, 300000000000000L, "300T",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 3, 3, 4}},
            {FORMAT_EN_US, 3000000000000000L, "3000T",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 4, 4, 5}},
            // Double
            {FORMAT_EN_US, 3000.0, "3K",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 1, 1, 2}},
            {FORMAT_EN_US, 30000.0, "30K",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 2, 2, 3}},
            {FORMAT_EN_US, 300000.0, "300K",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 3, 3, 4}},
            {FORMAT_EN_US, 3000000000000000.0, "3000T",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 4, 4, 5}},
            // BigInteger
            {FORMAT_EN_US, new BigInteger("12345678901234567890"), "12345679T",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 8, 8, 9}},
            // BigDecimal
            {FORMAT_EN_US, new BigDecimal("12345678901234567890.89"), "12345679T",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 8, 8, 9}},
            // Number as exponent
            {FORMAT_EN_US, 9.78313E+3, "10K",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 2, 2, 3}},
            // Less than 1000 no suffix
            {FORMAT_EN_LONG, 999, "999", new Format.Field[]{NumberFormat.Field.INTEGER}, new int[]{0, 3}},
            // Round the value and then format
            {FORMAT_EN_LONG, 999.99, "1 thousand",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 1, 1, 10}},
            // 10 thousand
            {FORMAT_EN_LONG, 99000, "99 thousand",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 2, 2, 11}},
            // Long path
            {FORMAT_EN_LONG, 330000, "330 thousand",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 3, 3, 12}},
            // Double path
            {FORMAT_EN_LONG, 3000.90, "3 thousand",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 1, 1, 10}},
            // BigInteger path
            {FORMAT_EN_LONG, new BigInteger("12345678901234567890"), "12345679 trillion",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 8, 8, 17}},
            // BigDecimal path
            {FORMAT_EN_LONG, new BigDecimal("12345678901234567890.89"), "12345679 trillion",
                new Format.Field[]{NumberFormat.Field.INTEGER, NumberFormat.Field.SUFFIX}, new int[]{0, 8, 8, 17}}
        };
    }

    @Test(dataProvider = "fieldPositions")
    public void testFormatToCharacterIterator(NumberFormat fmt, Object number,
            String expected, Format.Field[] expectedFields, int[] positions) {
        AttributedCharacterIterator iterator = fmt.formatToCharacterIterator(number);
        assertEquals(getText(iterator), expected, "Incorrect formatting of the number '"
                + number + "'");

        iterator.first();
        // Check start and end index of the formatted string
        assertEquals(iterator.getBeginIndex(), 0, "Incorrect start index: "
                + iterator.getBeginIndex() + " of the formatted string: " + expected);
        assertEquals(iterator.getEndIndex(), expected.length(), "Incorrect end index: "
                + iterator.getEndIndex() + " of the formatted string: " + expected);

        // Check the attributes returned by the formatToCharacterIterator
        assertEquals(iterator.getAllAttributeKeys(), Set.of(expectedFields),
                "Attributes do not match while formatting number: " + number);

        // Check the begin and end index for attributes
        iterator.first();
        int currentPosition = 0;
        do {
            int start = iterator.getRunStart();
            int end = iterator.getRunLimit();
            assertEquals(start, positions[currentPosition],
                    "Incorrect start position for the attribute(s): "
                    + iterator.getAttributes().keySet());
            assertEquals(end, positions[currentPosition + 1],
                    "Incorrect end position for the attribute(s): "
                    + iterator.getAttributes().keySet());
            currentPosition = currentPosition + 2;
            iterator.setIndex(end);
        } while (iterator.current() != CharacterIterator.DONE);
    }

    // Create the formatted string from returned AttributedCharacterIterator
    private String getText(AttributedCharacterIterator iterator) {
        StringBuffer buffer = new StringBuffer();
        for (char c = iterator.first(); c != CharacterIterator.DONE;
                c = iterator.next()) {
            buffer.append(c);
        }
        return buffer.toString();
    }

}
