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

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

/*
 * @test
 * @library /javax/xml/jaxp/libs /javax/xml/jaxp/unittest
 * @run testng/othervm -DrunSecMngr=true -Djava.security.manager=allow datatype.XMLGregorianCalendarTest
 * @run testng/othervm datatype.XMLGregorianCalendarTest
 * @summary Test XMLGregorianCalendar.
 */
@Listeners({jaxp.library.BasePolicy.class})
public class XMLGregorianCalendarTest {

    private static final boolean DEBUG = false;

    private static final int TEST_VALUE_FAIL = 0;

    private static final int TEST_VALUE_PASS = 1;

    private XMLGregorianCalendar calendar;

    @BeforeMethod
    public void setUp() {
        try {
            calendar = DatatypeFactory.newInstance().newXMLGregorianCalendar();
        } catch (DatatypeConfigurationException dce) {
            dce.printStackTrace();
            Assert.fail("Failed to create instance of DatatypeFactory " + dce.getMessage());
        }
    }

    @Test
    public final void testSetTime() {

        /**
         * Hour, minute, second values to test and expected result.
         */
        final int[] TEST_VALUES = { 24, 0, 0, TEST_VALUE_PASS, 24, 1, 0, TEST_VALUE_FAIL, 24, 0, 1, TEST_VALUE_FAIL, 24, DatatypeConstants.FIELD_UNDEFINED, 0,
                TEST_VALUE_FAIL, 24, 0, DatatypeConstants.FIELD_UNDEFINED, TEST_VALUE_FAIL };

        // create DatatypeFactory
        DatatypeFactory datatypeFactory = null;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException datatypeConfigurationException) {
            Assert.fail(datatypeConfigurationException.toString());
        }

        if (DEBUG) {
            System.err.println("DatatypeFactory created: " + datatypeFactory.toString());
        }

        // create XMLGregorianCalendar
        XMLGregorianCalendar xmlGregorianCalendar = datatypeFactory.newXMLGregorianCalendar();

