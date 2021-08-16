/*
 * Copyright (c) 2003, 2016, Oracle and/or its affiliates. All rights reserved.
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





public class BasicBoolean extends Basic {

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
