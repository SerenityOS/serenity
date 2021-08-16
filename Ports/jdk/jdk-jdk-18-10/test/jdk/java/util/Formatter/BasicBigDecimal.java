/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

/* Type-specific source code for unit test
 *
 * Regenerate the BasicX classes via genBasic.sh whenever this file changes.
 * We check in the generated source files so that the test tree can be used
 * independently of the rest of the source tree.
 */

// -- This file was mechanically generated: Do not edit! -- //

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DateFormatSymbols;
import java.util.*;

import static java.util.Calendar.*;





public class BasicBigDecimal extends Basic {

    private static void test(String fs, String exp, Object ... args) {
        Formatter f = new Formatter(new StringBuilder(), Locale.US);
        f.format(fs, args);
        ck(fs, exp, f.toString());

        f = new Formatter(new StringBuilder(), Locale.US);
        f.format("foo " + fs + " bar", args);
        ck(fs, "foo " + exp + " bar", f.toString());
    }

    private static void test(Locale l, String fs, String exp, Object ... args)
    {
        Formatter f = new Formatter(new StringBuilder(), l);
        f.format(fs, args);
        ck(fs, exp, f.toString());

        f = new Formatter(new StringBuilder(), l);
        f.format("foo " + fs + " bar", args);
        ck(fs, "foo " + exp + " bar", f.toString());
    }

    private static void test(String fs, Object ... args) {
        Formatter f = new Formatter(new StringBuilder(), Locale.US);
        f.format(fs, args);
        ck(fs, "fail", f.toString());
    }

    private static void test(String fs) {
        Formatter f = new Formatter(new StringBuilder(), Locale.US);
        f.format(fs, "fail");
        ck(fs, "fail", f.toString());
    }

    private static void testSysOut(String fs, String exp, Object ... args) {
        FileOutputStream fos = null;
        FileInputStream fis = null;
        try {
            PrintStream saveOut = System.out;
            fos = new FileOutputStream("testSysOut");
            System.setOut(new PrintStream(fos));
            System.out.format(Locale.US, fs, args);
            fos.close();

            fis = new FileInputStream("testSysOut");
            byte [] ba = new byte[exp.length()];
            int len = fis.read(ba);
            String got = new String(ba);
            if (len != ba.length)
                fail(fs, exp, got);
            ck(fs, exp, got);

            System.setOut(saveOut);
        } catch (FileNotFoundException ex) {
            fail(fs, ex.getClass());
        } catch (IOException ex) {
            fail(fs, ex.getClass());
        } finally {
            try {
                if (fos != null)
                    fos.close();
                if (fis != null)
                    fis.close();
            } catch (IOException ex) {
                fail(fs, ex.getClass());
            }
        }
    }

    private static void tryCatch(String fs, Class<?> ex) {
        boolean caught = false;
        try {
            test(fs);
        } catch (Throwable x) {
            if (ex.isAssignableFrom(x.getClass()))
                caught = true;
        }
        if (!caught)
            fail(fs, ex);
        else
            pass();
    }

    private static void tryCatch(String fs, Class<?> ex, Object ... args) {
        boolean caught = false;
        try {
            test(fs, args);
        } catch (Throwable x) {
            if (ex.isAssignableFrom(x.getClass()))
                caught = true;
        }
        if (!caught)
            fail(fs, ex);
        else
            pass();
    }














































































































    private static BigDecimal create(double v) {
        return new BigDecimal(v);
    }

    private static BigDecimal negate(BigDecimal v) {
        return v.negate();
    }

    private static BigDecimal mult(BigDecimal v, double mul) {
        return v.multiply(new BigDecimal(mul));
    }

    private static BigDecimal recip(BigDecimal v) {
        return BigDecimal.ONE.divide(v);
    }







































































