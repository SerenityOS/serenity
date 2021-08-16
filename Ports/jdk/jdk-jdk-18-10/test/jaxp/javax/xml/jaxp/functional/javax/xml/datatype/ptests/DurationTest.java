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

import static javax.xml.datatype.DatatypeConstants.DAYS;
import static javax.xml.datatype.DatatypeConstants.HOURS;
import static javax.xml.datatype.DatatypeConstants.MINUTES;
import static javax.xml.datatype.DatatypeConstants.MONTHS;
import static javax.xml.datatype.DatatypeConstants.SECONDS;
import static javax.xml.datatype.DatatypeConstants.YEARS;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.function.Function;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow javax.xml.datatype.ptests.DurationTest
 * @run testng/othervm javax.xml.datatype.ptests.DurationTest
 * @summary Class containing the test cases for Duration.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class DurationTest {

    private DatatypeFactory datatypeFactory;

    /*
     * Setup.
     */
    @BeforeClass
    public void setup() throws DatatypeConfigurationException {
        datatypeFactory = DatatypeFactory.newInstance();
    }

    @DataProvider(name = "legal-number-duration")
    public Object[][] getLegalNumberDuration() {
        return new Object[][] {
                // is positive, year, month, day, hour, minute, second
                { true, 1, 1, 1, 1, 1, 1 },
                { false, 1, 1, 1, 1, 1, 1 },
                { true, 1, 0, 0, 0, 0, 0 },
                { false, 1, 0, 0, 0, 0, 0 }
        };
    }

    /*
     * Test for constructor Duration(boolean isPositive,int years,int months,
     * int days,int hours,int minutes,int seconds).
     */
    @Test(dataProvider = "legal-number-duration")
    public void checkNumberDurationPos(boolean isPositive, int years, int months, int days, int hours, int minutes, int seconds) {
        datatypeFactory.newDuration(isPositive, years, months, days, hours, minutes, seconds);
    }

    @DataProvider(name = "illegal-number-duration")
    public Object[][] getIllegalNumberDuration() {
        return new Object[][] {
                // is positive, year, month, day, hour, minute, second
                { true, 1, 1, -1, 1, 1, 1 },
                { false, 1, 1, -1, 1, 1, 1 },
                { true, undef, undef, undef, undef, undef, undef },
                { false, undef, undef, undef, undef, undef, undef }
        };
    }

    /*
     * Test for constructor Duration(boolean isPositive,int years,int months,
     * int days,int hours,int minutes,int seconds), if any of the fields is
     * negative should throw IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class, dataProvider = "illegal-number-duration")
    public void checkDurationNumberNeg(boolean isPositive, int years, int months, int days, int hours, int minutes, int seconds) {
        datatypeFactory.newDuration(isPositive, years, months, days, hours, minutes, seconds);
    }

    @DataProvider(name = "legal-bigint-duration")
    public Object[][] getLegalBigIntegerDuration() {
        return new Object[][] {
                // is positive, year, month, day, hour, minute, second
                { true, zero, zero, zero, zero, zero, new BigDecimal(zero) },
                { false, zero, zero, zero, zero, zero, new BigDecimal(zero) },
                { true, one, one, one, one, one, new BigDecimal(one) },
                { false, one, one, one, one, one, new BigDecimal(one) },
                { true, null, null, null, null, null, new BigDecimal(one) },
                { false, null, null, null, null, null, new BigDecimal(one) } };
    }

    /*
     * Test for constructor Duration(boolean isPositive,BigInteger
     * years,BigInteger months, BigInteger days,BigInteger hours,BigInteger
     * minutes,BigDecimal seconds).
     */
    @Test(dataProvider = "legal-bigint-duration")
    public void checkBigIntegerDurationPos(boolean isPositive, BigInteger years, BigInteger months, BigInteger days, BigInteger hours, BigInteger minutes,
            BigDecimal seconds) {
        datatypeFactory.newDuration(isPositive, years, months, days, hours, minutes, seconds);
    }

    @DataProvider(name = "illegal-bigint-duration")
    public Object[][] getIllegalBigIntegerDuration() {
        return new Object[][] {
                // is positive, year, month, day, hour, minute, second
                { true, null, null, null, null, null, null },
                { false, null, null, null, null, null, null }
        };
    }

    /*
     * Test for constructor Duration(boolean isPositive,BigInteger
     * years,BigInteger months, BigInteger days,BigInteger hours,BigInteger
     * minutes,BigDecimal seconds), if all the fields are null should throw
     * IllegalArgumentException.
     */
    @Test(expectedExceptions = IllegalArgumentException.class, dataProvider = "illegal-bigint-duration")
    public void checkBigIntegerDurationNeg(boolean isPositive, BigInteger years, BigInteger months, BigInteger days, BigInteger hours, BigInteger minutes,
            BigDecimal seconds) {
        datatypeFactory.newDuration(isPositive, years, months, days, hours, minutes, seconds);
    }

    @DataProvider(name = "legal-millisec-duration")
    public Object[][] getLegalMilliSecondDuration() {
        return new Object[][] { { 1000000 }, { 0 }, { Long.MAX_VALUE }, { Long.MIN_VALUE }

        };
    }

    /*
     * Test for constructor Duration(long durationInMilliSeconds)
     */
    @Test(dataProvider = "legal-millisec-duration")
    public void checkMilliSecondDuration(long millisec) {
        datatypeFactory.newDuration(millisec);
    }

    @DataProvider(name = "legal-lexical-duration")
    public Object[][] getLegalLexicalDuration() {
        return new Object[][] { { "P1Y1M1DT1H1M1S" }, { "-P1Y1M1DT1H1M1S" } };
    }

    /*
     * Test for constructor Duration(java.lang.String lexicalRepresentation)
     */
    @Test(dataProvider = "legal-lexical-duration")
    public void checkLexicalDurationPos(String lexRepresentation) {
        datatypeFactory.newDuration(lexRepresentation);
    }

    @DataProvider(name = "illegal-lexical-duration")
    public Object[][] getIllegalLexicalDuration() {
        return new Object[][] {
                { null },
                { "P1Y1M1DT1H1M1S " },
                { " P1Y1M1DT1H1M1S" },
                { "X1Y1M1DT1H1M1S" },
                { "" },
                { "P1Y2MT" } // The designator 'T' shall be absent if all of the time items are absent in "PnYnMnDTnHnMnS"
        };
    }

    /*
     * Test for constructor Duration(java.lang.String lexicalRepresentation),
     * null should throw NullPointerException, invalid lex should throw
     * IllegalArgumentException
     */
    @Test(expectedExceptions = { NullPointerException.class, IllegalArgumentException.class }, dataProvider = "illegal-lexical-duration")
    public void checkLexicalDurationNeg(String lexRepresentation) {
        datatypeFactory.newDuration(lexRepresentation);
    }

    @DataProvider(name = "equal-duration")
    public Object[][] getEqualDurations() {
        return new Object[][] { { "P1Y1M1DT1H1M1S", "P1Y1M1DT1H1M1S" } };
    }

    /*
     * Test for compare() both durations valid and equal.
     */
    @Test(dataProvider = "equal-duration")
    public void checkDurationEqual(String lexRepresentation1, String lexRepresentation2) {
        Duration duration1 = datatypeFactory.newDuration(lexRepresentation1);
        Duration duration2 = datatypeFactory.newDuration(lexRepresentation2);
        assertTrue(duration1.equals(duration2));
    }

    @DataProvider(name = "greater-duration")
    public Object[][] getGreaterDuration() {
        return new Object[][] {
                { "P1Y1M1DT1H1M2S", "P1Y1M1DT1H1M1S" },
                { "P1Y1M1DT1H1M1S", "-P1Y1M1DT1H1M2S" },
                { "P1Y1M1DT1H1M2S", "-P1Y1M1DT1H1M1S" },
                { "-P1Y1M1DT1H1M1S", "-P1Y1M1DT1H1M2S" }, };
    }

    /*
     * Test for compare() both durations valid and lhs > rhs.
     */
    @Test(dataProvider = "greater-duration")
    public void checkDurationCompare(String lexRepresentation1, String lexRepresentation2) {
        Duration duration1 = datatypeFactory.newDuration(lexRepresentation1);
        Duration duration2 = datatypeFactory.newDuration(lexRepresentation2);
        assertTrue(duration1.compare(duration2) == DatatypeConstants.GREATER);
    }

    @DataProvider(name = "not-equal-duration")
    public Object[][] getNotEqualDurations() {
        return new Object[][] {
                { "P1Y1M1DT1H1M1S", "-P1Y1M1DT1H1M1S" },
                { "P2Y1M1DT1H1M1S", "P1Y1M1DT1H1M1S" } };
    }

    /*
     * Test for equals() both durations valid and lhs not equals rhs.
     */
    @Test(dataProvider = "not-equal-duration")
    public void checkDurationNotEqual(String lexRepresentation1, String lexRepresentation2) {
        Duration duration1 = datatypeFactory.newDuration(lexRepresentation1);
        Duration duration2 = datatypeFactory.newDuration(lexRepresentation2);
        Assert.assertNotEquals(duration1, duration2);
    }

    @DataProvider(name = "duration-sign")
    public Object[][] getDurationAndSign() {
        return new Object[][] {
                { "P0Y0M0DT0H0M0S", 0 },
                { "P1Y0M0DT0H0M0S", 1 },
                { "-P1Y0M0DT0H0M0S", -1 } };
    }

    /*
     * Test for Duration.getSign().
     */
    @Test(dataProvider = "duration-sign")
    public void checkDurationSign(String lexRepresentation, int sign) {
        Duration duration = datatypeFactory.newDuration(lexRepresentation);
        assertEquals(duration.getSign(), sign);
    }

    /*
     * Test for Duration.negate().
     */
    @Test
    public void checkDurationNegate() {
        Duration durationPos = datatypeFactory.newDuration("P1Y0M0DT0H0M0S");
        Duration durationNeg = datatypeFactory.newDuration("-P1Y0M0DT0H0M0S");

        assertEquals(durationPos.negate(), durationNeg);
        assertEquals(durationNeg.negate(), durationPos);
        assertEquals(durationPos.negate().negate(), durationPos);

    }

    /*
     * Test for Duration.isShorterThan(Duration) and
     * Duration.isLongerThan(Duration).
     */
    @Test
    public void checkDurationShorterLonger() {
        Duration shorter = datatypeFactory.newDuration("P1Y1M1DT1H1M1S");
        Duration longer = datatypeFactory.newDuration("P2Y1M1DT1H1M1S");

        assertTrue(shorter.isShorterThan(longer));
        assertFalse(longer.isShorterThan(shorter));
        assertFalse(shorter.isShorterThan(shorter));

        assertTrue(longer.isLongerThan(shorter));
        assertFalse(shorter.isLongerThan(longer));
        assertFalse(shorter.isLongerThan(shorter));
    }

    /*
     * Test for Duration.isSet().
     */
    @Test
    public void checkDurationIsSet() {
        Duration duration1 = datatypeFactory.newDuration(true, 1, 1, 1, 1, 1, 1);
        Duration duration2 = datatypeFactory.newDuration(true, 0, 0, 0, 0, 0, 0);

        assertTrue(duration1.isSet(YEARS));
        assertTrue(duration1.isSet(MONTHS));
        assertTrue(duration1.isSet(DAYS));
        assertTrue(duration1.isSet(HOURS));
        assertTrue(duration1.isSet(MINUTES));
        assertTrue(duration1.isSet(SECONDS));

        assertTrue(duration2.isSet(YEARS));
        assertTrue(duration2.isSet(MONTHS));
        assertTrue(duration2.isSet(DAYS));
        assertTrue(duration2.isSet(HOURS));
        assertTrue(duration2.isSet(MINUTES));
        assertTrue(duration2.isSet(SECONDS));

        Duration duration66 = datatypeFactory.newDuration(true, null, null, zero, null, null, null);
        assertFalse(duration66.isSet(YEARS));
        assertFalse(duration66.isSet(MONTHS));
        assertFalse(duration66.isSet(HOURS));
        assertFalse(duration66.isSet(MINUTES));
        assertFalse(duration66.isSet(SECONDS));

        Duration duration3 = datatypeFactory.newDuration("P1D");
        assertFalse(duration3.isSet(YEARS));
        assertFalse(duration3.isSet(MONTHS));
        assertFalse(duration3.isSet(HOURS));
        assertFalse(duration3.isSet(MINUTES));
        assertFalse(duration3.isSet(SECONDS));
    }

    /*
     * Test Duration.isSet(Field) throws NPE if the field parameter is null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void checkDurationIsSetNeg() {
        Duration duration = datatypeFactory.newDuration(true, 0, 0, 0, 0, 0, 0);
        duration.isSet(null);
    }

    /*
     * Test for -getField(DatatypeConstants.Field) DatatypeConstants.Field is
     * null - throws NPE.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void checkDurationGetFieldNeg() {
        Duration duration67 = datatypeFactory.newDuration("P1Y1M1DT1H1M1S");
        duration67.getField(null);
    }

    @DataProvider(name = "duration-fields")
    public Object[][] getDurationAndFields() {
        return new Object[][] {
                { "P1Y1M1DT1H1M1S", one, one, one, one, one, new BigDecimal(one) },
                { "PT1M", null, null, null, null, one, null },
                { "P1M", null, one, null, null, null, null } };
    }

    /*
     * Test for Duration.getField(DatatypeConstants.Field).
     */
    @Test(dataProvider = "duration-fields")
    public void checkDurationGetField(String lexRepresentation, BigInteger years, BigInteger months, BigInteger days, BigInteger hours, BigInteger minutes,
            BigDecimal seconds) {
        Duration duration = datatypeFactory.newDuration(lexRepresentation);

        assertEquals(duration.getField(YEARS), years);
        assertEquals(duration.getField(MONTHS), months);
        assertEquals(duration.getField(DAYS), days);
        assertEquals(duration.getField(HOURS), hours);
        assertEquals(duration.getField(MINUTES), minutes);
        assertEquals(duration.getField(SECONDS), seconds);
    }

    @DataProvider(name = "number-string")
    public Object[][] getNumberAndString() {
        return new Object[][] {
                // is positive, year, month, day, hour, minute, second, lexical
                { true, 1, 1, 1, 1, 1, 1, "P1Y1M1DT1H1M1S" },
                { false, 1, 1, 1, 1, 1, 1, "-P1Y1M1DT1H1M1S" },
                { true, 0, 0, 0, 0, 0, 0, "P0Y0M0DT0H0M0S" },
                { false, 0, 0, 0, 0, 0, 0, "P0Y0M0DT0H0M0S" }
        };
    }

    /*
     * Test for - toString().
     */
    @Test(dataProvider = "number-string")
    public void checkDurationToString(boolean isPositive, int years, int months, int days, int hours, int minutes, int seconds, String lexical) {
        Duration duration = datatypeFactory.newDuration(isPositive,  years,  months,  days,  hours,  minutes,  seconds);
        assertEquals(duration.toString(), lexical);

        assertEquals(datatypeFactory.newDuration(duration.toString()), duration);
    }

    @DataProvider(name = "duration-field")
    public Object[][] getDurationAndField() {
        Function<Duration, Integer> getyears = duration -> duration.getYears();
        Function<Duration, Integer> getmonths = duration -> duration.getMonths();
        Function<Duration, Integer> getdays = duration -> duration.getDays();
        Function<Duration, Integer> gethours = duration -> duration.getHours();
        Function<Duration, Integer> getminutes = duration -> duration.getMinutes();
        Function<Duration, Integer> getseconds = duration -> duration.getSeconds();
        return new Object[][] {
                { "P1Y1M1DT1H1M1S", getyears, 1 },
                { "P1M1DT1H1M1S", getyears, 0 },
                { "P1Y1M1DT1H1M1S", getmonths, 1 },
                { "P1Y1DT1H1M1S", getmonths, 0 },
                { "P1Y1M1DT1H1M1S", getdays, 1 },
                { "P1Y1MT1H1M1S", getdays, 0 },
                { "P1Y1M1DT1H1M1S", gethours, 1 },
                { "P1Y1M1DT1M1S", gethours, 0 },
                { "P1Y1M1DT1H1M1S", getminutes, 1 },
                { "P1Y1M1DT1H1S", getminutes, 0 },
                { "P1Y1M1DT1H1M1S", getseconds, 1 },
                { "P1Y1M1DT1H1M", getseconds, 0 },
                { "P1Y1M1DT1H1M100000000S", getseconds, 100000000 }, };
    }

    /*
     * Test for Duration.getYears(), getMonths(), etc.
     */
    @Test(dataProvider = "duration-field")
    public void checkDurationGetOneField(String lexRepresentation, Function<Duration, Integer> getter, int value) {
        Duration duration = datatypeFactory.newDuration(lexRepresentation);
        assertEquals(getter.apply(duration).intValue(), value);
    }

    /*
     * Test for - getField(SECONDS)
     */
    @Test
    public void checkDurationGetSecondsField() {
        Duration duration85 = datatypeFactory.newDuration("P1Y1M1DT1H1M100000000S");
        assertEquals((duration85.getField(SECONDS)).intValue(), 100000000);
    }

    /*
     * getTimeInMillis(java.util.Calendar startInstant) returns milliseconds
     * between startInstant and startInstant plus this Duration.
     */
    @Test
    public void checkDurationGetTimeInMillis() {
        Duration duration86 = datatypeFactory.newDuration("PT1M1S");
        Calendar calendar86 = Calendar.getInstance();
        assertEquals(duration86.getTimeInMillis(calendar86), 61000);
    }

    /*
     * getTimeInMillis(java.util.Calendar startInstant) returns milliseconds
     * between startInstant and startInstant plus this Duration throws NPE if
     * startInstant parameter is null.
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void checkDurationGetTimeInMillisNeg() {
        Duration duration87 = datatypeFactory.newDuration("PT1M1S");
        Calendar calendar87 = null;
        duration87.getTimeInMillis(calendar87);
    }

    @DataProvider(name = "duration-for-hash")
    public Object[][] getDurationsForHash() {
        return new Object[][] {
                { "P1Y1M1DT1H1M1S", "P1Y1M1DT1H1M1S" },
                { "P1D", "PT24H" },
                { "PT1H", "PT60M" },
                { "PT1M", "PT60S" },
                { "P1Y", "P12M" } };
    }

    /*
     * Test for Duration.hashcode(). hashcode() should return same value for
     * some equal durations.
     */
    @Test(dataProvider = "duration-for-hash")
    public void checkDurationHashCode(String lexRepresentation1, String lexRepresentation2) {
        Duration duration1 = datatypeFactory.newDuration(lexRepresentation1);
        Duration duration2 = datatypeFactory.newDuration(lexRepresentation2);
        int hash1 = duration1.hashCode();
        int hash2 = duration2.hashCode();
        assertTrue(hash1 == hash2, " generated hash1 : " + hash1 + " generated hash2 : " + hash2);
    }

    @DataProvider(name = "duration-for-add")
    public Object[][] getDurationsForAdd() {
        return new Object[][] {
                // initVal, addVal, resultVal
                { "P1Y1M1DT1H1M1S", "P1Y1M1DT1H1M1S", "P2Y2M2DT2H2M2S" },
                { "P1Y1M1DT1H1M1S", "-P1Y1M1DT1H1M1S", "P0Y0M0DT0H0M0S" },
                { "-P1Y1M1DT1H1M1S", "-P1Y1M1DT1H1M1S", "-P2Y2M2DT2H2M2S" }, };
    }

    /*
     * Test for add(Duration rhs).
     */
    @Test(dataProvider = "duration-for-add")
    public void checkDurationAdd(String initVal, String addVal, String result) {
        Duration durationInit = datatypeFactory.newDuration(initVal);
        Duration durationAdd = datatypeFactory.newDuration(addVal);
        Duration durationResult = datatypeFactory.newDuration(result);

        assertEquals(durationInit.add(durationAdd), durationResult);
    }

    @DataProvider(name = "duration-for-addneg")
    public Object[][] getDurationsForAddNeg() {
        return new Object[][] {
                // initVal, addVal
                { "P1Y1M1DT1H1M1S", null },
                { "P1Y", "-P1D" },
                { "-P1Y", "P1D" }, };
    }

    /*
     * Test for add(Duration rhs) 'rhs' is null , should throw NPE. "1 year" +
     * "-1 day" or "-1 year" + "1 day" should throw IllegalStateException
     */
    @Test(expectedExceptions = { NullPointerException.class, IllegalStateException.class }, dataProvider = "duration-for-addneg")
    public void checkDurationAddNeg(String initVal, String addVal) {
        Duration durationInit = datatypeFactory.newDuration(initVal);
        Duration durationAdd = addVal == null ? null : datatypeFactory.newDuration(addVal);

        durationInit.add(durationAdd);
    }

    /*
     * Test Duration#compare(Duration duration) with large durations.
     *
     * Bug # 4972785 UnsupportedOperationException is expected
     *
     */
    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void checkDurationCompareLarge() {
        String duration1Lex = "P100000000000000000000D";
        String duration2Lex = "PT2400000000000000000000H";

        Duration duration1 = datatypeFactory.newDuration(duration1Lex);
        Duration duration2 = datatypeFactory.newDuration(duration2Lex);
        duration1.compare(duration2);

    }

    /*
     * Test Duration#getXMLSchemaType().
     *
     * Bug # 5049544 Duration.getXMLSchemaType shall return the correct result
     *
     */
    @Test
    public void checkDurationGetXMLSchemaType() {
        // DURATION
        Duration duration = datatypeFactory.newDuration("P1Y1M1DT1H1M1S");
        QName duration_xmlSchemaType = duration.getXMLSchemaType();
        assertEquals(duration_xmlSchemaType, DatatypeConstants.DURATION, "Expected DatatypeConstants.DURATION, returned " + duration_xmlSchemaType.toString());

        // DURATION_DAYTIME
        Duration duration_dayTime = datatypeFactory.newDuration("P1DT1H1M1S");
        QName duration_dayTime_xmlSchemaType = duration_dayTime.getXMLSchemaType();
        assertEquals(duration_dayTime_xmlSchemaType, DatatypeConstants.DURATION_DAYTIME, "Expected DatatypeConstants.DURATION_DAYTIME, returned "
                + duration_dayTime_xmlSchemaType.toString());

        // DURATION_YEARMONTH
        Duration duration_yearMonth = datatypeFactory.newDuration("P1Y1M");
        QName duration_yearMonth_xmlSchemaType = duration_yearMonth.getXMLSchemaType();
        assertEquals(duration_yearMonth_xmlSchemaType, DatatypeConstants.DURATION_YEARMONTH, "Expected DatatypeConstants.DURATION_YEARMONTH, returned "
                + duration_yearMonth_xmlSchemaType.toString());

    }


    private final int undef = DatatypeConstants.FIELD_UNDEFINED;
    private final BigInteger zero = BigInteger.ZERO;
    private final BigInteger one = BigInteger.ONE;

}
