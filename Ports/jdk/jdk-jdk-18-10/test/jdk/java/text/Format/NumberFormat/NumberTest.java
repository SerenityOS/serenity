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

/**
 * @test
 * @bug 4122840 4135202 4408066 4838107 8008577
 * @summary test NumberFormat
 * @library /java/text/testlib
 * @modules java.base/sun.util.resources
 *          jdk.localedata
 * @compile -XDignore.symbol.file NumberTest.java
 * @run main/othervm -Djava.locale.providers=COMPAT,SPI NumberTest
 */

import java.util.*;
import java.text.*;
import sun.util.resources.LocaleData;

public class NumberTest extends IntlTest
{
    public static void main(String[] args) throws Exception {
        new NumberTest().run(args);
    }

    // Test pattern handling
    public void TestPatterns()
    {
    DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.US);
    String pat[]    = { "#.#", "#.", ".#", "#" };
    String newpat[] = { "#0.#", "#0.", "#.0", "#" };
    String num[]    = { "0",   "0.", ".0", "0" };
    for (int i=0; i<pat.length; ++i)
    {
        DecimalFormat fmt = new DecimalFormat(pat[i], sym);
        String newp = fmt.toPattern();
        if (!newp.equals(newpat[i]))
        errln("FAIL: Pattern " + pat[i] + " should transmute to " + newpat[i] +
              "; " + newp + " seen instead");

        String s = fmt.format(0);
        if (!s.equals(num[i]))
        {
        errln("FAIL: Pattern " + pat[i] + " should format zero as " + num[i] +
              "; " + s + " seen instead");
        logln("Min integer digits = " + fmt.getMinimumIntegerDigits());
        }
    }
    }

    // Test exponential pattern
    public void TestExponential() {
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.US);
        String pat[] = { "0.####E0", "00.000E00", "##0.####E000", "0.###E0;[0.###E0]"  };
        double val[] = { 0.01234, 123456789, 1.23e300, -3.141592653e-271 };
        long lval[] = { 0, -1, 1, 123456789 };
        String valFormat[] = {
                "1.234E-2", "1.2346E8", "1.23E300", "-3.1416E-271",
                "12.340E-03", "12.346E07", "12.300E299", "-31.416E-272",
                "12.34E-003", "123.4568E006", "1.23E300", "-314.1593E-273",
                "1.234E-2", "1.235E8", "1.23E300", "[3.142E-271]"
        };
        String lvalFormat[] = {
                "0E0", "-1E0", "1E0", "1.2346E8",
                "00.000E00", "-10.000E-01", "10.000E-01", "12.346E07",
                "0E000", "-1E000", "1E000", "123.4568E006",
                "0E0", "[1E0]", "1E0", "1.235E8"
        };
        double valParse[] = {
                0.01234, 123460000, 1.23E300, -3.1416E-271,
                0.01234, 123460000, 1.23E300, -3.1416E-271,
                0.01234, 123456800, 1.23E300, -3.141593E-271,
                0.01234, 123500000, 1.23E300, -3.142E-271,
        };
        long lvalParse[] = {
                0, -1, 1, 123460000,
                0, -1, 1, 123460000,
                0, -1, 1, 123456800,
                0, -1, 1, 123500000,
        };
        int ival = 0, ilval = 0;
        for (int p=0; p<pat.length; ++p) {
            DecimalFormat fmt = new DecimalFormat(pat[p], sym);
            logln("Pattern \"" + pat[p] + "\" -toPattern-> \"" +
                  fmt.toPattern() + '"');

            for (int v=0; v<val.length; ++v) {
                String s = fmt.format(val[v]);
                logln(" Format " + val[v] + " -> " + escape(s));
                if (!s.equals(valFormat[v+ival])) {
                    errln("FAIL: Expected " + valFormat[v+ival] +
                          ", got " + s +
                          ", pattern=" + fmt.toPattern());
                }

                ParsePosition pos = new ParsePosition(0);
                Number a = fmt.parse(s, pos);
                if (pos.getIndex() == s.length()) {
                    logln(" Parse -> " + a);
                    if (a.doubleValue() != valParse[v+ival]) {
                        errln("FAIL: Expected " + valParse[v+ival] +
                              ", got " + a.doubleValue() +
                              ", pattern=" + fmt.toPattern());
                    }
                } else {
                    errln(" FAIL: Partial parse (" + pos.getIndex() +
                          " chars) -> " + a);
                }
            }
            for (int v=0; v<lval.length; ++v) {
                String s = fmt.format(lval[v]);
                logln(" Format " + lval[v] + "L -> " + escape(s));
                if (!s.equals(lvalFormat[v+ilval])) {
                    errln("ERROR: Expected " + lvalFormat[v+ilval] +
                          ", got " + s +
                          ", pattern=" + fmt.toPattern());
                }

                ParsePosition pos = new ParsePosition(0);
                Number a = fmt.parse(s, pos);
                if (pos.getIndex() == s.length()) {
                    logln(" Parse -> " + a);
                    if (a.longValue() != lvalParse[v+ilval]) {
                        errln("FAIL: Expected " + lvalParse[v+ilval] +
                              ", got " + a +
                              ", pattern=" + fmt.toPattern());
                    }
                } else {
                    errln(" FAIL: Partial parse (" + pos.getIndex() +
                          " chars) -> " + a);
                }
            }
            ival += val.length;
            ilval += lval.length;
        }
    }

    // Test the handling of quotes
    public void TestQuotes()
    {
    String pat;
    DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.US);
    DecimalFormat fmt = new DecimalFormat(pat = "a'fo''o'b#", sym);
    String s = fmt.format(123);
    logln("Pattern \"" + pat + "\"");
    logln(" Format 123 -> " + escape(s));
    if (!s.equals("afo'ob123")) errln("FAIL: Expected afo'ob123");

    fmt = new DecimalFormat(pat = "a''b#", sym);
    s = fmt.format(123);
    logln("Pattern \"" + pat + "\"");
    logln(" Format 123 -> " + escape(s));
    if (!s.equals("a'b123")) errln("FAIL: Expected a'b123");
    }

    // Test the use of the currency sign
    public void TestCurrencySign()
    {
    DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.US);
    DecimalFormat fmt = new DecimalFormat("\u00A4#,##0.00;-\u00A4#,##0.00", sym);
    // Can't test this properly until currency API goes public
    // DecimalFormatSymbols sym = fmt.getDecimalFormatSymbols();

    String s = fmt.format(1234.56);
    logln("Pattern \"" + fmt.toPattern() + "\"");
    logln(" Format " + 1234.56 + " -> " + escape(s));
    if (!s.equals("$1,234.56")) errln("FAIL: Expected $1,234.56");
    s = fmt.format(-1234.56);
    logln(" Format " + -1234.56 + " -> " + escape(s));
    if (!s.equals("-$1,234.56")) errln("FAIL: Expected -$1,234.56");

    fmt = new DecimalFormat("\u00A4\u00A4 #,##0.00;\u00A4\u00A4 -#,##0.00", sym);
    s = fmt.format(1234.56);
    logln("Pattern \"" + fmt.toPattern() + "\"");
    logln(" Format " + 1234.56 + " -> " + escape(s));
    if (!s.equals("USD 1,234.56")) errln("FAIL: Expected USD 1,234.56");
    s = fmt.format(-1234.56);
    logln(" Format " + -1234.56 + " -> " + escape(s));
    if (!s.equals("USD -1,234.56")) errln("FAIL: Expected USD -1,234.56");
    }
    static String escape(String s)
    {
    StringBuffer buf = new StringBuffer();
    char HEX[] = { '0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F' };
    for (int i=0; i<s.length(); ++i)
    {
        char c = s.charAt(i);
        if (c <= (char)0x7F) buf.append(c);
        else
        {
        buf.append("\\U");
        buf.append(HEX[(c & 0xF000) >> 12]);
        buf.append(HEX[(c & 0x0F00) >> 8]);
        buf.append(HEX[(c & 0x00F0) >> 4]);
        buf.append(HEX[c & 0x000F]);
        }
    }
    return buf.toString();
    }

    // Test simple currency format
    // Bug 4024941; this code used to throw a NumberFormat exception
    public void TestCurrency() {
        NumberFormat currencyFmt =
                NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH);
        String s = currencyFmt.format(1.50);
        logln("Un pauvre ici a..........." + s);
        if (!s.equals("1,50 $")) {
            errln("FAIL: Expected 1,50 $; got " + s + "; "+ dumpFmt(currencyFmt));
        }
        currencyFmt = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        s = currencyFmt.format(1.50);
        logln("Un pauvre en Allemagne a.." + s);
        if (!s.equals("1,50 \u20AC")) {
            errln("FAIL: Expected 1,50 \u20AC; got " + s + "; " + dumpFmt(currencyFmt));
        }
        currencyFmt = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        s = currencyFmt.format(1.50);
        logln("Un pauvre en France a....." + s);
        if (!s.equals("1,50 \u20AC")) {
            errln("FAIL: Expected 1,50 \u20AC; got " + s + "; " + dumpFmt(currencyFmt));
        }
    }

    String dumpFmt(NumberFormat numfmt) {
        DecimalFormat fmt = (DecimalFormat)numfmt;
        StringBuffer buf = new StringBuffer();
        buf.append("pattern \"");
        buf.append(fmt.toPattern());
        buf.append("\", currency \"");
        buf.append(fmt.getDecimalFormatSymbols().getCurrencySymbol());
        buf.append("\"");
        return buf.toString();
    }

    // Test numeric parsing
    // Bug 4059870
    public void TestParse()
    {
    String arg = "0";
    java.text.DecimalFormat format = new java.text.DecimalFormat("00");
    try {
        Number n = format.parse(arg);
        logln("parse(" + arg + ") = " + n);
        if (n.doubleValue() != 0.0) errln("FAIL: Expected 0");
    } catch (Exception e) { errln("Exception caught: " + e); }
    }

    // Test rounding
    public void TestRounding487() {
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        roundingTest(nf, 0.00159999, 4, "0.0016");
        roundingTest(nf, 0.00995,  4, "0.01");
        roundingTest(nf, 12.7995,  3, "12.8");
        roundingTest(nf, 12.4999,  0, "12");
        roundingTest(nf, -19.5,  0, "-20");
    }

    void roundingTest(NumberFormat nf, double x, int maxFractionDigits, String expected) {
        nf.setMaximumFractionDigits(maxFractionDigits);
        String out = nf.format(x);
        logln("" + x + " formats with " + maxFractionDigits + " fractional digits to " + out);
        if (!out.equals(expected)) {
            errln("FAIL: Expected " + expected + ", got " + out);
        }
    }

    /**
     * Bug 4135202
     * DecimalFormat should recognize not only Latin digits 0-9 (\u0030-\u0039)
     * but also various other ranges of Unicode digits, such as Arabic
     * digits \u0660-\u0669 and Devanagari digits \u0966-\u096F, to name
     * a couple.
     * @see java.lang.Character#isDigit(char)
     */
    public void TestUnicodeDigits() {
        char[] zeros = {
            0x0030, // ISO-LATIN-1 digits ('0' through '9')
            0x0660, // Arabic-Indic digits
            0x06F0, // Extended Arabic-Indic digits
            0x0966, // Devanagari digits
            0x09E6, // Bengali digits
            0x0A66, // Gurmukhi digits
            0x0AE6, // Gujarati digits
            0x0B66, // Oriya digits
            0x0BE6, // Tamil digits
            0x0C66, // Telugu digits
            0x0CE6, // Kannada digits
            0x0D66, // Malayalam digits
            0x0E50, // Thai digits
            0x0ED0, // Lao digits
            0x0F20, // Tibetan digits
            0xFF10, // Fullwidth digits
        };
        NumberFormat format = NumberFormat.getInstance();
        for (int i=0; i<zeros.length; ++i) {
            char zero = zeros[i];
            StringBuffer buf = new StringBuffer();
            buf.append((char)(zero+3));
            buf.append((char)(zero+1));
            buf.append((char)(zero+4));
            int n = -1;
            try {
                n = format.parse(buf.toString()).intValue();
            }
            catch (ParseException e) { n = -2; }
            if (n != 314)
                errln("Can't parse Unicode " + Integer.toHexString(zero) + " as digit (" + n + ")");
            else
                logln("Parse digit " + Integer.toHexString(zero) + " ok");
        }
    }

    /**
     * Bug 4122840
     * Make sure that the currency symbol is not hard-coded in any locale.
     */
    public void TestCurrencySubstitution() {
        final String SYM = "<currency>";
        final String INTL_SYM = "<intl.currency>";
        Locale[] locales = NumberFormat.getAvailableLocales();
        for (int i=0; i<locales.length; ++i) {
            NumberFormat nf = NumberFormat.getCurrencyInstance(locales[i]);
            if (nf instanceof DecimalFormat) {
                DecimalFormat df = (DecimalFormat)nf;
                String genericPos = df.format(1234.5678);
                String genericNeg = df.format(-1234.5678);
                DecimalFormatSymbols sym = df.getDecimalFormatSymbols();
                sym.setCurrencySymbol(SYM);
                sym.setInternationalCurrencySymbol(INTL_SYM);
                // We have to make a new DecimalFormat from scratch in order
                // to make the new symbols 'take'.  This may be a bug or
                // design flaw in DecimalFormat.
                String[] patterns = LocaleData.getBundle("sun.text.resources.FormatData", locales[i])
                                              .getStringArray("NumberPatterns");
                df = new DecimalFormat(patterns[1 /*CURRENCYSTYLE*/], sym);
                String customPos = df.format(1234.5678);
                String customNeg = df.format(-1234.5678);
                if (genericPos.equals(customPos) || genericNeg.equals(customNeg)) {
                    errln("FAIL: " + locales[i] +
                          " not using currency symbol substitution: " + genericPos);
                }
                else {
                    if (customPos.indexOf(SYM) >= 0) {
                        if (customNeg.indexOf(INTL_SYM) >= 0)
                            errln("Fail: Positive and negative patterns use different symbols");
                        else
                            logln("Ok: " + locales[i] +
                                  " uses currency symbol: " + genericPos +
                                  ", " + customPos);
                    }
                    else if (customPos.indexOf(INTL_SYM) >= 0) {
                        if (customNeg.indexOf(SYM) >= 0)
                            errln("Fail: Positive and negative patterns use different symbols");
                        else
                            logln("Ok: " + locales[i] +
                                  " uses intl. currency symbol: " + genericPos +
                                  ", " + customPos);
                    }
                    else {
                        errln("FAIL: " + locales[i] +
                              " contains no currency symbol (impossible!)");
                    }
                }
            }
            else logln("Skipping " + locales[i] + "; not a DecimalFormat");
        }
    }

    public void TestIntegerFormat() throws ParseException {
        NumberFormat format = NumberFormat.getIntegerInstance(Locale.GERMANY);

        float[] formatInput = { 12345.67f, -12345.67f, -0, 0 };
        String[] formatExpected = { "12.346", "-12.346", "0", "0" };

        for (int i = 0; i < formatInput.length; i++) {
            String result = format.format(formatInput[i]);
            if (!result.equals(formatExpected[i])) {
                errln("FAIL: Expected " + formatExpected[i] + ", got " + result);
            }
        }

        String[] parseInput = { "0", "-0", "12.345,67", "-12.345,67" };
        float[] parseExpected = { 0, 0, 12345, -12345 };

        for (int i = 0; i < parseInput.length; i++) {
            float result = format.parse(parseInput[i]).floatValue();
            if (result != parseExpected[i]) {
                errln("FAIL: Expected " + parseExpected[i] + ", got " + result);
            }
        }
    }
}
