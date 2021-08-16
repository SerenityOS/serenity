/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6609740
 * @summary Checks the formatting and parsing of a number based
 *          on the positive and negative sub-patterns, also
 *          checks few invalid number patterns
 */
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public class Bug6609740 {

    public static void main(String[] args) {

        double dNumber = -3456.349347;
        String fOutput = "(3,456.35)";
        String[] validCases = {"#,##0.0#;(#,##0.0#)", "#,##0.0#;(#)",
            "#,##0.0#;(#,##0)"};

        // formatting with the valid cases
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        for (String pattern : validCases) {
            formatOnPattern(nf, pattern, dNumber, fOutput);
        }

        // parsing with the valid cases
        String parseString = "(3,456.35)";
        Number pOutput = -3456.35;
        for (String pattern : validCases) {
            parseOnPattern(nf, pattern, parseString, pOutput);
        }

        // should throw parse exception
        String[] invalidParseCases = {"#,##0.0#;0", "#,##0.0#;()"};
        for (String pattern : invalidParseCases) {
            if (nf instanceof DecimalFormat) {
                ((DecimalFormat) nf).applyPattern(pattern);
            }

            try {
                nf.parse(parseString);
            } catch (ParseException ex) {
                continue;
            }
            throw new RuntimeException("[FAILED: Should throw"
                    + " ParseException for pattern: "
                    + pattern + " and input: " + parseString + "]");
        }

        // should throw exception on invalid patterns
        // invalid patterns: no positive subpattern, zero after non-zero in
        // the decimal part i.e. 0#0, multiple decimal separators,
        // multiple percent, malformed pattern
        String[] invalidPatterns = {";(#,##0.0#)", "#,##0.0#0;(#)",
            "#,##0.0.#", "#,##0%%", ".#,##0"};
        for (String pattern : invalidPatterns) {
            if (nf instanceof DecimalFormat) {
                try {
                    ((DecimalFormat) nf).applyPattern(pattern);
                } catch (IllegalArgumentException ex) {
                    continue;
                }
                throw new RuntimeException("[FAILED: Should throw"
                        + " IllegalArgumentException for invalid pattern: "
                        + pattern + "]");
            }
        }
    }

    private static void formatOnPattern(NumberFormat nf, String pattern,
            double number, String expected) {

        if (nf instanceof DecimalFormat) {
            ((DecimalFormat) nf).applyPattern(pattern);
        }

        String formatted = nf.format(number);
        if (!formatted.equals(expected)) {
            throw new RuntimeException("[FAILED: Unable to format the number"
                    + " based on the pattern: '" + pattern + "', Expected : '"
                    + expected + "', Found: '" + formatted + "']");
        }
    }

    private static void parseOnPattern(NumberFormat nf, String pattern,
            String parseString, Number expected) {

        if (nf instanceof DecimalFormat) {
            ((DecimalFormat) nf).applyPattern(pattern);
        }

        try {
            Number output = nf.parse(parseString);
            if (expected.doubleValue() != output.doubleValue()) {
                throw new RuntimeException("[FAILED: Unable to parse the number"
                        + " based on the pattern: '" + pattern + "', Expected : '"
                        + expected + "', Found: '" + output + "']");
            }
        } catch (ParseException ex) {
            throw new RuntimeException("[FAILED: Unable to parse the pattern:"
                    + " '" + pattern + "']", ex);
        }
    }

}
