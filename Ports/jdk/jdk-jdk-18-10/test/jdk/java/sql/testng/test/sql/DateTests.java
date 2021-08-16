/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package test.sql;

import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import static org.testng.Assert.*;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import util.BaseTest;

public class DateTests extends BaseTest {

    /*
     * Validate an IllegalArgumentException is thrown for an invalid Date string
     */
    @Test(dataProvider = "invalidDateValues",
            expectedExceptions = IllegalArgumentException.class)
    public void test(String d) throws Exception {
        Date.valueOf(d);
    }

    /*
     * Test that a date created from a date string is equal to the value
     * returned from toString()
     */
    @Test(dataProvider = "validDateValues")
    public void test00(String d, String expectedD) {
        Date d1 = Date.valueOf(d);
        Date d2 = Date.valueOf(expectedD);
        assertTrue(d1.equals(d2) && d2.equals(d1)
                && d1.toString().equals(expectedD), "Error d1 != d2");
    }

    /*
     * Validate that a Date.after() returns false when same date is compared
     */
    @Test
    public void test01() {
        Date d = Date.valueOf("1961-08-30");
        assertFalse(d.after(d), "Error d.after(d) = true");
    }

    /*
     * Validate that a Date.after() returns true when later date is compared to
     * earlier date
     */
    @Test
    public void test2() {
        Date d = Date.valueOf("1961-08-30");
        Date d2 = new Date(System.currentTimeMillis());
        assertTrue(d2.after(d), "Error d2.after(d) = false");
    }

    /*
     * Validate that a Date.after() returns false when earlier date is compared
     * to later date
     */
    @Test
    public void test3() {
        Date d = Date.valueOf("1961-08-30");
        Date d2 = new Date(d.getTime());
        assertFalse(d.after(d2), "Error d.after(d2) = true");
    }

    /*
     * Validate that a Date.after() returns false when date compared to another
     * date created from the original date
     */
    @Test
    public void test4() {
        Date d = Date.valueOf("1961-08-30");
        Date d2 = new Date(d.getTime());
        assertFalse(d.after(d2), "Error d.after(d2) = true");
        assertFalse(d2.after(d), "Error d2.after(d) = true");
    }

    /*
     * Validate that a Date.before() returns false when same date is compared
     */
    @Test
    public void test5() {
        Date d = Date.valueOf("1961-08-30");
        assertFalse(d.before(d), "Error d.before(d) = true");
    }

    /*
     * Validate that a Date.before() returns true when earlier date is compared
     * to later date
     */
    @Test
    public void test6() {
        Date d = Date.valueOf("1961-08-30");
        Date d2 = new Date(System.currentTimeMillis());
        assertTrue(d.before(d2), "Error d.before(d2) = false");
    }

    /*
     * Validate that a Date.before() returns false when later date is compared
     * to earlier date
     */
    @Test
    public void test7() {
        Date d = Date.valueOf("1961-08-30");
        Date d2 = new Date(d.getTime());
        assertFalse(d2.before(d), "Error d2.before(d) = true");
    }

    /*
     * Validate that a Date.before() returns false when date compared to another
     * date created from the original date
     */
    @Test
    public void test8() {
        Date d = Date.valueOf("1961-08-30");
        Date d2 = new Date(d.getTime());
        assertFalse(d.before(d2), "Error d.before(d2) = true");
        assertFalse(d2.before(d), "Error d2.before(d) = true");
    }

    /*
     * Validate that a Date.compareTo returns 0 when both Date objects are the
     * same
     */
    @Test
    public void test9() {
        Date d = Date.valueOf("1961-08-30");
        assertTrue(d.compareTo(d) == 0, "Error d.compareTo(d) !=0");
    }

    /*
     * Validate that a Date.compareTo returns 0 when both Date objects represent
     * the same date
     */
    @Test
    public void test10() {
        Date d = Date.valueOf("1961-08-30");
        Date d2 = new Date(d.getTime());
        assertTrue(d.compareTo(d2) == 0, "Error d.compareTo(d2) !=0");
    }

    /*
     * Validate that a Date.compareTo returns -1 when comparing a date to a
     * later date
     */
    @Test
    public void test11() {
        Date d = Date.valueOf("1961-08-30");
        Date d2 = new Date(System.currentTimeMillis());
        assertTrue(d.compareTo(d2) == -1, "Error d.compareTo(d2) != -1");
    }

    /*
     * Validate that a Date.compareTo returns 1 when comparing a date to an
     * earlier date
     */
    @Test
    public void test12() {
        Date d = Date.valueOf("1961-08-30");
        Date d2 = new Date(System.currentTimeMillis());
        assertTrue(d2.compareTo(d) == 1, "Error d.compareTo(d2) != 1");
    }

