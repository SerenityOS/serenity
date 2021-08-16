/*
 * Copyright (c) 1999, 2016, Oracle and/or its affiliates. All rights reserved.
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

package javax.xml.datatype.ptests;

import static java.util.Calendar.HOUR;
import static java.util.Calendar.MINUTE;
import static java.util.Calendar.YEAR;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 5049592 5041845 5048932 5064587 5040542 5049531 5049528
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.datatype.ptests.XMLGregorianCalendarTest
 * @run testng/othervm javax.xml.datatype.ptests.XMLGregorianCalendarTest
 * @summary Class containing the test cases for XMLGregorianCalendar
 */
@Listeners({jaxp.library.BasePolicy.class})
public class XMLGregorianCalendarTest {

    private DatatypeFactory datatypeFactory;

    @BeforeClass
    public void setup() throws DatatypeConfigurationException {
        datatypeFactory = DatatypeFactory.newInstance();
    }

    @DataProvider(name = "valid-milliseconds")
    public Object[][] getValidMilliSeconds() {
        return new Object[][] { { 0 }, { 1 }, { 2 }, { 16 }, { 1000 }   };
    }

    /*
     * Test DatatypeFactory.newXMLGregorianCalendar(..) with milliseconds > 1.
     *
     * Bug # 5049592
     *
     */
    @Test(dataProvider = "valid-milliseconds")
    public void checkNewCalendar(int ms) {
        // valid milliseconds
        XMLGregorianCalendar calendar = datatypeFactory.newXMLGregorianCalendar(2004, // year
                6, // month
                2, // day
                19, // hour
                20, // minute
                59, // second
                ms, // milliseconds
                840 // timezone
                );
        // expected success

        assertEquals(calendar.getMillisecond(), ms);
    }

    /*
     * Test DatatypeFactory.newXMLGregorianCalendarTime(..).
     *
     * Bug # 5049592
     */
    @Test(dataProvider = "valid-milliseconds")
    public void checkNewTime(int ms) {
        // valid milliseconds
        XMLGregorianCalendar calendar2 = datatypeFactory.newXMLGregorianCalendarTime(19, // hour
                20, // minute
                59, // second
                ms, // milliseconds
                840 // timezone
                );
        // expected success

        assertEquals(calendar2.getMillisecond(), ms);
    }

    @DataProvider(name = "invalid-milliseconds")
    public Object[][] getInvalidMilliSeconds() {
        return new Object[][] { { -1 }, { 1001 } };
    }

    /*
     * Test DatatypeFactory.newXMLGregorianCalendar(..).
     *
     * Bug # 5049592 IllegalArgumentException is thrown if milliseconds < 0 or >
     * 1001.
     *
     */
    @Test(expectedExceptions = IllegalArgumentException.class, dataProvider = "invalid-milliseconds")
    public void checkNewCalendarNeg(int milliseconds) {
        // invalid milliseconds
        datatypeFactory.newXMLGregorianCalendar(2004, // year
                6, // month
                2, // day
                19, // hour
                20, // minute
                59, // second
                milliseconds, // milliseconds
                840 // timezone
                );
    }

    /*
     * Test DatatypeFactory.newXMLGregorianCalendarTime(..).
     *
     * Bug # 5049592 IllegalArgumentException is thrown if milliseconds < 0 or >
     * 1001.
     *
     */
    @Test(expectedExceptions = IllegalArgumentException.class, dataProvider = "invalid-milliseconds")
    public void checkNewTimeNeg(int milliseconds) {
        // invalid milliseconds
        datatypeFactory.newXMLGregorianCalendarTime(19, // hour
                20, // minute
                59, // second
                milliseconds, // milliseconds
                840 // timezone
                );
    }

