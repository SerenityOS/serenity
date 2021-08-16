/*
 * Copyright (c) 1997, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4052223 4089987 4469904 4326988 4486735 8008577 8045998 8140571
 *      8216969
 * @summary test DateFormat and SimpleDateFormat.
 * @library /java/text/testlib
 * @modules jdk.localedata
 * @run main/othervm -Djava.locale.providers=COMPAT,SPI DateFormatTest
 */

import java.util.*;
import java.text.*;
import static java.util.GregorianCalendar.*;

public class DateFormatTest extends IntlTest
{
    public static void main(String[] args) throws Exception {
        Locale reservedLocale = Locale.getDefault();
        try {
            Locale.setDefault(Locale.US);
            new DateFormatTest().run(args);
        } finally {
            // restore the reserved locale
            Locale.setDefault(reservedLocale);
        }
    }

    // Test 4 digit year parsing with pattern "yy"
    @SuppressWarnings("deprecation")
    public void TestYearParsing()
    {
        String str = "7/Sep/2001";
        Date exp = new Date(2001-1900, SEPTEMBER, 7);
        String pat = "d/MMM/yy";
        SimpleDateFormat sdf = new SimpleDateFormat(pat, Locale.US);
        try {
            Date d = sdf.parse(str);
            logln(str + " parses with " + pat + " to " + d);
            if (d.getTime() != exp.getTime()) {
                errln("FAIL: Expected " + exp);
            }
        }
        catch (ParseException e) {
            errln(str + " parse fails with " + pat);
        }
    }

    // Test written by Wally Wedel and emailed to me.
    public void TestWallyWedel()
    {
        /*
         * Instantiate a TimeZone so we can get the ids.
         */
        TimeZone tz = new SimpleTimeZone(7,"");
        /*
         * Computational variables.
         */
        int offset, hours, minutes;
        /*
         * Instantiate a SimpleDateFormat set up to produce a full time
         zone name.
         */
        SimpleDateFormat sdf = new SimpleDateFormat("zzzz");
        /*
         * A String array for the time zone ids.
         */
        String[] ids = TimeZone.getAvailableIDs();
        /*
         * How many ids do we have?
         */
        logln("Time Zone IDs size: " + ids.length);
        /*
         * Column headings (sort of)
         */
        logln("Ordinal ID offset(h:m) name");
        /*
         * Loop through the tzs.
         */
        Date today = new Date();
        Calendar cal = Calendar.getInstance();
        for (int i = 0; i < ids.length; i++) {
            // logln(i + " " + ids[i]);
            TimeZone ttz = TimeZone.getTimeZone(ids[i]);
            // offset = ttz.getRawOffset();
            cal.setTimeZone(ttz);
            cal.setTime(today);
            offset = cal.get(Calendar.ZONE_OFFSET) + cal.get(Calendar.DST_OFFSET);
            // logln(i + " " + ids[i] + " offset " + offset);
            char sign = '+';
            if (offset < 0) { sign = '-'; offset = -offset; }
            hours = offset/3600000;
            minutes = (offset%3600000)/60000;
            String dstOffset = "" + sign + (hours < 10 ? "0" : "") +
                hours + ':' + (minutes < 10 ? "0" : "") + minutes;
            /*
             * Instantiate a date so we can display the time zone name.
             */
            sdf.setTimeZone(ttz);
            /*
             * Format the output.
             */
            StringBuffer tzS = new StringBuffer();
            sdf.format(today,tzS, new FieldPosition(0));
            String fmtOffset = tzS.toString();
            String fmtDstOffset = null;
            if (fmtOffset.startsWith("GMT"))
            {
                fmtDstOffset = fmtOffset.substring(3);
            }
            /*
             * Show our result.
             */
            boolean ok = fmtDstOffset == null || fmtDstOffset.equals(dstOffset);
            if (ok)
            {
                logln(i + " " + ids[i] + " " + dstOffset +
                      " " + fmtOffset +
                      (fmtDstOffset != null ? " ok" : " ?"));
            }
            else
            {
                errln(i + " " + ids[i] + " " + dstOffset +
                      " " + fmtOffset + " *** FAIL ***");
            }
        }
    }

    // Test equals
    public void TestEquals()
    {
        DateFormat fmtA = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.FULL);

