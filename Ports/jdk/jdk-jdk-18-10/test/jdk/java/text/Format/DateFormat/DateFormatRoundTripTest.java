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
 * @summary test Date Format (Round Trip)
 * @bug 8008577
 * @library /java/text/testlib
 * @run main/othervm -Djava.locale.providers=COMPAT,SPI DateFormatRoundTripTest
 */

import java.text.*;
import java.util.*;

public class DateFormatRoundTripTest extends IntlTest {

    static Random RANDOM = null;

    static final long FIXED_SEED = 3141592653589793238L; // Arbitrary fixed value

    // Useful for turning up subtle bugs: Use -infinite and run while at lunch.
    boolean INFINITE = false; // Warning -- makes test run infinite loop!!!

    boolean random = false;

    // Options used to reproduce failures
    Locale locale = null;
    String pattern = null;
    Date initialDate = null;

    Locale[] avail;
    TimeZone defaultZone;

    // If SPARSENESS is > 0, we don't run each exhaustive possibility.
    // There are 24 total possible tests per each locale.  A SPARSENESS
    // of 12 means we run half of them.  A SPARSENESS of 23 means we run
    // 1 of them.  SPARSENESS _must_ be in the range 0..23.
    static final int SPARSENESS = 18;

    static final int TRIALS = 4;

    static final int DEPTH = 5;

    static SimpleDateFormat refFormat =
        new SimpleDateFormat("EEE MMM dd HH:mm:ss.SSS zzz yyyy G");

    public DateFormatRoundTripTest(boolean rand, long seed, boolean infinite,
                                   Date date, String pat, Locale loc) {
        random = rand;
        if (random) {
            RANDOM = new Random(seed);
        }
        INFINITE = infinite;

        initialDate = date;
        locale = loc;
        pattern = pat;
    }

    /**
     * Parse a name like "fr_FR" into new Locale("fr", "FR", "");
     */
    static Locale createLocale(String name) {
        String country = "",
               variant = "";
        int i;
        if ((i = name.indexOf('_')) >= 0) {
            country = name.substring(i+1);
            name = name.substring(0, i);
        }
        if ((i = country.indexOf('_')) >= 0) {
            variant = country.substring(i+1);
            country = country.substring(0, i);
        }
        return new Locale(name, country, variant);
    }

    public static void main(String[] args) throws Exception {
        // Command-line parameters
        Locale loc = null;
        boolean infinite = false;
        boolean random = false;
        long seed = FIXED_SEED;
        String pat = null;
        Date date = null;

        List<String> newArgs = new ArrayList<>();
        for (int i=0; i<args.length; ++i) {
            if (args[i].equals("-locale")
                && (i+1) < args.length) {
                loc = createLocale(args[i+1]);
                ++i;
            } else if (args[i].equals("-date")
                       && (i+1) < args.length) {
                date = new Date(Long.parseLong(args[i+1]));
                ++i;
            } else if (args[i].equals("-pattern")
                && (i+1) < args.length) {
                pat = args[i+1];
                ++i;
            } else if (args[i].equals("-INFINITE")) {
                infinite = true;
            } else if (args[i].equals("-random")) {
                random = true;
            } else if (args[i].equals("-randomseed")) {
                random = true;
                seed = System.currentTimeMillis();
            } else if (args[i].equals("-seed")
                       && (i+1) < args.length) {
                random = true;
                seed = Long.parseLong(args[i+1]);
                ++i;
            } else {
                newArgs.add(args[i]);
            }
        }

        if (newArgs.size() != args.length) {
            args = new String[newArgs.size()];
            newArgs.addAll(Arrays.asList(args));
        }

        new DateFormatRoundTripTest(random, seed, infinite, date, pat, loc).run(args);
    }

    /**
     * Print a usage message for this test class.
     */
    void usage() {
        System.out.println(getClass().getName() +
                           ": [-pattern <pattern>] [-locale <locale>] [-date <ms>] [-INFINITE]");
        System.out.println(" [-random | -randomseed | -seed <seed>]");
        System.out.println("* Warning: Some patterns will fail with some locales.");
        System.out.println("* Do not use -pattern unless you know what you are doing!");
        System.out.println("When specifying a locale, use a format such as fr_FR.");
        System.out.println("Use -pattern, -locale, and -date to reproduce a failure.");
        System.out.println("-random     Random with fixed seed (same data every run).");
        System.out.println("-randomseed Random with a random seed.");
        System.out.println("-seed <s>   Random using <s> as seed.");
        super.usage();
    }

