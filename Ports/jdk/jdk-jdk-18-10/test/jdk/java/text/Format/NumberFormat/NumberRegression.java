/*
 * Copyright (c) 1997, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4052223 4059870 4061302 4062486 4066646 4068693 4070798 4071005 4071014
 * 4071492 4071859 4074454 4074620 4075713 4083018 4086575 4087244 4087245
 * 4087251 4087535 4088161 4088503 4090489 4090504 4092480 4092561 4095713
 * 4098741 4099404 4101481 4106658 4106662 4106664 4108738 4110936 4122840
 * 4125885 4134034 4134300 4140009 4141750 4145457 4147295 4147706 4162198
 * 4162852 4167494 4170798 4176114 4179818 4185761 4212072 4212073 4216742
 * 4217661 4243011 4243108 4330377 4233840 4241880 4833877 8008577 8227313
 * @summary Regression tests for NumberFormat and associated classes
 * @library /java/text/testlib
 * @build IntlTest HexDumpReader TestUtils
 * @modules java.base/sun.util.resources
 *          jdk.localedata
 * @compile -XDignore.symbol.file NumberRegression.java
 * @run main/othervm -Djava.locale.providers=COMPAT,SPI NumberRegression
 */

/*
(C) Copyright Taligent, Inc. 1996 - All Rights Reserved
(C) Copyright IBM Corp. 1996 - All Rights Reserved

  The original version of this source code and documentation is copyrighted and
owned by Taligent, Inc., a wholly-owned subsidiary of IBM. These materials are
provided under terms of a License Agreement between Taligent and Sun. This
technology is protected by multiple US and International patents. This notice and
attribution to Taligent may not be removed.
  Taligent is a registered trademark of Taligent, Inc.
*/

import java.text.*;
import java.util.*;
import java.math.BigDecimal;
import java.io.*;
import java.math.BigInteger;
import sun.util.resources.LocaleData;

public class NumberRegression extends IntlTest {

    public static void main(String[] args) throws Exception {
        new NumberRegression().run(args);
    }

    /**
     * NumberFormat.equals comparing with null should always return false.
     */
    public void Test4075713(){

        try {
            MyNumberFormatTest tmp = new MyNumberFormatTest();
            if (!tmp.equals(null))
                logln("NumberFormat.equals passed");
        } catch (NullPointerException e) {
            errln("(new MyNumberFormatTest()).equals(null) throws unexpected exception");
        }
    }

    /**
     * NumberFormat.equals comparing two obj equal even the setGroupingUsed
     * flag is different.
     */
    public void Test4074620() {

        MyNumberFormatTest nf1 = new MyNumberFormatTest();
        MyNumberFormatTest nf2 = new MyNumberFormatTest();

        nf1.setGroupingUsed(false);
        nf2.setGroupingUsed(true);

        if (nf1.equals(nf2)) errln("Test for bug 4074620 failed");
        else logln("Test for bug 4074620 passed.");
        return;
    }


    /**
     * DecimalFormat.format() incorrectly uses maxFractionDigits setting.
     */

    public void Test4088161 (){
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesAsciiDigits(locale)) {
            logln("Skipping this test because locale is " + locale);
            return;
        }

