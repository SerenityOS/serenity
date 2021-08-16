/*
 * Copyright (c) 2005, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4092330 6246348
 * @summary Tests for rounding mode in NumberFormat
 */

import java.math.*;
import java.text.*;

public class NumberFormatRounding {

    static final String AE = "ArithmeticException";

    static final double src[] = {5.5, 2.5, 1.6, 1.1, 1.0, -1.0, -1.1, -1.6, -2.5, -5.5,
                                 5.501, -5.501, 5.500, -5.500, 1.001, -1.001, 4.501, -4.501, 4.500, -4.500};
    static final String up[] = {"6", "3", "2", "2", "1", "-1", "-2", "-2", "-3", "-6",
                                "6", "-6", "6", "-6", "2", "-2", "5", "-5", "5", "-5"};
    static final String down[] = {"5", "2", "1", "1", "1", "-1", "-1", "-1", "-2", "-5",
                                  "5", "-5", "5", "-5", "1", "-1", "4", "-4", "4", "-4"};
    static final String ceiling[] = {"6", "3", "2", "2", "1", "-1", "-1", "-1", "-2", "-5",
                                     "6", "-5", "6", "-5", "2", "-1", "5", "-4", "5", "-4"};
    static final String floor[] = {"5", "2", "1", "1", "1", "-1", "-2", "-2", "-3", "-6",
                                   "5", "-6", "5", "-6", "1", "-2", "4", "-5", "4", "-5"};
    static final String half_up[] = {"6", "3", "2", "1", "1", "-1", "-1", "-2", "-3", "-6",
                                     "6", "-6", "6", "-6", "1", "-1", "5", "-5", "5", "-5"};
    static final String half_down[] = {"5", "2", "2", "1", "1", "-1", "-1", "-2", "-2", "-5",
                                       "6", "-6", "5", "-5", "1", "-1", "5", "-5", "4", "-4"};
    static final String half_even[] = {"6", "2", "2", "1", "1", "-1", "-1", "-2", "-2", "-6",
                                       "6", "-6", "6", "-6", "1", "-1", "5", "-5", "4", "-4"};
    static final String unnecessary[] = {AE, AE, AE, AE, "1", "-1", AE, AE, AE, AE,
                                         AE, AE, AE, AE, AE, AE, AE, AE, AE, AE};

    public static void main(String[] args) {
        basicTest();

        roundTest(RoundingMode.UP, up);
        roundTest(RoundingMode.DOWN, down);
        roundTest(RoundingMode.CEILING, ceiling);
        roundTest(RoundingMode.FLOOR, floor);
        roundTest(RoundingMode.HALF_UP, half_up);
        roundTest(RoundingMode.HALF_DOWN, half_down);
        roundTest(RoundingMode.HALF_EVEN, half_even);
        roundTest(RoundingMode.UNNECESSARY, unnecessary);
    }

    static void basicTest() {
        NumberFormat nf = NumberFormat.getIntegerInstance();

        if (nf.getRoundingMode() != RoundingMode.HALF_EVEN) {
            throw new RuntimeException("default rounding is not HALF_EVEN");
        }

        try {
            nf.setRoundingMode(null);
            throw new RuntimeException(
                "NullPointerException is not thrown by calling setRoundingMode(null)");
        } catch (NullPointerException npe) {
            // continue testing
        }

        ChoiceFormat cf = new ChoiceFormat("");

        try {
            cf.setRoundingMode(RoundingMode.HALF_EVEN);
            throw new RuntimeException(
                "UnsupportedOperationException is not thrown by calling setRoundingMode()");
        } catch (UnsupportedOperationException uoe) {
            // continue testing
        }

        try {
            cf.getRoundingMode();
            throw new RuntimeException(
                "UnsupportedOperationException is not thrown by calling getRoundingMode()");
        } catch (UnsupportedOperationException uoe) {
            // continue testing
        }
    }

    static void roundTest(RoundingMode rm, String[] expected) {
        NumberFormat nf = NumberFormat.getIntegerInstance();
        nf.setRoundingMode(rm);

        if (nf.getRoundingMode() != rm) {
            throw new RuntimeException("set rounding mode is not returned by get method");
        }

        for (int i = 0; i < src.length; i ++) {
            String result = null;
            try {
                result = nf.parse(nf.format(src[i])).toString();
                if (!result.equals(expected[i])) {
                    throw new RuntimeException("rounding test #"+i+" failed. mode: "+rm+" src: "+src[i]+" expected: "+expected[i]+" result: "+result);
                }
            } catch (ArithmeticException ae) {
                if (expected[i].equals(AE)) {
                    continue;
                } else {
                    result = AE;
                    throw new RuntimeException("rounding test #"+i+" failed. mode: "+rm+" src: "+src[i]+" expected: "+expected[i]+" result: "+result);
                }
            } catch (ParseException pe) {
                throw new RuntimeException("ParseException ocurred.", pe);
            }
        }
    }
}
