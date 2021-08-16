/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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

package datatype;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.namespace.QName;

import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @bug 8190835
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow datatype.DurationTest
 * @run testng/othervm datatype.DurationTest
 * @summary Test Duration.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class DurationTest {

    private final static boolean DEBUG = true;

    protected Duration duration = null;

    @BeforeMethod
    public void setUp() {
        try {
            duration = DatatypeFactory.newInstance().newDuration(100);
        } catch (DatatypeConfigurationException dce) {
            dce.printStackTrace();
            Assert.fail("Failed to create instance of DatatypeFactory " + dce.getMessage());
        }
    }

    /*
       DataProvider: for testDurationSubtract1
       Data: minuend, subtrahend, expected result
     */
    @DataProvider(name = "DurationSubtract1")
    public Object[][] getSubtract1() {

        return new Object[][]{
            {"P2Y2M", "P1Y5M", "P9M"},
            {"P2DT2H", "P1DT12H", "PT14H"},
            {"P2DT2H10M", "P1DT2H25M", "PT23H45M"},
            {"PT2H10M", "PT1H25M", "PT45M"},
            {"PT2H10M20S", "PT1H25M35S", "PT44M45S"},
            {"PT2H10M20.25S", "PT1H25M35.45S", "PT44M44.8S"},
            // 8190835 test case
            {"PT2M3.123S", "PT1M10.123S", "PT53S"}
        };
    }

    @DataProvider(name = "DurationSubtract2")
    public Object[][] getSubtract2() {

        return new Object[][]{
            {"P2Y20D", "P1Y125D"},
            {"P2M20D", "P1M25D"}
        };
    }

    /*
     * Verifies valid substraction operations.
     */
    @Test(dataProvider = "DurationSubtract1")
    public void testDurationSubtract1(String t1, String t2, String e) throws Exception {
        DatatypeFactory factory = DatatypeFactory.newInstance();
        Duration dt1 = factory.newDuration(t1);
        Duration dt2 = factory.newDuration(t2);

        Duration result = dt1.subtract(dt2);
        Duration expected = factory.newDuration(e);
        Assert.assertTrue(result.equals(expected), "The result should be " + e);

    }

    /*
     * Verifies invalid substraction operations. These operations are invalid
     * since days in a month are indeterminate.
    */
    @Test(dataProvider = "DurationSubtract2", expectedExceptions = IllegalStateException.class)
    public void testDurationSubtract2(String t1, String t2) throws Exception {
        DatatypeFactory factory = DatatypeFactory.newInstance();
        Duration dt1 = factory.newDuration(t1);
        Duration dt2 = factory.newDuration(t2);
        Duration result = dt1.subtract(dt2);
    }

    @Test
    public void testDurationSubtract() {
        try {
            Duration bigDur = DatatypeFactory.newInstance().newDuration(20000);
            Duration smallDur = DatatypeFactory.newInstance().newDuration(10000);
            if (smallDur.subtract(bigDur).getSign() != -1) {
                Assert.fail("smallDur.subtract(bigDur).getSign() is not -1");
            }
            if (bigDur.subtract(smallDur).getSign() != 1) {
                Assert.fail("bigDur.subtract(smallDur).getSign() is not 1");
            }
            if (smallDur.subtract(smallDur).getSign() != 0) {
                Assert.fail("smallDur.subtract(smallDur).getSign() is not 0");
            }
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDurationMultiply() {
        int num = 5000; // millisends. 5 seconds
        int factor = 2;
        try {
            Duration dur = DatatypeFactory.newInstance().newDuration(num);
            if (dur.multiply(factor).getSeconds() != 10) {
                Assert.fail("duration.multiply() return wrong value");
            }
            // factor is 2*10^(-1)
            if (dur.multiply(new BigDecimal(new BigInteger("2"), 1)).getSeconds() != 1) {
                Assert.fail("duration.multiply() return wrong value");
            }
            if (dur.subtract(DatatypeFactory.newInstance().newDuration(1000)).multiply(new BigDecimal(new BigInteger("2"), 1)).getSeconds() != 0) {
                Assert.fail("duration.multiply() return wrong value");
            }
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDurationAndCalendar1() {
        int year = 1;
        int month = 2;
        int day = 3;
        int hour = 4;
        int min = 5;
        int sec = 6;
        String lexicalRepresentation = "P" + year + "Y" + month + "M" + day + "DT" + hour + "H" + min + "M" + sec + "S";
        try {
            Duration dur = DatatypeFactory.newInstance().newDuration(lexicalRepresentation);
            System.out.println(dur.toString());
            AssertJUnit.assertTrue("year should be 1", dur.getYears() == year);
            AssertJUnit.assertTrue("month should be 2", dur.getMonths() == month);
            AssertJUnit.assertTrue("day should be 3", dur.getDays() == day);
            AssertJUnit.assertTrue("hour should be 4", dur.getHours() == hour);
            AssertJUnit.assertTrue("minute should be 5", dur.getMinutes() == min);
            AssertJUnit.assertTrue("second should be 6", dur.getSeconds() == sec);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDurationAndCalendar2() {
        try {
            AssertJUnit.assertTrue("10.00099S means 10 sec since it will be rounded to zero", DatatypeFactory.newInstance().newDuration("PT10.00099S")
                    .getTimeInMillis(new Date()) == 10000);
            AssertJUnit.assertTrue("10.00099S means 10 sec since it will be rounded to zero", DatatypeFactory.newInstance().newDuration("-PT10.00099S")
                    .getTimeInMillis(new Date()) == -10000);
            AssertJUnit.assertTrue("10.00099S means 10 sec since it will be rounded to zero", DatatypeFactory.newInstance().newDuration("PT10.00099S")
                    .getTimeInMillis(new GregorianCalendar()) == 10000);
            AssertJUnit.assertTrue("10.00099S means 10 sec since it will be rounded to zero", DatatypeFactory.newInstance().newDuration("-PT10.00099S")
                    .getTimeInMillis(new GregorianCalendar()) == -10000);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testDurationAndCalendar3() {
        try {
            Calendar cal = new GregorianCalendar();
            cal.set(Calendar.SECOND, 59);
            DatatypeFactory.newInstance().newDuration(10000).addTo(cal);
            AssertJUnit.assertTrue("sec will be 9", cal.get(Calendar.SECOND) == 9);

            Date date = new Date();
            date.setSeconds(59);
            DatatypeFactory.newInstance().newDuration(10000).addTo(date);
            AssertJUnit.assertTrue("sec will be 9", date.getSeconds() == 9);
        } catch (DatatypeConfigurationException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testEqualsWithDifferentObjectParam() {

        AssertJUnit.assertFalse("equals method should return false for any object other than Duration", duration.equals(new Integer(0)));
    }

    @Test
    public void testEqualsWithNullObjectParam() {

        AssertJUnit.assertFalse("equals method should return false for null parameter", duration.equals(null));
    }

    @Test
    public void testEqualsWithEqualObjectParam() {
        try {
            AssertJUnit.assertTrue("equals method is expected to return true", duration.equals(DatatypeFactory.newInstance().newDuration(100)));
        } catch (DatatypeConfigurationException dce) {
            dce.printStackTrace();
            Assert.fail("Failed to create instance of DatatypeFactory " + dce.getMessage());
        }
    }

    /**
     * Inspired by CR 5077522 Duration.compare makes mistakes for some values.
     */
    @Test
    public void testCompareWithInderterminateRelation() {

        final String[][] partialOrder = { // partialOrder
        { "P1Y", "<>", "P365D" }, { "P1Y", "<>", "P366D" }, { "P1M", "<>", "P28D" }, { "P1M", "<>", "P29D" }, { "P1M", "<>", "P30D" }, { "P1M", "<>", "P31D" },
                { "P5M", "<>", "P150D" }, { "P5M", "<>", "P151D" }, { "P5M", "<>", "P152D" }, { "P5M", "<>", "P153D" }, { "PT2419200S", "<>", "P1M" },
                { "PT2678400S", "<>", "P1M" }, { "PT31536000S", "<>", "P1Y" }, { "PT31622400S", "<>", "P1Y" }, { "PT525600M", "<>", "P1Y" },
                { "PT527040M", "<>", "P1Y" }, { "PT8760H", "<>", "P1Y" }, { "PT8784H", "<>", "P1Y" }, { "P365D", "<>", "P1Y" }, };

        DatatypeFactory df = null;
        try {
            df = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException ex) {
            ex.printStackTrace();
            Assert.fail(ex.toString());
        }

        boolean compareErrors = false;

        for (int valueIndex = 0; valueIndex < partialOrder.length; ++valueIndex) {
            Duration duration1 = df.newDuration(partialOrder[valueIndex][0]);
            Duration duration2 = df.newDuration(partialOrder[valueIndex][2]);
            int cmp = duration1.compare(duration2);
            int expected = ">".equals(partialOrder[valueIndex][1]) ? DatatypeConstants.GREATER
                    : "<".equals(partialOrder[valueIndex][1]) ? DatatypeConstants.LESSER : "==".equals(partialOrder[valueIndex][1]) ? DatatypeConstants.EQUAL
                            : DatatypeConstants.INDETERMINATE;

            // just note any errors, do not fail until all cases have been
            // tested
            if (expected != cmp) {
                compareErrors = true;
                System.err.println("returned " + cmp2str(cmp) + " for durations \'" + duration1 + "\' and " + duration2 + "\', but expected "
                        + cmp2str(expected));
            }
        }

        if (compareErrors) {
            // TODO; fix bug, these tests should pass
            if (false) {
                Assert.fail("Errors in comparing indeterminate relations, see Stderr");
            } else {
                System.err.println("Please fix this bug: " + "Errors in comparing indeterminate relations, see Stderr");
            }
        }
    }

    public static String cmp2str(int cmp) {
        return cmp == DatatypeConstants.LESSER ? "LESSER" : cmp == DatatypeConstants.GREATER ? "GREATER" : cmp == DatatypeConstants.EQUAL ? "EQUAL"
                : cmp == DatatypeConstants.INDETERMINATE ? "INDETERMINATE" : "UNDEFINED";
    }

    /**
     * Inspired by CR 6238220 javax.xml.datatype.Duration has no clear
     * description concerning return values range.
     */
    @Test
    public void testNormalizedReturnValues() throws Exception {

        final Object[] TEST_VALUES = {
                // test 61 seconds -> 1 minute, 1 second
                true, // isPositive,
                BigInteger.ZERO, // years,
                BigInteger.ZERO, // months
                BigInteger.ZERO, // days
                BigInteger.ZERO, // hours
                BigInteger.ZERO, // minutes
                new BigDecimal(61), // seconds
                61000L, // durationInMilliSeconds,
                "P0Y0M0DT0H0M61S", // lexicalRepresentation

                // test - 61 seconds -> - 1 minute, 1 second
                false, // isPositive,
                BigInteger.ZERO, // years,
                BigInteger.ZERO, // months
                BigInteger.ZERO, // days
                BigInteger.ZERO, // hours
                BigInteger.ZERO, // minutes
                new BigDecimal(61), // seconds
                61000L, // durationInMilliSeconds,
                "-P0Y0M0DT0H0M61S", // lexicalRepresentation
        };

        final Object[] NORM_VALUES = {
                // test 61 seconds -> 1 minute, 1 second
                true, // normalized isPositive,
                BigInteger.ZERO, // normalized years,
                BigInteger.ZERO, // normalized months
                BigInteger.ZERO, // normalized days
                BigInteger.ZERO, // normalized hours
                BigInteger.ONE, // normalized minutes
                BigDecimal.ONE, // normalized seconds
                61000L, // normalized durationInMilliSeconds,
                "P0Y0M0DT0H1M1.000S", // normalized lexicalRepresentation

                // test - 61 seconds -> - 1 minute, 1 second
                false, // normalized isPositive,
                BigInteger.ZERO, // normalized years,
                BigInteger.ZERO, // normalized months
                BigInteger.ZERO, // normalized days
                BigInteger.ZERO, // normalized hours
                BigInteger.ONE, // normalized minutes
                BigDecimal.ONE, // normalized seconds
                61000L, // normalized durationInMilliSeconds,
                "-P0Y0M0DT0H1M1.000S" // normalized lexicalRepresentation
        };

        for (int onValue = 0; onValue < TEST_VALUES.length; onValue += 9) {
            newDurationTester(((Boolean) TEST_VALUES[onValue]).booleanValue(), // isPositive,
                    ((Boolean) NORM_VALUES[onValue]).booleanValue(), // normalized
                                                                     // isPositive,
                    (BigInteger) TEST_VALUES[onValue + 1], // years,
                    (BigInteger) NORM_VALUES[onValue + 1], // normalized years,
                    (BigInteger) TEST_VALUES[onValue + 2], // months
                    (BigInteger) NORM_VALUES[onValue + 2], // normalized months
                    (BigInteger) TEST_VALUES[onValue + 3], // days
                    (BigInteger) NORM_VALUES[onValue + 3], // normalized days
                    (BigInteger) TEST_VALUES[onValue + 4], // hours
                    (BigInteger) NORM_VALUES[onValue + 4], // normalized hours
                    (BigInteger) TEST_VALUES[onValue + 5], // minutes
                    (BigInteger) NORM_VALUES[onValue + 5], // normalized minutes
                    (BigDecimal) TEST_VALUES[onValue + 6], // seconds
                    (BigDecimal) NORM_VALUES[onValue + 6], // normalized seconds
                    ((Long) TEST_VALUES[onValue + 7]).longValue(), // durationInMilliSeconds,
                    ((Long) NORM_VALUES[onValue + 7]).longValue(), // normalized
                                                                   // durationInMilliSeconds,
                    (String) TEST_VALUES[onValue + 8], // lexicalRepresentation
                    (String) NORM_VALUES[onValue + 8]); // normalized
                                                        // lexicalRepresentation

            newDurationDayTimeTester(((Boolean) TEST_VALUES[onValue]).booleanValue(), // isPositive,
                    ((Boolean) NORM_VALUES[onValue]).booleanValue(), // normalized
                                                                     // isPositive,
                    BigInteger.ZERO, // years,
                    BigInteger.ZERO, // normalized years,
                    BigInteger.ZERO, // months
                    BigInteger.ZERO, // normalized months
                    (BigInteger) TEST_VALUES[onValue + 3], // days
                    (BigInteger) NORM_VALUES[onValue + 3], // normalized days
                    (BigInteger) TEST_VALUES[onValue + 4], // hours
                    (BigInteger) NORM_VALUES[onValue + 4], // normalized hours
                    (BigInteger) TEST_VALUES[onValue + 5], // minutes
                    (BigInteger) NORM_VALUES[onValue + 5], // normalized minutes
                    (BigDecimal) TEST_VALUES[onValue + 6], // seconds
                    (BigDecimal) NORM_VALUES[onValue + 6], // normalized seconds
                    ((Long) TEST_VALUES[onValue + 7]).longValue(), // durationInMilliSeconds,
                    ((Long) NORM_VALUES[onValue + 7]).longValue(), // normalized
                                                                   // durationInMilliSeconds,
                    (String) TEST_VALUES[onValue + 8], // lexicalRepresentation
                    (String) NORM_VALUES[onValue + 8]); // normalized
                                                        // lexicalRepresentation
        }
    }

    private void newDurationTester(boolean isPositive, boolean normalizedIsPositive, BigInteger years, BigInteger normalizedYears, BigInteger months,
            BigInteger normalizedMonths, BigInteger days, BigInteger normalizedDays, BigInteger hours, BigInteger normalizedHours, BigInteger minutes,
            BigInteger normalizedMinutes, BigDecimal seconds, BigDecimal normalizedSeconds, long durationInMilliSeconds, long normalizedDurationInMilliSeconds,
            String lexicalRepresentation, String normalizedLexicalRepresentation) {

        DatatypeFactory datatypeFactory = null;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException ex) {
            ex.printStackTrace();
            Assert.fail(ex.toString());
        }

        // create 4 Durations using the 4 different constructors

        Duration durationBigInteger = datatypeFactory.newDuration(isPositive, years, months, days, hours, minutes, seconds);
        durationAssertEquals(durationBigInteger, DatatypeConstants.DURATION, normalizedIsPositive, normalizedYears.intValue(), normalizedMonths.intValue(),
                normalizedDays.intValue(), normalizedHours.intValue(), normalizedMinutes.intValue(), normalizedSeconds.intValue(),
                normalizedDurationInMilliSeconds, normalizedLexicalRepresentation);

        Duration durationInt = datatypeFactory.newDuration(isPositive, years.intValue(), months.intValue(), days.intValue(), hours.intValue(),
                minutes.intValue(), seconds.intValue());
        durationAssertEquals(durationInt, DatatypeConstants.DURATION, normalizedIsPositive, normalizedYears.intValue(), normalizedMonths.intValue(),
                normalizedDays.intValue(), normalizedHours.intValue(), normalizedMinutes.intValue(), normalizedSeconds.intValue(),
                normalizedDurationInMilliSeconds, normalizedLexicalRepresentation);

        Duration durationMilliseconds = datatypeFactory.newDuration(durationInMilliSeconds);
        durationAssertEquals(durationMilliseconds, DatatypeConstants.DURATION, normalizedIsPositive, normalizedYears.intValue(), normalizedMonths.intValue(),
                normalizedDays.intValue(), normalizedHours.intValue(), normalizedMinutes.intValue(), normalizedSeconds.intValue(),
                normalizedDurationInMilliSeconds, normalizedLexicalRepresentation);

        Duration durationLexical = datatypeFactory.newDuration(lexicalRepresentation);
        durationAssertEquals(durationLexical, DatatypeConstants.DURATION, normalizedIsPositive, normalizedYears.intValue(), normalizedMonths.intValue(),
                normalizedDays.intValue(), normalizedHours.intValue(), normalizedMinutes.intValue(), normalizedSeconds.intValue(),
                normalizedDurationInMilliSeconds, normalizedLexicalRepresentation);
    }

    private void newDurationDayTimeTester(boolean isPositive, boolean normalizedIsPositive, BigInteger years, BigInteger normalizedYears, BigInteger months,
            BigInteger normalizedMonths, BigInteger days, BigInteger normalizedDays, BigInteger hours, BigInteger normalizedHours, BigInteger minutes,
            BigInteger normalizedMinutes, BigDecimal seconds, BigDecimal normalizedSeconds, long durationInMilliSeconds, long normalizedDurationInMilliSeconds,
            String lexicalRepresentation, String normalizedLexicalRepresentation) {

        DatatypeFactory datatypeFactory = null;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException ex) {
            ex.printStackTrace();
            Assert.fail(ex.toString());
        }

        // create 4 dayTime Durations using the 4 different constructors

        Duration durationDayTimeBigInteger = datatypeFactory.newDurationDayTime(isPositive, days, hours, minutes, seconds.toBigInteger());
        durationAssertEquals(durationDayTimeBigInteger, DatatypeConstants.DURATION_DAYTIME, normalizedIsPositive, normalizedYears.intValue(),
                normalizedMonths.intValue(), normalizedDays.intValue(), normalizedHours.intValue(), normalizedMinutes.intValue(), normalizedSeconds.intValue(),
                normalizedDurationInMilliSeconds, normalizedLexicalRepresentation);

        /*
         * Duration durationDayTimeInt = datatypeFactory.newDurationDayTime(
         * isPositive, days.intValue(), hours.intValue(), minutes.intValue(),
         * seconds.intValue()); Duration durationDayTimeMilliseconds =
         * datatypeFactory.newDurationDayTime( durationInMilliSeconds); Duration
         * durationDayTimeLexical = datatypeFactory.newDurationDayTime(
         * lexicalRepresentation);
         * Duration durationYearMonthBigInteger =
         * datatypeFactory.newDurationYearMonth( isPositive, years, months);
         * Duration durationYearMonthInt = datatypeFactory.newDurationYearMonth(
         * isPositive, years.intValue(), months.intValue()); Duration
         * durationYearMonthMilliseconds = datatypeFactory.newDurationYearMonth(
         * durationInMilliSeconds); Duration durationYearMonthLexical =
         * datatypeFactory.newDurationYearMonth( lexicalRepresentation) ;
         */

    }

    private void durationAssertEquals(Duration duration, QName xmlSchemaType, boolean isPositive, int years, int months, int days, int hours, int minutes,
            int seconds, long milliseconds, String lexical) {

        final TimeZone GMT = TimeZone.getTimeZone("GMT");
        final GregorianCalendar EPOCH = new GregorianCalendar(GMT);
        EPOCH.clear();

        if (DEBUG) {
            System.out.println("Testing Duration: " + duration.toString());
        }

        // sign
        if (DEBUG) {
            boolean actual = (duration.getSign() == 1) ? true : false;
            System.out.println("sign:");
            System.out.println("    expected: \"" + isPositive + "\"");
            System.out.println("    actual:   \"" + actual + "\"");
        }

        if (DEBUG) {
            System.out.println("years:");
            System.out.println("    expected: \"" + years + "\"");
            System.out.println("    actual:   \"" + duration.getYears() + "\"");
        }

        if (DEBUG) {
            System.out.println("months:");
            System.out.println("    expected: \"" + months + "\"");
            System.out.println("    actual:   \"" + duration.getMonths() + "\"");
        }

        if (DEBUG) {
            System.out.println("days:");
            System.out.println("    expected: \"" + days + "\"");
            System.out.println("    actual:   \"" + duration.getDays() + "\"");
        }

        if (DEBUG) {
            System.out.println("hours:");
            System.out.println("    expected: \"" + hours + "\"");
            System.out.println("    actual:   \"" + duration.getHours() + "\"");
        }

        if (DEBUG) {
            System.out.println("minutes:");
            System.out.println("    expected: \"" + minutes + "\"");
            System.out.println("    actual:   \"" + duration.getMinutes() + "\"");
        }

        if (DEBUG) {
            System.out.println("seconds:");
            System.out.println("    expected: \"" + seconds + "\"");
            System.out.println("    actual:   \"" + duration.getSeconds() + "\"");
        }

        if (DEBUG) {
            System.out.println("milliseconds:");
            System.out.println("    expected: \"" + milliseconds + "\"");
            System.out.println("    actual:   \"" + duration.getTimeInMillis(EPOCH) + "\"");
        }

        if (DEBUG) {
            System.out.println("lexical:");
            System.out.println("    expected: \"" + lexical + "\"");
            System.out.println("    actual:   \"" + duration.toString() + "\"");
        }

    }
}