        // test each value
        for (int onTestValue = 0; onTestValue < TEST_VALUES.length; onTestValue = onTestValue + 4) {

            if (DEBUG) {
                System.err.println("testing values: (" + TEST_VALUES[onTestValue] + ", " + TEST_VALUES[onTestValue + 1] + ", " + TEST_VALUES[onTestValue + 2]
                        + ") expected (0=fail, 1=pass): " + TEST_VALUES[onTestValue + 3]);
            }

            try {
                // set time
                xmlGregorianCalendar.setTime(TEST_VALUES[onTestValue], TEST_VALUES[onTestValue + 1], TEST_VALUES[onTestValue + 2]);

                if (DEBUG) {
                    System.err.println("XMLGregorianCalendar created: \"" + xmlGregorianCalendar.toString() + "\"");
                }

                // was this expected to fail?
                if (TEST_VALUES[onTestValue + 3] == TEST_VALUE_FAIL) {
                    Assert.fail("the values: (" + TEST_VALUES[onTestValue] + ", " + TEST_VALUES[onTestValue + 1] + ", " + TEST_VALUES[onTestValue + 2]
                            + ") are invalid, " + "yet it created the XMLGregorianCalendar \"" + xmlGregorianCalendar.toString() + "\"");
                }
            } catch (Exception exception) {

                if (DEBUG) {
                    System.err.println("Exception in creating XMLGregorianCalendar: \"" + exception.toString() + "\"");
                }

                // was this expected to succed?
                if (TEST_VALUES[onTestValue + 3] == TEST_VALUE_PASS) {
                    Assert.fail("the values: (" + TEST_VALUES[onTestValue] + ", " + TEST_VALUES[onTestValue + 1] + ", " + TEST_VALUES[onTestValue + 2]
                            + ") are valid yet it failed with \"" + exception.toString() + "\"");
                }
                // expected failure
            }
        }
    }

    @Test
    public final void testSetHour() {

        /**
         * Hour values to test and expected result.
         */
        final int[] TEST_VALUES = {
                // setTime(H, M, S), hour override, expected result
                0, 0, 0, 0, TEST_VALUE_PASS, 0, 0, 0, 23, TEST_VALUE_PASS, 0, 0, 0, 24, TEST_VALUE_PASS,
                // creates invalid state
                0, 0, 0, DatatypeConstants.FIELD_UNDEFINED, TEST_VALUE_FAIL,
                // violates Schema Errata
                0, 0, 1, 24, TEST_VALUE_FAIL };

        // create DatatypeFactory
        DatatypeFactory datatypeFactory = null;
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException datatypeConfigurationException) {
            Assert.fail(datatypeConfigurationException.toString());
        }

        if (DEBUG) {
            System.err.println("DatatypeFactory created: " + datatypeFactory.toString());
        }

        // create XMLGregorianCalendar
        XMLGregorianCalendar xmlGregorianCalendar = datatypeFactory.newXMLGregorianCalendar();

        // test each value
        for (int onTestValue = 0; onTestValue < TEST_VALUES.length; onTestValue = onTestValue + 5) {

            if (DEBUG) {
                System.err.println("testing values: (" + TEST_VALUES[onTestValue] + ", " + TEST_VALUES[onTestValue + 1] + ", " + TEST_VALUES[onTestValue + 2]
                        + ", " + TEST_VALUES[onTestValue + 3] + ") expected (0=fail, 1=pass): " + TEST_VALUES[onTestValue + 4]);
            }

            try {
                // set time to known valid value
                xmlGregorianCalendar.setTime(TEST_VALUES[onTestValue], TEST_VALUES[onTestValue + 1], TEST_VALUES[onTestValue + 2]);
                // now explicitly set hour
                xmlGregorianCalendar.setHour(TEST_VALUES[onTestValue + 3]);

                if (DEBUG) {
                    System.err.println("XMLGregorianCalendar created: \"" + xmlGregorianCalendar.toString() + "\"");
                }

                // was this expected to fail?
                if (TEST_VALUES[onTestValue + 4] == TEST_VALUE_FAIL) {
                    Assert.fail("the values: (" + TEST_VALUES[onTestValue] + ", " + TEST_VALUES[onTestValue + 1] + ", " + TEST_VALUES[onTestValue + 2] + ", "
                            + TEST_VALUES[onTestValue + 3] + ") are invalid, " + "yet it created the XMLGregorianCalendar \"" + xmlGregorianCalendar.toString()
                            + "\"");
                }
            } catch (Exception exception) {

                if (DEBUG) {
                    System.err.println("Exception in creating XMLGregorianCalendar: \"" + exception.toString() + "\"");
                }

                // was this expected to succed?
                if (TEST_VALUES[onTestValue + 4] == TEST_VALUE_PASS) {
                    Assert.fail("the values: (" + TEST_VALUES[onTestValue] + ", " + TEST_VALUES[onTestValue + 1] + ", " + TEST_VALUES[onTestValue + 2] + ", "
                            + TEST_VALUES[onTestValue + 3] + ") are valid yet it failed with \"" + exception.toString() + "\"");
                }
                // expected failure
            }
        }
    }

    @Test
    public void testEqualsWithDifferentObjectParam() {

        Assert.assertFalse(calendar.equals(new Integer(0)), "equals method should return false for any object other" + " than XMLGregorianCalendar");
    }

    @Test
    public void testEqualsWithNullObjectParam() {

        Assert.assertFalse(calendar.equals(null), "equals method should return false for null parameter");
    }

    @Test
    public void testEqualsWithEqualObjectParam() {

        try {
            Assert.assertTrue(calendar.equals(DatatypeFactory.newInstance().newXMLGregorianCalendar()), "equals method is expected to return true");
        } catch (DatatypeConfigurationException dce) {
            dce.printStackTrace();
            Assert.fail("Failed to create instance of DatatypeFactory " + dce.getMessage());
        }
    }

    @Test
    public void testToString() {
        try {
            String inputDateTime = "2006-10-23T22:15:01.000000135+08:00";
            DatatypeFactory factory = DatatypeFactory.newInstance();
            XMLGregorianCalendar calendar = factory.newXMLGregorianCalendar(inputDateTime);
            String toStr = calendar.toString();
            Assert.assertTrue(toStr.indexOf("E") == -1, "String value cannot contain exponent");
        } catch (DatatypeConfigurationException dce) {
            dce.printStackTrace();
            Assert.fail("Failed to create instance of DatatypeFactory " + dce.getMessage());
        }
    }
}