    @DataProvider(name = "data-for-add")
    public Object[][] getDataForAdd() {
        return new Object[][] {
                //calendar1, calendar2, duration
                { "1999-12-31T00:00:00Z", "2000-01-01T00:00:00Z", "P1D" },
                { "2000-12-31T00:00:00Z", "2001-01-01T00:00:00Z", "P1D" },
                { "1998-12-31T00:00:00Z", "1999-01-01T00:00:00Z", "P1D" },
                { "2001-12-31T00:00:00Z", "2002-01-01T00:00:00Z", "P1D" },
                { "2003-04-11T00:00:00Z", "2003-04-12T00:00:00Z", "P1D" },
                { "2003-04-11T00:00:00Z", "2003-04-14T00:00:00Z", "P3D" },
                { "2003-04-30T00:00:00Z", "2003-05-01T00:00:00Z", "P1D" },
                { "2003-02-28T00:00:00Z", "2003-03-01T00:00:00Z", "P1D" },
                { "2000-02-29T00:00:00Z", "2000-03-01T00:00:00Z", "P1D" },
                { "2000-02-28T00:00:00Z", "2000-02-29T00:00:00Z", "P1D" },
                { "1998-01-11T00:00:00Z", "1998-04-11T00:00:00Z", "P90D" },
                { "1999-05-11T00:00:00Z", "2002-05-11T00:00:00Z", "P1096D" }};
    }

    /*
     * Test XMLGregorianCalendar.add(Duration).
     *
     */
    @Test(dataProvider = "data-for-add")
    public void checkAddDays(String cal1, String cal2, String dur) {

        XMLGregorianCalendar calendar1 = datatypeFactory.newXMLGregorianCalendar(cal1);
        XMLGregorianCalendar calendar2 = datatypeFactory.newXMLGregorianCalendar(cal2);

        Duration duration = datatypeFactory.newDuration(dur);

        XMLGregorianCalendar calendar1Clone = (XMLGregorianCalendar)calendar1.clone();

        calendar1Clone.add(duration);
        assertEquals(calendar1Clone, calendar2);

        calendar2.add(duration.negate());
        assertEquals(calendar2, calendar1);

    }

    @DataProvider(name = "gMonth")
    public Object[][] getGMonth() {
        return new Object[][] {
                { "2000-02" },
                { "2000-03" },
                { "2018-02" }};
    }
    /*
     * Test XMLGregorianCalendar#isValid(). for gMonth
     *
     * Bug # 5041845
     *
     */
    @Test(dataProvider = "gMonth")
    public void checkIsValid(String month) {

        XMLGregorianCalendar gMonth = datatypeFactory.newXMLGregorianCalendar(month);
        gMonth.setYear(null);
        Assert.assertTrue(gMonth.isValid(), gMonth.toString() + " should isValid");

    }

    @DataProvider(name = "lexical01")
    public Object[][] getLexicalRepresentForNormalize01() {
        return new Object[][] { { "2000-01-16T12:00:00Z" }, { "2000-01-16T12:00:00" } };
    }

    /*
     * Test XMLGregorianCalendar#normalize(...).
     *
     * Bug # 5048932 XMLGregorianCalendar.normalize works
     *
     */
    @Test(dataProvider = "lexical01")
    public void checkNormalize01(String lexical) {
        XMLGregorianCalendar lhs = datatypeFactory.newXMLGregorianCalendar(lexical);
        lhs.normalize();
    }

    @DataProvider(name = "lexical02")
    public Object[][] getLexicalRepresentForNormalize02() {
        return new Object[][] { { "2000-01-16T00:00:00.01Z" }, { "2000-01-16T00:00:00.01" }, { "13:20:00" } };
    }

    /*
     * Test XMLGregorianCalendar#normalize(...).
     *
     * Bug # 5064587 XMLGregorianCalendar.normalize shall not change timezone
     *
     */
    @Test(dataProvider = "lexical02")
    public void checkNormalize02(String lexical) {
        XMLGregorianCalendar orig = datatypeFactory.newXMLGregorianCalendar(lexical);
        XMLGregorianCalendar normalized = datatypeFactory.newXMLGregorianCalendar(lexical).normalize();

        assertEquals(normalized.getTimezone(), orig.getTimezone());
        assertEquals(normalized.getMillisecond(), orig.getMillisecond());
    }