    static private class TestCase {
        private int[] date;
        TimeZone zone;
        FormatFactory ff;
        boolean timeOnly;
        private Date _date;

        TestCase(int[] d, TimeZone z, FormatFactory f, boolean timeOnly) {
            date = d;
            zone = z;
            ff  = f;
            this.timeOnly = timeOnly;
        }

        TestCase(Date d, TimeZone z, FormatFactory f, boolean timeOnly) {
            date = null;
            _date = d;
            zone = z;
            ff  = f;
            this.timeOnly = timeOnly;
        }

        /**
         * Create a format for testing.
         */
        DateFormat createFormat() {
            return ff.createFormat();
        }

        /**
         * Return the Date of this test case; must be called with the default
         * zone set to this TestCase's zone.
         */
        @SuppressWarnings("deprecation")
        Date getDate() {
            if (_date == null) {
                // Date constructor will work right iff we are in the target zone
                int h = 0;
                int m = 0;
                int s = 0;
                if (date.length >= 4) {
                    h = date[3];
                    if (date.length >= 5) {
                        m = date[4];
                        if (date.length >= 6) {
                            s = date[5];
                        }
                    }
                }
                _date = new Date(date[0] - 1900, date[1] - 1, date[2],
                                 h, m, s);
            }
            return _date;
        }

        public String toString() {
            return String.valueOf(getDate().getTime()) + " " +
                refFormat.format(getDate()) + " : " + ff.createFormat().format(getDate());
        }
    };

    private interface FormatFactory {
        DateFormat createFormat();
    }

    TestCase[] TESTS = {
        // Feb 29 2004 -- ordinary leap day
        new TestCase(new int[] {2004, 2, 29}, null,
                     new FormatFactory() { public DateFormat createFormat() {
                         return DateFormat.getDateTimeInstance(DateFormat.LONG,
                                                               DateFormat.LONG);
                     }}, false),

        // Feb 29 2000 -- century leap day
        new TestCase(new int[] {2000, 2, 29}, null,
                     new FormatFactory() { public DateFormat createFormat() {
                         return DateFormat.getDateTimeInstance(DateFormat.LONG,
                                                               DateFormat.LONG);
                     }}, false),

        // 0:00:00 Jan 1 1999 -- first second of normal year
        new TestCase(new int[] {1999, 1, 1}, null,
                     new FormatFactory() { public DateFormat createFormat() {
                         return DateFormat.getDateTimeInstance();
                     }}, false),

        // 23:59:59 Dec 31 1999 -- last second of normal year
        new TestCase(new int[] {1999, 12, 31, 23, 59, 59}, null,
                     new FormatFactory() { public DateFormat createFormat() {
                         return DateFormat.getDateTimeInstance();
                     }}, false),

        // 0:00:00 Jan 1 2004 -- first second of leap year
        new TestCase(new int[] {2004, 1, 1}, null,
                     new FormatFactory() { public DateFormat createFormat() {
                         return DateFormat.getDateTimeInstance();
                     }}, false),

        // 23:59:59 Dec 31 2004 -- last second of leap year
        new TestCase(new int[] {2004, 12, 31, 23, 59, 59}, null,
                     new FormatFactory() { public DateFormat createFormat() {
                         return DateFormat.getDateTimeInstance();
                     }}, false),

        // October 25, 1998 1:59:59 AM PDT -- just before DST cessation
        new TestCase(new Date(909305999000L), TimeZone.getTimeZone("PST"),
                     new FormatFactory() { public DateFormat createFormat() {
                         return DateFormat.getDateTimeInstance(DateFormat.LONG,
                                                               DateFormat.LONG);
                     }}, false),

        // October 25, 1998 1:00:00 AM PST -- just after DST cessation
        new TestCase(new Date(909306000000L), TimeZone.getTimeZone("PST"),
                     new FormatFactory() { public DateFormat createFormat() {
                         return DateFormat.getDateTimeInstance(DateFormat.LONG,
                                                               DateFormat.LONG);
                     }}, false),

        // April 4, 1999 1:59:59 AM PST -- just before DST onset
        new TestCase(new int[] {1999, 4, 4, 1, 59, 59},
                     TimeZone.getTimeZone("PST"),
                     new FormatFactory() { public DateFormat createFormat() {
                         return DateFormat.getDateTimeInstance(DateFormat.LONG,
                                                               DateFormat.LONG);
                     }}, false),

        // April 4, 1999 3:00:00 AM PDT -- just after DST onset
        new TestCase(new Date(923220000000L), TimeZone.getTimeZone("PST"),
                     new FormatFactory() { public DateFormat createFormat() {
                         return DateFormat.getDateTimeInstance(DateFormat.LONG,
                                                               DateFormat.LONG);
                     }}, false),

        // October 4, 1582 11:59:59 PM PDT -- just before Gregorian change
        new TestCase(new int[] {1582, 10, 4, 23, 59, 59}, null,
                     new FormatFactory() { public DateFormat createFormat() {
                         return DateFormat.getDateTimeInstance(DateFormat.LONG,
                                                               DateFormat.LONG);
                     }}, false),

        // October 15, 1582 12:00:00 AM PDT -- just after Gregorian change
        new TestCase(new int[] {1582, 10, 15, 0, 0, 0}, null,
                     new FormatFactory() { public DateFormat createFormat() {
                         return DateFormat.getDateTimeInstance(DateFormat.LONG,
                                                               DateFormat.LONG);
                     }}, false),
    };

