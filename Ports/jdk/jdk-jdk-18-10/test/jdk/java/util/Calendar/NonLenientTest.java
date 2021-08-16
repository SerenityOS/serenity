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

/*
 * @test
 * @bug 4147269 4266783 4726030
 * @summary Make sure that validation is adequate in non-lenient mode.
 * @library /java/text/testlib
 */

import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import static java.util.Calendar.*;

public class NonLenientTest extends IntlTest {

    public static void main(String[] args) throws Exception {
        Locale reservedLocale = Locale.getDefault();
        TimeZone reservedTimeZone = TimeZone.getDefault();
        try {
            Locale.setDefault(Locale.US);
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"));
            new NonLenientTest().run(args);
        } finally {
            // restore the reserved locale and time zone
            Locale.setDefault(reservedLocale);
            TimeZone.setDefault(reservedTimeZone);
        }
    }

    public void TestValidationInNonLenient() {
        Koyomi cal = getNonLenient();

        // 2003 isn't a leap year.
        cal.set(2003, FEBRUARY, 29);
        validate(cal, "2003/2/29");

        // October has only 31 days.
        cal.set(2003, OCTOBER, 32);
        validate(cal, "2003/10/32");

        // 2003/10/31 is Friday.
        cal.set(2003, OCTOBER, 31);
        cal.set(DAY_OF_WEEK, SUNDAY);
        validate(cal, "2003/10/31 SUNDAY");

        // 2003/10/31 is the 304th day of the year.
        cal.clear();
        cal.set(DAY_OF_YEAR, 1);
        cal.set(2003, OCTOBER, 31);
        validate(cal, "2003/10/31 DAY_OF_YEAR=1");

        // 2003/10 isn't the 1st week of the year.
        cal.clear();
        cal.set(YEAR, 2003);
        cal.set(WEEK_OF_YEAR, 1);
        cal.set(MONTH, OCTOBER);
        validate(cal, "2003/10 WEEK_OF_YEAR=1");

        // The 1st week of 2003 doesn't have Monday.
        cal.clear();
        cal.set(YEAR, 2003);
        cal.set(WEEK_OF_YEAR, 1);
        cal.set(DAY_OF_WEEK, MONDAY);
        validate(cal, "2003 WEEK_OF_YEAR=1 MONDAY.");

        // 2003 has 52 weeks.
        cal.clear();
        cal.set(YEAR, 2003);
        cal.set(WEEK_OF_YEAR, 53);
        cal.set(DAY_OF_WEEK, WEDNESDAY);
        validate(cal, "2003 WEEK_OF_YEAR=53");

        /*
         * These test cases assume incompatible behavior in Tiger as
         * the result of the validation bug fixes. However, it looks
         * like we have to allow applications to set ZONE_OFFSET and
         * DST_OFFSET values to modify the time zone offsets given by
         * a TimeZone. The definition of non-leniency for time zone
         * offsets is somewhat vague.  (See 6231602)
         *
         * The following test cases are now disabled.

        // America/Los_Angeles is GMT-08:00
        cal.clear();
        cal.set(2003, OCTOBER, 31);
        cal.set(ZONE_OFFSET, 0);
        validate(cal, "ZONE_OFFSET=0:00 in America/Los_Angeles");

        // 2003/10/31 shouldn't be in DST.
        cal.clear();
        cal.set(2003, OCTOBER, 31);
        cal.set(DST_OFFSET, 60*60*1000);
        validate(cal, "2003/10/31 DST_OFFSET=1:00 in America/Los_Angeles");

        */
    }

    /**
     * 4266783: java.util.GregorianCalendar: incorrect validation in non-lenient
     */
    public void Test4266783() {
        Koyomi cal = getNonLenient();
        // 2003/1 has up to 5 weeks.
        cal.set(YEAR, 2003);
        cal.set(MONTH, JANUARY);
        cal.set(WEEK_OF_MONTH, 6);
        cal.set(DAY_OF_WEEK, SUNDAY);
        validate(cal, "6th Sunday in Jan 2003");
    }

    /**
     * 4726030: GregorianCalendar doesn't check invalid dates in non-lenient
     */
    public void Test4726030() {
        Koyomi cal = getNonLenient();
        // Default year is 1970 in GregorianCalendar which isn't a leap year.
        cal.set(MONTH, FEBRUARY);
        cal.set(DAY_OF_MONTH, 29);
        validate(cal, "2/29 in the default year 1970");
    }

    /**
     * 4147269: java.util.GregorianCalendar.computeTime() works wrong when lenient is false
     */
    public void Test4147269() {
        Koyomi calendar = getNonLenient();
        Date date = (new GregorianCalendar(1996, 0, 3)).getTime();

        for (int field = 0; field < FIELD_COUNT; field++) {
            calendar.setTime(date);
            int max = calendar.getActualMaximum(field);
            int value = max + 1;
            calendar.set(field, value);
            try {
                calendar.computeTime(); // call method under test
                errln("Test failed with field " + Koyomi.getFieldName(field)
                        + "\n\tdate before:  " + date
                        + "\n\tdate after:   " + calendar.getTime()
                        + "\n\tvalue: " + value + "  (max = " + max + ")");
            } catch (IllegalArgumentException e) {
            }
        }

        for (int field = 0; field < FIELD_COUNT; field++) {
            calendar.setTime(date);
            int min = calendar.getActualMinimum(field);
            int value = min - 1;
            calendar.set(field, value);
            try {
                calendar.computeTime(); // call method under test
                errln("Test failed with field " + Koyomi.getFieldName(field)
                        + "\n\tdate before:  " + date
                        + "\n\tdate after:   " + calendar.getTime()
                        + "\n\tvalue: " + value + "  (min = " + min + ")");
            } catch (IllegalArgumentException e) {
            }
        }
    }

    void validate(Koyomi cal, String desc) {
        int[] originalFields = cal.getFields();
        int setFields = cal.getSetStateFields();

        try {
            cal.complete();
            errln(desc + " should throw IllegalArgumentException in non-lenient.");
        } catch (IllegalArgumentException e) {
        }

        // The code below will be executed with the -nothrow option

        // In non-lenient, calendar field values that have beeb set by
        // user shouldn't be modified.
        int[] afterFields = cal.getFields();
        for (int i = 0; i < FIELD_COUNT; i++) {
            if (cal.isSet(i) && originalFields[i] != afterFields[i]) {
                errln("    complete() modified fields[" + Koyomi.getFieldName(i) + "] got "
                        + afterFields[i] + ", expected " + originalFields[i]);
            }
        }
        // In non-lenient, set state of fields shouldn't be modified.
        int afterSetFields = cal.getSetStateFields();
        if (setFields != afterSetFields) {
            errln("    complate() modified set states: before 0x" + toHex(setFields)
                    + ", after 0x" + toHex(afterSetFields));
        }
    }

    static Koyomi getNonLenient() {
        Koyomi cal = new Koyomi();
        cal.clear();
        cal.setLenient(false);
        return cal;
    }

    static String toHex(int x) {
        return Integer.toHexString(x);
    }
}
