/*
 * Copyright (c) 1998, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.text.*;
import java.util.*;
import java.io.*;

/**
 * @test
 * @bug 4029195 4052408 4056591 4059917 4060212 4061287 4065240 4071441 4073003
 * 4089106 4100302 4101483 4103340 4103341 4104136 4104522 4106807 4108407
 * 4134203 4138203 4148168 4151631 4151706 4153860 4162071 4182066 4209272 4210209
 * 4213086 4250359 4253490 4266432 4406615 4413980 8008577
 * @library /java/text/testlib
 * @run main/othervm -Djava.locale.providers=COMPAT,SPI DateFormatRegression
 */
public class DateFormatRegression extends IntlTest {

    public static void main(String[] args) throws Exception {
        new DateFormatRegression().run(args);
    }

    public void Test4029195() {
        @SuppressWarnings("deprecation")
        Date today = new Date();

        logln("today: " + today);

        SimpleDateFormat sdf = (SimpleDateFormat)SimpleDateFormat.getDateInstance();
        logln("pattern: " + sdf.toPattern());
        logln("today: " + sdf.format(today));

        sdf.applyPattern("G yyyy DDD");
        String todayS = sdf.format(today);
        logln("today: " + todayS);
        try {
            today = sdf.parse(todayS);
            logln("today date: " + today);
        } catch(Exception e) {
            logln("Error reparsing date: " + e.getMessage());
        }

        try {
            String rt = sdf.format(sdf.parse(todayS));
            logln("round trip: " + rt);
            if (!rt.equals(todayS)) errln("Fail: Want " + todayS + " Got " + rt);
        }
        catch (ParseException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }
    }

    public void Test4052408() {
        DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.SHORT,
                                                        DateFormat.SHORT, Locale.US);
        @SuppressWarnings("deprecation")
        Date date = new Date(97, Calendar.MAY, 3, 8, 55);
        String str;
        logln(str = fmt.format(date));

        if (!str.equals("5/3/97 8:55 AM"))
            errln("Fail: Test broken; Want 5/3/97 8:55 AM Got " + str);
        Map<Integer,String> expected = new HashMap<>();
        expected.put(DateFormat.MONTH_FIELD, "5");
        expected.put(DateFormat.DATE_FIELD, "3");
        expected.put(DateFormat.YEAR_FIELD, "97");
        expected.put(DateFormat.HOUR1_FIELD, "8");
        expected.put(DateFormat.MINUTE_FIELD, "55");
        expected.put(DateFormat.AM_PM_FIELD, "AM");

