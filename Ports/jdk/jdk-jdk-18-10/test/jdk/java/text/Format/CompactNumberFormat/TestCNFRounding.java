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
 * @summary Checks the rounding of formatted number in compact number formatting
 * @run testng/othervm TestCNFRounding
 */

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TestCNFRounding {

    private static final List<RoundingMode> MODES = List.of(
            RoundingMode.HALF_EVEN,
            RoundingMode.HALF_UP,
            RoundingMode.HALF_DOWN,
            RoundingMode.UP,
            RoundingMode.DOWN,
            RoundingMode.CEILING,
            RoundingMode.FLOOR);

    @DataProvider(name = "roundingData")
    Object[][] roundingData() {
        return new Object[][]{
            // Number, half_even, half_up, half_down, up, down, ceiling, floor
            {5500, new String[]{"6K", "6K", "5K", "6K", "5K", "6K", "5K"}},
            {2500, new String[]{"2K", "3K", "2K", "3K", "2K", "3K", "2K"}},
            {1600, new String[]{"2K", "2K", "2K", "2K", "1K", "2K", "1K"}},
            {1100, new String[]{"1K", "1K", "1K", "2K", "1K", "2K", "1K"}},
            {1000, new String[]{"1K", "1K", "1K", "1K", "1K", "1K", "1K"}},
            {-1000, new String[]{"-1K", "-1K", "-1K", "-1K", "-1K", "-1K", "-1K"}},
            {-1100, new String[]{"-1K", "-1K", "-1K", "-2K", "-1K", "-1K", "-2K"}},
            {-1600, new String[]{"-2K", "-2K", "-2K", "-2K", "-1K", "-1K", "-2K"}},
            {-2500, new String[]{"-2K", "-3K", "-2K", "-3K", "-2K", "-2K", "-3K"}},
            {-5500, new String[]{"-6K", "-6K", "-5K", "-6K", "-5K", "-5K", "-6K"}},
            {5501, new String[]{"6K", "6K", "6K", "6K", "5K", "6K", "5K"}},
            {-5501, new String[]{"-6K", "-6K", "-6K", "-6K", "-5K", "-5K", "-6K"}},
            {1001, new String[]{"1K", "1K", "1K", "2K", "1K", "2K", "1K"}},
            {-1001, new String[]{"-1K", "-1K", "-1K", "-2K", "-1K", "-1K", "-2K"}},
            {4501, new String[]{"5K", "5K", "5K", "5K", "4K", "5K", "4K"}},
            {-4501, new String[]{"-5K", "-5K", "-5K", "-5K", "-4K", "-4K", "-5K"}},
            {4500, new String[]{"4K", "5K", "4K", "5K", "4K", "5K", "4K"}},
            {-4500, new String[]{"-4K", "-5K", "-4K", "-5K", "-4K", "-4K", "-5K"}},};
    }

    @DataProvider(name = "roundingFract")
    Object[][] roundingFract() {
        return new Object[][]{
            // Number, half_even, half_up, half_down, up, down, ceiling, floor
            {5550, new String[]{"5.5K", "5.5K", "5.5K", "5.6K", "5.5K", "5.6K", "5.5K"}},
            {2550, new String[]{"2.5K", "2.5K", "2.5K", "2.6K", "2.5K", "2.6K", "2.5K"}},
            {1660, new String[]{"1.7K", "1.7K", "1.7K", "1.7K", "1.6K", "1.7K", "1.6K"}},
            {1110, new String[]{"1.1K", "1.1K", "1.1K", "1.2K", "1.1K", "1.2K", "1.1K"}},
            {1000, new String[]{"1.0K", "1.0K", "1.0K", "1.0K", "1.0K", "1.0K", "1.0K"}},
            {-1000, new String[]{"-1.0K", "-1.0K", "-1.0K", "-1.0K", "-1.0K", "-1.0K", "-1.0K"}},
            {-1110, new String[]{"-1.1K", "-1.1K", "-1.1K", "-1.2K", "-1.1K", "-1.1K", "-1.2K"}},
            {-1660, new String[]{"-1.7K", "-1.7K", "-1.7K", "-1.7K", "-1.6K", "-1.6K", "-1.7K"}},
            {-2550, new String[]{"-2.5K", "-2.5K", "-2.5K", "-2.6K", "-2.5K", "-2.5K", "-2.6K"}},
            {-5550, new String[]{"-5.5K", "-5.5K", "-5.5K", "-5.6K", "-5.5K", "-5.5K", "-5.6K"}},
            {5551, new String[]{"5.6K", "5.6K", "5.6K", "5.6K", "5.5K", "5.6K", "5.5K"}},
            {-5551, new String[]{"-5.6K", "-5.6K", "-5.6K", "-5.6K", "-5.5K", "-5.5K", "-5.6K"}},
            {1001, new String[]{"1.0K", "1.0K", "1.0K", "1.1K", "1.0K", "1.1K", "1.0K"}},
            {-1001, new String[]{"-1.0K", "-1.0K", "-1.0K", "-1.1K", "-1.0K", "-1.0K", "-1.1K"}},
            {4551, new String[]{"4.6K", "4.6K", "4.6K", "4.6K", "4.5K", "4.6K", "4.5K"}},
            {-4551, new String[]{"-4.6K", "-4.6K", "-4.6K", "-4.6K", "-4.5K", "-4.5K", "-4.6K"}},
            {4500, new String[]{"4.5K", "4.5K", "4.5K", "4.5K", "4.5K", "4.5K", "4.5K"}},
            {-4500, new String[]{"-4.5K", "-4.5K", "-4.5K", "-4.5K", "-4.5K", "-4.5K", "-4.5K"}},};
    }

    @DataProvider(name = "rounding2Fract")
    Object[][] rounding2Fract() {
        return new Object[][]{
            // Number, half_even, half_up, half_down
            {1115, new String[]{"1.11K", "1.11K", "1.11K"}},
            {1125, new String[]{"1.12K", "1.13K", "1.12K"}},
            {1135, new String[]{"1.14K", "1.14K", "1.14K"}},
            {3115, new String[]{"3.12K", "3.12K", "3.12K"}},
            {3125, new String[]{"3.12K", "3.13K", "3.12K"}},
            {3135, new String[]{"3.13K", "3.13K", "3.13K"}},
            {6865, new String[]{"6.87K", "6.87K", "6.87K"}},
            {6875, new String[]{"6.88K", "6.88K", "6.87K"}},
            {6885, new String[]{"6.88K", "6.88K", "6.88K"}},
            {3124, new String[]{"3.12K", "3.12K", "3.12K"}},
            {3126, new String[]{"3.13K", "3.13K", "3.13K"}},
            {3128, new String[]{"3.13K", "3.13K", "3.13K"}},
            {6864, new String[]{"6.86K", "6.86K", "6.86K"}},
            {6865, new String[]{"6.87K", "6.87K", "6.87K"}},
            {6868, new String[]{"6.87K", "6.87K", "6.87K"}},
            {4685, new String[]{"4.68K", "4.68K", "4.68K"}},
            {4687, new String[]{"4.69K", "4.69K", "4.69K"}},
            {4686, new String[]{"4.69K", "4.69K", "4.69K"}},};
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullMode() {
        NumberFormat fmt = NumberFormat
                .getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);
        fmt.setRoundingMode(null);
    }

    @Test
    public void testDefaultRoundingMode() {
        NumberFormat fmt = NumberFormat
                .getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);
        assertEquals(fmt.getRoundingMode(), RoundingMode.HALF_EVEN,
                "Default RoundingMode should be " + RoundingMode.HALF_EVEN);
    }

    @Test(dataProvider = "roundingData")
    public void testRounding(Object number, String[] expected) {
        for (int index = 0; index < MODES.size(); index++) {
            testRoundingMode(number, expected[index], 0, MODES.get(index));
        }
    }

    @Test(dataProvider = "roundingFract")
    public void testRoundingFract(Object number, String[] expected) {
        for (int index = 0; index < MODES.size(); index++) {
            testRoundingMode(number, expected[index], 1, MODES.get(index));
        }
    }

    @Test(dataProvider = "rounding2Fract")
    public void testRounding2Fract(Object number, String[] expected) {
        List<RoundingMode> rModes = List.of(RoundingMode.HALF_EVEN,
                RoundingMode.HALF_UP, RoundingMode.HALF_DOWN);
        for (int index = 0; index < rModes.size(); index++) {
            testRoundingMode(number, expected[index], 2, rModes.get(index));
        }
    }

    private void testRoundingMode(Object number, String expected,
            int fraction, RoundingMode rounding) {
        NumberFormat fmt = NumberFormat
                .getCompactNumberInstance(Locale.US, NumberFormat.Style.SHORT);
        fmt.setRoundingMode(rounding);
        assertEquals(fmt.getRoundingMode(), rounding,
                "RoundingMode set is not returned by getRoundingMode");

        fmt.setMinimumFractionDigits(fraction);
        String result = fmt.format(number);
        assertEquals(result, expected, "Incorrect formatting of number "
                + number + " using rounding mode: " + rounding);
    }

}
