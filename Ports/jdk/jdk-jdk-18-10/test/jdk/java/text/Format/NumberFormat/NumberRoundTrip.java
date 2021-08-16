/*
 * Copyright (c) 1997, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @summary round trip test NumberFormat
 * @library /java/text/testlib
 * @key randomness
 */

import java.text.*;
import java.util.*;

/**
 * This class tests the round-trip behavior of NumberFormat, DecimalFormat, and DigitList.
 * Round-trip behavior is tested by taking a numeric value and formatting it, then
 * parsing the resulting string, and comparing this result with the original value.
 * Two tests are applied:  String preservation, and numeric preservation.  String
 * preservation is exact; numeric preservation is not.  However, numeric preservation
 * should extend to the few least-significant bits.
 * //bug472
 */
public class NumberRoundTrip extends IntlTest {
    static final boolean STRING_COMPARE = true;
    static final boolean EXACT_NUMERIC_COMPARE = false;
    static final double MAX_ERROR = 1e-14;
    static boolean DEBUG = false;
    static double max_numeric_error = 0;
    static double min_numeric_error = 1;

    String localeName, formatName;

    public static void main(String[] args) throws Exception {
        if (args.length > 0 && args[0].equals("-debug")) {
            DEBUG = true;
            String[] newargs = new String[args.length - 1];
            System.arraycopy(args, 1, newargs, 0, newargs.length);
            args = newargs;
        }
        new NumberRoundTrip().run(args);
    }

    public void TestNumberFormatRoundTrip() {
        logln("Default Locale");
        localeName = "Default Locale";
        formatName = "getInstance";
        doTest(NumberFormat.getInstance());
        formatName = "getNumberInstance";
        doTest(NumberFormat.getNumberInstance());
        formatName = "getCurrencyInstance";
        doTest(NumberFormat.getCurrencyInstance());
        formatName = "getPercentInstance";
        doTest(NumberFormat.getPercentInstance());

        Locale[] loc = NumberFormat.getAvailableLocales();
        for (int i=0; i<loc.length; ++i) {
            logln(loc[i].getDisplayName());
            localeName = loc[i].toString();
            formatName = "getInstance";
            doTest(NumberFormat.getInstance(loc[i]));
            formatName = "getNumberInstance";
            doTest(NumberFormat.getNumberInstance(loc[i]));
            formatName = "getCurrencyInstance";
            doTest(NumberFormat.getCurrencyInstance(loc[i]));
            formatName = "getPercentInstance";
            doTest(NumberFormat.getPercentInstance(loc[i]));
        }

        logln("Numeric error " +
              min_numeric_error + " to " +
              max_numeric_error);
    }

    public void doTest(NumberFormat fmt) {
        doTest(fmt, Double.NaN);
        doTest(fmt, Double.POSITIVE_INFINITY);
        doTest(fmt, Double.NEGATIVE_INFINITY);

        doTest(fmt, 500);
        doTest(fmt, 0);
        doTest(fmt, 5555555555555555L);
        doTest(fmt, 55555555555555555L);
        doTest(fmt, 9223372036854775807L);
        doTest(fmt, 9223372036854775808.0);
        doTest(fmt, -9223372036854775808L);
        doTest(fmt, -9223372036854775809.0);

        for (int i=0; i<2; ++i) {
            doTest(fmt, randomDouble(1));
            doTest(fmt, randomDouble(10000));
            doTest(fmt, Math.floor(randomDouble(10000)));
            doTest(fmt, randomDouble(1e50));
            doTest(fmt, randomDouble(1e-50));
            doTest(fmt, randomDouble(1e100));
            // The use of double d such that isInfinite(100d) causes the
            // numeric test to fail with percent formats (bug 4266589).
            // Largest double s.t. 100d < Inf: d=1.7976931348623156E306
            doTest(fmt, randomDouble(1e306));
            doTest(fmt, randomDouble(1e-323));
            doTest(fmt, randomDouble(1e-100));
        }
    }

    /**
     * Return a random value from -range..+range.
     */
    public double randomDouble(double range) {
        double a = Math.random();
        return (2.0 * range * a) - range;
    }

    public void doTest(NumberFormat fmt, double value) {
        doTest(fmt, Double.valueOf(value));
    }

    public void doTest(NumberFormat fmt, long value) {
        doTest(fmt, Long.valueOf(value));
    }

    static double proportionalError(Number a, Number b) {
        double aa = a.doubleValue(), bb = b.doubleValue();
        double error = aa - bb;
        if (aa != 0 && bb != 0) error /= aa;
        return Math.abs(error);
    }

    public void doTest(NumberFormat fmt, Number value) {
        fmt.setMaximumFractionDigits(Integer.MAX_VALUE);
        String s = fmt.format(value), s2 = null;
        Number n = null;
        String err = "";
        try {
            if (DEBUG) logln("  " + value + " F> " + escape(s));
            n = fmt.parse(s);
            if (DEBUG) logln("  " + escape(s) + " P> " + n);
            s2 = fmt.format(n);
            if (DEBUG) logln("  " + n + " F> " + escape(s2));

            if (STRING_COMPARE) {
                if (!s.equals(s2)) {
                    if (fmt instanceof DecimalFormat) {
                        logln("Text mismatch: expected: " + s + ", got: " + s2 + " --- Try BigDecimal parsing.");
                        ((DecimalFormat)fmt).setParseBigDecimal(true);
                        n = fmt.parse(s);
                        if (DEBUG) logln("  " + escape(s) + " P> " + n);
                        s2 = fmt.format(n);
                        if (DEBUG) logln("  " + n + " F> " + escape(s2));
                        ((DecimalFormat)fmt).setParseBigDecimal(false);

                        if (!s.equals(s2)) {
                            err = "STRING ERROR(DecimalFormat): ";
                        }
                    } else {
                        err = "STRING ERROR(NumberFormat): ";
                    }
                }
            }

            if (EXACT_NUMERIC_COMPARE) {
                if (value.doubleValue() != n.doubleValue()) {
                    err += "NUMERIC ERROR: ";
                }
            } else {
                // Compute proportional error
                double error = proportionalError(value, n);

                if (error > MAX_ERROR) {
                    err += "NUMERIC ERROR " + error + ": ";
                }

                if (error > max_numeric_error) max_numeric_error = error;
                if (error < min_numeric_error) min_numeric_error = error;
            }

            String message = value + typeOf(value) + " F> " +
                escape(s) + " P> " +
                n + typeOf(n) + " F> " +
                escape(s2);
            if (err.length() > 0) {
                errln("*** " + err + " with " +
                      formatName + " in " + localeName +
                      " " + message);
            } else {
                logln(message);
            }
        } catch (ParseException e) {
            errln("*** " + e.toString() + " with " +
                  formatName + " in " + localeName);
        }
    }

    static String typeOf(Number n) {
        if (n instanceof Long) return " Long";
        if (n instanceof Double) return " Double";
        return " Number";
    }

    static String escape(String s) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<s.length(); ++i) {
            char c = s.charAt(i);
            if (c < (char)0xFF) {
                buf.append(c);
            } else {
                buf.append("\\U");
                buf.append(Integer.toHexString((c & 0xF000) >> 12));
                buf.append(Integer.toHexString((c & 0x0F00) >> 8));
                buf.append(Integer.toHexString((c & 0x00F0) >> 4));
                buf.append(Integer.toHexString(c & 0x000F));
            }
        }
        return buf.toString();
    }
}