    public static void test() {
        TimeZone.setDefault(TimeZone.getTimeZone("GMT-0800"));

        // Any characters not explicitly defined as conversions, date/time
        // conversion suffixes, or flags are illegal and are reserved for
        // future extensions.  Use of such a character in a format string will
        // cause an UnknownFormatConversionException or
        // UnknownFormatFlagsException to be thrown.
        tryCatch("%q", UnknownFormatConversionException.class);
        tryCatch("%t&", UnknownFormatConversionException.class);
        tryCatch("%&d", UnknownFormatConversionException.class);
        tryCatch("%^b", UnknownFormatConversionException.class);

        //---------------------------------------------------------------------
        // Formatter.java class javadoc examples
        //---------------------------------------------------------------------
        test(Locale.FRANCE, "e = %+10.4f", "e =    +2,7183", Math.E);
        test("%4$2s %3$2s %2$2s %1$2s", " d  c  b  a", "a", "b", "c", "d");
        test("Amount gained or lost since last statement: $ %,(.2f",
             "Amount gained or lost since last statement: $ (6,217.58)",
             (new BigDecimal("-6217.58")));
        Calendar c = new GregorianCalendar(1969, JULY, 20, 16, 17, 0);
        testSysOut("Local time: %tT", "Local time: 16:17:00", c);

        test("Unable to open file '%1$s': %2$s",
             "Unable to open file 'food': No such file or directory",
             "food", "No such file or directory");
        Calendar duke = new GregorianCalendar(1995, MAY, 23, 19, 48, 34);
        duke.set(Calendar.MILLISECOND, 584);
        test("Duke's Birthday: %1$tB %1$te, %1$tY",
             "Duke's Birthday: May 23, 1995",
             duke);
        test("Duke's Birthday: %1$tB %1$te, %1$tY",
             "Duke's Birthday: May 23, 1995",
             duke.getTime());
        test("Duke's Birthday: %1$tB %1$te, %1$tY",
             "Duke's Birthday: May 23, 1995",
             duke.getTimeInMillis());

        test("%4$s %3$s %2$s %1$s %4$s %3$s %2$s %1$s",
             "d c b a d c b a", "a", "b", "c", "d");
        test("%s %s %<s %<s", "a b b b", "a", "b", "c", "d");
        test("%s %s %s %s", "a b c d", "a", "b", "c", "d");
        test("%2$s %s %<s %s", "b a a b", "a", "b", "c", "d");

        //---------------------------------------------------------------------
        // %b
        //
        // General conversion applicable to any argument.
        //---------------------------------------------------------------------
        test("%b", "true", true);
        test("%b", "false", false);
        test("%B", "TRUE", true);
        test("%B", "FALSE", false);
        test("%b", "true", Boolean.TRUE);
        test("%b", "false", Boolean.FALSE);
        test("%B", "TRUE", Boolean.TRUE);
        test("%B", "FALSE", Boolean.FALSE);
        test("%14b", "          true", true);
        test("%-14b", "true          ", true);
        test("%5.1b", "    f", false);
        test("%-5.1b", "f    ", false);

        test("%b", "true", "foo");
        test("%b", "false", (Object)null);

        // Boolean.java hardcodes the Strings for "true" and "false", so no
        // localization is possible.
        test(Locale.FRANCE, "%b", "true", true);
        test(Locale.FRANCE, "%b", "false", false);

        // If you pass in a single array to a varargs method, the compiler
        // uses it as the array of arguments rather than treating it as a
        // single array-type argument.
        test("%b", "false", (Object[])new String[2]);
        test("%b", "true", new String[2], new String[2]);

        int [] ia = { 1, 2, 3 };
        test("%b", "true", ia);

        //---------------------------------------------------------------------
        // %b - errors
        //---------------------------------------------------------------------
        tryCatch("%#b", FormatFlagsConversionMismatchException.class);
        tryCatch("%-b", MissingFormatWidthException.class);
        // correct or side-effect of implementation?
        tryCatch("%.b", UnknownFormatConversionException.class);
        tryCatch("%,b", FormatFlagsConversionMismatchException.class);

        //---------------------------------------------------------------------
        // %c
        //
        // General conversion applicable to any argument.
        //---------------------------------------------------------------------
        test("%c", "i", 'i');
        test("%C", "I", 'i');
        test("%4c",  "   i", 'i');
        test("%-4c", "i   ", 'i');
        test("%4C",  "   I", 'i');
        test("%-4C", "I   ", 'i');
        test("%c", "i", new Character('i'));
        test("%c", "H", (byte) 72);
        test("%c", "i", (short) 105);
        test("%c", "!", (int) 33);
        test("%c", "\u007F", Byte.MAX_VALUE);
        test("%c", new String(Character.toChars(Short.MAX_VALUE)),
             Short.MAX_VALUE);
        test("%c", "null", (Object) null);

        //---------------------------------------------------------------------
        // %c - errors
        //---------------------------------------------------------------------
        tryCatch("%c", IllegalFormatConversionException.class,
                 Boolean.TRUE);
        tryCatch("%c", IllegalFormatConversionException.class,
                 (float) 0.1);
        tryCatch("%c", IllegalFormatConversionException.class,
                 new Object());
        tryCatch("%c", IllegalFormatCodePointException.class,
                 Byte.MIN_VALUE);
        tryCatch("%c", IllegalFormatCodePointException.class,
                 Short.MIN_VALUE);
        tryCatch("%c", IllegalFormatCodePointException.class,
                 Integer.MIN_VALUE);
        tryCatch("%c", IllegalFormatCodePointException.class,
                 Integer.MAX_VALUE);

        tryCatch("%#c", FormatFlagsConversionMismatchException.class);
        tryCatch("%,c", FormatFlagsConversionMismatchException.class);
        tryCatch("%(c", FormatFlagsConversionMismatchException.class);
        tryCatch("%$c", UnknownFormatConversionException.class);
        tryCatch("%.2c", IllegalFormatPrecisionException.class);

        //---------------------------------------------------------------------
        // %s
        //
        // General conversion applicable to any argument.
        //---------------------------------------------------------------------
        test("%s", "Hello, Duke", "Hello, Duke");
        test("%S", "HELLO, DUKE", "Hello, Duke");
        test("%20S", "         HELLO, DUKE", "Hello, Duke");
        test("%20s", "         Hello, Duke", "Hello, Duke");
        test("%-20s", "Hello, Duke         ", "Hello, Duke");
        test("%-20.5s", "Hello               ", "Hello, Duke");
        test("%s", "null", (Object)null);

        StringBuffer sb = new StringBuffer("foo bar");
        test("%s", sb.toString(), sb);
        test("%S", sb.toString().toUpperCase(), sb);

        //---------------------------------------------------------------------
        // %s - errors
        //---------------------------------------------------------------------
        tryCatch("%-s", MissingFormatWidthException.class);
        tryCatch("%--s", DuplicateFormatFlagsException.class);
        tryCatch("%#s", FormatFlagsConversionMismatchException.class, 0);
        tryCatch("%#s", FormatFlagsConversionMismatchException.class, 0.5f);
        tryCatch("%#s", FormatFlagsConversionMismatchException.class, "hello");
        tryCatch("%#s", FormatFlagsConversionMismatchException.class, null);

        //---------------------------------------------------------------------
        // %h
        //
        // General conversion applicable to any argument.
        //---------------------------------------------------------------------
        test("%h", Integer.toHexString("Hello, Duke".hashCode()),
             "Hello, Duke");
        test("%10h", "  ddf63471", "Hello, Duke");
        test("%-10h", "ddf63471  ", "Hello, Duke");
        test("%-10H", "DDF63471  ", "Hello, Duke");
        test("%10h", "  402e0000", 15.0);
        test("%10H", "  402E0000", 15.0);

        //---------------------------------------------------------------------
        // %h - errors
        //---------------------------------------------------------------------
        tryCatch("%#h", FormatFlagsConversionMismatchException.class);

        //---------------------------------------------------------------------
        // flag/conversion errors
        //---------------------------------------------------------------------
        tryCatch("%F", UnknownFormatConversionException.class);

        tryCatch("%#g", FormatFlagsConversionMismatchException.class);























































































































































































































































































































































        //---------------------------------------------------------------------
        // %s - BigDecimal
        //---------------------------------------------------------------------
        BigDecimal one = BigDecimal.ONE;
        BigDecimal ten = BigDecimal.TEN;
        BigDecimal pi  = new BigDecimal(Math.PI);
        BigDecimal piToThe300 = pi.pow(300);

        test("%s", "3.141592653589793115997963468544185161590576171875", pi);










































        //---------------------------------------------------------------------
        // flag/conversion errors
        //---------------------------------------------------------------------
        tryCatch("%d", IllegalFormatConversionException.class, one);
        tryCatch("%,.4e", FormatFlagsConversionMismatchException.class, one);

        //---------------------------------------------------------------------
        // %e
        //
        // Floating-point conversions applicable to float, double, and
        // BigDecimal.
        //---------------------------------------------------------------------
        test("%e", "null", (Object)null);

        //---------------------------------------------------------------------
        // %e - float and double
        //---------------------------------------------------------------------
        // double PI = 3.141 592 653 589 793 238 46;
        test("%e", "3.141593e+00", pi);
        test("%.0e", "1e+01", ten);
        test("%#.0e", "1.e+01", ten);
        test("%E", "3.141593E+00", pi);
        test("%10.3e", " 3.142e+00", pi);
        test("%10.3e", "-3.142e+00", negate(pi));
        test("%010.3e", "03.142e+00", pi);
        test("%010.3e", "-3.142e+00", negate(pi));
        test("%-12.3e", "3.142e+00   ", pi);
        test("%-12.3e", "-3.142e+00  ", negate(pi));
        test("%.3e", "3.142e+00", pi);
        test("%.3e", "-3.142e+00", negate(pi));
        test("%.3e", "3.142e+06", mult(pi, 1000000.0));
        test("%.3e", "-3.142e+06", mult(pi, -1000000.0));

        test(Locale.FRANCE, "%e", "3,141593e+00", pi);

        // double PI^300
        //    = 13962455701329742638131355433930076081862072808 ... e+149

        //---------------------------------------------------------------------
        // %e - BigDecimal
        //---------------------------------------------------------------------
        test("%.3e", "1.396e+149", piToThe300);
        test("%.3e", "-1.396e+149", piToThe300.negate());
        test("%.3e", "1.000e-100", recip(ten.pow(100)));
        test("%.3e", "-1.000e-100", negate(recip(ten.pow(100))));

        test("%3.0e", "1e-06", new BigDecimal("0.000001"));
        test("%3.0e", "1e-05", new BigDecimal("0.00001"));
        test("%3.0e", "1e-04", new BigDecimal("0.0001"));
        test("%3.0e", "1e-03", new BigDecimal("0.001"));
        test("%3.0e", "1e-02", new BigDecimal("0.01"));
        test("%3.0e", "1e-01", new BigDecimal("0.1"));
        test("%3.0e", "9e-01", new BigDecimal("0.9"));
        test("%3.1e", "9.0e-01", new BigDecimal("0.9"));
        test("%3.0e", "1e+00", new BigDecimal("1.00"));
        test("%3.0e", "1e+01", new BigDecimal("10.00"));
        test("%3.0e", "1e+02", new BigDecimal("99.19"));
        test("%3.1e", "9.9e+01", new BigDecimal("99.19"));
        test("%3.0e", "1e+02", new BigDecimal("99.99"));
        test("%3.0e", "1e+02", new BigDecimal("100.00"));
        test("%#3.0e", "1.e+03",    new BigDecimal("1000.00"));
        test("%3.0e", "1e+04",     new BigDecimal("10000.00"));
        test("%3.0e", "1e+05",    new BigDecimal("100000.00"));
        test("%3.0e", "1e+06",   new BigDecimal("1000000.00"));
        test("%3.0e", "1e+07",  new BigDecimal("10000000.00"));
        test("%3.0e", "1e+08", new BigDecimal("100000000.00"));


        test("%10.3e", " 1.000e+00", one);
        test("%+.3e", "+3.142e+00", pi);
        test("%+.3e", "-3.142e+00", negate(pi));
        test("% .3e", " 3.142e+00", pi);
        test("% .3e", "-3.142e+00", negate(pi));
        test("%#.0e", "3.e+00", create(3.0));
        test("%#.0e", "-3.e+00", create(-3.0));
        test("%.0e", "3e+00", create(3.0));
        test("%.0e", "-3e+00", create(-3.0));

        test("%(.4e", "3.1416e+06", mult(pi, 1000000.0));
        test("%(.4e", "(3.1416e+06)", mult(pi, -1000000.0));

        //---------------------------------------------------------------------
        // %e - boundary problems
        //---------------------------------------------------------------------
        test("%3.0e", "1e-06", 0.000001);
        test("%3.0e", "1e-05", 0.00001);
        test("%3.0e", "1e-04", 0.0001);
        test("%3.0e", "1e-03", 0.001);
        test("%3.0e", "1e-02", 0.01);
        test("%3.0e", "1e-01", 0.1);
        test("%3.0e", "9e-01", 0.9);
        test("%3.1e", "9.0e-01", 0.9);
        test("%3.0e", "1e+00", 1.00);
        test("%3.0e", "1e+01", 10.00);
        test("%3.0e", "1e+02", 99.19);
        test("%3.1e", "9.9e+01", 99.19);
        test("%3.0e", "1e+02", 99.99);
        test("%3.0e", "1e+02", 100.00);
        test("%#3.0e", "1.e+03",     1000.00);
        test("%3.0e", "1e+04",     10000.00);
        test("%3.0e", "1e+05",    100000.00);
        test("%3.0e", "1e+06",   1000000.00);
        test("%3.0e", "1e+07",  10000000.00);
        test("%3.0e", "1e+08", 100000000.00);

        //---------------------------------------------------------------------
        // %f
        //
        // Floating-point conversions applicable to float, double, and
        // BigDecimal.
        //---------------------------------------------------------------------
        test("%f", "null", (Object)null);
        test("%f", "3.141593", pi);
        test(Locale.FRANCE, "%f", "3,141593", pi);
        test("%10.3f", "     3.142", pi);
        test("%10.3f", "    -3.142", negate(pi));
        test("%010.3f", "000003.142", pi);
        test("%010.3f", "-00003.142", negate(pi));
        test("%-10.3f", "3.142     ", pi);
        test("%-10.3f", "-3.142    ", negate(pi));
        test("%.3f", "3.142", pi);
        test("%.3f", "-3.142", negate(pi));
        test("%+.3f", "+3.142", pi);
        test("%+.3f", "-3.142", negate(pi));
        test("% .3f", " 3.142", pi);
        test("% .3f", "-3.142", negate(pi));
        test("%#.0f", "3.", create(3.0));
        test("%#.0f", "-3.", create(-3.0));
        test("%.0f", "3", create(3.0));
        test("%.0f", "-3", create(-3.0));
        test("%.3f", "10.000", ten);
        test("%.3f", "1.000", one);
        test("%10.3f", "     1.000", one);

        //---------------------------------------------------------------------
        // %f - boundary problems
        //---------------------------------------------------------------------
        test("%3.0f", "  0", 0.000001);
        test("%3.0f", "  0", 0.00001);
        test("%3.0f", "  0", 0.0001);
        test("%3.0f", "  0", 0.001);
        test("%3.0f", "  0", 0.01);
        test("%3.0f", "  0", 0.1);
        test("%3.0f", "  1", 0.9);
        test("%3.1f", "0.9", 0.9);
        test("%3.0f", "  1", 1.00);
        test("%3.0f", " 10", 10.00);
        test("%3.0f", " 99", 99.19);
        test("%3.1f", "99.2", 99.19);
        test("%3.0f", "100", 99.99);
        test("%3.0f", "100", 100.00);
        test("%#3.0f", "1000.",     1000.00);
        test("%3.0f", "10000",     10000.00);
        test("%3.0f", "100000",    100000.00);
        test("%3.0f", "1000000",   1000000.00);
        test("%3.0f", "10000000",  10000000.00);
        test("%3.0f", "100000000", 100000000.00);
        test("%10.0f", "   1000000",   1000000.00);
        test("%,10.0f", " 1,000,000",   1000000.00);
        test("%,10.1f", "1,000,000.0",   1000000.00);
        test("%,3.0f", "1,000,000",   1000000.00);
        test("%,3.0f", "10,000,000",  10000000.00);
        test("%,3.0f", "100,000,000", 100000000.00);
        test("%,3.0f", "10,000,000",  10000000.00);
        test("%,3.0f", "100,000,000", 100000000.00);

        //---------------------------------------------------------------------
        // %f - BigDecimal
        //---------------------------------------------------------------------
        test("%4.0f", "  99", new BigDecimal("99.19"));
        test("%4.1f", "99.2", new BigDecimal("99.19"));

        BigDecimal val = new BigDecimal("99.95");
        test("%4.0f", " 100", val);
        test("%#4.0f", "100.", val);
        test("%4.1f", "100.0", val);
        test("%4.2f", "99.95", val);
        test("%4.3f", "99.950", val);

        val = new BigDecimal(".99");
        test("%4.1f", " 1.0", val);
        test("%4.2f", "0.99", val);
        test("%4.3f", "0.990", val);

        // #6476425
        val = new BigDecimal("0.00001");
        test("%.0f", "0", val);
        test("%.1f", "0.0", val);
        test("%.2f", "0.00", val);
        test("%.3f", "0.000", val);
        test("%.4f", "0.0000", val);
        test("%.5f", "0.00001", val);

        val = new BigDecimal("1.00001");
        test("%.0f", "1", val);
        test("%.1f", "1.0", val);
        test("%.2f", "1.00", val);
        test("%.3f", "1.000", val);
        test("%.4f", "1.0000", val);
        test("%.5f", "1.00001", val);

        val = new BigDecimal("1.23456");
        test("%.0f", "1", val);
        test("%.1f", "1.2", val);
        test("%.2f", "1.23", val);
        test("%.3f", "1.235", val);
        test("%.4f", "1.2346", val);
        test("%.5f", "1.23456", val);
        test("%.6f", "1.234560", val);

        val = new BigDecimal("9.99999");
        test("%.0f", "10", val);
        test("%.1f", "10.0", val);
        test("%.2f", "10.00", val);
        test("%.3f", "10.000", val);
        test("%.4f", "10.0000", val);
        test("%.5f", "9.99999", val);
        test("%.6f", "9.999990", val);


        val = new BigDecimal("1.99999");
        test("%.0f", "2", val);
        test("%.1f", "2.0", val);
        test("%.2f", "2.00", val);
        test("%.3f", "2.000", val);
        test("%.4f", "2.0000", val);
        test("%.5f", "1.99999", val);
        test("%.6f", "1.999990", val);

        val = new BigDecimal(0.9996);
        test("%.0f", "1", val);
        test("%.1f", "1.0", val);
        test("%.2f", "1.00", val);
        test("%.3f", "1.000", val);
        test("%.4f", "0.9996", val);
        test("%.5f", "0.99960", val);
        test("%.6f", "0.999600", val);

        val = new BigDecimal(BigInteger.ZERO, 6);
        test("%.4f", "0.0000", val);
        val = new BigDecimal(BigInteger.ZERO, -6);
        test("%.4f", "0.0000", val);




















        //---------------------------------------------------------------------
        // %f - float, double, Double, BigDecimal
        //---------------------------------------------------------------------
        test("%.3f", "3141592.654", mult(pi, 1000000.0));
        test("%.3f", "-3141592.654", mult(pi, -1000000.0));
        test("%,.4f", "3,141,592.6536", mult(pi, 1000000.0));
        test(Locale.FRANCE, "%,.4f", "3\u202f141\u202f592,6536", mult(pi, 1000000.0));
        test("%,.4f", "-3,141,592.6536", mult(pi, -1000000.0));
        test("%(.4f", "3141592.6536", mult(pi, 1000000.0));
        test("%(.4f", "(3141592.6536)", mult(pi, -1000000.0));
        test("%(,.4f", "3,141,592.6536", mult(pi, 1000000.0));
        test("%(,.4f", "(3,141,592.6536)", mult(pi, -1000000.0));




        //---------------------------------------------------------------------
        // %g
        //
        // Floating-point conversions applicable to float, double, and
        // BigDecimal.
        //---------------------------------------------------------------------
        test("%g", "null", (Object)null);
        test("%g", "3.14159", pi);
        test(Locale.FRANCE, "%g", "3,14159", pi);
        test("%.0g", "1e+01", ten);
        test("%G", "3.14159", pi);
        test("%10.3g", "      3.14", pi);
        test("%10.3g", "     -3.14", negate(pi));
        test("%010.3g", "0000003.14", pi);
        test("%010.3g", "-000003.14", negate(pi));
        test("%-12.3g", "3.14        ", pi);
        test("%-12.3g", "-3.14       ", negate(pi));
        test("%.3g", "3.14", pi);
        test("%.3g", "-3.14", negate(pi));
        test("%.3g", "3.14e+08", mult(pi, 100000000.0));
        test("%.3g", "-3.14e+08", mult(pi, -100000000.0));

        test("%.3g", "1.00e-05", recip(create(100000.0)));
        test("%.3g", "-1.00e-05", recip(create(-100000.0)));
        test("%.0g", "-1e-05", recip(create(-100000.0)));
        test("%.0g", "1e+05", create(100000.0));
        test("%.3G", "1.00E-05", recip(create(100000.0)));
        test("%.3G", "-1.00E-05", recip(create(-100000.0)));

        test("%.1g", "-0", -0.0);
        test("%3.0g", " -0", -0.0);
        test("%.1g", "0", 0.0);
        test("%3.0g", "  0", 0.0);
        test("%.1g", "0", +0.0);
        test("%3.0g", "  0", +0.0);

        test("%3.0g", "1e-06", 0.000001);
        test("%3.0g", "1e-05", 0.00001);
        test("%3.0g", "1e-05", 0.0000099);
        test("%3.1g", "1e-05", 0.0000099);
        test("%3.2g", "9.9e-06", 0.0000099);
        test("%3.0g", "0.0001", 0.0001);
        test("%3.0g", "9e-05",  0.00009);
        test("%3.0g", "0.0001", 0.000099);
        test("%3.1g", "0.0001", 0.000099);
        test("%3.2g", "9.9e-05", 0.000099);
        test("%3.0g", "0.001", 0.001);
        test("%3.0g", "0.001", 0.00099);
        test("%3.1g", "0.001", 0.00099);
        test("%3.2g", "0.00099", 0.00099);
        test("%3.3g", "0.00100", 0.001);
        test("%3.4g", "0.001000", 0.001);
        test("%3.0g", "0.01", 0.01);
        test("%3.0g", "0.1", 0.1);
        test("%3.0g", "0.9", 0.9);
        test("%3.1g", "0.9", 0.9);
        test("%3.0g", "  1", 1.00);
        test("%3.2g", " 10", 10.00);
        test("%3.0g", "1e+01", 10.00);
        test("%3.0g", "1e+02", 99.19);
        test("%3.1g", "1e+02", 99.19);
        test("%3.2g", " 99", 99.19);
        test("%3.0g", "1e+02", 99.9);
        test("%3.1g", "1e+02", 99.9);
        test("%3.2g", "1.0e+02", 99.9);
        test("%3.0g", "1e+02", 99.99);
        test("%3.0g", "1e+02", 100.00);
        test("%3.0g", "1e+03", 999.9);
        test("%3.1g", "1e+03", 999.9);
        test("%3.2g", "1.0e+03", 999.9);
        test("%3.3g", "1.00e+03", 999.9);
        test("%3.4g", "999.9", 999.9);
        test("%3.4g", "1000", 999.99);
        test("%3.0g", "1e+03", 1000.00);
        test("%3.0g", "1e+04",     10000.00);
        test("%3.0g", "1e+05",    100000.00);
        test("%3.0g", "1e+06",   1000000.00);
        test("%3.0g", "1e+07",  10000000.00);
        test("%3.9g", "100000000",  100000000.00);
        test("%3.10g", "100000000.0", 100000000.00);

        tryCatch("%#3.0g", FormatFlagsConversionMismatchException.class, 1000.00);

        // double PI^300
        //    = 13962455701329742638131355433930076081862072808 ... e+149

        //---------------------------------------------------------------------
        // %g - BigDecimal
        //---------------------------------------------------------------------
        test("%.3g", "1.40e+149", piToThe300);
        test("%.3g", "-1.40e+149", piToThe300.negate());
        test(Locale.FRANCE, "%.3g", "-1,40e+149", piToThe300.negate());
        test("%.3g", "1.00e-100", recip(ten.pow(100)));
        test("%.3g", "-1.00e-100", negate(recip(ten.pow(100))));

        test("%3.0g", "1e-06", new BigDecimal("0.000001"));
        test("%3.0g", "1e-05", new BigDecimal("0.00001"));
        test("%3.0g", "0.0001", new BigDecimal("0.0001"));
        test("%3.0g", "0.001", new BigDecimal("0.001"));
        test("%3.3g", "0.00100", new BigDecimal("0.001"));
        test("%3.4g", "0.001000", new BigDecimal("0.001"));
        test("%3.0g", "0.01", new BigDecimal("0.01"));
        test("%3.0g", "0.1", new BigDecimal("0.1"));
        test("%3.0g", "0.9", new BigDecimal("0.9"));
        test("%3.1g", "0.9", new BigDecimal("0.9"));
        test("%3.0g", "  1", new BigDecimal("1.00"));
        test("%3.2g", " 10", new BigDecimal("10.00"));
        test("%3.0g", "1e+01", new BigDecimal("10.00"));
        test("%3.0g", "1e+02", new BigDecimal("99.19"));
        test("%3.1g", "1e+02", new BigDecimal("99.19"));
        test("%3.2g", " 99", new BigDecimal("99.19"));
        test("%3.0g", "1e+02", new BigDecimal("99.99"));
        test("%3.0g", "1e+02", new BigDecimal("100.00"));
        test("%3.0g", "1e+03", new BigDecimal("1000.00"));
        test("%3.0g", "1e+04",      new BigDecimal("10000.00"));
        test("%3.0g", "1e+05",      new BigDecimal("100000.00"));
        test("%3.0g", "1e+06",      new BigDecimal("1000000.00"));
        test("%3.0g", "1e+07",      new BigDecimal("10000000.00"));
        test("%3.9g", "100000000",  new BigDecimal("100000000.00"));
        test("%3.10g", "100000000.0", new BigDecimal("100000000.00"));


        test("%.3g", "10.0", ten);
        test("%.3g", "1.00", one);
        test("%10.3g", "      1.00", one);
        test("%+10.3g", "     +3.14", pi);
        test("%+10.3g", "     -3.14", negate(pi));
        test("% .3g", " 3.14", pi);
        test("% .3g", "-3.14", negate(pi));
        test("%.0g", "3", create(3.0));
        test("%.0g", "-3", create(-3.0));

        test("%(.4g", "3.142e+08", mult(pi, 100000000.0));
        test("%(.4g", "(3.142e+08)", mult(pi, -100000000.0));







        test("%,.11g", "3,141,592.6536", mult(pi, 1000000.0));
        test("%(,.11g", "(3,141,592.6536)", mult(pi, -1000000.0));

























































































        //---------------------------------------------------------------------
        // %f, %e, %g, %a - Boundaries
        //---------------------------------------------------------------------











































































































































































































































        //---------------------------------------------------------------------
        // %t
        //
        // Date/Time conversions applicable to Calendar, Date, and long.
        //---------------------------------------------------------------------
        test("%tA", "null", (Object)null);
        test("%TA", "NULL", (Object)null);

        //---------------------------------------------------------------------
        // %t - errors
        //---------------------------------------------------------------------
        tryCatch("%t", UnknownFormatConversionException.class);
        tryCatch("%T", UnknownFormatConversionException.class);
        tryCatch("%tP", UnknownFormatConversionException.class);
        tryCatch("%TP", UnknownFormatConversionException.class);
        tryCatch("%.5tB", IllegalFormatPrecisionException.class);
        tryCatch("%#tB", FormatFlagsConversionMismatchException.class);
        tryCatch("%-tB", MissingFormatWidthException.class);































































































        //---------------------------------------------------------------------
        // %n
        //---------------------------------------------------------------------
        test("%n", System.getProperty("line.separator"), (Object)null);
        test("%n", System.getProperty("line.separator"), "");

        tryCatch("%,n", IllegalFormatFlagsException.class);
        tryCatch("%.n", UnknownFormatConversionException.class);
        tryCatch("%5.n", UnknownFormatConversionException.class);
        tryCatch("%5n", IllegalFormatWidthException.class);
        tryCatch("%.7n", IllegalFormatPrecisionException.class);
        tryCatch("%<n", IllegalFormatFlagsException.class);

        //---------------------------------------------------------------------
        // %%
        //---------------------------------------------------------------------
        test("%%", "%", (Object)null);
        test("%%", "%", "");

        test("%5%", "    %", (Object)null);
        test("%5%", "    %", "");
        test("%-5%", "%    ", (Object)null);
        test("%-5%", "%    ", "");

        tryCatch("%.5%", IllegalFormatPrecisionException.class);
        tryCatch("%5.5%", IllegalFormatPrecisionException.class);

        tryCatch("%%%", UnknownFormatConversionException.class);
        // perhaps an IllegalFormatArgumentIndexException should be defined?
        tryCatch("%<%", IllegalFormatFlagsException.class);
    }
}