        DateFormat fmtB = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.FULL);

        if (!fmtA.equals(fmtB)) {
            errln("FAIL");
        }
    }

    // Check out some specific parsing problem
    @SuppressWarnings("deprecation")
    public void TestTwoDigitYearDSTParse()
    {
        SimpleDateFormat fullFmt =
            new SimpleDateFormat("EEE MMM dd HH:mm:ss.SSS zzz yyyy G");

        //DateFormat fmt = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.FULL,
        //                                                Locale.ENGLISH);
        SimpleDateFormat fmt = new SimpleDateFormat("dd-MMM-yy h:mm:ss 'o''clock' a z",
                                                    Locale.ENGLISH);
        //Date date = new Date(2004-1900, Calendar.APRIL, 3, 2, 20, 47);
        //logln(fmt.format(date)); // This shows what the current locale format is
        //logln(((SimpleDateFormat)fmt).toPattern());
        TimeZone save = TimeZone.getDefault();
        TimeZone PST  = TimeZone.getTimeZone("PST");
        String s = "03-Apr-04 2:20:47 o'clock AM PST";
        int hour = 2;
        try {
            TimeZone.setDefault(PST);
            Date d = fmt.parse(s);
            logln(s + " P> " + fullFmt.format(d));
            if (d.getHours() != hour) {
                errln("FAIL: Should parse to hour " + hour);
            }
        }
        catch (ParseException e) { errln("FAIL: " + e.getMessage()); }
        finally {
            TimeZone.setDefault(save);
        }
    }

    static String escape(String s)
    {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<s.length(); ++i)
        {
            char c = s.charAt(i);
            if (c <= (char)0x7F) {
                buf.append(c);
            } else {
                buf.append("\\u");
                buf.append(Integer.toHexString((c & 0xF000) >> 12));
                buf.append(Integer.toHexString((c & 0x0F00) >> 8));
                buf.append(Integer.toHexString((c & 0x00F0) >> 4));
                buf.append(Integer.toHexString(c & 0x000F));
            }
        }
        return buf.toString();
    }

    // Test field position return values
    static String fieldNames[] = {
        "ERA_FIELD", "YEAR_FIELD", "MONTH_FIELD",
        "WEEK_OF_YEAR_FIELD", "WEEK_OF_MONTH_FIELD", "DATE_FIELD",
        "DAY_OF_YEAR_FIELD", "DAY_OF_WEEK_FIELD", "DAY_OF_WEEK_IN_MONTH_FIELD",
        "AM_PM_FIELD", "HOUR0_FIELD", "HOUR1_FIELD",
        "HOUR_OF_DAY0_FIELD", "HOUR_OF_DAY1_FIELD",
        "MINUTE_FIELD", "SECOND_FIELD",
        "MILLISECOND_FIELD", "TIMEZONE_FIELD",
    };
    static int fieldIDs[] = {
        DateFormat.ERA_FIELD, DateFormat.YEAR_FIELD, DateFormat.MONTH_FIELD,
        DateFormat.WEEK_OF_YEAR_FIELD, DateFormat.WEEK_OF_MONTH_FIELD, DateFormat.DATE_FIELD,
        DateFormat.DAY_OF_YEAR_FIELD, DateFormat.DAY_OF_WEEK_FIELD, DateFormat.DAY_OF_WEEK_IN_MONTH_FIELD,
        DateFormat.AM_PM_FIELD, DateFormat.HOUR0_FIELD, DateFormat.HOUR1_FIELD,
        DateFormat.HOUR_OF_DAY0_FIELD, DateFormat.HOUR_OF_DAY1_FIELD,
        DateFormat.MINUTE_FIELD, DateFormat.SECOND_FIELD,
        DateFormat.MILLISECOND_FIELD, DateFormat.TIMEZONE_FIELD,
    };

    /**
     * Bug 4089987
     */
    public void TestFieldPosition()
    {
        DateFormat[] dateFormats = {
            DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.FULL,
                                           Locale.US),

            DateFormat.getDateTimeInstance(DateFormat.FULL,DateFormat.FULL,Locale.FRANCE),
            new SimpleDateFormat("G, y, M, d, k, H, m, s, S, E, D, F, w, W, a, h, K, z"),
            new SimpleDateFormat("G, yy, M, d, k, H, m, s, S, E, D, F, w, W, a, h, K, z"),
            new SimpleDateFormat( "GGGG, yyyy, MMMM, dddd, kkkk, HHHH, mmmm, ssss, " +
                                  "SSSS, EEEE, DDDD, " +
                                  "FFFF, wwww, WWWW, aaaa, hhhh, KKKK, zzzz")
        };
        String[] expected =
        {
            "", "1997", "August", "", "", "13", "", "Wednesday", "",
            "PM", "", "2", "", "", "34", "12", "", "PDT",

            "", "1997", "ao\u00FBt", "", "", "13", "", "mercredi", "", "",
            "", "", "14", "", "34", "", "", "PDT" /*"GMT-07:00"*/,

            "AD", "1997", "8", "33", "3", "13", "225", "Wed", "2", "PM",
            "2", "2", "14", "14", "34", "12", "513", "PDT",

            "AD", "97", "8", "33", "3", "13", "225", "Wed", "2", "PM",
            "2", "2", "14", "14", "34", "12", "513", "PDT",

            "AD", "1997", "August", "0033", "0003", "0013", "0225",
            "Wednesday", "0002", "PM", "0002", "0002", "0014", "0014",
            "0034", "0012", "0513", "Pacific Daylight Time",
        };
        Date someDate = new Date(871508052513L);
        TimeZone PST = TimeZone.getTimeZone("PST");
        for (int j = 0, exp = 0; j < dateFormats.length; ++j) {
            DateFormat df = dateFormats[j];
            if (!(df instanceof SimpleDateFormat)) {
                continue;
            }
            df.setTimeZone(PST);
            logln(" Pattern = " + ((SimpleDateFormat)df).toPattern());
            logln("  Result = " + df.format(someDate));
            for (int i = 0; i < fieldIDs.length; ++i)
            {
                String field = getFieldText(df, fieldIDs[i], someDate);
                if (!field.equals(expected[exp])) {
                    errln("FAIL: field #" + i + " " + fieldNames[i] + " = \"" +
                            escape(field) + "\", expected \"" + escape(expected[exp]) + "\"");
                }
                ++exp;
            }
        }
    }
    // get the string value for the given field for the given date
    static String getFieldText(DateFormat df, int field, Date date)
    {
        StringBuffer buffer = new StringBuffer();
        FieldPosition pos = new FieldPosition(field);
        df.format(date, buffer, pos);
        return buffer.toString().substring(pos.getBeginIndex(),
                                           pos.getEndIndex());
    }

    // Test parsing of partial strings
    @SuppressWarnings("deprecation")
    public void TestPartialParse994()
    {
        SimpleDateFormat f = new SimpleDateFormat();
        Calendar cal = new GregorianCalendar(2014 - 80, JANUARY, 1);
        f.set2DigitYearStart(cal.getTime());
        tryPat994(f, "yy/MM/dd HH:mm:ss", "97/01/17 10:11:42", new Date(97, 1-1, 17, 10, 11, 42));
        tryPat994(f, "yy/MM/dd HH:mm:ss", "97/01/17 10:", null);
        tryPat994(f, "yy/MM/dd HH:mm:ss", "97/01/17 10", null);
        tryPat994(f, "yy/MM/dd HH:mm:ss", "97/01/17 ", null);
        tryPat994(f, "yy/MM/dd HH:mm:ss", "97/01/17", null);
    }

    void tryPat994(SimpleDateFormat format, String pat, String str, Date expected)
    {
        logln("Pattern \"" + pat + "\"   String \"" + str + "\"");
        try {
            format.applyPattern(pat);
            Date date = format.parse(str);
            String f = format.format(date);
            logln(" parse(" + str + ") -> " + date.toString());
            logln(" format -> " + f);
            if (expected == null ||
                !date.equals(expected)) {
                errln("FAIL: Expected " + expected);
            }
            if (!f.equals(str)) {
                errln("FAIL: Expected " + str);
            }
        }
        catch(ParseException e) {
            logln("ParseException: " + e.getMessage());
            if (expected != null) {
                errln("FAIL: Expected " + expected);
            }
        }
        catch(Exception e) {
            errln("*** Exception:");
            e.printStackTrace();
        }
    }

    // Test pattern with runs things together
    public void TestRunTogetherPattern985()
    {
        String format = "yyyyMMddHHmmssSSS";
        String now, then;

        SimpleDateFormat formatter = new SimpleDateFormat(format);

        Date date1 = new Date();
        now = formatter.format(date1);

        logln(now);

        ParsePosition pos = new ParsePosition(0);

        Date date2 = formatter.parse(now, pos);
        if (date2 == null) {
            then = "Parse stopped at " + pos.getIndex();
        } else {
            then = formatter.format(date2);
        }

        logln(then);

        if (!date2.equals(date1)) {
            errln("FAIL");
        }
    }

    // Test patterns which run numbers together
    @SuppressWarnings("deprecation")
    public void TestRunTogetherPattern917()
    {
        SimpleDateFormat fmt;
        String myDate;

        fmt = new SimpleDateFormat( "yyyy/MM/dd" );
        myDate = "1997/02/03";
        _testIt917( fmt, myDate, new Date(97, 2-1, 3) );

        fmt = new SimpleDateFormat( "yyyyMMdd" );
        myDate = "19970304";
        _testIt917( fmt, myDate, new Date(97, 3-1, 4) );

    }
    void _testIt917( SimpleDateFormat fmt, String str, Date expected )
    {
        logln( "pattern=" + fmt.toPattern() + "   string=" + str );

        Object o;
        try {
            o = fmt.parseObject( str );
        } catch( ParseException e ) {
            e.printStackTrace();
            return;
        }
        logln( "Parsed object: " + o );
        if (!o.equals(expected)) {
            errln("FAIL: Expected " + expected);
        }

        String formatted = fmt.format( o );
        logln( "Formatted string: " + formatted );
        if (!formatted.equals(str)) {
            errln("FAIL: Expected " + str);
        }
    }

    // Test Czech month formatting -- this can cause a problem because the June and
    // July month names share a common prefix.
    @SuppressWarnings("deprecation")
    public void TestCzechMonths459()
    {
        // Use Czech, which has month names with shared prefixes for June and July
        DateFormat fmt = DateFormat.getDateInstance(DateFormat.FULL, new Locale("cs", "", ""));
        //((SimpleDateFormat)fmt).applyPattern("MMMM d yyyy");
        logln("Pattern " + ((SimpleDateFormat)fmt).toPattern());

        Date june = new Date(97, Calendar.JUNE, 15);
        Date july = new Date(97, Calendar.JULY, 15);

        String juneStr = fmt.format(june);
        String julyStr = fmt.format(july);

        try {
            logln("format(June 15 1997) = " + juneStr);
            Date d = fmt.parse(juneStr);
            String s = fmt.format(d);
            int month = d.getMonth();
            logln("  -> parse -> " + s + " (month = " + month + ")");
            if (month != JUNE) {
                errln("FAIL: Month should be June");
            }

            logln("format(July 15 1997) = " + julyStr);
            d = fmt.parse(julyStr);
            s = fmt.format(d);
            month = d.getMonth();
            logln("  -> parse -> " + s + " (month = " + month + ")");
            if (month != JULY) {
                errln("FAIL: Month should be July");
            }
        }
        catch (ParseException e) {
            errln("Exception: " + e);
        }
    }

    // Test big D (day of year) versus little d (day of month)
    @SuppressWarnings("deprecation")
    public void TestLetterDPattern212()
    {
        String dateString = "1995-040.05:01:29";
        String bigD = "yyyy-DDD.hh:mm:ss";
        String littleD = "yyyy-ddd.hh:mm:ss";
        Date expLittleD = new Date(95, 0, 1, 5, 1, 29);
        Date expBigD =  new Date(expLittleD.getTime() + 39*24*3600000L); // 39 days
        expLittleD = expBigD; // Expect the same, with default lenient parsing
        logln( "dateString= " + dateString );
        SimpleDateFormat formatter = new SimpleDateFormat(bigD);
        ParsePosition pos = new ParsePosition(0);
        Date myDate = formatter.parse( dateString, pos );
        logln("Using " + bigD + " -> " + myDate);
        if (myDate.getTime() != expBigD.getTime()) {
            errln("FAIL: Expected " + expBigD + " got " + myDate);
        }

        formatter = new SimpleDateFormat(littleD);
        pos = new ParsePosition(0);
        myDate = formatter.parse( dateString, pos );
        logln("Using " + littleD + " -> " + myDate);
        if (myDate.getTime() != expLittleD.getTime()) {
            errln("FAIL: Expected " + expLittleD + " got " + myDate);
        }
    }

    // Test the 'G' day of year pattern
    @SuppressWarnings("deprecation")
    public void TestDayOfYearPattern195()
    {
        Date today = new Date();
        Date expected = new Date(today.getYear(), today.getMonth(), today.getDate());

        logln("Test Date: " + today);

        SimpleDateFormat sdf =
            (SimpleDateFormat)SimpleDateFormat.getDateInstance();

        tryPattern(sdf, today, null, expected);
        tryPattern(sdf, today, "G yyyy DDD", expected);
    }

    void tryPattern(SimpleDateFormat sdf, Date d, String pattern, Date expected)
    {
        if (pattern != null) {
            sdf.applyPattern(pattern);
        }
        logln("pattern: " + sdf.toPattern());

        String formatResult = sdf.format(d);
        logln(" format -> " + formatResult);
        try {
            Date d2 = sdf.parse(formatResult);
            logln(" parse(" + formatResult +  ") -> " + d2);
            if (d2.getTime() != expected.getTime()) {
                errln("FAIL: Expected " + expected);
            }
            String format2 = sdf.format(d2);
            logln(" format -> " + format2);
            if (!formatResult.equals(format2)) {
                errln("FAIL: Round trip drift");
            }
        }
        catch(Exception e) {
            errln("Error: " + e.getMessage());
        }
    }

    // Test a pattern with single quotes
    @SuppressWarnings("deprecation")
    public void TestQuotePattern161()
    {
        // This pattern used to end in " zzz" but that makes this test zone-dependent
        SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy 'at' hh:mm:ss a zzz");
        Date currentTime_1 = new Date(97, Calendar.AUGUST, 13, 10, 42, 28);
        String dateString = formatter.format(currentTime_1);
        String exp = "08/13/1997 at 10:42:28 AM ";
        logln("format(" + currentTime_1 + ") = " + dateString);
        if (!dateString.regionMatches(0, exp, 0, exp.length())) {
            errln("FAIL: Expected " + exp);
        }
    }

    // Test the parsing of bad input strings
    /** Demonstrates a number of bugs in DateFormat.parse(String) where
     *  either StringIndexOutOfBoundsException is thrown or null is
     *  returned instead of ParseException. To reproduce, run this program
     *  and notice all the "SHOULD NOT HAPPEN" errors.  Note also that the
     *  1 line that should be correct is off by 100 years.  (In this day
     *  and age, no one would assume that 1/1/00 is Jan 1 1900.)
     **/
    public void TestBadInput135()
    {
        int        looks[] = { DateFormat.SHORT, DateFormat.MEDIUM,
                               DateFormat.LONG,  DateFormat.FULL };
        String     strings[] = { "Mar 15", "Mar 15 1997", "asdf",
                                 "3/1/97 1:23:", "3/1/00 1:23:45 AM" };
        DateFormat full = DateFormat.getDateTimeInstance(DateFormat.LONG,
                                                         DateFormat.LONG);
        String expected = "March 1, 2000 1:23:45 AM ";
        for ( int i = 0;  i < strings.length;  ++i ){
            String text = strings[i];
            for ( int j = 0;  j < looks.length;  ++j ){
                int dateLook = looks[j];
                for ( int k = 0;  k < looks.length;  ++k ){
                    int timeLook = looks[k];
                    DateFormat df = DateFormat.getDateTimeInstance(dateLook, timeLook);
                    String prefix = text + ", " + dateLook + "/" + timeLook + ": ";
                    try {
                        Date when = df.parse(text);
                        if ( when == null ){
                            errln(prefix +
                                  "SHOULD NOT HAPPEN: parse returned null.");
                            continue;
                        }
                        String format = full.format(when);
                        logln(prefix + "OK: " + format);
                        // Only match the start -- not the zone, which could vary
                        if (!format.regionMatches(0, expected, 0, expected.length())) {
                            errln("FAIL: Expected " + expected);
                        }
                    }
                    catch ( ParseException e ){
                        //errln(prefix + e); // This is expected.
                    }
                    catch ( StringIndexOutOfBoundsException e ){
                        errln(prefix + "SHOULD NOT HAPPEN: " + e);
                    }
                }
            }
        }
    }

    final private static String parseFormats[] =
    {
        "MMMM d, yyyy",  // january 1, 1970 or jan 1, 1970
        "MMMM d yyyy",   // january 1 1970 or jan 1 1970
        "M/d/yy",        // 1/1/70
        "d MMMM, yyyy",  // 1 january, 1970 or 1 jan, 1970
        "d MMMM yyyy",   // 1 january 1970 or 1 jan 1970
        "d MMMM",        // 1 january or 1 jan
        "MMMM d",        // january 1 or jan 1
        "yyyy",          // 1970
        "h:mm a MMMM d, yyyy" // Date and Time
    };
    final private static String inputStrings[] =
    {
        "bogus string",         null, null, null, null, null, null, null, null, null,
        "April 1, 1997",        "April 1, 1997", null, null, null, null, null, "April 1", null, null,
        "Jan 1, 1970",          "January 1, 1970", null, null, null, null, null, "January 1", null, null,
        "Jan 1 2037",           null, "January 1 2037", null, null, null, null, "January 1", null, null,
        "1/1/70",               null, null, "1/1/70", null, null, null, null, "0001", null,
        "5 May 1997",           null, null, null, null, "5 May 1997", "5 May", null, "0005", null,
        "16 May",               null, null, null, null, null, "16 May", null, "0016", null,
        "April 30",             null, null, null, null, null, null, "April 30", null, null,
        "1998",                 null, null, null, null, null, null, null, "1998", null,
        "1",                    null, null, null, null, null, null, null, "0001", null, // Bug620
        "3:00 pm Jan 1, 1997",  null, null, null, null, null, null, null, "0003", "3:00 PM January 1, 1997",
    };
    // More testing of the parsing of bad input
    @SuppressWarnings("UnusedAssignment")
    public void TestBadInput135a()
    {
        SimpleDateFormat dateParse = new SimpleDateFormat();
        String s;
        Date date;
        int PFLENGTH = parseFormats.length;

        dateParse.applyPattern("d MMMM, yyyy");
        dateParse.setTimeZone(TimeZone.getDefault());
        s = "not parseable";
        logln("Trying to parse \"" + s + "\" with " + dateParse.toPattern());
        try {
            date = dateParse.parse(s);
            errln("FAIL: Expected exception during parse");
        } catch (Exception ex) {
            logln("Exception during parse: " + ex); // This is expected
        }

        for (int i=0; i<inputStrings.length; i += (PFLENGTH+1))
        {
            ParsePosition parsePosition = new ParsePosition(0);
            s = inputStrings[i];

            for (int index=0; index<PFLENGTH; ++index)
            {
                String expected = inputStrings[i + 1 + index];
                dateParse.applyPattern(parseFormats[index]);
                dateParse.setTimeZone(TimeZone.getDefault());
                // logln("Trying to parse \"" + s + "\" with " + dateParse.toPattern());
                try {
                    parsePosition.setIndex(0);
                    date = dateParse.parse(s, parsePosition);
                    if (parsePosition.getIndex() != 0) {
                        if (date == null) {
                            errln("ERROR: null result with pos " +
                                    parsePosition.getIndex() + " " +
                                    s.substring(0, parsePosition.getIndex()) + "|" +
                                    s.substring(parsePosition.getIndex()));
                        } else {
                            String result = dateParse.format(date);
                            logln("Parsed \"" + s + "\" using \"" + dateParse.toPattern() +
                                  "\" to: " + result);
                            if (expected == null) {
                                errln("FAIL: Expected parse failure");
                            } else if (!expected.equals(result)) {
                                errln("FAIL: Expected " + expected);
                            }
                        }
                    } else {
                        // logln("Not parsed.");
                        if (expected != null) {
                            errln("FAIL: Expected " + expected);
                        }
                    }
                } catch (Exception ex) {
                    errln("An exception was thrown during parse: " + ex);
                }
            }
        }
    }

    // Test the handling of 2-digit dates
    public void TestTwoDigitYear() {
        SimpleDateFormat fmt = new SimpleDateFormat("M/d/yy");

        // find out the expected 2-digit year values for "6/5/17" and "6/4/34"
        long start = fmt.get2DigitYearStart().getTime();
        Calendar cal = new Calendar.Builder().setInstant(start).build();
        int startYear = cal.get(YEAR);
        cal.add(YEAR, 100);
        long end = cal.getTimeInMillis();
        int endYear = cal.get(YEAR);
        int xx17 = 0, xx34 = 0;
        for (int year = startYear; year <= endYear; year++) {
            int yy = year % 100;
            if (yy == 17 && xx17 == 0) {
                xx17 = yearValue(start, end, year, JUNE, 5);
            } else if (yy == 34 && xx34 == 0) {
                xx34 = yearValue(start, end, year, JUNE, 4);
            }
            if (xx17 != 0 && xx34 != 0) {
                break;
            }
        }
        if (xx17 == 0 || xx34 == 0) {
            errln("Failed: producing expected values: 2DigitYearStart: " + new Date(start)
                  + ", xx17 = " + xx17 + ", xx34 = " + xx34);
        }
        logln("2DigitYearStart: " + new Date(start) + ", xx17 = " + xx17 + ", xx34 = " + xx34);

        parse2DigitYear(fmt, "6/5/17", new GregorianCalendar(xx17, JUNE, 5).getTime());
        parse2DigitYear(fmt, "6/4/34", new GregorianCalendar(xx34, JUNE, 4).getTime());
    }

    private int yearValue(long start, long end, int year, int month, int dayOfMonth) {
        Calendar cal = new GregorianCalendar(year, month, dayOfMonth);
        long time = cal.getTimeInMillis();
        return (start <= time && time < end) ? year : 0;
    }

    private void parse2DigitYear(SimpleDateFormat fmt, String str, Date expected) {
        try {
            Date d = fmt.parse(str);
            logln("Parsing \"" + str + "\" with " +
                  fmt.toPattern() +
                  "  => " + d.toString());
            if (d.getTime() != expected.getTime()) {
                errln("FAIL: Expected " + expected);
            }
        } catch (ParseException e) {
            errln("FAIL: Got exception");
        }
    }

    // Test behavior of DateFormat with applied time zone
    public void TestDateFormatZone061()
    {
        Date date;
        DateFormat formatter;

        // 25-Mar-97 00:00:00 GMT
        date = new Date( 859248000000L );
        logln( "Date 1997/3/25 00:00 GMT: " + date );
        formatter = new SimpleDateFormat("dd-MMM-yyyyy HH:mm", Locale.UK);
        formatter.setTimeZone( TimeZone.getTimeZone( "GMT" ) );

        String temp = formatter.format( date );
        logln( "Formatted in GMT to: " + temp );

        /* Parse date string */
        try {
            Date tempDate = formatter.parse( temp );
            logln( "Parsed to: " + tempDate );
            if (tempDate.getTime() != date.getTime()) {
                errln("FAIL: Expected " + date);
            }
        }
        catch( Throwable t ) {
            errln( "Date Formatter throws: " +
                   t.toString() );
        }
    }

    // Make sure DateFormat uses the correct zone.
    public void TestDateFormatZone146()
    {
        TimeZone saveDefault = TimeZone.getDefault();

        try {
            TimeZone thedefault = TimeZone.getTimeZone("GMT");
            TimeZone.setDefault(thedefault);
            // java.util.Locale.setDefault(new java.util.Locale("ar", "", ""));

            // check to be sure... its GMT all right
            TimeZone testdefault = TimeZone.getDefault();
            String testtimezone = testdefault.getID();
            if (testtimezone.equals("GMT")) {
                logln("Test timezone = " + testtimezone);
            } else {
                errln("Test timezone should be GMT, not " + testtimezone);
            }

            // now try to use the default GMT time zone
            GregorianCalendar greenwichcalendar =
                new GregorianCalendar(1997, 3, 4, 23, 0);
            //*****************************greenwichcalendar.setTimeZone(TimeZone.getDefault());
            //greenwichcalendar.set(1997, 3, 4, 23, 0);
            // try anything to set hour to 23:00 !!!
            greenwichcalendar.set(Calendar.HOUR_OF_DAY, 23);
            // get time
            Date greenwichdate = greenwichcalendar.getTime();
            // format every way
            String[] DATA = {
                "simple format:  ", "04/04/97 23:00 GMT",
                    "MM/dd/yy HH:mm z",
                "full format:    ", "Friday, April 4, 1997 11:00:00 o'clock PM GMT",
                    "EEEE, MMMM d, yyyy h:mm:ss 'o''clock' a z",
                "long format:    ", "April 4, 1997 11:00:00 PM GMT",
                    "MMMM d, yyyy h:mm:ss a z",
                "default format: ", "04-Apr-97 11:00:00 PM",
                    "dd-MMM-yy h:mm:ss a",
                "short format:   ", "4/4/97 11:00 PM",
                    "M/d/yy h:mm a",
            };

            for (int i=0; i<DATA.length; i+=3) {
                DateFormat fmt = new SimpleDateFormat(DATA[i+2], Locale.ENGLISH);
                fmt.setCalendar(greenwichcalendar);
                String result = fmt.format(greenwichdate);
                logln(DATA[i] + result);
                if (!result.equals(DATA[i+1])) {
                    errln("FAIL: Expected " + DATA[i+1]
                            + ", got " + result);
                }
            }
        }
        finally {
            TimeZone.setDefault(saveDefault);
        }
    }