    public void TestDateFormatRoundTrip() {
        avail = DateFormat.getAvailableLocales();
        logln("DateFormat available locales: " + avail.length);
        logln("Default TimeZone: " +
              (defaultZone = TimeZone.getDefault()).getID());

        if (random || initialDate != null) {
            if (RANDOM == null) {
                // Need this for sparse coverage to reduce combinatorial explosion,
                // even for non-random looped testing (i.e., with explicit date but
                // not pattern or locale).
                RANDOM = new Random(FIXED_SEED);
            }
            loopedTest();
        } else {
            for (int i=0; i<TESTS.length; ++i) {
                doTest(TESTS[i]);
            }
        }
    }

    /**
     * TimeZone must be set to tc.zone before this method is called.
     */
    private void doTestInZone(TestCase tc) {
        logln(escape(tc.toString()));
        Locale save = Locale.getDefault();
        try {
            if (locale != null) {
                Locale.setDefault(locale);
                doTest(locale, tc.createFormat(), tc.timeOnly, tc.getDate());
            } else {
                for (int i=0; i<avail.length; ++i) {
                    Locale.setDefault(avail[i]);
                    doTest(avail[i], tc.createFormat(), tc.timeOnly, tc.getDate());
                }
            }
        } finally {
            Locale.setDefault(save);
        }
    }

    private void doTest(TestCase tc) {
        if (tc.zone == null) {
            // Just run in the default zone
            doTestInZone(tc);
        } else {
            try {
                TimeZone.setDefault(tc.zone);
                doTestInZone(tc);
            } finally {
                TimeZone.setDefault(defaultZone);
            }
        }
    }

    private void loopedTest() {
        if (INFINITE) {
            // Special infinite loop test mode for finding hard to reproduce errors
            if (locale != null) {
                logln("ENTERING INFINITE TEST LOOP, LOCALE " + locale.getDisplayName());
                for (;;) doTest(locale);
            } else {
                logln("ENTERING INFINITE TEST LOOP, ALL LOCALES");
                for (;;) {
                    for (int i=0; i<avail.length; ++i) {
                        doTest(avail[i]);
                    }
                }
            }
        }
        else {
            if (locale != null) {
                doTest(locale);
            } else {
                doTest(Locale.getDefault());

                for (int i=0; i<avail.length; ++i) {
                    doTest(avail[i]);
                }
            }
        }
    }