    /*
     * Validate that a Date made from a LocalDate are equal
     */
    @Test
    public void test13() {
        Date d = Date.valueOf("1961-08-30");
        LocalDate ldt = d.toLocalDate();
        Date d2 = Date.valueOf(ldt);
        assertTrue(d.equals(d2), "Error d != d2");
    }

    /*
     * Validate that a Date LocalDate value, made from a LocalDate are equal
     */
    @Test
    public void test14() {
        LocalDate ldt = LocalDate.now();
        Date d = Date.valueOf(ldt);
        assertTrue(ldt.equals(d.toLocalDate()),
                "Error LocalDate values are not equal");
    }

    /*
     * Validate an NPE occurs when a null LocalDate is passed to valueOf
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void test15() throws Exception {
        LocalDate ld = null;
        Date.valueOf(ld);
    }

    /*
     * Validate an UnsupportedOperationException occurs when toInstant() is
     * called
     */
    @Test(expectedExceptions = UnsupportedOperationException.class)
    public void test16() throws Exception {
        Date d = Date.valueOf("1961-08-30");
        Instant instant = d.toInstant();
    }

    /*
     * Validate that two Date objects are equal when one is created from the
     * toString() of the other
     */
    @Test
    public void test17() {
        Date d = Date.valueOf("1961-08-30");
        Date d2 = Date.valueOf(d.toString());
        assertTrue(d.equals(d2) && d2.equals(d), "Error d != d2");
    }

    /*
     * Validate that two Date values one created using valueOf and another via a
     * constructor are equal
     */
    @Test
    public void test18() {

        Date d = Date.valueOf("1961-08-30");
        Date d2 = new Date(61, 7, 30);
        assertTrue(d.equals(d2), "Error d != d2");
    }

    /*
     * Validate that two Date values one created using getTime() of the other
     * are equal
     */
    @Test
    public void test19() {

        Date d = Date.valueOf("1961-08-30");
        Date d2 = new Date(d.getTime());
        assertTrue(d.equals(d2), "Error d != d2");
    }

    /*
     * Validate that a Date value is equal to itself
     */
    @Test
    public void test20() {

        Date d = Date.valueOf("1961-08-30");
        assertTrue(d.equals(d), "Error d != d");
    }

    /*
     * Validate an IllegalArgumentException is thrown for calling getHours
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test21() throws Exception {
        Date d = Date.valueOf("1961-08-30");
        d.getHours();
    }

    /*
     * Validate an IllegalArgumentException is thrown for calling getMinutes
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test22() throws Exception {
        Date d = Date.valueOf("1961-08-30");
        d.getMinutes();
    }

    /*
     * Validate an IllegalArgumentException is thrown for calling getSeconds
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test23() throws Exception {
        Date d = Date.valueOf("1961-08-30");
        d.getSeconds();
    }

    /*
     * Validate an IllegalArgumentException is thrown for calling setHours
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test24() throws Exception {
        Date d = Date.valueOf("1961-08-30");
        d.setHours(8);
    }

    /*
     * Validate an IllegalArgumentException is thrown for calling setMinutes
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test25() throws Exception {
        Date d = Date.valueOf("1961-08-30");
        d.setMinutes(0);
    }

    /*
     * Validate an IllegalArgumentException is thrown for calling setSeconds
     */
    @Test(expectedExceptions = IllegalArgumentException.class)
    public void test26() throws Exception {
        Date d = Date.valueOf("1961-08-30");
        d.setSeconds(0);
    }

    /*
     * DataProvider used to provide Date which are not valid and are used
     * to validate that an IllegalArgumentException will be thrown from the
     * valueOf method
     */
    @DataProvider(name = "invalidDateValues")
    private Object[][] invalidDateValues() {
        return new Object[][]{
            {"20009-11-01"},
            {"09-11-01"},
            {"-11-01"},
            {"2009-111-01"},
            {"2009--01"},
            {"2009-13-01"},
            {"2009-11-011"},
            {"2009-11-"},
            {"2009-11-00"},
            {"2009-11-33"},
            {"--"},
            {""},
            {null},
            {"-"},
            {"2009"},
            {"2009-01"},
            {"---"},
            {"2009-13--1"},
            {"1900-1-0"},
            {"2009-01-01 10:50:01"},
            {"1996-12-10 12:26:19.1"},
            {"10:50:01"}
        };
    }

    /*
     * DataProvider used to provide Dates which are  valid and are used
     * to validate that an IllegalArgumentException will not be thrown from the
     * valueOf method and the corect value from toString() is returned
     */
    @DataProvider(name = "validDateValues")
    private Object[][] validDateValues() {
        return new Object[][]{
            {"2009-08-30", "2009-08-30"},
            {"2009-01-8", "2009-01-08"},
            {"2009-1-01", "2009-01-01"},
            {"2009-1-1", "2009-01-01"}

        };
    }
}
