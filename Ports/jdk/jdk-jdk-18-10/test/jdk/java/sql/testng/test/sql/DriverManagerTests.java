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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import util.StubDriver;

public class DriverManagerTests {

    private final String StubDriverURL = "jdbc:tennis:boy";
    private final String StubDriverDAURL = "jdbc:luckydog:tennis";
    private final String InvalidURL = "jdbc:cardio:tennis";
    private String[] results = {"output", "more output", "and more", "the end"};
    private String noOutput = "should not find this";

    public DriverManagerTests() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
        removeAllDrivers();
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Utility method to remove all registered drivers
     */
    private static void removeAllDrivers() {
        java.util.Enumeration e = DriverManager.getDrivers();
        while (e.hasMoreElements()) {
            try {
                DriverManager.deregisterDriver((Driver) (e.nextElement()));
            } catch (SQLException ex) {
                System.out.print(ex.getMessage());
            }
        }
    }

    /**
     * Utility method to see if a driver is registered
     */
    private boolean isDriverRegistered(Driver d) {
        boolean foundDriver = false;
        java.util.Enumeration e = DriverManager.getDrivers();
        while (e.hasMoreElements()) {
            if (d == (Driver) e.nextElement()) {
                foundDriver = true;
                break;
            }
        }
        return foundDriver;
    }

    /**
     * Validate that values set using setLoginTimeout will be returned by
     * getLoginTimeout
     */
    @Test
    public void test() {
        int[] vals = {-1, 0, 5};
        for (int val : vals) {
            DriverManager.setLoginTimeout(val);
            assertEquals(val, DriverManager.getLoginTimeout());
        }
    }

    /**
     * Validate that NullPointerException is thrown when null is passed to
     * registerDriver
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void test1() throws Exception {
        Driver d = null;
        DriverManager.registerDriver(d);
    }

    /**
     * Validate that NullPointerException is thrown when null is passed to
     * registerDriver
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void test2() throws Exception {
        Driver d = null;
        DriverManager.registerDriver(d, null);
    }

    /**
     * Validate that a null value allows for deRegisterDriver to return
     */
    @Test
    public void test3() throws Exception {
        DriverManager.deregisterDriver(null);

    }

    /**
     * Validate that SQLException is thrown when there is no Driver to service
     * the URL
     */
    @Test(expectedExceptions = SQLException.class)
    public void test4() throws Exception {
        DriverManager.getConnection(InvalidURL);
    }

    /**
     * Validate that SQLException is thrown when there is no Driver to service
     * the URL
     */
    @Test(expectedExceptions = SQLException.class)
    public void test5() throws Exception {
        DriverManager.getConnection(InvalidURL, new Properties());
    }

    /**
     * Validate that SQLException is thrown when there is no Driver to service
     * the URL
     */
    @Test(expectedExceptions = SQLException.class)
    public void test6() throws Exception {
        DriverManager.getConnection(InvalidURL, "LuckyDog", "tennisanyone");
    }

    /**
     * Validate that SQLException is thrown when null is passed for the URL
     */
    @Test(expectedExceptions = SQLException.class)
    public void test7() throws Exception {
        DriverManager.getConnection(null);
    }

    /**
     * Validate that SQLException is thrown when null is passed for the URL
     */
    @Test(expectedExceptions = SQLException.class)
    public void test8() throws Exception {
        DriverManager.getConnection(null, new Properties());
    }

    /**
     * Validate that SQLException is thrown when null is passed for the URL
     */
    @Test(expectedExceptions = SQLException.class)
    public void test9() throws Exception {
        DriverManager.getConnection(null, "LuckyDog", "tennisanyone");
    }

    /**
     * Validate that SQLException is thrown when there is no Driver to service
     * the URL
     */
    @Test(expectedExceptions = SQLException.class)
    public void test10() throws Exception {
        DriverManager.getDriver(InvalidURL);
    }

    /**
     * Validate that SQLException is thrown when null is passed for the URL
     */
    @Test(expectedExceptions = SQLException.class)
    public void test11() throws Exception {
        DriverManager.getDriver(null);
    }

    /**
     * Validate that a non-null Driver is returned by getDriver when a valid URL
     * is specified
     */
    @Test
    public void test12() throws Exception {

        DriverManager.registerDriver(new StubDriver());
        assertTrue(DriverManager.getDriver(StubDriverURL) != null);
    }

    /**
     * Validate that SQLException is thrown when the URL is not valid for any of
     * the registered drivers
     */
    @Test(expectedExceptions = SQLException.class)
    public void test13() throws Exception {
        DriverManager.registerDriver(new StubDriver());
        DriverManager.getDriver(InvalidURL);
    }