    void doTest(Locale loc) {
        if (!INFINITE) logln("Locale: " + loc.getDisplayName());

        if (pattern != null) {
            doTest(loc, new SimpleDateFormat(pattern, loc));
            return;
        }

        // Total possibilities = 24
        //  4 date
        //  4 time
        //  16 date-time
        boolean[] TEST_TABLE = new boolean[24];
        for (int i=0; i<24; ++i) TEST_TABLE[i] = true;

        // If we have some sparseness, implement it here.  Sparseness decreases
        // test time by eliminating some tests, up to 23.
        if (!INFINITE) {
            for (int i=0; i<SPARSENESS; ) {
                int random = (int)(java.lang.Math.random() * 24);
                if (random >= 0 && random < 24 && TEST_TABLE[i]) {
                    TEST_TABLE[i] = false;
                    ++i;
                }
            }
        }

        int itable = 0;
        for (int style=DateFormat.FULL; style<=DateFormat.SHORT; ++style) {
            if (TEST_TABLE[itable++])
                doTest(loc, DateFormat.getDateInstance(style, loc));
        }

        for (int style=DateFormat.FULL; style<=DateFormat.SHORT; ++style) {
            if (TEST_TABLE[itable++])
                doTest(loc, DateFormat.getTimeInstance(style, loc), true);
        }

        for (int dstyle=DateFormat.FULL; dstyle<=DateFormat.SHORT; ++dstyle) {
            for (int tstyle=DateFormat.FULL; tstyle<=DateFormat.SHORT; ++tstyle) {
                if (TEST_TABLE[itable++])
                    doTest(loc, DateFormat.getDateTimeInstance(dstyle, tstyle, loc));
            }
        }
    }

    void doTest(Locale loc, DateFormat fmt) { doTest(loc, fmt, false); }

    void doTest(Locale loc, DateFormat fmt, boolean timeOnly) {
        doTest(loc, fmt, timeOnly, initialDate != null ? initialDate : generateDate());
    }