/* HS : Commented out for now, need to be changed not to use hardcoded results.
    public void TestLocaleDateFormat() // Bug 495
    {
        Date testDate = new Date (97, Calendar.SEPTEMBER, 15);
        DateFormat dfFrench = DateFormat.getDateTimeInstance(DateFormat.FULL,
                                                             DateFormat.FULL, Locale.FRENCH);
        DateFormat dfUS = DateFormat.getDateTimeInstance(DateFormat.FULL,
                                                         DateFormat.FULL, Locale.US);
        String expectedFRENCH = "lundi 15 septembre 1997 00 h 00 GMT-07:00";
        String expectedUS = "Monday, September 15, 1997 12:00:00 o'clock AM PDT";
        logln("Date set to : " + testDate);
        String out = dfFrench.format(testDate);
        logln("Date Formated with French Locale " + out);
        if (!out.equals(expectedFRENCH)) errln("FAIL: Expected " + expectedFRENCH);
        out = dfUS.format(testDate);
        logln("Date Formated with US Locale " + out);
        if (!out.equals(expectedUS)) errln("FAIL: Expected " + expectedUS);
    }
*/
    /**
     * Bug 4056591
     */
/*
test commented out pending API-change approval
    public void Test2YearStartDate() throws ParseException
    {
        // create a SimpleDateFormat to test with; dump out if it's not a SimpleDateFormat
        DateFormat test = DateFormat.getDateInstance(DateFormat.SHORT, Locale.US);

        if (!(test instanceof SimpleDateFormat)) {
            errln("DateFormat.getInstance() didn't return an instance of SimpleDateFormat!");
            return;
        }

        SimpleDateFormat sdf = (SimpleDateFormat)test;
        String testString1 = "3/10/67";
        String testString2 = "3/16/43";
        String testString3 = "7/21/43";

        // set 2-digit start date to 1/1/1900
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.set(1900, 0, 1);
        sdf.set2DigitStartDate(cal.getTime());

        // check to make sure get2DigitStartDate() returns the value we passed to
        // set2DigitStartDate()
        Date date = sdf.get2DigitStartDate();
        cal.setTime(date);
        if (cal.get(Calendar.YEAR) != 1900 || cal.get(Calendar.MONTH) != 0 ||
                        cal.get(Calendar.DATE) != 1)
            errln("SimpleDateFormat.get2DigitStartDate() returned " + (cal.get(Calendar.MONTH)
                        + 1) + "/" + cal.get(Calendar.DATE) + "/" + cal.get(Calendar.YEAR) +
                        " instead of 1/1/1900.");

        // try parsing "3/10/67" and "3/16/43" with the 2-digit start date set to 1/1/1900
        date = sdf.parse(testString1);
        cal.setTime(date);
        if (cal.get(Calendar.YEAR) != 1967)
            errln("Parsing \"3/10/67\" with 2-digit start date set to 1/1/1900 yielded a year of "
                            + cal.get(Calendar.YEAR) + " instead of 1967.");
        if (cal.get(Calendar.MONTH) != 2 || cal.get(Calendar.DATE) != 10)
            errln("Parsing \"3/10/67\" with 2-digit start date set to 1/1/1900 failed: got " +
                            (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DATE) +
                            " instead of 3/10.");
        date = sdf.parse(testString2);
        cal.setTime(date);
        if (cal.get(Calendar.YEAR) != 1943)
            errln("Parsing \"3/16/43\" with 2-digit start date set to 1/1/1900 yielded a year of "
                            + cal.get(Calendar.YEAR) + " instead of 1943.");
        if (cal.get(Calendar.MONTH) != 2 || cal.get(Calendar.DATE) != 16)
            errln("Parsing \"3/16/43\" with 2-digit start date set to 1/1/1900 failed: got " +
                            (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DATE) +
                            " instead of 3/16.");

        // try parsing "3/10/67" and "3/16/43" with the 2-digit start date set to 1/1/2000
        cal.set(2000, 0, 1);
        sdf.set2DigitStartDate(cal.getTime());
        date = sdf.parse(testString1);
        cal.setTime(date);
        if (cal.get(Calendar.YEAR) != 2067)
            errln("Parsing \"3/10/67\" with 2-digit start date set to 1/1/2000 yielded a year of "
                            + cal.get(Calendar.YEAR) + " instead of 2067.");
        if (cal.get(Calendar.MONTH) != 2 || cal.get(Calendar.DATE) != 10)
            errln("Parsing \"3/10/67\" with 2-digit start date set to 1/1/2000 failed: got " +
                            (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DATE) +
                            " instead of 3/10.");
        date = sdf.parse(testString2);
        cal.setTime(date);
        if (cal.get(Calendar.YEAR) != 2043)
            errln("Parsing \"3/16/43\" with 2-digit start date set to 1/1/2000 yielded a year of "
                            + cal.get(Calendar.YEAR) + " instead of 1943.");
        if (cal.get(Calendar.MONTH) != 2 || cal.get(Calendar.DATE) != 16)
            errln("Parsing \"3/16/43\" with 2-digit start date set to 1/1/2000 failed: got " +
                            (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DATE) +
                            " instead of 3/16.");

        // try parsing "3/10/67" and "3/16/43" with the 2-digit start date set to 1/1/1950
        cal.set(1950, 0, 1);
        sdf.set2DigitStartDate(cal.getTime());
        date = sdf.parse(testString1);
        cal.setTime(date);
        if (cal.get(Calendar.YEAR) != 1967)
            errln("Parsing \"3/10/67\" with 2-digit start date set to 1/1/1950 yielded a year of "
                            + cal.get(Calendar.YEAR) + " instead of 1967.");
        if (cal.get(Calendar.MONTH) != 2 || cal.get(Calendar.DATE) != 10)
            errln("Parsing \"3/10/67\" with 2-digit start date set to 1/1/1950 failed: got " +
                            (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DATE) +
                            " instead of 3/10.");
        date = sdf.parse(testString2);
        cal.setTime(date);
        if (cal.get(Calendar.YEAR) != 2043)
            errln("Parsing \"3/16/43\" with 2-digit start date set to 1/1/1950 yielded a year of "
                            + cal.get(Calendar.YEAR) + " instead of 1943.");
        if (cal.get(Calendar.MONTH) != 2 || cal.get(Calendar.DATE) != 16)
            errln("Parsing \"3/16/43\" with 2-digit start date set to 1/1/1950 failed: got " +
                            (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DATE) +
                            " instead of 3/16.");

        // try parsing "3/16/43" and "7/21/43" with the 2-digit start date set to 6/1/1943
        cal.set(1943, 5, 1);
        sdf.set2DigitStartDate(cal.getTime());
        date = sdf.parse(testString2);
        cal.setTime(date);
        if (cal.get(Calendar.YEAR) != 2043)
            errln("Parsing \"3/16/43\" with 2-digit start date set to 6/1/1943 yielded a year of "
                            + cal.get(Calendar.YEAR) + " instead of 2043.");
        if (cal.get(Calendar.MONTH) != 2 || cal.get(Calendar.DATE) != 16)
            errln("Parsing \"3/16/43\" with 2-digit start date set to 6/1/1943 failed: got " +
                            (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DATE) +
                            " instead of 3/16.");
        date = sdf.parse(testString3);
        cal.setTime(date);
        if (cal.get(Calendar.YEAR) != 1943)
            errln("Parsing \"7/21/43\" with 2-digit start date set to 6/1/1943 yielded a year of "
                            + cal.get(Calendar.YEAR) + " instead of 1943.");
        if (cal.get(Calendar.MONTH) != 6 || cal.get(Calendar.DATE) != 21)
            errln("Parsing \"7/21/43\" with 2-digit start date set to 6/1/1943 failed: got " +
                            (cal.get(Calendar.MONTH) + 1) + "/" + cal.get(Calendar.DATE) +
                            " instead of 7/21.");

        // and finally, check one more time to make sure get2DigitStartDate() returns the last
        // value we passed to set2DigitStartDate()
        date = sdf.get2DigitStartDate();
        cal.setTime(date);
        if (cal.get(Calendar.YEAR) != 1943 || cal.get(Calendar.MONTH) != 5 ||
                        cal.get(Calendar.DATE) != 1)
            errln("SimpleDateFormat.get2DigitStartDate() returned " + (cal.get(Calendar.MONTH)
                        + 1) + "/" + cal.get(Calendar.DATE) + "/" + cal.get(Calendar.YEAR) +
                        " instead of 6/1/1943.");
    }
*/

    /**
     * ParsePosition.errorIndex tests.
     */
    @SuppressWarnings("deprecation")
    public void Test4052223()
    {
        String str = "7/SOS/2001";
        Date exp = new Date(101, Calendar.SEPTEMBER, 7);
        String pat = "d/MMM/yy";
        SimpleDateFormat sdf = new SimpleDateFormat(pat);
        ParsePosition pos = new ParsePosition(0);
        Date d = sdf.parse(str, pos);
        logln(str + " parses with " + pat + " to " + d);
        if (d == null && pos.getErrorIndex() == 2) {
            logln("Expected null returned, failed at : " + pos.getErrorIndex());
        } else {
            errln("Failed, parse " + str + " got : " + d + ", index=" + pos.getErrorIndex());
        }
    }

    /**
     * Bug4469904 -- th_TH date format doesn't use Thai B.E.
     */
    public void TestBuddhistEraBugId4469904() {
        String era = "\u0e1e.\u0e28.";
        Locale loc = new Locale("th", "TH");
        Calendar cal = Calendar.getInstance(Locale.US);
        cal.set(2001, 7, 23);
        Date date = cal.getTime();
        DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, loc);
        String output = df.format(date);
        int index = output.indexOf(era);
        if (index == -1) {
            errln("Test4469904: Failed. Buddhist Era abbrev not present.");
        }
    }

    /**
     * 4326988: API: SimpleDateFormat throws NullPointerException when parsing with null pattern
     */
    @SuppressWarnings("UnusedAssignment")
    public void Test4326988() {
        String[] wrongPatterns = {
            "hh o''clock",
            "hh 'o''clock",     // unterminated quote
            "''''''''''''oclock",
            "efgxyz",
        };
        String[] goodPatterns = {
            "hh 'o''clock'",
            "'''''''''''''o'",
            "'efgxyz'",
            ":;,.-",
        };

        // Check NullPointerException
        try {
            SimpleDateFormat fmt = new SimpleDateFormat(null);
            errln("SimpleDateFormat() doesn't throw NPE with null pattern");
        } catch (NullPointerException e) {
            // Okay
        }
        try {
            Locale loc = null;
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd", loc);
            errln("SimpleDateFormat() doesn't throw NPE with null locale");
        } catch (NullPointerException e) {
            // Okay
        }
        try {
            DateFormatSymbols symbols = null;
            SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd", symbols);
            errln("SimpleDateFormat() doesn't throw NPE with null DateFormatSymbols");
        } catch (NullPointerException e) {
            // Okay
        }
        try {
            SimpleDateFormat fmt = new SimpleDateFormat();
            fmt.applyPattern(null);
            errln("applyPattern() doesn't throw NPE with null pattern");
        } catch (NullPointerException e) {
            // Okay
        }

        // Check IllegalParameterException
        for (int i = 0; i < wrongPatterns.length; i++) {
            try {
                SimpleDateFormat fmt = new SimpleDateFormat(wrongPatterns[i]);
                errln("SimpleDateFormat(\"" + wrongPatterns[i] + "\")" +
                      " doesn't throw an IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                // Okay
            }
            try {
                SimpleDateFormat fmt = new SimpleDateFormat(wrongPatterns[i],
                                                            DateFormatSymbols.getInstance());
                errln("SimpleDateFormat(\"" + wrongPatterns[i] + "\", DateFormatSymbols) doesn't " +
                      "throw an IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                // Okay
            }
            try {
                SimpleDateFormat fmt = new SimpleDateFormat(wrongPatterns[i],
                                                            Locale.US);
                errln("SimpleDateFormat(\"" + wrongPatterns[i] +
                      "\", Locale) doesn't throw an IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                // Okay
            }
            try {
                SimpleDateFormat fmt = new SimpleDateFormat();
                fmt.applyPattern(wrongPatterns[i]);
                errln("SimpleDateFormat.applyPattern(\"" + wrongPatterns[i] +
                      "\") doesn't throw an IllegalArgumentException");
            } catch (IllegalArgumentException e) {
                // Okay
            }
        }

        for (int i = 0; i < goodPatterns.length; i++) {
            SimpleDateFormat fmt;
            fmt = new SimpleDateFormat(goodPatterns[i]);
            fmt = new SimpleDateFormat(goodPatterns[i],
                                       DateFormatSymbols.getInstance());
            fmt = new SimpleDateFormat(goodPatterns[i],
                                       Locale.US);
            fmt = new SimpleDateFormat();
            fmt.applyPattern(goodPatterns[i]);
        }
    }

    /**
     * 4486735: RFE: SimpleDateFormat performance improvement
     *
     * Another round trip test
     */
    @SuppressWarnings("deprecation")
    public void Test4486735() throws Exception {
        TimeZone initialTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
        Locale[] locales = Locale.getAvailableLocales();
        String[] zones = { "GMT", "America/Los_Angeles", "Europe/London", "Asia/Tokyo" };

        // Round to minutes. Some FULL formats don't have seconds.
        long time = System.currentTimeMillis()/60000 * 60000;
        Date date = new Date(time);
        logln("the test date: " + date);

        try {
            for (int z = 0; z < zones.length; z++) {
                TimeZone.setDefault(TimeZone.getTimeZone(zones[z]));
                for (int i = 0; i < locales.length; i++) {
                    Locale loc = locales[i];
                    DateFormat df = DateFormat.getDateTimeInstance(DateFormat.FULL,
                                                                   DateFormat.FULL,
                                                                   loc);
                    String s = df.format(date);
                    logln(s);
                    Date parsedDate = df.parse(s);
                    long parsedTime = parsedDate.getTime();
                    if (time != parsedTime) {
                        // See if the time is in daylight-standard time transition. (JDK-8140571)
                        // Date-time formats in some locales don't have time zone information.
                        TimeZone tz = TimeZone.getDefault();
                        if (tz.inDaylightTime(date) && !tz.inDaylightTime(parsedDate)) {
                            if (time == parsedTime - tz.getDSTSavings()) {
                                // OK (in "fall-back")
                                continue;
                            }
                        }
                        errln("round trip conversion failed: timezone="+zones[z]+
                              ", locale=" + loc +
                              ", expected=" + time + ", got=" + parsedTime);
                    }
                }
            }

            // Long format test
            String pat =
               "'xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +  // 100
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +  // 200
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +  // 300
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +  // 400
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +  // 500
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx'" +
                "\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:" +
                "\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:" +
                "\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:" +
                "\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593':'" +
                "\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:" +
                "yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy" +  // 100
                "yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy" +
                "yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy" +  // 200
                "yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy" +
                "yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy" +  // 300
                "yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy\u5e74";

            // Note that >4 y's produces just "2001" until 1.3.1. This
            // was fixed in 1.4.
            String expected =
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" +
                "\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:" +
                "\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:" +
                "\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:" +
                "\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:" +
                "\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:\u6642\u9593:" +
                "00000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000000000" +
                "00000000000000000000000000000000000000000000002001\u5e74";
            SimpleDateFormat sdf = new SimpleDateFormat(pat);
            String s = sdf.format(new Date(2001-1900, Calendar.JANUARY, 1));
            if (!expected.equals(s)) {
                errln("wrong format result: expected="+expected+", got="+s);
            }
            Date longday = sdf.parse(s);
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(longday);
            if (cal.get(YEAR) != 2001) {
                errln("wrong parse result: expected=2001, got=" + cal.get(YEAR));
            }
        } catch (Exception e) {
            throw e;
        } finally {
            // Restore the initial time zone
            TimeZone.setDefault(initialTimeZone);
        }
    }

    public void Test8216969() throws Exception {
        Locale locale = new Locale("ru");
        String format = "\u0434\u0435\u043a";
        String standalone = "\u0434\u0435\u043a.";

        // Check that format form is used so that the dot is parsed correctly.
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMM.yyyy", locale);
        System.out.println(simpleDateFormat.parse("28 " + format + ".2018"));

        // Check that standalone form is used.
        simpleDateFormat = new SimpleDateFormat("MMM", locale);
        System.out.println(simpleDateFormat.parse(standalone));
    }
}