        DecimalFormat df = new DecimalFormat();
        double d = 100;
        df.setMinimumFractionDigits(0);
        df.setMaximumFractionDigits(16);
        StringBuffer sBuf1 = new StringBuffer("");
        FieldPosition fp1 = new FieldPosition(0);
        logln("d = " + d);
        logln("maxFractionDigits = " + df.getMaximumFractionDigits());
        logln(" format(d) = '" + df.format(d, sBuf1, fp1) + "'");
        df.setMaximumFractionDigits(17);
        StringBuffer sBuf2 = new StringBuffer("");
        FieldPosition fp2 = new FieldPosition(0);
        logln("maxFractionDigits = " + df.getMaximumFractionDigits());
        df.format(d, sBuf2, fp2);
        String expected = "100";
        if (!sBuf2.toString().equals(expected))
            errln(" format(d) = '" + sBuf2 + "'");
    }
    /**
     * DecimalFormatSymbols should be cloned in the ctor DecimalFormat.
     * DecimalFormat(String, DecimalFormatSymbols).
     */
    public void Test4087245 (){
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance();
        DecimalFormat df = new DecimalFormat("#,##0.0", symbols);
        long n = 123;
        StringBuffer buf1 = new StringBuffer();
        StringBuffer buf2 = new StringBuffer();
        logln("format(" + n + ") = " +
        df.format(n, buf1, new FieldPosition(0)));
        symbols.setDecimalSeparator('p'); // change value of field
        logln("format(" + n + ") = " +
        df.format(n, buf2, new FieldPosition(0)));
        if (!buf1.toString().equals(buf2.toString()))
            errln("Test for bug 4087245 failed");
    }
    /**
     * DecimalFormat.format() incorrectly formats 0.0
     */
    public void Test4087535 ()
    {
        DecimalFormat df = new DecimalFormat();
        df.setMinimumIntegerDigits(0);

        double n = 0;
        String buffer = new String();
        buffer = df.format(n);
        if (buffer.length() == 0)
            errln(n + ": '" + buffer + "'");
        n = 0.1;
        buffer = df.format(n);
        if (buffer.length() == 0)
            errln(n + ": '" + buffer + "'");
    }

    /**
     * DecimalFormat.format fails when groupingSize is set to 0.
     */
    public void Test4088503 (){
        DecimalFormat df = new DecimalFormat();
        df.setGroupingSize(0);
        StringBuffer sBuf = new StringBuffer("");
        FieldPosition fp = new FieldPosition(0);
        try {
            logln(df.format(123, sBuf, fp).toString());
        } catch (Exception foo) {
            errln("Test for bug 4088503 failed.");
        }

    }
    /**
     * NumberFormat.getCurrencyInstance is wrong.
     */
    public void Test4066646 () {
        float returnfloat = 0.0f;
        assignFloatValue(2.04f);
        assignFloatValue(2.03f);
        assignFloatValue(2.02f);
        assignFloatValue(0.0f);
    }

    public float assignFloatValue(float returnfloat)
    {
        logln(" VALUE " + returnfloat);
        NumberFormat nfcommon =  NumberFormat.getCurrencyInstance(Locale.US);
        nfcommon.setGroupingUsed(false);

        String stringValue = nfcommon.format(returnfloat).substring(1);
        if (Float.valueOf(stringValue).floatValue() != returnfloat)
            errln(" DISPLAYVALUE " + stringValue);
        return returnfloat;
    } // End Of assignFloatValue()

    /**
     * DecimalFormat throws exception when parsing "0"
     */
    public void Test4059870() {
        DecimalFormat format = new DecimalFormat("00");
        try {
            logln(format.parse("0").toString());
        } catch (Exception e) { errln("Test for bug 4059870 failed : " + e); }
    }
    /**
     * DecimalFormatSymbol.equals should always return false when
     * comparing with null.
     */

    public void Test4083018 (){
        DecimalFormatSymbols dfs = DecimalFormatSymbols.getInstance();
        try {
            if (!dfs.equals(null))
                logln("Test Passed!");
        } catch (Exception foo) {
            errln("Test for bug 4083018 failed => Message : " + foo.getMessage());
        }
    }
    /**
     * DecimalFormat does not round up correctly.
     */
    public void Test4071492 (){
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        double x = 0.00159999;
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(4);
        String out = nf.format(x);
        logln("0.00159999 formats with 4 fractional digits to " + out);
        String expected = "0.0016";
        if (!out.equals(expected))
            errln("FAIL: Expected " + expected);
        Locale.setDefault(savedLocale);
    }

    /**
     * A space as a group separator for localized pattern causes
     * wrong format.  WorkAround : use non-breaking space.
     */
    public void Test4086575() {

        NumberFormat nf = NumberFormat.getInstance(Locale.FRANCE);
        logln("nf toPattern1: " + ((DecimalFormat)nf).toPattern());
        logln("nf toLocPattern1: " + ((DecimalFormat)nf).toLocalizedPattern());

        // No group separator
        logln("...applyLocalizedPattern ###,00;(###,00) ");
        ((DecimalFormat)nf).applyLocalizedPattern("###,00;(###,00)");
        logln("nf toPattern2: " + ((DecimalFormat)nf).toPattern());
        logln("nf toLocPattern2: " + ((DecimalFormat)nf).toLocalizedPattern());

        logln("nf: " + nf.format(1234)); // 1234,00
        logln("nf: " + nf.format(-1234)); // (1234,00)

        // Space as group separator

        logln("...applyLocalizedPattern # ###,00;(# ###,00) ");
        ((DecimalFormat)nf).applyLocalizedPattern("#\u00a0###,00;(#\u00a0###,00)");
        logln("nf toPattern2: " + ((DecimalFormat)nf).toPattern());
        logln("nf toLocPattern2: " + ((DecimalFormat)nf).toLocalizedPattern());
        String buffer = nf.format(1234);
        if (!buffer.equals("1\u00a0234,00"))
            errln("nf : " + buffer); // Expect 1 234,00
        buffer = nf.format(-1234);
        if (!buffer.equals("(1\u00a0234,00)"))
            errln("nf : " + buffer); // Expect (1 234,00)

        // Erroneously prints:
        // 1234,00 ,
        // (1234,00 ,)

    }
    /**
     * DecimalFormat.parse returns wrong value
     */
    public void Test4068693()
    {
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        logln("----- Test Application -----");
        ParsePosition pos;
        DecimalFormat df = new DecimalFormat();
        Double d = (Double)df.parse("123.55456", pos=new ParsePosition(0));
        if (!d.toString().equals("123.55456")) {
            errln("Result -> " + d);
        }
        Locale.setDefault(savedLocale);
    }

    /* bugs 4069754, 4067878
     * null pointer thrown when accessing a deserialized DecimalFormat
     * object.
     */
    public void Test4069754()
    {
        try {
            myformat it = new myformat();
            logln(it.Now());
            FileOutputStream ostream = new FileOutputStream("t.tmp");
            ObjectOutputStream p = new ObjectOutputStream(ostream);
            p.writeObject(it);
            ostream.close();
            logln("Saved ok.");

            FileInputStream istream = new FileInputStream("t.tmp");
            ObjectInputStream p2 = new ObjectInputStream(istream);
            myformat it2 = (myformat)p2.readObject();
            logln(it2.Now());
            istream.close();
            logln("Loaded ok.");
        } catch (Exception foo) {
            errln("Test for bug 4069754 or 4057878 failed => Exception: " + foo.getMessage());
        }
    }

    /**
     * DecimalFormat.applyPattern(String) allows illegal patterns
     */
    public void Test4087251 (){
        DecimalFormat df = new DecimalFormat();
        try {
            df.applyPattern("#.#.#");
            logln("toPattern() returns \"" + df.toPattern() + "\"");
            errln("applyPattern(\"#.#.#\") doesn't throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            logln("Caught Illegal Argument Error !");
        }
        // Second test; added 5/11/98 when reported to fail on 1.2b3
        try {
            df.applyPattern("#0.0#0#0");
            logln("toPattern() returns \"" + df.toPattern() + "\"");
            errln("applyPattern(\"#0.0#0#0\") doesn't throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            logln("Ok - IllegalArgumentException for #0.0#0#0");
        }
    }

    /**
     * DecimalFormat.format() loses precision
     */
    public void Test4090489 (){
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        DecimalFormat df = new DecimalFormat();
        df.setMinimumFractionDigits(10);
        df.setGroupingUsed(false);
        double d = 1.000000000000001E7;
        BigDecimal bd = new BigDecimal(d);
        StringBuffer sb = new StringBuffer("");
        FieldPosition fp = new FieldPosition(0);
        logln("d = " + d);
        logln("BigDecimal.toString():  " + bd.toString());
        df.format(d, sb, fp);
        if (!sb.toString().equals("10000000.0000000100")) {
            errln("DecimalFormat.format(): " + sb.toString());
        }
        Locale.setDefault(savedLocale);
    }

    /**
     * DecimalFormat.format() loses precision
     */
    public void Test4090504 ()
    {
        double d = 1;
        logln("d = " + d);
        DecimalFormat df = new DecimalFormat();
        StringBuffer sb;
        FieldPosition fp;
        try {
            for (int i = 17; i <= 20; i++) {
                df.setMaximumFractionDigits(i);
                sb = new StringBuffer("");
                fp = new FieldPosition(0);
                logln("  getMaximumFractionDigits() = " + i);
                logln("  formated: " + df.format(d, sb, fp));
            }
        } catch (Exception foo) {
            errln("Bug 4090504 regression test failed. Message : " + foo.getMessage());
        }
    }
    /**
     * DecimalFormat.parse(String str, ParsePosition pp) loses precision
     */
    public void Test4095713 ()
    {
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        DecimalFormat df = new DecimalFormat();
        String str = "0.1234";
        Double d1 = 0.1234;
        Double d2 = (Double) df.parse(str, new ParsePosition(0));
        logln(d1.toString());
        if (d2.doubleValue() != d1.doubleValue())
            errln("Bug 4095713 test failed, new double value : " + d2);
        Locale.setDefault(savedLocale);
    }

    /**
     * DecimalFormat.parse() fails when multiplier is not set to 1
     */
    public void Test4092561 ()
    {
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        DecimalFormat df = new DecimalFormat();

        String str = Long.toString(Long.MIN_VALUE);
        logln("Long.MIN_VALUE : " + df.parse(str, new ParsePosition(0)).toString());
        df.setMultiplier(100);
        Number num = df.parse(str, new ParsePosition(0));
        if (num.doubleValue() != -9.223372036854776E16) {
            errln("Bug 4092561 test failed when multiplier is not set to 1. Expected: -9.223372036854776E16, got: " + num.doubleValue());
        }

        df.setMultiplier(-100);
        num = df.parse(str, new ParsePosition(0));
        if (num.doubleValue() != 9.223372036854776E16) {
            errln("Bug 4092561 test failed when multiplier is not set to 1. Expected: 9.223372036854776E16, got: " + num.doubleValue());
        }

        str = Long.toString(Long.MAX_VALUE);
        logln("Long.MAX_VALUE : " + df.parse(str, new ParsePosition(0)).toString());

        df.setMultiplier(100);
        num = df.parse(str, new ParsePosition(0));
        if (num.doubleValue() != 9.223372036854776E16) {
            errln("Bug 4092561 test failed when multiplier is not set to 1. Expected: 9.223372036854776E16, got: " + num.doubleValue());
        }

        df.setMultiplier(-100);
        num = df.parse(str, new ParsePosition(0));
        if (num.doubleValue() != -9.223372036854776E16) {
            errln("Bug 4092561 test failed when multiplier is not set to 1. Expected: -9.223372036854776E16, got: " + num.doubleValue());
        }

        Locale.setDefault(savedLocale);
    }

    /**
     * DecimalFormat: Negative format ignored.
     */
    public void Test4092480 ()
    {
        DecimalFormat dfFoo = new DecimalFormat("000");

        try {
            dfFoo.applyPattern("0000;-000");
            if (!dfFoo.toPattern().equals("#0000"))
                errln("dfFoo.toPattern : " + dfFoo.toPattern());
            logln(dfFoo.format(42));
            logln(dfFoo.format(-42));
            dfFoo.applyPattern("000;-000");
            if (!dfFoo.toPattern().equals("#000"))
                errln("dfFoo.toPattern : " + dfFoo.toPattern());
            logln(dfFoo.format(42));
            logln(dfFoo.format(-42));

            dfFoo.applyPattern("000;-0000");
            if (!dfFoo.toPattern().equals("#000"))
                errln("dfFoo.toPattern : " + dfFoo.toPattern());
            logln(dfFoo.format(42));
            logln(dfFoo.format(-42));

            dfFoo.applyPattern("0000;-000");
            if (!dfFoo.toPattern().equals("#0000"))
                errln("dfFoo.toPattern : " + dfFoo.toPattern());
            logln(dfFoo.format(42));
            logln(dfFoo.format(-42));
        } catch (Exception foo) {
            errln("Message " + foo.getMessage());
        }
    }
    /**
     * NumberFormat.getCurrencyInstance() produces format that uses
     * decimal separator instead of monetary decimal separator.
     *
     * Rewrote this test not to depend on the actual pattern.  Pattern should
     * never contain the monetary separator!  Decimal separator in pattern is
     * interpreted as monetary separator if currency symbol is seen!
     */
    public void Test4087244 () {
        Locale de = new Locale("pt", "PT");
        DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance(de);
        DecimalFormatSymbols sym = df.getDecimalFormatSymbols();
        sym.setMonetaryDecimalSeparator('$');
        df.setDecimalFormatSymbols(sym);
        char decSep = sym.getDecimalSeparator();
        char monSep = sym.getMonetaryDecimalSeparator();
        char zero = sym.getZeroDigit();
        if (decSep == monSep) {
            errln("ERROR in test: want decimal sep != monetary sep");
        } else {
            df.setMinimumIntegerDigits(1);
            df.setMinimumFractionDigits(2);
            String str = df.format(1.23);
            String monStr = "1" + monSep + "23";
            String decStr = "1" + decSep + "23";
            if (str.indexOf(monStr) >= 0 && str.indexOf(decStr) < 0) {
                logln("OK: 1.23 -> \"" + str + "\" contains \"" +
                      monStr + "\" and not \"" + decStr + '"');
            } else {
                errln("FAIL: 1.23 -> \"" + str + "\", should contain \"" +
                      monStr +
                      "\" and not \"" + decStr + '"');
            }
        }
    }
    /**
     * Number format data rounding errors for locale FR
     */
    public void Test4070798 () {
        NumberFormat formatter;
        String tempString;
        /* User error :
        String expectedDefault = "-5\u00a0789,987";
        String expectedCurrency = "5\u00a0789,98 F";
        String expectedPercent = "-578\u00a0998%";
        */
        String expectedDefault = "-5\u00a0789,988";
        String expectedCurrency = "5\u00a0789,99 \u20AC";
        // changed for bug 6547501
        String expectedPercent = "-578\u00a0999 %";

        formatter = NumberFormat.getNumberInstance(Locale.FRANCE);
        tempString = formatter.format (-5789.9876);

        if (tempString.equals(expectedDefault)) {
            logln ("Bug 4070798 default test passed.");
        } else {
            errln("Failed:" +
            " Expected " + expectedDefault +
            " Received " + tempString );
        }


        formatter = NumberFormat.getCurrencyInstance(Locale.FRANCE);
        tempString = formatter.format( 5789.9876 );

        if (tempString.equals(expectedCurrency) ) {
            logln ("Bug 4070798 currency test assed.");
        } else {
            errln("Failed:" +
            " Expected " + expectedCurrency +
            " Received " + tempString );
        }


        formatter = NumberFormat.getPercentInstance(Locale.FRANCE);
        tempString = formatter.format (-5789.9876);

        if (tempString.equals(expectedPercent) ) {
            logln ("Bug 4070798 percentage test passed.");
        } else {
            errln("Failed:" +
            " Expected " + expectedPercent +
            " Received " + tempString );
        }
    }
    /**
     * Data rounding errors for French (Canada) locale
     */
    public void Test4071005 () {

        NumberFormat formatter;
        String tempString;
    /* user error :
        String expectedDefault = "-5 789,987";
        String expectedCurrency = "5 789,98 $";
        String expectedPercent = "-578 998%";
    */
        String expectedDefault = "-5\u00a0789,988";
        String expectedCurrency = "5\u00a0789,99 $";
        // changed for bug 6547501
        String expectedPercent = "-578\u00a0999 %";

        formatter = NumberFormat.getNumberInstance(Locale.CANADA_FRENCH);
        tempString = formatter.format (-5789.9876);
        if (tempString.equals(expectedDefault)) {
            logln ("Bug 4071005 default test passed.");
        } else {
            errln("Failed:" +
            " Expected " + expectedDefault +
            " Received " + tempString );
        }

        formatter = NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH);
        tempString = formatter.format( 5789.9876 ) ;

        if (tempString.equals(expectedCurrency) ) {
            logln ("Bug 4071005 currency test passed.");
        } else {
            errln("Failed:" +
            " Expected " + expectedCurrency +
            " Received " + tempString );
        }
        formatter = NumberFormat.getPercentInstance(Locale.CANADA_FRENCH);
        tempString = formatter.format (-5789.9876);

        if (tempString.equals(expectedPercent) ) {
            logln ("Bug 4071005 percentage test passed.");
        } else {
            errln("Failed:" +
            " Expected " + expectedPercent +
            " Received " + tempString );
        }
    }

    /**
     * Data rounding errors for German (Germany) locale
     */
    public void Test4071014 () {
        NumberFormat formatter;
        String tempString;
        /* user error :
        String expectedDefault = "-5.789,987";
        String expectedCurrency = "5.789,98 DM";
        String expectedPercent = "-578.998%";
        */
        String expectedDefault = "-5.789,988";
        String expectedCurrency = "5.789,99 \u20AC";
        String expectedPercent = "-578.999%";

        formatter = NumberFormat.getNumberInstance(Locale.GERMANY);
        tempString = formatter.format (-5789.9876);

        if (tempString.equals(expectedDefault)) {
            logln ("Bug 4071014 default test passed.");
        } else {
            errln("Failed:" +
            " Expected " + expectedDefault +
            " Received " + tempString );
        }

        formatter = NumberFormat.getCurrencyInstance(Locale.GERMANY);
        tempString = formatter.format( 5789.9876 ) ;

        if (tempString.equals(expectedCurrency) ) {
            logln ("Bug 4071014 currency test passed.");
        } else {
            errln("Failed:" +
            " Expected " + expectedCurrency +
            " Received " + tempString );
        }

        formatter = NumberFormat.getPercentInstance(Locale.GERMANY);
        tempString = formatter.format (-5789.9876);

        if (tempString.equals(expectedPercent) ) {
            logln ("Bug 4071014 percentage test passed.");
        } else {
            errln("Failed:" +
            " Expected " + expectedPercent +
            " Received " + tempString );
        }

    }
    /**
     * Data rounding errors for Italian locale number formats
     */
    public void Test4071859 () {
        NumberFormat formatter;
        String tempString;
        /* user error :
        String expectedDefault = "-5.789,987";
        String expectedCurrency = "-L. 5.789,98";
        String expectedPercent = "-578.998%";
        */
        String expectedDefault = "-5.789,988";
        String expectedCurrency = "-\u20AC 5.789,99";
        String expectedPercent = "-578.999%";

        formatter = NumberFormat.getNumberInstance(Locale.ITALY);
        tempString = formatter.format (-5789.9876);

        if (tempString.equals(expectedDefault)) {
            logln ("Bug 4071859 default test passed.");
        } else {
            errln("Failed:" +
            " Expected " + expectedDefault +
            " Received " + tempString );
        }

        formatter = NumberFormat.getCurrencyInstance(Locale.ITALY);
        tempString = formatter.format( -5789.9876 ) ;

        if (tempString.equals(expectedCurrency) ) {
            logln ("Bug 4071859 currency test passed.");
        } else {
            errln("Failed:" +
            " Expected " + expectedCurrency +
            " Received " + tempString );
        }

        formatter = NumberFormat.getPercentInstance(Locale.ITALY);
        tempString = formatter.format (-5789.9876);

        if (tempString.equals(expectedPercent) ) {
            logln ("Bug 4071859 percentage test passed.");
        } else {
            errln("Failed:" +
            " Expected " + expectedPercent +
            " Received " + tempString );
        }

    }

    /* bug 4071859
     * Test rounding for nearest even.
     */
    public void Test4093610()
    {
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        DecimalFormat df = new DecimalFormat("#0.#");

        roundingTest(df, 12.15, "12.2"); // Rounding-up. Above tie (12.150..)
        roundingTest(df, 12.25, "12.2"); // No round-up. Exact + half-even rule.
        roundingTest(df, 12.45, "12.4"); // No round-up. Below tie (12.449..)
        roundingTest(df, 12.450000001,"12.5"); // Rounding-up. Above tie.
        roundingTest(df, 12.55, "12.6"); // Rounding-up. Above tie (12.550..)
        roundingTest(df, 12.650000001,"12.7"); // Rounding-up. Above tie.
        roundingTest(df, 12.75, "12.8"); // Rounding-up. Exact + half-even rule.
        roundingTest(df, 12.750000001,"12.8"); // Rounding-up. Above tie.
        roundingTest(df, 12.85, "12.8"); // No round-up. Below tie (12.849..)
        roundingTest(df, 12.850000001,"12.9"); // Rounding-up. Above tie.
        roundingTest(df, 12.950000001,"13");   // Rounding-up. Above tie.

        Locale.setDefault(savedLocale);
    }

    void roundingTest(DecimalFormat df, double x, String expected)
    {
        String out = df.format(x);
        logln("" + x + " formats with 1 fractional digits to " + out);
        if (!out.equals(expected)) errln("FAIL: Expected " + expected);
    }
    /**
     * Tests the setMaximumFractionDigits limit.
     */
    public void Test4098741()
    {
        try {
            NumberFormat fmt = NumberFormat.getPercentInstance();
            fmt.setMaximumFractionDigits(20);
            logln(fmt.format(.001));
        } catch (Exception foo) {
            errln("Bug 4098471 failed with exception thrown : " + foo.getMessage());
        }
    }
    /**
     * Tests illegal pattern exception.
     * Fix comment : HShih A31 Part1 will not be fixed and javadoc needs to be updated.
     * Part2 has been fixed.
     */
    public void Test4074454()
    {
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        try {
            DecimalFormat fmt = new DecimalFormat("#,#00.00;-#.#");
            logln("Inconsistent negative pattern is fine.");
            DecimalFormat newFmt = new DecimalFormat("#,#00.00 p''ieces;-#,#00.00 p''ieces");
            String tempString = newFmt.format(3456.78);
            if (!tempString.equals("3,456.78 p'ieces"))
                errln("Failed!  3,456.78 p'ieces expected, but got : " + tempString);
        } catch (Exception foo) {
            errln("An exception was thrown for any inconsistent negative pattern.");
        }
        Locale.setDefault(savedLocale);
    }

    /**
     * Tests all different comments.
     * Response to some comments :
     * [1] DecimalFormat.parse API documentation is more than just one line.
     * This is not a reproducable doc error in 116 source code.
     * [2] See updated javadoc.
     * [3] Fixed.
     * [4] NumberFormat.parse(String, ParsePosition) : If parsing fails,
     * a null object will be returned.  The unchanged parse position also
     * reflects an error.
     * NumberFormat.parse(String) : If parsing fails, an ParseException
     * will be thrown.
     * See updated javadoc for more details.
     * [5] See updated javadoc.
     * [6] See updated javadoc.
     * [7] This is a correct behavior if the DateFormat object is linient.
     * Otherwise, an IllegalArgumentException will be thrown when formatting
     * "January 35".  See GregorianCalendar class javadoc for more details.
     */
    public void Test4099404()
    {
        try {
            DecimalFormat fmt = new DecimalFormat("000.0#0");
            errln("Bug 4099404 failed applying illegal pattern \"000.0#0\"");
        } catch (Exception foo) {
            logln("Bug 4099404 pattern \"000.0#0\" passed");
        }
        try {
            DecimalFormat fmt = new DecimalFormat("0#0.000");
            errln("Bug 4099404 failed applying illegal pattern \"0#0.000\"");
        } catch (Exception foo) {
            logln("Bug 4099404 pattern \"0#0.000\" passed");
        }
    }
    /**
     * DecimalFormat.applyPattern doesn't set minimum integer digits
     */
    public void Test4101481()
    {
        DecimalFormat sdf = new DecimalFormat("#,##0");
        if (sdf.getMinimumIntegerDigits() != 1)
            errln("Minimum integer digits : " + sdf.getMinimumIntegerDigits());
    }
    /**
     * Tests ParsePosition.setErrorPosition() and ParsePosition.getErrorPosition().
     */
    public void Test4052223()
    {
        try {
            DecimalFormat fmt = new DecimalFormat("#,#00.00");
            Number num = fmt.parse("abc3");
            errln("Bug 4052223 failed : can't parse string \"a\".  Got " + num);
        } catch (ParseException foo) {
            logln("Caught expected ParseException : " + foo.getMessage() + " at index : " + foo.getErrorOffset());
        }
    }
    /**
     * API tests for API addition request A9.
     */
    public void Test4061302()
    {
        DecimalFormatSymbols fmt = DecimalFormatSymbols.getInstance();
        String currency = fmt.getCurrencySymbol();
        String intlCurrency = fmt.getInternationalCurrencySymbol();
        char monDecSeparator = fmt.getMonetaryDecimalSeparator();
        if (currency.equals("") ||
            intlCurrency.equals("") ||
            monDecSeparator == 0) {
            errln("getCurrencySymbols failed, got empty string.");
        }
        logln("Before set ==> Currency : " + currency + " Intl Currency : " + intlCurrency + " Monetary Decimal Separator : " + monDecSeparator);
        fmt.setCurrencySymbol("XYZ");
        fmt.setInternationalCurrencySymbol("ABC");
        fmt.setMonetaryDecimalSeparator('*');
        currency = fmt.getCurrencySymbol();
        intlCurrency = fmt.getInternationalCurrencySymbol();
        monDecSeparator = fmt.getMonetaryDecimalSeparator();
        if (!currency.equals("XYZ") ||
            !intlCurrency.equals("ABC") ||
            monDecSeparator != '*') {
            errln("setCurrencySymbols failed.");
        }
        logln("After set ==> Currency : " + currency + " Intl Currency : " + intlCurrency + " Monetary Decimal Separator : " + monDecSeparator);
    }
    /**
     * API tests for API addition request A23. FieldPosition.getBeginIndex and
     * FieldPosition.getEndIndex.
     */
    public void Test4062486()
    {
        DecimalFormat fmt = new DecimalFormat("#,##0.00");
        StringBuffer formatted = new StringBuffer();
        FieldPosition field = new FieldPosition(0);
        Double num = 1234.5;
        fmt.format(num, formatted, field);
        if (field.getBeginIndex() != 0 && field.getEndIndex() != 5)
            errln("Format 1234.5 failed. Begin index: " + field.getBeginIndex() + " End index: " + field.getEndIndex());
        field.setBeginIndex(7);
        field.setEndIndex(4);
        if (field.getBeginIndex() != 7 && field.getEndIndex() != 4)
            errln("Set begin/end field indexes failed. Begin index: " + field.getBeginIndex() + " End index: " + field.getEndIndex());
    }

    /**
     * DecimalFormat.parse incorrectly works with a group separator.
     */
    public void Test4108738()
    {

        DecimalFormat df = new DecimalFormat("#,##0.###",
        DecimalFormatSymbols.getInstance(java.util.Locale.US));
        String text = "1.222,111";
        Number num = df.parse(text,new ParsePosition(0));
        if (!num.toString().equals("1.222"))
            errln("\"" + text + "\"  is parsed as " + num);
        text = "1.222x111";
        num = df.parse(text,new ParsePosition(0));
        if (!num.toString().equals("1.222"))
            errln("\"" + text + "\"  is parsed as " + num);
    }

    /**
     * DecimalFormat.format() incorrectly formats negative doubles.
     */
    public void Test4106658()
    {
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        DecimalFormat df = new DecimalFormat(); // Corrected; see 4147706
        double d1 = -0.0;
        double d2 = -0.0001;
        StringBuffer buffer = new StringBuffer();
        logln("pattern: \"" + df.toPattern() + "\"");
        df.format(d1, buffer, new FieldPosition(0));
        if (!buffer.toString().equals("-0")) { // Corrected; see 4147706
            errln(d1 + "      is formatted as " + buffer);
        }
        buffer.setLength(0);
        df.format(d2, buffer, new FieldPosition(0));
        if (!buffer.toString().equals("-0")) { // Corrected; see 4147706
            errln(d2 + "      is formatted as " + buffer);
        }
        Locale.setDefault(savedLocale);
    }

    /**
     * DecimalFormat.parse returns 0 if string parameter is incorrect.
     */
    public void Test4106662()
    {
        DecimalFormat df = new DecimalFormat();
        String text = "x";
        ParsePosition pos1 = new ParsePosition(0), pos2 = new ParsePosition(0);

        logln("pattern: \"" + df.toPattern() + "\"");
        Number num = df.parse(text, pos1);
        if (num != null) {
            errln("Test Failed: \"" + text + "\" is parsed as " + num);
        }
        df = null;
        df = new DecimalFormat("$###.00");
        num = df.parse("$", pos2);
        if (num != null){
            errln("Test Failed: \"$\" is parsed as " + num);
        }
    }

    /**
     * NumberFormat.parse doesn't return null
     */
    public void Test4114639()
    {
        NumberFormat format = NumberFormat.getInstance();
        String text = "time 10:x";
        ParsePosition pos = new ParsePosition(8);
        Number result = format.parse(text, pos);
        if (result != null) errln("Should return null but got : " + result); // Should be null; it isn't
    }

    /**
     * DecimalFormat.format(long n) fails if n * multiplier > MAX_LONG.
     */
    public void Test4106664()
    {
        DecimalFormat df = new DecimalFormat();
        long n = 1234567890123456L;
        int m = 12345678;
        BigInteger bigN = BigInteger.valueOf(n);
        bigN = bigN.multiply(BigInteger.valueOf(m));
        df.setMultiplier(m);
        df.setGroupingUsed(false);
        logln("formated: " +
            df.format(n, new StringBuffer(), new FieldPosition(0)));
        logln("expected: " + bigN.toString());
    }
    /**
     * DecimalFormat.format incorrectly formats -0.0.
     */
    public void Test4106667()
    {
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        DecimalFormat df = new DecimalFormat();
        df.setPositivePrefix("+");
        double d = -0.0;
        logln("pattern: \"" + df.toPattern() + "\"");
        StringBuffer buffer = new StringBuffer();
        df.format(d, buffer, new FieldPosition(0));
        if (!buffer.toString().equals("-0")) { // Corrected; see 4147706
            errln(d + "  is formatted as " + buffer);
        }
        Locale.setDefault(savedLocale);
    }

    /**
     * DecimalFormat.setMaximumIntegerDigits() works incorrectly.
     */
    public void Test4110936()
    {
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumIntegerDigits(128);
        logln("setMaximumIntegerDigits(128)");
        if (nf.getMaximumIntegerDigits() != 128)
            errln("getMaximumIntegerDigits() returns " +
                nf.getMaximumIntegerDigits());
    }

    /**
     * Locale data should use generic currency symbol
     *
     * 1) Make sure that all currency formats use the generic currency symbol.
     * 2) Make sure we get the same results using the generic symbol or a
     *    hard-coded one.
     */
    public void Test4122840()
    {
        Locale[] locales = NumberFormat.getAvailableLocales();

        for (int i = 0; i < locales.length; i++) {
            ResourceBundle rb = LocaleData.getBundle("sun.text.resources.FormatData",
                                                     locales[i]);
            //
            // Get the currency pattern for this locale.  We have to fish it
            // out of the ResourceBundle directly, since DecimalFormat.toPattern
            // will return the localized symbol, not \00a4
            //
            String[] numPatterns = (String[])rb.getObject("NumberPatterns");
            String pattern = numPatterns[1];

            if (pattern.indexOf("\u00A4") == -1 ) {
                errln("Currency format for " + locales[i] +
                        " does not contain generic currency symbol:" +
                        pattern );
            }

            // Create a DecimalFormat using the pattern we got and format a number
            DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(locales[i]);
            DecimalFormat fmt1 = new DecimalFormat(pattern, symbols);

            String result1 = fmt1.format(1.111);

            //
            // Now substitute in the locale's currency symbol and create another
            // pattern.  Replace the decimal separator with the monetary separator.
            //
            char decSep = symbols.getDecimalSeparator();
            char monSep = symbols.getMonetaryDecimalSeparator();
            StringBuffer buf = new StringBuffer(pattern);
            for (int j = 0; j < buf.length(); j++) {
                if (buf.charAt(j) == '\u00a4') {
                    String cur = "'" + symbols.getCurrencySymbol() + "'";
                    buf.replace(j, j+1, cur);
                    j += cur.length() - 1;
                }
            }
            symbols.setDecimalSeparator(monSep);
            DecimalFormat fmt2 = new DecimalFormat(buf.toString(), symbols);

            String result2 = fmt2.format(1.111);

            if (!result1.equals(result2)) {
                errln("Results for " + locales[i] + " differ: " +
                      result1 + " vs " + result2);
            }
        }
    }

    /**
     * DecimalFormat.format() delivers wrong string.
     */
    public void Test4125885()
    {
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        double rate = 12.34;
        DecimalFormat formatDec = new DecimalFormat ("000.00");
        logln("toPattern: " + formatDec.toPattern());
        String rateString= formatDec.format(rate);
        if (!rateString.equals("012.34"))
            errln("result : " + rateString + " expected : 012.34");
        rate = 0.1234;
        formatDec = null;
        formatDec = new DecimalFormat ("+000.00%;-000.00%");
        logln("toPattern: " + formatDec.toPattern());
        rateString= formatDec.format(rate);
        if (!rateString.equals("+012.34%"))
            errln("result : " + rateString + " expected : +012.34%");
        Locale.setDefault(savedLocale);
    }

    /**
     **
     * DecimalFormat produces extra zeros when formatting numbers.
     */
    public void Test4134034() {
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        DecimalFormat nf = new DecimalFormat("##,###,###.00");

        String f = nf.format(9.02);
        if (f.equals("9.02")) logln(f + " ok"); else errln("9.02 -> " + f + "; want 9.02");

        f = nf.format(0);
        if (f.equals(".00")) logln(f + " ok"); else errln("0 -> " + f + "; want .00");
        Locale.setDefault(savedLocale);
    }

    /**
     * CANNOT REPRODUCE - This bug could not be reproduced.  It may be
     * a duplicate of 4134034.
     *
     * JDK 1.1.6 Bug, did NOT occur in 1.1.5
     * Possibly related to bug 4125885.
     *
     * This class demonstrates a regression in version 1.1.6
     * of DecimalFormat class.
     *
     * 1.1.6 Results
     * Value 1.2 Format #.00 Result '01.20' !!!wrong
     * Value 1.2 Format 0.00 Result '001.20' !!!wrong
     * Value 1.2 Format 00.00 Result '0001.20' !!!wrong
     * Value 1.2 Format #0.0# Result '1.2'
     * Value 1.2 Format #0.00 Result '001.20' !!!wrong
     *
     * 1.1.5 Results
     * Value 1.2 Format #.00 Result '1.20'
     * Value 1.2 Format 0.00 Result '1.20'
     * Value 1.2 Format 00.00 Result '01.20'
     * Value 1.2 Format #0.0# Result '1.2'
     * Value 1.2 Format #0.00 Result '1.20'
     */
    public void Test4134300() {
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        String[] DATA = {
         // Pattern      Expected string
            "#.00",      "1.20",
            "0.00",      "1.20",
            "00.00",     "01.20",
            "#0.0#",     "1.2",
            "#0.00",     "1.20",
        };
        for (int i=0; i<DATA.length; i+=2) {
            String result = new DecimalFormat(DATA[i]).format(1.2);
            if (!result.equals(DATA[i+1])) {
                errln("Fail: 1.2 x " + DATA[i] + " = " + result +
                      "; want " + DATA[i+1]);
            }
            else {
                logln("Ok: 1.2 x " + DATA[i] + " = " + result);
            }
        }
        Locale.setDefault(savedLocale);
    }

    /**
     * Empty pattern produces double negative prefix.
     */
    public void Test4140009() {
        for (int i=0; i<2; ++i) {
            DecimalFormat f = null;
            switch (i) {
            case 0:
                f = new DecimalFormat("",
                            DecimalFormatSymbols.getInstance(Locale.ENGLISH));
                break;
            case 1:
                f = new DecimalFormat("#.#",
                            DecimalFormatSymbols.getInstance(Locale.ENGLISH));
                f.applyPattern("");
                break;
            }
            String s = f.format(123.456);
            if (!s.equals("123.456"))
                errln("Fail: Format empty pattern x 123.456 => " + s);
            s = f.format(-123.456);
            if (!s.equals("-123.456"))
                errln("Fail: Format empty pattern x -123.456 => " + s);
        }
    }

    /**
     * BigDecimal numbers get their fractions truncated by NumberFormat.
     */
    public void Test4141750() {
        try {
            String str = "12345.67";
            BigDecimal bd = new BigDecimal(str);
            NumberFormat nf = NumberFormat.getInstance(Locale.US);
            String sd = nf.format(bd);
            if (!sd.endsWith("67")) {
                errln("Fail: " + str + " x format -> " + sd);
            }
        }
        catch (Exception e) {
            errln(e.toString());
            e.printStackTrace();
        }
    }

    /**
     * DecimalFormat toPattern() doesn't quote special characters or handle
     * single quotes.
     */
    public void Test4145457() {
        try {
            DecimalFormat nf = (DecimalFormat)NumberFormat.getInstance();
            DecimalFormatSymbols sym = nf.getDecimalFormatSymbols();
            sym.setDecimalSeparator('\'');
            nf.setDecimalFormatSymbols(sym);
            double pi = 3.14159;

            String[] PATS = { "#.00 'num''ber'", "''#.00''" };

            for (int i=0; i<PATS.length; ++i) {
                nf.applyPattern(PATS[i]);
                String out = nf.format(pi);
                String pat = nf.toPattern();
                double val = nf.parse(out).doubleValue();

                nf.applyPattern(pat);
                String out2 = nf.format(pi);
                String pat2 = nf.toPattern();
                double val2 = nf.parse(out2).doubleValue();

                if (!pat.equals(pat2))
                    errln("Fail with \"" + PATS[i] + "\": Patterns should concur, \"" +
                          pat + "\" vs. \"" + pat2 + "\"");
                else
                    logln("Ok \"" + PATS[i] + "\" toPattern() -> \"" + pat + '"');

                if (val == val2 && out.equals(out2)) {
                    logln("Ok " + pi + " x \"" + PATS[i] + "\" -> \"" +
                          out + "\" -> " + val + " -> \"" +
                          out2 + "\" -> " + val2);
                }
                else {
                    errln("Fail " + pi + " x \"" + PATS[i] + "\" -> \"" +
                          out + "\" -> " + val + " -> \"" +
                          out2 + "\" -> " + val2);
                }
            }
        }
        catch (ParseException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }
    }

    /**
     * DecimalFormat.applyPattern() sets minimum integer digits incorrectly.
     * CANNOT REPRODUCE
     * This bug is a duplicate of 4139344, which is a duplicate of 4134300
     */
    public void Test4147295() {
        DecimalFormat sdf = new DecimalFormat();
        String pattern = "#,###";
        logln("Applying pattern \"" + pattern + "\"");
        sdf.applyPattern(pattern);
        int minIntDig = sdf.getMinimumIntegerDigits();
        if (minIntDig != 0) {
            errln("Test failed");
            errln(" Minimum integer digits : " + minIntDig);
            errln(" new pattern: " + sdf.toPattern());
        } else {
            logln("Test passed");
            logln(" Minimum integer digits : " + minIntDig);
        }
    }

    /**
     * DecimalFormat formats -0.0 as +0.0
     * See also older related bug 4106658, 4106667
     */
    public void Test4147706() {
        DecimalFormat df = new DecimalFormat("#,##0.0##");
        df.setDecimalFormatSymbols(DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        double d1 = -0.0;
        double d2 = -0.0001;
        StringBuffer f1 = df.format(d1, new StringBuffer(), new FieldPosition(0));
        StringBuffer f2 = df.format(d2, new StringBuffer(), new FieldPosition(0));
        if (!f1.toString().equals("-0.0")) {
            errln(d1 + " x \"" + df.toPattern() + "\" is formatted as \"" + f1 + '"');
        }
        if (!f2.toString().equals("-0.0")) {
            errln(d2 + " x \"" + df.toPattern() + "\" is formatted as \"" + f2 + '"');
        }
    }

    /**
     * NumberFormat cannot format Double.MAX_VALUE
     */
    public void Test4162198() {
        double dbl = Double.MAX_VALUE;
        NumberFormat f = NumberFormat.getInstance();
        f.setMaximumFractionDigits(Integer.MAX_VALUE);
        f.setMaximumIntegerDigits(Integer.MAX_VALUE);
        String s = f.format(dbl);
        logln("The number " + dbl + " formatted to " + s);
        Number n = null;
        try {
            n = f.parse(s);
        } catch (java.text.ParseException e) {
            errln("Caught a ParseException:");
            e.printStackTrace();
        }
        logln("The string " + s + " parsed as " + n);
        if (n.doubleValue() != dbl) {
            errln("Round trip failure");
        }
    }

    /**
     * NumberFormat does not parse negative zero.
     */
    public void Test4162852() throws ParseException {
        for (int i=0; i<2; ++i) {
            NumberFormat f = (i == 0) ? NumberFormat.getInstance()
                : NumberFormat.getPercentInstance();
            double d = -0.0;
            String s = f.format(d);
            double e = f.parse(s).doubleValue();
            logln("" +
                  d + " -> " +
                  '"' + s + '"' + " -> " +
              e);
            if (e != 0.0 || 1.0/e > 0.0) {
                logln("Failed to parse negative zero");
            }
        }
    }

    /**
     * NumberFormat truncates data
     */
    public void Test4167494() throws Exception {
        NumberFormat fmt = NumberFormat.getInstance(Locale.US);

        double a = Double.MAX_VALUE;
        String s = fmt.format(a);
        double b = fmt.parse(s).doubleValue();
        boolean match = a == b;
        if (match) {
            logln("" + a + " -> \"" + s + "\" -> " + b + " ok");
        } else {
            errln("" + a + " -> \"" + s + "\" -> " + b + " FAIL");
        }

        // We don't test Double.MIN_VALUE because the locale data for the US
        // currently doesn't specify enough digits to display Double.MIN_VALUE.
        // This is correct for now; however, we leave this here as a reminder
        // in case we want to address this later.
        if (false) {
            a = Double.MIN_VALUE;
            s = fmt.format(a);
            b = fmt.parse(s).doubleValue();
            match = a == b;
            if (match) {
                logln("" + a + " -> \"" + s + "\" -> " + b + " ok");
            } else {
                errln("" + a + " -> \"" + s + "\" -> " + b + " FAIL");
            }
        }
    }

    /**
     * DecimalFormat.parse() fails when ParseIntegerOnly set to true
     */
    public void Test4170798() {
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        DecimalFormat df = new DecimalFormat();
        df.setParseIntegerOnly(true);
        Number n = df.parse("-0.0", new ParsePosition(0));
        if (!(n instanceof Long || n instanceof Integer)
            || n.intValue() != 0) {
            errln("FAIL: parse(\"-0.0\") returns " +
                  n + " (" + n.getClass().getName() + ')');
        }
        Locale.setDefault(savedLocale);
    }

    /**
     * toPattern only puts the first grouping separator in.
     */
    public void Test4176114() {
        String[] DATA = {
            "00", "#00",
            "000", "#000", // No grouping
            "#000", "#000", // No grouping
            "#,##0", "#,##0",
            "#,000", "#,000",
            "0,000", "#0,000",
            "00,000", "#00,000",
            "000,000", "#,000,000",
            "0,000,000,000,000.0000", "#0,000,000,000,000.0000", // Reported
        };
        for (int i=0; i<DATA.length; i+=2) {
            DecimalFormat df = new DecimalFormat(DATA[i]);
            String s = df.toPattern();
            if (!s.equals(DATA[i+1])) {
                errln("FAIL: " + DATA[i] + " -> " + s + ", want " + DATA[i+1]);
            }
        }
    }

    /**
     * DecimalFormat is incorrectly rounding numbers like 1.2501 to 1.2
     */
    public void Test4179818() {
        String DATA[] = {
            // Input  Pattern  Expected output
            "1.2511", "#.#",   "1.3",
            "1.2501", "#.#",   "1.3",
            "0.9999", "#",     "1",
        };
        DecimalFormat fmt = new DecimalFormat("#",
                DecimalFormatSymbols.getInstance(Locale.US));
        for (int i=0; i<DATA.length; i+=3) {
            double in = Double.valueOf(DATA[i]);
            String pat = DATA[i+1];
            String exp = DATA[i+2];
            fmt.applyPattern(pat);
            String out = fmt.format(in);
            if (out.equals(exp)) {
                logln("Ok: " + in + " x " + pat + " = " + out);
            } else {
                errln("FAIL: " + in + " x  " + pat + " = " + out +
                      ", expected " + exp);
            }
        }
    }

    public void Test4185761() throws IOException, ClassNotFoundException {
        /* Code used to write out the initial files, which are
         * then edited manually:
        NumberFormat nf = NumberFormat.getInstance(Locale.US);
        nf.setMinimumIntegerDigits(0x111); // Keep under 309
        nf.setMaximumIntegerDigits(0x112); // Keep under 309
        nf.setMinimumFractionDigits(0x113); // Keep under 340
        nf.setMaximumFractionDigits(0x114); // Keep under 340
        FileOutputStream ostream =
            new FileOutputStream("NumberFormat4185761");
        ObjectOutputStream p = new ObjectOutputStream(ostream);
        p.writeObject(nf);
        ostream.close();
        */

        // File                 minint maxint minfrac maxfrac
        // NumberFormat4185761a  0x122  0x121   0x124   0x123
        // NumberFormat4185761b  0x311  0x312   0x313   0x314
        // File a is bad because the mins are smaller than the maxes.
        // File b is bad because the values are too big for a DecimalFormat.
        // These files have a sufix ".ser.txt".

        InputStream istream = HexDumpReader.getStreamFromHexDump("NumberFormat4185761a.ser.txt");
        ObjectInputStream p = new ObjectInputStream(istream);
        try {
            NumberFormat nf = (NumberFormat) p.readObject();
            errln("FAIL: Deserialized bogus NumberFormat int:" +
                  nf.getMinimumIntegerDigits() + ".." +
                  nf.getMaximumIntegerDigits() + " frac:" +
                  nf.getMinimumFractionDigits() + ".." +
                  nf.getMaximumFractionDigits());
        } catch (InvalidObjectException e) {
            logln("Ok: " + e.getMessage());
        }
        istream.close();

        istream = HexDumpReader.getStreamFromHexDump("NumberFormat4185761b.ser.txt");
        p = new ObjectInputStream(istream);
        try {
            NumberFormat nf = (NumberFormat) p.readObject();
            errln("FAIL: Deserialized bogus DecimalFormat int:" +
                  nf.getMinimumIntegerDigits() + ".." +
                  nf.getMaximumIntegerDigits() + " frac:" +
                  nf.getMinimumFractionDigits() + ".." +
                  nf.getMaximumFractionDigits());
        } catch (InvalidObjectException e) {
            logln("Ok: " + e.getMessage());
        }
        istream.close();
    }


    /**
     * Some DecimalFormatSymbols changes are not picked up by DecimalFormat.
     * This includes the minus sign, currency symbol, international currency
     * symbol, percent, and permille.  This is filed as bugs 4212072 and
     * 4212073.
     */
    public void Test4212072() throws IOException, ClassNotFoundException {
        DecimalFormatSymbols sym = DecimalFormatSymbols.getInstance(Locale.US);
        DecimalFormat fmt = new DecimalFormat("#", sym);

        sym.setMinusSign('^');
        fmt.setDecimalFormatSymbols(sym);
        if (!fmt.format(-1).equals("^1")) {
            errln("FAIL: -1 x (minus=^) -> " + fmt.format(-1) +
                  ", exp ^1");
        }
        if (!fmt.getNegativePrefix().equals("^")) {
            errln("FAIL: (minus=^).getNegativePrefix -> " +
                  fmt.getNegativePrefix() + ", exp ^");
        }
        sym.setMinusSign('-');

        fmt.applyPattern("#%");
        sym.setPercent('^');
        fmt.setDecimalFormatSymbols(sym);
        if (!fmt.format(0.25).equals("25^")) {
            errln("FAIL: 0.25 x (percent=^) -> " + fmt.format(0.25) +
                  ", exp 25^");
        }
        if (!fmt.getPositiveSuffix().equals("^")) {
            errln("FAIL: (percent=^).getPositiveSuffix -> " +
                  fmt.getPositiveSuffix() + ", exp ^");
        }
        sym.setPercent('%');

        fmt.applyPattern("#\u2030");
        sym.setPerMill('^');
        fmt.setDecimalFormatSymbols(sym);
        if (!fmt.format(0.25).equals("250^")) {
            errln("FAIL: 0.25 x (permill=^) -> " + fmt.format(0.25) +
                  ", exp 250^");
        }
        if (!fmt.getPositiveSuffix().equals("^")) {
            errln("FAIL: (permill=^).getPositiveSuffix -> " +
                  fmt.getPositiveSuffix() + ", exp ^");
        }
        sym.setPerMill('\u2030');

        fmt.applyPattern("\u00A4#.00");
        sym.setCurrencySymbol("usd");
        fmt.setDecimalFormatSymbols(sym);
        if (!fmt.format(12.5).equals("usd12.50")) {
            errln("FAIL: 12.5 x (currency=usd) -> " + fmt.format(12.5) +
                  ", exp usd12.50");
        }
        if (!fmt.getPositivePrefix().equals("usd")) {
            errln("FAIL: (currency=usd).getPositivePrefix -> " +
                  fmt.getPositivePrefix() + ", exp usd");
        }
        sym.setCurrencySymbol("$");

        fmt.applyPattern("\u00A4\u00A4#.00");
        sym.setInternationalCurrencySymbol("DOL");
        fmt.setDecimalFormatSymbols(sym);
        if (!fmt.format(12.5).equals("DOL12.50")) {
            errln("FAIL: 12.5 x (intlcurrency=DOL) -> " + fmt.format(12.5) +
                  ", exp DOL12.50");
        }
        if (!fmt.getPositivePrefix().equals("DOL")) {
            errln("FAIL: (intlcurrency=DOL).getPositivePrefix -> " +
                  fmt.getPositivePrefix() + ", exp DOL");
        }
        sym.setInternationalCurrencySymbol("USD");

        // Since the pattern logic has changed, make sure that patterns round
        // trip properly.  Test stream in/out integrity too.
        Locale[] avail = NumberFormat.getAvailableLocales();
        for (int i=0; i<avail.length; ++i) {
            for (int j=0; j<3; ++j) {
                NumberFormat nf;
                switch (j) {
                case 0:
                    nf = NumberFormat.getInstance(avail[i]);
                    break;
                case 1:
                    nf = NumberFormat.getCurrencyInstance(avail[i]);
                    break;
                default:
                    nf = NumberFormat.getPercentInstance(avail[i]);
                    break;
                }
                DecimalFormat df = (DecimalFormat) nf;

                // Test toPattern/applyPattern round trip
                String pat = df.toPattern();
                DecimalFormatSymbols symb = DecimalFormatSymbols.getInstance(avail[i]);
                DecimalFormat f2 = new DecimalFormat(pat, symb);
                if (!df.equals(f2)) {
                    errln("FAIL: " + avail[i] + " -> \"" + pat +
                          "\" -> \"" + f2.toPattern() + '"');
                }

                // Test toLocalizedPattern/applyLocalizedPattern round trip
                pat = df.toLocalizedPattern();
                f2.applyLocalizedPattern(pat);
                if (!df.equals(f2)) {
                    errln("FAIL: " + avail[i] + " -> localized \"" + pat +
                          "\" -> \"" + f2.toPattern() + '"');
                }

                // Test writeObject/readObject round trip
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);
                oos.writeObject(df);
                oos.flush();
                baos.close();
                byte[] bytes = baos.toByteArray();
                ObjectInputStream ois =
                    new ObjectInputStream(new ByteArrayInputStream(bytes));
                f2 = (DecimalFormat) ois.readObject();
                if (!df.equals(f2)) {
                    errln("FAIL: Stream in/out " + avail[i] + " -> \"" + pat +
                          "\" -> " +
                          (f2 != null ? ("\""+f2.toPattern()+'"') : "null"));
                }

            }
        }
    }

    /**
     * DecimalFormat.parse() fails for mulipliers 2^n.
     */
    public void Test4216742() throws ParseException {
        DecimalFormat fmt = (DecimalFormat) NumberFormat.getInstance(Locale.US);
        long[] DATA = { Long.MIN_VALUE, Long.MAX_VALUE, -100000000L, 100000000L};
        for (int i=0; i<DATA.length; ++i) {
            String str = Long.toString(DATA[i]);
            for (int m = 1; m <= 100; m++) {
                fmt.setMultiplier(m);
                long n = fmt.parse(str).longValue();
                if (n > 0 != DATA[i] > 0) {
                    errln("\"" + str + "\" parse(x " + fmt.getMultiplier() +
                          ") => " + n);
                }
            }
        }
    }

    /**
     * DecimalFormat formats 1.001 to "1.00" instead of "1" with 2 fraction
     * digits.
     */
    public void Test4217661() {
        Object[] DATA = {
            0.001, "0",
            1.001, "1",
            0.006, "0.01",
            1.006, "1.01",
        };
        NumberFormat fmt = NumberFormat.getInstance(Locale.US);
        fmt.setMaximumFractionDigits(2);
        for (int i=0; i<DATA.length; i+=2) {
            String s = fmt.format((Double) DATA[i]);
            if (!s.equals(DATA[i+1])) {
                errln("FAIL: Got " + s + ", exp " + DATA[i+1]);
            }
        }
    }

    /**
     * 4243011: Formatting .5 rounds to "1" instead of "0"
     */
    public void Test4243011() {
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        double DATA[] = {0.5, 1.5, 2.5, 3.5, 4.5};
        String EXPECTED[] = {"0.", "2.", "2.", "4.", "4."};

        DecimalFormat format = new DecimalFormat("0.");
        for (int i = 0; i < DATA.length; i++) {
            String result = format.format(DATA[i]);
            if (result.equals(EXPECTED[i])) {
                logln("OK: got " + result);
            } else {
                errln("FAIL: got " + result);
            }
        }
        Locale.setDefault(savedLocale);
    }

    /**
     * 4243108: format(0.0) gives "0.1" if preceded by parse("99.99")
     */
    public void Test4243108() {
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        DecimalFormat f = new DecimalFormat("#.#");
        String result = f.format(0.0);
        if (result.equals("0")) {
            logln("OK: got " + result);
        } else {
            errln("FAIL: got " + result);
        }
        try {
            double dResult = f.parse("99.99").doubleValue();
            if (dResult == 99.99) {
                logln("OK: got " + dResult);
            } else {
                errln("FAIL: got " + dResult);
            }
        } catch (ParseException e) {
            errln("Caught a ParseException:");
            e.printStackTrace();
        }
        result = f.format(0.0);
        if (result.equals("0")) {
            logln("OK: got " + result);
        } else {
            errln("FAIL: got " + result);
        }
        Locale.setDefault(savedLocale);
    }

    /**
     * 4330377: DecimalFormat engineering notation gives incorrect results
     */
    public void test4330377() {
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        double[] input = {5000.0, 500.0, 50.0, 5.0, 0.5, 0.05, 0.005, 0.0005,
               5050.0, 505.0, 50.5, 5.05, 0.505, 0.0505, 0.00505, 0.000505};
        String[] pattern = {"000.#E0", "##0.#E0", "#00.#E0"};
        String[][] expected = {
            // it's questionable whether "#00.#E0" should result in post-decimal
            // zeroes, i.e., whether "5.0E3", "5.0E0", "5.0E-3" are really good
            {"500E1", "5E3", "5.0E3"},
            {"500E0", "500E0", "500E0"},
            {"500E-1", "50E0", "50E0"},
            {"500E-2", "5E0", "5.0E0"},
            {"500E-3", "500E-3", "500E-3"},
            {"500E-4", "50E-3", "50E-3"},
            {"500E-5", "5E-3", "5.0E-3"},
            {"500E-6", "500E-6", "500E-6"},
            {"505E1", "5.05E3", "5.05E3"},
            {"505E0", "505E0", "505E0"},
            {"505E-1", "50.5E0", "50.5E0"},
            {"505E-2", "5.05E0", "5.05E0"},
            {"505E-3", "505E-3", "505E-3"},
            {"505E-4", "50.5E-3", "50.5E-3"},
            {"505E-5", "5.05E-3", "5.05E-3"},
            {"505E-6", "505E-6", "505E-6"}
        };
        for (int i = 0; i < input.length; i++) {
            for (int j = 0; j < pattern.length; j++) {
                DecimalFormat format = new DecimalFormat(pattern[j]);
                String result = format.format(input[i]);
                if (!result.equals(expected[i][j])) {
                    errln("FAIL: input: " + input[i] +
                            ", pattern: " + pattern[j] +
                            ", expected: " + expected[i][j] +
                            ", got: " + result);
                }
            }
        }
        Locale.setDefault(savedLocale);
    }

    /**
     * 4233840: NumberFormat does not round correctly
     */
    public void test4233840() {
        float f = 0.0099f;

        NumberFormat nf = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.US));
        nf.setMinimumFractionDigits(2);

        String result = nf.format(f);

        if (!result.equals("0.01")) {
            errln("FAIL: input: " + f + ", expected: 0.01, got: " + result);
        }
    }

    /**
     * 4241880: Decimal format doesnt round a double properly when the number is less than 1
     */
    public void test4241880() {
        Locale savedLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
        double[] input = {
                .019, .009, .015, .016, .014,
                .004, .005, .006, .007, .008,
                .5, 1.5, .25, .55, .045,
                .035, .0005, .0015,
        };
        String[] pattern = {
                "##0%", "##0%", "##0%", "##0%", "##0%",
                "##0%", "##0%", "##0%", "##0%", "##0%",
                "#,##0", "#,##0", "#,##0.0", "#,##0.0", "#,##0.00",
                "#,##0.00", "#,##0.000", "#,##0.000",
        };
        String[] expected = {
                "2%", "1%", "2%", "2%", "1%",
                "0%", "0%", "1%", "1%", "1%",
                "0", "2", "0.2", "0.6", "0.04",
                "0.04", "0.000", "0.002",
        };
        for (int i = 0; i < input.length; i++) {
            DecimalFormat format = new DecimalFormat(pattern[i]);
            String result = format.format(input[i]);
            if (!result.equals(expected[i])) {
                errln("FAIL: input: " + input[i] +
                        ", pattern: " + pattern[i] +
                        ", expected: " + expected[i] +
                        ", got: " + result);
            }
        }
        Locale.setDefault(savedLocale);
    }

    /**
     * Test for get/setMonetaryGroupingSeparator() methods.
     * @since 15
     */
    public void test8227313() throws ParseException {
        var nrmSep = 'n';
        var monSep = 'm';
        var curSym = "Cur";
        var inputNum = 10;
        var nrmPattern = ",#";
        var monPattern = "\u00a4 ,#";
        var expectedNrmFmt = "1n0";
        var expectedMonFmt = "Cur 1m0";

        var ndfs = DecimalFormatSymbols.getInstance();
        ndfs.setGroupingSeparator(nrmSep);
        var nf = new DecimalFormat(nrmPattern, ndfs);
        var mdfs = DecimalFormatSymbols.getInstance();
        mdfs.setMonetaryGroupingSeparator(monSep);
        mdfs.setCurrencySymbol(curSym);
        var mf = new DecimalFormat(monPattern, mdfs);

        // get test
        char gotNrmSep = mdfs.getGroupingSeparator();
        char gotMonSep = mdfs.getMonetaryGroupingSeparator();
        if (gotMonSep != monSep) {
            errln("FAIL: getMonetaryGroupingSeparator() returned incorrect value. expected: "
                    + monSep + ", got: " + gotMonSep);
        }
        if (gotMonSep == gotNrmSep) {
            errln("FAIL: getMonetaryGroupingSeparator() returned the same value with " +
                    "getGroupingSeparator(): monetary sep: " + gotMonSep +
                    ", normal sep: " + gotNrmSep);
        }

        // format test
        var formatted = mf.format(inputNum);
        if (!formatted.equals(expectedMonFmt)) {
            errln("FAIL: format failed. expected: " + expectedMonFmt +
                    ", got: " + formatted);
        }
        formatted = nf.format(inputNum);
        if (!formatted.equals(expectedNrmFmt)) {
            errln("FAIL: normal format failed. expected: " + expectedNrmFmt +
                    ", got: " + formatted);
        }

        // parse test
        Number parsed = mf.parse(expectedMonFmt);
        if (parsed.intValue() != inputNum) {
            errln("FAIL: parse failed. expected: " + inputNum +
                    ", got: " + parsed);
        }
        parsed = nf.parse(expectedNrmFmt);
        if (parsed.intValue() != inputNum) {
            errln("FAIL: normal parse failed. expected: " + inputNum +
                    ", got: " + parsed);
        }
    }
}

@SuppressWarnings("serial")
class myformat implements Serializable
{
    DateFormat _dateFormat = DateFormat.getDateInstance();

    public String Now()
    {
        GregorianCalendar calendar = new GregorianCalendar();
        Date t = calendar.getTime();
        String nowStr = _dateFormat.format(t);
        return nowStr;
    }
}

@SuppressWarnings("serial")
class MyNumberFormatTest extends NumberFormat {
    public StringBuffer format(double number, StringBuffer toAppendTo, FieldPosition pos) {
        return new StringBuffer("");
    }
    public StringBuffer format(long number,StringBuffer toAppendTo, FieldPosition pos) {
        return new StringBuffer("");
    }
    public Number parse(String text, ParsePosition parsePosition) {
        return 0;
    }
}