    void doTest(Locale loc, DateFormat fmt, boolean timeOnly, Date date) {
        // Skip testing with the JapaneseImperialCalendar which
        // doesn't support the Gregorian year semantices with 'y'.
        if (fmt.getCalendar().getClass().getName().equals("java.util.JapaneseImperialCalendar")) {
            return;
        }

        String pat = ((SimpleDateFormat)fmt).toPattern();
        String deqPat = dequotePattern(pat); // Remove quoted elements

        boolean hasEra = (deqPat.indexOf("G") != -1);
        boolean hasZone = (deqPat.indexOf("z") != -1);

        Calendar cal = fmt.getCalendar();

        // Because patterns contain incomplete data representing the Date,
        // we must be careful of how we do the roundtrip.  We start with
        // a randomly generated Date because they're easier to generate.
        // From this we get a string.  The string is our real starting point,
        // because this string should parse the same way all the time.  Note
        // that it will not necessarily parse back to the original date because
        // of incompleteness in patterns.  For example, a time-only pattern won't
        // parse back to the same date.

        try {
            for (int i=0; i<TRIALS; ++i) {
                Date[] d = new Date[DEPTH];
                String[] s = new String[DEPTH];
                String error = null;

                d[0] = date;

                // We go through this loop until we achieve a match or until
                // the maximum loop count is reached.  We record the points at
                // which the date and the string starts to match.  Once matching
                // starts, it should continue.
                int loop;
                int dmatch = 0; // d[dmatch].getTime() == d[dmatch-1].getTime()
                int smatch = 0; // s[smatch].equals(s[smatch-1])
                for (loop=0; loop<DEPTH; ++loop) {
                    if (loop > 0) d[loop] = fmt.parse(s[loop-1]);
                    s[loop] = fmt.format(d[loop]);

                    if (loop > 0) {
                        if (smatch == 0) {
                            boolean match = s[loop].equals(s[loop-1]);
                            if (smatch == 0) {
                                if (match) smatch = loop;
                            }
                            else if (!match) {
                                // This should never happen; if it does, fail.
                                smatch = -1;
                                error = "FAIL: String mismatch after match";
                            }
                        }

                        if (dmatch == 0) {
                            boolean match = d[loop].getTime() == d[loop-1].getTime();
                            if (dmatch == 0) {
                                if (match) dmatch = loop;
                            }
                            else if (!match) {
                                // This should never happen; if it does, fail.
                                dmatch = -1;
                                error = "FAIL: Date mismatch after match";
                            }
                        }

                        if (smatch != 0 && dmatch != 0) break;
                    }
                }
                // At this point loop == DEPTH if we've failed, otherwise loop is the
                // max(smatch, dmatch), that is, the index at which we have string and
                // date matching.

                // Date usually matches in 2.  Exceptions handled below.
                int maxDmatch = 2;
                int maxSmatch = 1;
                if (dmatch > maxDmatch) {
                    // Time-only pattern with zone information and a starting date in PST.
                    if (timeOnly && hasZone && fmt.getTimeZone().inDaylightTime(d[0])) {
                        maxDmatch = 3;
                        maxSmatch = 2;
                    }
                }

                // String usually matches in 1.  Exceptions are checked for here.
                if (smatch > maxSmatch) { // Don't compute unless necessary
                    // Starts in BC, with no era in pattern
                    if (!hasEra && getField(cal, d[0], Calendar.ERA) == GregorianCalendar.BC)
                        maxSmatch = 2;
                    // Starts in DST, no year in pattern
                    else if (fmt.getTimeZone().inDaylightTime(d[0]) &&
                             deqPat.indexOf("yyyy") == -1)
                        maxSmatch = 2;
                    // Two digit year with zone and year change and zone in pattern
                    else if (hasZone &&
                             fmt.getTimeZone().inDaylightTime(d[0]) !=
                             fmt.getTimeZone().inDaylightTime(d[dmatch]) &&
                             getField(cal, d[0], Calendar.YEAR) !=
                             getField(cal, d[dmatch], Calendar.YEAR) &&
                             deqPat.indexOf("y") != -1 &&
                             deqPat.indexOf("yyyy") == -1)
                        maxSmatch = 2;
                    // Two digit year, year change, DST changeover hour.  Example:
                    //    FAIL: Pattern: dd/MM/yy HH:mm:ss
                    //     Date matched in 2, wanted 2
                    //     String matched in 2, wanted 1
                    //        Thu Apr 02 02:35:52.110 PST 1795 AD F> 02/04/95 02:35:52
                    //     P> Sun Apr 02 01:35:52.000 PST 1995 AD F> 02/04/95 01:35:52
                    //     P> Sun Apr 02 01:35:52.000 PST 1995 AD F> 02/04/95 01:35:52 d== s==
                    // The problem is that the initial time is not a DST onset day, but
                    // then the year changes, and the resultant parsed time IS a DST
                    // onset day.  The hour "2:XX" makes no sense if 2:00 is the DST
                    // onset, so DateFormat interprets it as 1:XX (arbitrary -- could
                    // also be 3:XX, same problem).  This results in an extra iteration
                    // for String match convergence.
                    else if (!justBeforeOnset(cal, d[0]) && justBeforeOnset(cal, d[dmatch]) &&
                             getField(cal, d[0], Calendar.YEAR) !=
                             getField(cal, d[dmatch], Calendar.YEAR) &&
                             deqPat.indexOf("y") != -1 &&
                             deqPat.indexOf("yyyy") == -1)
                        maxSmatch = 2;
                    // Another spurious failure:
                    // FAIL: Pattern: dd MMMM yyyy hh:mm:ss
                    //  Date matched in 2, wanted 2
                    //  String matched in 2, wanted 1
                    //     Sun Apr 05 14:28:38.410 PDT 3998 AD F> 05 April 3998 02:28:38
                    //  P> Sun Apr 05 01:28:38.000 PST 3998 AD F> 05 April 3998 01:28:38
                    //  P> Sun Apr 05 01:28:38.000 PST 3998 AD F> 05 April 3998 01:28:38 d== s==
                    // The problem here is that with an 'hh' pattern, hour from 1-12,
                    // a lack of AM/PM -- that is, no 'a' in pattern, and an initial
                    // time in the onset hour + 12:00.
                    else if (deqPat.indexOf('h') >= 0
                             && deqPat.indexOf('a') < 0
                             && justBeforeOnset(cal, new Date(d[0].getTime() - 12*60*60*1000L))
                             && justBeforeOnset(cal, d[1]))
                        maxSmatch = 2;
                }

                if (dmatch > maxDmatch || smatch > maxSmatch
                    || dmatch < 0 || smatch < 0) {
                    StringBuffer out = new StringBuffer();
                    if (error != null) {
                        out.append(error + '\n');
                    }
                    out.append("FAIL: Pattern: " + pat + ", Locale: " + loc + '\n');
                    out.append("      Initial date (ms): " + d[0].getTime() + '\n');
                    out.append("     Date matched in " + dmatch
                               + ", wanted " + maxDmatch + '\n');
                    out.append("     String matched in " + smatch
                               + ", wanted " + maxSmatch);

                    for (int j=0; j<=loop && j<DEPTH; ++j) {
                        out.append("\n    " +
                                   (j>0?" P> ":"    ") + refFormat.format(d[j]) + " F> " +
                                   escape(s[j]) +
                                   (j>0&&d[j].getTime()==d[j-1].getTime()?" d==":"") +
                                   (j>0&&s[j].equals(s[j-1])?" s==":""));
                    }
                    errln(escape(out.toString()));
                }
            }
        }
        catch (ParseException e) {
            errln(e.toString());
        }
    }