        StringBuffer buf = new StringBuffer();
        String fieldNames[] = {
            "ERA_FIELD",
            "YEAR_FIELD",
            "MONTH_FIELD",
            "DATE_FIELD",
            "HOUR_OF_DAY1_FIELD",
            "HOUR_OF_DAY0_FIELD",
            "MINUTE_FIELD",
            "SECOND_FIELD",
            "MILLISECOND_FIELD",
            "DAY_OF_WEEK_FIELD",
            "DAY_OF_YEAR_FIELD",
            "DAY_OF_WEEK_IN_MONTH_FIELD",
            "WEEK_OF_YEAR_FIELD",
            "WEEK_OF_MONTH_FIELD",
            "AM_PM_FIELD",
            "HOUR1_FIELD",
            "HOUR0_FIELD",
            "TIMEZONE_FIELD",
        };
        boolean pass = true;
        for (int i=0; i<=17; ++i) {
            FieldPosition pos = new FieldPosition(i);
            fmt.format(date, buf, pos);
            char[] dst = new char[pos.getEndIndex() - pos.getBeginIndex()];
            buf.getChars(pos.getBeginIndex(), pos.getEndIndex(), dst, 0);
            str = new String(dst);
            log(i + ": " + fieldNames[i] +
                             ", \"" + str + "\", " +
                             pos.getBeginIndex() + ", " +
                             pos.getEndIndex());
            String exp = expected.get(i);
            if ((exp == null && str.length() == 0) ||
                str.equals(exp))
                logln(" ok");
            else {
                logln(" expected " + exp);
                pass = false;
            }
        }
        if (!pass) errln("Fail: FieldPosition not set right by DateFormat");
    }

    /**
     * Verify the function of the [s|g]et2DigitYearStart() API.
     */
    @SuppressWarnings("deprecation")
    public void Test4056591() {
        try {
            SimpleDateFormat fmt = new SimpleDateFormat("yyMMdd", Locale.US);
            Date start = new Date(1809-1900, Calendar.DECEMBER, 25);
            fmt.set2DigitYearStart(start);
            if (!fmt.get2DigitYearStart().equals(start))
                errln("get2DigitYearStart broken");
            Object[] DATA = {
                "091225", new Date(1809-1900, Calendar.DECEMBER, 25),
                "091224", new Date(1909-1900, Calendar.DECEMBER, 24),
                "091226", new Date(1809-1900, Calendar.DECEMBER, 26),
                "611225", new Date(1861-1900, Calendar.DECEMBER, 25),
            };
            for (int i=0; i<DATA.length; i+=2) {
                String s = (String) DATA[i];
                Date exp = (Date) DATA[i+1];
                Date got = fmt.parse(s);
                logln(s + " -> " + got + "; exp " + exp);
                if (!got.equals(exp)) errln("set2DigitYearStart broken");
            }
        }
        catch (ParseException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }
    }

    public void Test4059917() {
        Locale locale = Locale.getDefault();
        if (!TestUtils.usesAsciiDigits(locale)) {
            logln("Skipping this test because locale is " + locale);
            return;
        }

        SimpleDateFormat fmt;
        String myDate;

        fmt = new SimpleDateFormat( "yyyy/MM/dd" );
        myDate = "1997/01/01";
        aux917( fmt, myDate );

        fmt = new SimpleDateFormat( "yyyyMMdd" );
        myDate = "19970101";
        aux917( fmt, myDate );
    }

    void aux917( SimpleDateFormat fmt, String str ) {
        try {
            logln( "==================" );
            logln( "testIt: pattern=" + fmt.toPattern() +
                   " string=" + str );

            Object o;
            o = fmt.parseObject( str );
            logln( "Parsed object: " + o );

            String formatted = fmt.format( o );
            logln( "Formatted string: " + formatted );
            if (!formatted.equals(str)) errln("Fail: Want " + str + " Got " + formatted);
        }
        catch (ParseException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }
    }

    public void Test4060212() {
      Locale savedLocale = Locale.getDefault();
      Locale.setDefault(Locale.US);
      try {
        String dateString = "1995-040.05:01:29";

        logln( "dateString= " + dateString );
        logln("Using yyyy-DDD.hh:mm:ss");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-DDD.hh:mm:ss");
        ParsePosition pos = new ParsePosition(0);
        Date myDate = formatter.parse( dateString, pos );
        String myString = DateFormat.getDateTimeInstance( DateFormat.FULL,
                                                          DateFormat.LONG).format( myDate );
        logln( myString );
        Calendar cal = new GregorianCalendar();
        cal.setTime(myDate);
        if (cal.get(Calendar.DAY_OF_YEAR) != 40)
            errln("Fail: Got " + cal.get(Calendar.DAY_OF_YEAR) +
                                " Want 40");

        logln("Using yyyy-ddd.hh:mm:ss");
        formatter = new SimpleDateFormat("yyyy-ddd.hh:mm:ss");
        pos = new ParsePosition(0);
        myDate = formatter.parse( dateString, pos );
        myString = DateFormat.getDateTimeInstance( DateFormat.FULL,
                                                   DateFormat.LONG).format( myDate );
        logln( myString );
        cal.setTime(myDate);
        if (cal.get(Calendar.DAY_OF_YEAR) != 40)
            errln("Fail: Got " + cal.get(Calendar.DAY_OF_YEAR) +
                                " Want 40");
      }
      finally {
         Locale.setDefault(savedLocale);
      }
    }

    public void Test4061287() {
        SimpleDateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        try {
            logln(df.parse("35/01/1971").toString());
        }
        catch (ParseException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }
        df.setLenient(false);
        boolean ok = false;
        try {
            logln(df.parse("35/01/1971").toString());
        } catch (ParseException e) {ok=true;}
        if (!ok) errln("Fail: Lenient not working");
    }

    @SuppressWarnings("deprecation")
    public void Test4065240() {
        Date curDate;
        DateFormat shortdate, fulldate;
        String strShortDate, strFullDate;
        Locale saveLocale = Locale.getDefault();
        TimeZone saveZone = TimeZone.getDefault();
        try {
            Locale curLocale = new Locale("de","DE");
            Locale.setDefault(curLocale);
            TimeZone.setDefault(TimeZone.getTimeZone("EST"));
            curDate = new Date(98, 0, 1);
            shortdate = DateFormat.getDateInstance(DateFormat.SHORT);
            fulldate = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG
                                                      );
            strShortDate = new String("The current date (short form) is " + shortdate.
                                      format(curDate));
            strFullDate = new String("The current date (long form) is " + fulldate.format(curDate));

            logln(strShortDate);
            logln(strFullDate);

            // UPDATE THIS AS ZONE NAME RESOURCE FOR <EST> in de_DE is updated
            if (!strFullDate.endsWith("EST")
                    && !strFullDate.endsWith("GMT-05:00")) {
                errln("Fail: Want GMT-05:00");
            }
        }
        finally {
            Locale.setDefault(saveLocale);
            TimeZone.setDefault(saveZone);
        }
    }

    /*
      DateFormat.equals is too narrowly defined.  As a result, MessageFormat
      does not work correctly.  DateFormat.equals needs to be written so
      that the Calendar sub-object is not compared using Calendar.equals,
      but rather compared for equivalency.  This may necessitate adding a
      (package private) method to Calendar to test for equivalency.

      Currently this bug breaks MessageFormat.toPattern
      */
    @SuppressWarnings("deprecation")
    public void Test4071441() {
        DateFormat fmtA = DateFormat.getInstance();
        DateFormat fmtB = DateFormat.getInstance();
        Calendar calA = fmtA.getCalendar();
        Calendar calB = fmtB.getCalendar();
        Date epoch = new Date(0);
        Date xmas = new Date(61, Calendar.DECEMBER, 25);
        calA.setTime(epoch);
        calB.setTime(epoch);
        if (!calA.equals(calB))
            errln("Fail: Can't complete test; Calendar instances unequal");
        if (!fmtA.equals(fmtB))
            errln("Fail: DateFormat unequal when Calendars equal");
        calB.setTime(xmas);
        if (calA.equals(calB))
            errln("Fail: Can't complete test; Calendar instances equal");
        if (!fmtA.equals(fmtB))
            errln("Fail: DateFormat unequal when Calendars equivalent");
        logln("DateFormat.equals ok");
    }

    /* The java.text.DateFormat.parse(String) method expects for the
      US locale a string formatted according to mm/dd/yy and parses it
      correctly.

      When given a string mm/dd/yyyy it only parses up to the first
      two y's, typically resulting in a date in the year 1919.

      Please extend the parsing method(s) to handle strings with
      four-digit year values (probably also applicable to various
      other locales.  */
    public void Test4073003() {
        try {
            DateFormat fmt = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);
            String[] tests = { "12/25/61", "12/25/1961", "4/3/2010", "4/3/10" };
            for (int i=0; i<tests.length; i+=2) {
                Date d = fmt.parse(tests[i]);
                Date dd = fmt.parse(tests[i+1]);
                String s = fmt.format(d);
                String ss = fmt.format(dd);
                if (!d.equals(dd))
                    errln("Fail: " + d + " != " + dd);
                if (!s.equals(ss))
                    errln("Fail: " + s + " != " + ss);
                logln("Ok: " + s + " " + d);
            }
        }
        catch (ParseException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }
    }

    public void Test4089106() {
        TimeZone def = TimeZone.getDefault();
        try {
            TimeZone z = new SimpleTimeZone((int)(1.25 * 3600000), "FAKEZONE");
            TimeZone.setDefault(z);
            SimpleDateFormat f = new SimpleDateFormat();
            if (!f.getTimeZone().equals(z))
                errln("Fail: SimpleTimeZone should use TimeZone.getDefault()");
        }
        finally {
            TimeZone.setDefault(def);
        }
    }

    public void Test4100302() {
        Locale[] locales = new Locale[] {
            Locale.CANADA,
            Locale.CANADA_FRENCH,
            Locale.CHINA,
            Locale.CHINESE,
            Locale.ENGLISH,
            Locale.FRANCE,
            Locale.FRENCH,
            Locale.GERMAN,
            Locale.GERMANY,
            Locale.ITALIAN,
            Locale.ITALY,
            Locale.JAPAN,
            Locale.JAPANESE,
            Locale.KOREA,
            Locale.KOREAN,
            Locale.PRC,
            Locale.SIMPLIFIED_CHINESE,
            Locale.TAIWAN,
            Locale.TRADITIONAL_CHINESE,
            Locale.UK,
            Locale.US
            };
        try {
            boolean pass = true;
            for(int i = 0; i < locales.length; i++) {

                Format format = DateFormat.getDateTimeInstance(DateFormat.FULL,
                                                               DateFormat.FULL, locales[i]);
                byte[] bytes;

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);

                oos.writeObject(format);
                oos.flush();

                baos.close();
                bytes = baos.toByteArray();

                ObjectInputStream ois =
                    new ObjectInputStream(new ByteArrayInputStream(bytes));

                if (!format.equals(ois.readObject())) {
                    pass = false;
                    logln("DateFormat instance for locale " +
                          locales[i] + " is incorrectly serialized/deserialized.");
                } else {
                    logln("DateFormat instance for locale " +
                          locales[i] + " is OKAY.");
                }
            }
            if (!pass) errln("Fail: DateFormat serialization/equality bug");
        }
        catch (IOException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }
    }

    /**
     * Test whether DataFormat can be serialized/deserialized correctly
     * even if invalid/customized TimeZone is used.
     */
    public void Test4413980() {
        TimeZone savedTimeZone = TimeZone.getDefault();
        try {
            boolean pass = true;
            String[] IDs = new String[] {"Undefined", "PST", "US/Pacific",
                                         "GMT+3:00", "GMT-01:30"};
            for (int i = 0; i < IDs.length; i++) {
                TimeZone tz = TimeZone.getTimeZone(IDs[i]);
                TimeZone.setDefault(tz);

                Format format = DateFormat.getDateTimeInstance(DateFormat.FULL,
                                                               DateFormat.FULL);
                byte[] bytes;

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(baos);

                oos.writeObject(format);
                oos.flush();

                baos.close();
                bytes = baos.toByteArray();

                ObjectInputStream ois =
                    new ObjectInputStream(new ByteArrayInputStream(bytes));

                if (!format.equals(ois.readObject())) {
                    pass = false;
                    logln("DateFormat instance which uses TimeZone <" +
                          IDs[i] + "> is incorrectly serialized/deserialized.");
                } else {
                    logln("DateFormat instance which uses TimeZone <" +
                          IDs[i] + "> is correctly serialized/deserialized.");
                }
            }
            if (!pass) {
                errln("Fail: DateFormat serialization/equality bug");
            }
        }
        catch (IOException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            errln("Fail: " + e);
            e.printStackTrace();
        }
        finally {
            TimeZone.setDefault(savedTimeZone);
        }
    }

    public void Test4101483() {
        SimpleDateFormat sdf = new SimpleDateFormat("z", Locale.US);
        FieldPosition fp = new FieldPosition(DateFormat.TIMEZONE_FIELD);
        @SuppressWarnings("deprecation")
        Date d= new Date(9234567890L);
        StringBuffer buf = new StringBuffer("");
        logln(sdf.format(d, buf, fp).toString());
        logln(d + " => " + buf);
        logln("beginIndex = " + fp.getBeginIndex());
        logln("endIndex = " + fp.getEndIndex());
        if (fp.getBeginIndex() == fp.getEndIndex()) errln("Fail: Empty field");
    }

    /**
     * Bug 4103340
     * Bug 4138203
     * This bug really only works in Locale.US, since that's what the locale
     * used for Date.toString() is.  Bug 4138203 reports that it fails on Korean
     * NT; it would actually have failed on any non-US locale.  Now it should
     * work on all locales.
     */
    public void Test4103340() {
        // choose a date that is the FIRST of some month
        // and some arbitrary time
        @SuppressWarnings("deprecation")
        Date d=new Date(97, 3, 1, 1, 1, 1);
        SimpleDateFormat df=new SimpleDateFormat("MMMM", Locale.US);

        String s = d.toString();
        String s2 = df.format(d);
        logln("Date="+s);
        logln("DF="+s2);
        if (s.indexOf(s2.substring(0,2)) == -1)
            errln("Months should match");
    }

    public void Test4103341() {
        TimeZone saveZone  =TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("CST"));
            SimpleDateFormat simple = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            if (!simple.getTimeZone().equals(TimeZone.getDefault()))
                errln("Fail: SimpleDateFormat not using default zone");
        }
        finally {
            TimeZone.setDefault(saveZone);
        }
    }

    public void Test4104136() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        String pattern = "'time' hh:mm";
        sdf.applyPattern(pattern);
        logln("pattern: \"" + pattern + "\"");

        @SuppressWarnings("deprecation")
        Object[] DATA = {
            "time 10:30", new ParsePosition(10), new Date(70, Calendar.JANUARY, 1, 10, 30),
            "time 10:x", new ParsePosition(0), null,
            "time 10x", new ParsePosition(0), null,
        };
        for (int i=0; i<DATA.length; i+=3) {
            String text = (String) DATA[i];
            ParsePosition finish = (ParsePosition) DATA[i+1];
            Date exp = (Date) DATA[i+2];

            ParsePosition pos = new ParsePosition(0);
            Date d = sdf.parse(text, pos);
            logln(" text: \"" + text + "\"");
            logln(" index: " + pos.getIndex());
            logln(" result: " + d);
            if (pos.getIndex() != finish.getIndex())
                errln("Fail: Expected pos " + finish.getIndex());
            if (!((d == null && exp == null) ||
                  d.equals(exp)))
                errln("Fail: Expected result " + exp);
        }
    }

    /**
     * CANNOT REPRODUCE
     * According to the bug report, this test should throw a
     * StringIndexOutOfBoundsException during the second parse.  However,
     * this is not seen.
     */
    public void Test4104522() {
        SimpleDateFormat sdf = new SimpleDateFormat();
        String pattern = "'time' hh:mm";
        sdf.applyPattern(pattern);
        logln("pattern: \"" + pattern + "\"");

        // works correctly
        ParsePosition pp = new ParsePosition(0);
        String text = "time ";
        Date date = sdf.parse(text, pp);
        logln(" text: \"" + text + "\"" +
              " date: " + date);

        // works wrong
        pp = new ParsePosition(0);
        text = "time";
        date = sdf.parse(text, pp);
        logln(" text: \"" + text + "\"" +
              " date: " + date);
    }

    public void Test4106807() {
        Date date;
        DateFormat df = DateFormat.getDateTimeInstance();
        Object[] data = {
            new SimpleDateFormat("yyyyMMddHHmmss"),       "19980211140000",
            new SimpleDateFormat("yyyyMMddHHmmss'Z'"),    "19980211140000",
            new SimpleDateFormat("yyyyMMddHHmmss''"),     "19980211140000",
            new SimpleDateFormat("yyyyMMddHHmmss'a''a'"), "19980211140000a",
            new SimpleDateFormat("yyyyMMddHHmmss %"),     "19980211140000 ",
        };
        GregorianCalendar gc = new GregorianCalendar();
        TimeZone timeZone = TimeZone.getDefault();

        TimeZone gmt = (TimeZone)timeZone.clone();

        gmt.setRawOffset(0);

        for (int i=0; i<data.length; i+=2) {
            SimpleDateFormat format = (SimpleDateFormat) data[i];
            String dateString = (String) data[i+1];
            try {
                format.setTimeZone(gmt);
                date = format.parse(dateString);
                logln(df.format(date));
                gc.setTime(date);
                logln("" + gc.get(Calendar.ZONE_OFFSET));
                logln(format.format(date));
            }
            catch (ParseException e) {
                logln("No way Jose");
            }
        }
    }

    /**
     * SimpleDateFormat won't parse "GMT"
     */
    public void Test4134203() {
        String dateFormat = "MM/dd/yy HH:mm:ss zzz";
        SimpleDateFormat fmt = new SimpleDateFormat(dateFormat);
        ParsePosition p0 = new ParsePosition(0);
        Date d = fmt.parse("01/22/92 04:52:00 GMT", p0);
        logln(d.toString());
        // In the failure case an exception is thrown by parse();
        // if no exception is thrown, the test passes.
    }

    /**
     * Another format for GMT string parse
     */
    public void Test4266432() {
        String dateFormat = "MM/dd HH:mm:ss zzz yyyy";
        SimpleDateFormat fmt = new SimpleDateFormat(dateFormat);
        ParsePosition p0 = new ParsePosition(0);
        Date d = fmt.parse("01/22 04:52:00 GMT 1992", p0);
        logln(d.toString());
        // In the failure case an exception is thrown by parse();
        // if no exception is thrown, the test passes.
    }

    /**
     * Millisecond field is limited to 3 digits; also test general millisecond
     * handling.
     *
     * NOTE: Updated for fixed semantics as of Kestrel.  See
     * 4253490
     */
    public void Test4148168() throws ParseException {
        SimpleDateFormat fmt = new SimpleDateFormat("", Locale.US);
        int ms = 456;
        String[] PAT = { "S", "SS", "SSS", "SSSS", "SSSSS",
                         "SSSSSSSSSSSSSSSSSSSS" };
        String[] OUT = { "456", "456", "456", "0456", "00456",
                         "00000000000000000456" };
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(Calendar.MILLISECOND, ms);
        Date d = cal.getTime();
        for (int i=0; i<OUT.length; ++i) {
            fmt.applyPattern(PAT[i]);
            String str = fmt.format(d);
            if (!str.equals(OUT[i])) {
                errln("FAIL: " + ms + " ms x \"" + PAT[i] + "\" -> \"" +
                      str + "\", exp \"" + OUT[i] + '"');
            }
        }

        // Test parsing
        fmt.applyPattern("s.S");
        String[] IN = { "1.4", "1.04", "1.004", "1.45", "1.456",
                        "1.4567", "1.45678" };
        int[] MS = { 4, 4, 4, 45, 456, 567, 678 };
        for (int i=0; i<IN.length; ++i) {
            d = fmt.parse(IN[i]);
            cal.setTime(d);
            ms = cal.get(Calendar.MILLISECOND);
            if (ms != MS[i]) {
                errln("FAIL: parse(\"" + IN[i] + "\" x \"s.S\") -> " +
                      ms + " ms, exp " + MS[i] + " ms");
            }
        }
    }

    /**
     * SimpleDateFormat incorrect handling of 2 single quotes in format()
     */
    public void Test4151631() {
        String pattern = "'TO_DATE('''dd'-'MM'-'yyyy HH:mm:ss''' , ''DD-MM-YYYY HH:MI:SS'')'";
        logln("pattern=" + pattern);
        SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.US);
        @SuppressWarnings("deprecation")
        String result = format.format(new Date(1998-1900, Calendar.JUNE, 30, 13, 30, 0));
        if (!result.equals("TO_DATE('30-06-1998 13:30:00' , 'DD-MM-YYYY HH:MI:SS')")) {
            errln("Fail: result=" + result);
        }
        else {
            logln("Pass: result=" + result);
        }
    }

    /**
     * 'z' at end of date format throws index exception in SimpleDateFormat
     * CANNOT REPRODUCE THIS BUG ON 1.2FCS
     */
    @SuppressWarnings("deprecation")
    public void Test4151706() {
        SimpleDateFormat fmt =
            new SimpleDateFormat("EEEE, dd-MMM-yy HH:mm:ss z", Locale.US);
        try {
            Date d = fmt.parse("Thursday, 31-Dec-98 23:00:00 GMT");
            if (d.getTime() != Date.UTC(1998-1900, Calendar.DECEMBER, 31, 23, 0, 0))
                errln("Incorrect value: " + d);
        } catch (Exception e) {
            errln("Fail: " + e);
        }
    }

    /**
     * SimpleDateFormat fails to parse redundant data.
     * This is actually a bug down in GregorianCalendar, but it was reported
     * as follows...
     */
    public void Test4153860() throws ParseException {
      Locale savedLocale = Locale.getDefault();
      Locale.setDefault(Locale.US);
      try {
        SimpleDateFormat sf = (SimpleDateFormat)DateFormat.getDateTimeInstance();
        // Set the pattern
        sf.applyPattern("yyyy.MM-dd");
        // Try to create a Date for February 4th
        Date d1 = sf.parse("1998.02-04");
        // Set the pattern, this time to use the W value
        sf.applyPattern("yyyy.MM-dd W");
        // Try to create a Date for February 4th
        Date d2 = sf.parse("1998.02-04 1");
        if (!d1.equals(d2)) {
            errln("Parse failed, got " + d2 +
                  ", expected " + d1);
        }
      }
      finally {
        Locale.setDefault(savedLocale);
      }
    }

    /**
     * Confirm that "EST"(GMT-5:00) and "CST"(GMT-6:00) are used in US
     * as "EST" or "CST", not Australian "EST" and "CST".
     */
    @SuppressWarnings("deprecation")
    public void Test4406615() {
      Locale savedLocale = Locale.getDefault();
      TimeZone savedTimeZone = TimeZone.getDefault();
      Locale.setDefault(Locale.US);
      TimeZone.setDefault(TimeZone.getTimeZone("PST"));

      Date d1, d2;
      String dt = "Mon, 1 Jan 2001 00:00:00";
      SimpleDateFormat sdf =
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");

      try {
        d1 = sdf.parse(dt+" EST");
        d2 = sdf.parse(dt+" CST");

        if (d1.getYear() != (2000-1900) || d1.getMonth() != 11 ||
            d1.getDate() != 31 || d1.getHours() != 21 || d1.getMinutes() != 0 ||
            d2.getYear() != (2000-1900) || d2.getMonth() != 11 ||
            d2.getDate() != 31 || d2.getHours() != 22 || d2.getMinutes() != 0) {
            errln("Parse failed, d1 = " + d1 + ", d2 = " + d2);
        } else {
            logln("Parse passed");
        }
      }
      catch (Exception e) {
            errln("Parse failed, got Exception " + e);
      }
      finally {
        Locale.setDefault(savedLocale);
        TimeZone.setDefault(savedTimeZone);
      }
    }

    /**
     * Cannot reproduce this bug under 1.2 FCS -- it may be a convoluted duplicate
     * of some other bug that has been fixed.
     */
    public void Test4162071() {
        String dateString = "Thu, 30-Jul-1999 11:51:14 GMT";
        String format = "EEE', 'dd-MMM-yyyy HH:mm:ss z"; // RFC 822/1123
        SimpleDateFormat df = new
            SimpleDateFormat(format, Locale.US);

        try {
            Date x = df.parse(dateString);
            logln("Parse format \"" + format + "\" ok");
            logln(dateString + " -> " + df.format(x));
        } catch (Exception e) {
            errln("Parse format \"" + format + "\" failed.");
        }
    }

    /**
     * DateFormat shouldn't parse year "-1" as a two-digit year (e.g., "-1" -> 1999).
     */
    public void Test4182066() {
      Locale savedLocale = Locale.getDefault();
      Locale.setDefault(Locale.US);
      try {
        SimpleDateFormat fmt = new SimpleDateFormat("MM/dd/yy",
                                                    DateFormatSymbols.getInstance(Locale.US));
        SimpleDateFormat dispFmt = new SimpleDateFormat("MMM dd yyyy GG",
                                                        DateFormatSymbols.getInstance(Locale.US));
        /* We expect 2-digit year formats to put 2-digit years in the right
         * window.  Out of range years, that is, anything less than "00" or
         * greater than "99", are treated as literal years.  So "1/2/3456"
         * becomes 3456 AD.  Likewise, "1/2/-3" becomes -3 AD == 2 BC.
         */
        @SuppressWarnings("deprecation")
        Object[] DATA = {
            "02/29/00",   new Date(2000-1900, Calendar.FEBRUARY, 29),
            "01/23/01",   new Date(2001-1900, Calendar.JANUARY,  23),
            "04/05/-1",   new Date(  -1-1900, Calendar.APRIL,     5),
            "01/23/-9",   new Date(  -9-1900, Calendar.JANUARY,  23),
            "11/12/1314", new Date(1314-1900, Calendar.NOVEMBER, 12),
            "10/31/1",    new Date(   1-1900, Calendar.OCTOBER,  31),
            "09/12/+1",   null, // "+1" isn't recognized by US NumberFormat
            "09/12/001",  new Date(   1-1900, Calendar.SEPTEMBER,12),
        };
        StringBuffer out = new StringBuffer();
        boolean pass = true;
        for (int i=0; i<DATA.length; i+=2) {
            String str = (String) DATA[i];
            Date expected = (Date) DATA[i+1];
            Date actual;
            try {
                actual = fmt.parse(str);
            } catch (ParseException e) {
                actual = null;
            }
            String actStr = actual != null
                ? dispFmt.format(actual) : String.valueOf(actual);
            if (expected == actual
                || (expected != null && expected.equals(actual))) {
                out.append(str + " => " + actStr + "\n");
            } else {
                String expStr = expected != null
                    ? dispFmt.format(expected) : String.valueOf(expected);
                out.append("FAIL: " + str + " => " + actStr
                           + ", expected " + expStr + "\n");
                pass = false;
            }
        }
        if (pass) {
            log(out.toString());
        } else {
            err(out.toString());
        }
      }
      finally {
        Locale.setDefault(savedLocale);
      }
    }

    /**
     * Bug 4210209
     * Bug 4209272
     * DateFormat cannot parse Feb 29 2000 when setLenient(false)
     */
    public void Test4210209() {
        String pattern = "MMM d, yyyy";
        DateFormat fmt = new SimpleDateFormat(pattern,
                                              DateFormatSymbols.getInstance(Locale.US));
        fmt.getCalendar().setLenient(false);
        @SuppressWarnings("deprecation")
        Date d = new Date(2000-1900, Calendar.FEBRUARY, 29);
        String s = fmt.format(d);
        logln(d + " x " + pattern + " => " + s);
        ParsePosition pos = new ParsePosition(0);
        d = fmt.parse(s, pos);
        logln(d + " <= " + pattern + " x " + s);
        logln("Parse pos = " + pos);
        if (pos.getErrorIndex() != -1) {
            errln("FAIL");
        }

        // The underlying bug is in GregorianCalendar.  If the following lines
        // succeed, the bug is fixed.  If the bug isn't fixed, they will throw
        // an exception.
        GregorianCalendar cal = new GregorianCalendar();
        cal.clear();
        cal.setLenient(false);
        cal.set(2000, Calendar.FEBRUARY, 29); // This should work!
        logln(cal.getTime().toString());
    }

    /**
     * DateFormat.getDateTimeInstance() allows illegal parameters.
     */
    public void Test4213086() {
        int[] DATA = {
            // Style value, 0/1 for illegal/legal
            -99, 0,
             -1, 0,
              0, 1,
              1, 1,
              2, 1,
              3, 1,
              4, 0,
             99, 0,
        };
        String[] DESC = {
            "getDateTimeInstance(date)",
            "getDateTimeInstance(time)",
            "getDateInstance",
            "getTimeInstance",
        };
        String[] GOT = {
            "disallowed", "allowed", "<invalid>"
        };
        for (int i=0; i<DATA.length; i+=2) {
            int got = 2;
            for (int j=0; j<4; ++j) {
                Exception e = null;
                try {
                    DateFormat df;
                    switch (j) {
                    case 0:
                        df = DateFormat.getDateTimeInstance(DATA[i], 0);
                        break;
                    case 1:
                        df = DateFormat.getDateTimeInstance(0, DATA[i]);
                        break;
                    case 2:
                        df = DateFormat.getDateInstance(DATA[i]);
                        break;
                    case 3:
                        df = DateFormat.getTimeInstance(DATA[i]);
                        break;
                    }
                    got = 1;
                } catch (IllegalArgumentException iae) {
                    got = 0;
                } catch (Exception ex) {
                    e = ex;
                }
                if (got != DATA[i+1] || e != null) {
                    errln("FAIL: DateFormat." + DESC[j] + " style " + DATA[i] + " " +
                          (e != null ? e.toString() : GOT[got]));
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    public void Test4253490() throws ParseException {
        SimpleDateFormat fmt = new SimpleDateFormat("S", Locale.US);

        GregorianCalendar cal = new GregorianCalendar();

        int      FORMAT_MS  = 12;
        String[] FORMAT_PAT = {  "S", "SS", "SSS", "SSSS" };
        String[] FORMAT_TO  = { "12", "12", "012", "0012" };

        String   PARSE_PAT  = "S";
        String[] PARSE_STR  = { "1", "12", "125", "1250" };
        int[]    PARSE_TO   = {  1,   12,   125,   250   };
        String   PARSE_LPAT  = "SSSSS";

        // Test formatting.  We want to make sure all digits are output
        // and that they are zero-padded on the left if necessary.
        cal.setTime(new Date(0L));
        cal.set(Calendar.MILLISECOND, FORMAT_MS);
        Date d = cal.getTime();
        for (int i=0; i<FORMAT_PAT.length; ++i) {
            fmt.applyPattern(FORMAT_PAT[i]);
            String s = fmt.format(d);
            if (s.equals(FORMAT_TO[i])) {
                logln(String.valueOf(FORMAT_MS) + " ms f* \"" +
                      FORMAT_PAT[i] + "\" -> \"" + s + '"');
            } else {
                errln("FAIL: " + FORMAT_MS + " ms f* \"" +
                      FORMAT_PAT[i] + "\" -> \"" + s + "\", expect \"" +
                      FORMAT_TO[i] + '"');
            }
        }

        // Test parsing.  We want to make sure all digits are read.
        fmt.applyPattern(PARSE_PAT);
        for (int i=0; i<PARSE_STR.length; ++i) {
            cal.setTime(fmt.parse(PARSE_STR[i]));
            int ms = cal.get(Calendar.MILLISECOND);
            if (ms == PARSE_TO[i]) {
                logln("\"" + PARSE_STR[i] + "\" p* \"" +
                      PARSE_PAT + "\" -> " + ms + " ms");
            } else {
                errln("FAIL: \"" + PARSE_STR[i] + "\" p* \"" +
                      PARSE_PAT + "\" -> " + ms + " ms, expect " +
                      PARSE_TO[i] + " ms");
            }
        }

        // Test LONG parsing.  We want to make sure all digits are read.
        fmt.applyPattern(PARSE_LPAT);
        for (int i=0; i<PARSE_STR.length; ++i) {
            cal.setTime(fmt.parse(PARSE_STR[i]));
            int ms = cal.get(Calendar.MILLISECOND);
            if (ms == PARSE_TO[i]) {
                logln("\"" + PARSE_STR[i] + "\" p* \"" +
                      PARSE_LPAT + "\" -> " + ms + " ms");
            } else {
                errln("FAIL: \"" + PARSE_STR[i] + "\" p* \"" +
                      PARSE_LPAT + "\" -> " + ms + " ms, expect " +
                      PARSE_TO[i] + " ms");
            }
        }
    }

    /**
     * Bug in handling of time instance; introduces in fix for 4213086.
     */
    public void Test4250359() {
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT,
                                                   Locale.US);
        @SuppressWarnings("deprecation")
        Date d = new Date(1999-1900, Calendar.DECEMBER, 25,
                          1, 2, 3);
        String s = df.format(d);
        // If the bug is present, we see "1:02 AM 1:02 AM".
        // Look for more than one instance of "AM".
        int i = s.indexOf("AM");
        int j = s.indexOf("AM", i+1);
        if (i < 0 || j >= 0) {
            errln("FAIL: getTimeInstance().format(d) => \"" +
                  s + "\"");
        }
    }

    /**
     * Test whether SimpleDataFormat (DateFormatSymbols) can format/parse
     * non-localized time zones.
     */
    public void Test4261506() {
        Locale savedLocale = Locale.getDefault();
        TimeZone savedTimeZone = TimeZone.getDefault();
        Locale.setDefault(Locale.JAPAN);

        // XXX: Test assumes "PST" is not TimeZoneNames_ja. Need to
        // pick up another time zone when L10N is done to that file.
        TimeZone.setDefault(TimeZone.getTimeZone("PST"));
        SimpleDateFormat fmt = new SimpleDateFormat("yy/MM/dd hh:ss zzz", Locale.JAPAN);
        @SuppressWarnings("deprecation")
        String result = fmt.format(new Date(1999 - 1900, 0, 1));
        logln("format()=>" + result);
        if (!result.endsWith("PST")) {
            errln("FAIL: SimpleDataFormat.format() did not retrun PST");
        }

        Date d = null;
        try {
            d = fmt.parse("99/1/1 10:10 PST");
        } catch (ParseException e) {
            errln("FAIL: SimpleDataFormat.parse() could not parse PST");
        }

        result = fmt.format(d);
        logln("roundtrip:" + result);
        if (!result.equals("99/01/01 10:10 PST")) {
            errln("FAIL: SimpleDataFomat timezone roundtrip failed");
        }

        Locale.setDefault(savedLocale);
        TimeZone.setDefault(savedTimeZone);
    }

}

//eof