    /*
     * Test XMLGregorianCalendar#toGregorianCalendar( TimeZone timezone, Locale
     * aLocale, XMLGregorianCalendar defaults)
     *
     * Bug # 5040542 the defaults XMLGregorianCalendar parameter shall take
     * effect
     *
     */
    @Test
    public void checkToGregorianCalendar01() {

        XMLGregorianCalendar time_16_17_18 = datatypeFactory.newXMLGregorianCalendar("16:17:18");
        XMLGregorianCalendar date_2001_02_03 = datatypeFactory.newXMLGregorianCalendar("2001-02-03");
        GregorianCalendar calendar = date_2001_02_03.toGregorianCalendar(null, null, time_16_17_18);

        int year = calendar.get(YEAR);
        int minute = calendar.get(MINUTE);

        assertTrue((year == 2001 && minute == 17), " expecting year == 2001, minute == 17" + ", result is year == " + year + ", minute == " + minute);


        calendar = time_16_17_18.toGregorianCalendar(null, null, date_2001_02_03);

        year = calendar.get(YEAR);
        minute = calendar.get(MINUTE);

        assertTrue((year == 2001 && minute == 17), " expecting year == 2001, minute == 17" + ", result is year == " + year + ", minute == " + minute);


        date_2001_02_03.setMinute(3);
        date_2001_02_03.setYear(null);

        XMLGregorianCalendar date_time = datatypeFactory.newXMLGregorianCalendar("2003-04-11T02:13:01Z");

        calendar = date_2001_02_03.toGregorianCalendar(null, null, date_time);

        year = calendar.get(YEAR);
        minute = calendar.get(MINUTE);
        int hour = calendar.get(HOUR);

        assertTrue((year == 2003 && hour == 2 && minute == 3), " expecting year == 2003, hour == 2, minute == 3" + ", result is year == " + year + ", hour == " + hour + ", minute == " + minute);


    }

    /*
     * Test XMLGregorianCalendar#toGregorianCalendar( TimeZone timezone, Locale
     * aLocale, XMLGregorianCalendar defaults) with the 'defaults' parameter
     * being null.
     *
     * Bug # 5049531 XMLGregorianCalendar.toGregorianCalendar(..) can accept
     * 'defaults' is null
     *
     */
    @Test
    public void checkToGregorianCalendar02() {

        XMLGregorianCalendar calendar = datatypeFactory.newXMLGregorianCalendar("2004-05-19T12:00:00+06:00");
        calendar.toGregorianCalendar(TimeZone.getDefault(), Locale.getDefault(), null);
    }

    @DataProvider(name = "calendar")
    public Object[][] getXMLGregorianCalendarData() {
        return new Object[][] {
                // year, month, day, hour, minute, second
                { 1970, 1, 1, 0, 0, 0 }, // DATETIME
                { 1970, 1, 1, undef, undef, undef }, // DATE
                { undef, undef, undef, 1, 0, 0 }, // TIME
                { 1970, 1, undef, undef, undef, undef }, // GYEARMONTH
                { undef, 1, 1, undef, undef, undef }, // GMONTHDAY
                { 1970, undef, undef, undef, undef, undef }, // GYEAR
                { undef, 1, undef, undef, undef, undef }, // GMONTH
                { undef, undef, 1, undef, undef, undef } // GDAY
        };
    }

    /*
     * Test XMLGregorianCalendar#toString()
     *
     * Bug # 5049528
     *
     */
    @Test(dataProvider = "calendar")
    public void checkToStringPos(final int year, final int month, final int day, final int hour, final int minute, final int second) {
        XMLGregorianCalendar calendar = datatypeFactory.newXMLGregorianCalendar(year, month, day, hour, minute, second, undef, undef);
        calendar.toString();
    }

    /*
     * Negative Test XMLGregorianCalendar#toString()
     *
     * Bug # 5049528 XMLGregorianCalendar.toString throws IllegalStateException
     * if all parameters are undef
     *
     */
    @Test(expectedExceptions = IllegalStateException.class)
    public void checkToStringNeg() {
        XMLGregorianCalendar calendar = datatypeFactory.newXMLGregorianCalendar(undef, undef, undef, undef, undef, undef, undef, undef);
        // expected to fail
        calendar.toString();
    }

    private final int undef = DatatypeConstants.FIELD_UNDEFINED;

}