    /**
     * Validate that a Connection object is returned when a valid URL is
     * specified to getConnection
     *
     */
    @Test
    public void test14() throws Exception {

        DriverManager.registerDriver(new StubDriver());
        assertTrue(
                DriverManager.getConnection(StubDriverURL) != null);
        assertTrue(DriverManager.getConnection(StubDriverURL,
                "LuckyDog", "tennisanyone") != null);
        Properties props = new Properties();
        props.put("user", "LuckyDog");
        props.put("password", "tennisanyone");
        assertTrue(
                DriverManager.getConnection(StubDriverURL,
                        props) != null);
    }

    /**
     * Register a driver and make sure you find it via its URL. Deregister the
     * driver and validate it is not longer registered
     *
     * @throws Exception
     */
    @Test()
    public void test15() throws Exception {
        DriverManager.registerDriver(new StubDriver());
        Driver d = DriverManager.getDriver(StubDriverURL);
        assertTrue(d != null);
        assertTrue(isDriverRegistered(d));
        DriverManager.deregisterDriver(d);
        assertFalse(isDriverRegistered(d));
    }

    /**
     * Validate that DriverAction.release is called when a driver is registered
     * via registerDriver(Driver, DriverAction)
     *
     * @throws Exception
     */
    @Test
    public void test16() throws Exception {
        File file = new File(util.StubDriverDA.DriverActionCalled);
        file.delete();
        assertFalse(file.exists());
        Driver d = null;
        Class.forName("util.StubDriverDA");
        d = DriverManager.getDriver(StubDriverDAURL);
        DriverManager.deregisterDriver(d);
        assertFalse(isDriverRegistered(d), "Driver is registered");
        assertTrue(file.exists());
    }

    /**
     * Create a PrintStream and use to send output via DriverManager.println
     * Validate that if you disable the stream, the output sent is not present
     */
    @Test
    public void tests17() throws Exception {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(os);
        DriverManager.setLogStream(ps);
        assertTrue(DriverManager.getLogStream() == ps);

        DriverManager.println(results[0]);
        DriverManager.setLogStream((PrintStream) null);
        assertTrue(DriverManager.getLogStream() == null);
        DriverManager.println(noOutput);
        DriverManager.setLogStream(ps);
        DriverManager.println(results[1]);
        DriverManager.println(results[2]);
        DriverManager.println(results[3]);
        DriverManager.setLogStream((PrintStream) null);
        DriverManager.println(noOutput);

        /*
         * Check we do not get the output when the stream is disabled
         */
        InputStreamReader is
                = new InputStreamReader(new ByteArrayInputStream(os.toByteArray()));
        BufferedReader reader = new BufferedReader(is);
        for (String result : results) {
            assertTrue(result.equals(reader.readLine()));
        }
    }

    /**
     * Create a PrintWriter and use to to send output via DriverManager.println
     * Validate that if you disable the writer, the output sent is not present
     */
    @Test
    public void tests18() throws Exception {
        CharArrayWriter cw = new CharArrayWriter();
        PrintWriter pw = new PrintWriter(cw);
        DriverManager.setLogWriter(pw);
        assertTrue(DriverManager.getLogWriter() == pw);

        DriverManager.println(results[0]);
        DriverManager.setLogWriter(null);
        assertTrue(DriverManager.getLogWriter() == null);
        DriverManager.println(noOutput);
        DriverManager.setLogWriter(pw);
        DriverManager.println(results[1]);
        DriverManager.println(results[2]);
        DriverManager.println(results[3]);
        DriverManager.setLogWriter(null);
        DriverManager.println(noOutput);

        /*
         * Check we do not get the output when the stream is disabled
         */
        BufferedReader reader
                = new BufferedReader(new CharArrayReader(cw.toCharArray()));
        for (String result : results) {
            assertTrue(result.equals(reader.readLine()));
        }
    }

    /**
     * Register some driver implementations and validate that the driver
     * elements covered by the Enumeration obtained from
     * {@link DriverManager#getDrivers()} are the same as driver elements
     * covered by the stream obtained from {@link DriverManager#drivers()}}
     */
    @Test
    public void tests19() throws Exception {
        int n = 8;
        for (int i = 0; i < n; i++) {
            DriverManager.registerDriver(new StubDriver());
        }

        Collection<Driver> expectedDrivers = Collections.list(DriverManager.getDrivers());
        assertEquals(expectedDrivers.size(), n);
        Collection<Driver> drivers = DriverManager.drivers().collect(Collectors.toList());

        assertEquals(drivers, expectedDrivers);
    }
}