    /**
     * Return a field of the given date
     */
    static int getField(Calendar cal, Date d, int f) {
        // Should be synchronized, but we're single threaded so it's ok
        cal.setTime(d);
        return cal.get(f);
    }

    /**
     * Return true if the given Date is in the 1 hour window BEFORE the
     * change from STD to DST for the given Calendar.
     */
    static final boolean justBeforeOnset(Calendar cal, Date d) {
        return nearOnset(cal, d, false);
    }

    /**
     * Return true if the given Date is in the 1 hour window AFTER the
     * change from STD to DST for the given Calendar.
     */
    static final boolean justAfterOnset(Calendar cal, Date d) {
        return nearOnset(cal, d, true);
    }

    /**
     * Return true if the given Date is in the 1 hour (or whatever the
     * DST savings is) window before or after the onset of DST.
     */
    static boolean nearOnset(Calendar cal, Date d, boolean after) {
        cal.setTime(d);
        if ((cal.get(Calendar.DST_OFFSET) == 0) == after) {
            return false;
        }
        int delta;
        try {
            delta = ((SimpleTimeZone) cal.getTimeZone()).getDSTSavings();
        } catch (ClassCastException e) {
            delta = 60*60*1000; // One hour as ms
        }
        cal.setTime(new Date(d.getTime() + (after ? -delta : delta)));
        return (cal.get(Calendar.DST_OFFSET) == 0) == after;
    }

    static String escape(String s) {
        StringBuffer buf = new StringBuffer();
        for (int i=0; i<s.length(); ++i) {
            char c = s.charAt(i);
            if (c < '\u0080') buf.append(c);
            else {
                buf.append("\\u");
                if (c < '\u1000') {
                    buf.append('0');
                    if (c < '\u0100') {
                        buf.append('0');
                        if (c < '\u0010') {
                            buf.append('0');
                        }
                    }
                }
                buf.append(Integer.toHexString(c));
            }
        }
        return buf.toString();
    }

    /**
     * Remove quoted elements from a pattern.  E.g., change "hh:mm 'o''clock'"
     * to "hh:mm ?".  All quoted elements are replaced by one or more '?'
     * characters.
     */
    static String dequotePattern(String pat) {
        StringBuffer out = new StringBuffer();
        boolean inQuote = false;
        for (int i=0; i<pat.length(); ++i) {
            char ch = pat.charAt(i);
            if (ch == '\'') {
                if ((i+1)<pat.length()
                    && pat.charAt(i+1) == '\'') {
                    // Handle "''"
                    out.append('?');
                    ++i;
                } else {
                    inQuote = !inQuote;
                    if (inQuote) {
                        out.append('?');
                    }
                }
            } else if (!inQuote) {
                out.append(ch);
            }
        }
        return out.toString();
    }

    static Date generateDate() {
        double a = (RANDOM.nextLong() & 0x7FFFFFFFFFFFFFFFL ) /
            ((double)0x7FFFFFFFFFFFFFFFL);

        // Now 'a' ranges from 0..1; scale it to range from 0 to 8000 years
        a *= 8000;

        // Range from (4000-1970) BC to (8000-1970) AD
        a -= 4000;

        // Now scale up to ms
        a *= 365.25 * 24 * 60 * 60 * 1000;

        return new Date((long)a);
    }
}

//eof
