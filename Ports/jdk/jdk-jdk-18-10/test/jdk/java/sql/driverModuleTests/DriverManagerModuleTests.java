/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/*
 * @test
 * @library /java/sql/modules
 * @build luckydogdriver/* mystubdriver/*
 * @run testng/othervm DriverManagerModuleTests
 * @summary Tests that a JDBC Driver that is a module can be loaded
 * via the service-provider loading mechanism.
 */
public class DriverManagerModuleTests {

    private final String LUCKYDOGDRIVER_URL = "jdbc:tennis:myDB";
    private static final String STUBDRIVERURL = "jdbc:stub:myDB";
    private static final String CONNECTION_CLASS_NAME = "com.luckydogtennis.StubConnection";

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Validate JDBC drivers as modules will be accessible. One driver will be
     * loaded and registered via the service-provider loading mechanism. The
     * other driver will need to be explictly loaded
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test() throws Exception {
        System.out.println("\n$$$ runing Test()\n");
        dumpRegisteredDrivers();
        Driver d = DriverManager.getDriver(STUBDRIVERURL);
        assertNotNull(d, "StubDriver should not be null");
        assertTrue(isDriverRegistered(d));
        Driver d2 = null;

        // This driver should not be found until it is explictly loaded
        try {
            d2 = DriverManager.getDriver(LUCKYDOGDRIVER_URL);
        } catch (SQLException e) {
            // ignore expected Exception
        }
        assertNull(d2, "LuckyDogDriver should  be null");
        loadDriver();
        d2 = DriverManager.getDriver(LUCKYDOGDRIVER_URL);
        assertNotNull(d2, "LuckyDogDriver should not be null");
        assertTrue(isDriverRegistered(d2), "Driver was NOT registered");

        dumpRegisteredDrivers();
        DriverManager.deregisterDriver(d2);
        assertFalse(isDriverRegistered(d2), "Driver IS STILL registered");
        dumpRegisteredDrivers();

    }

    /**
     * Validate that a Connection can be obtained from a JDBC driver which is a
     * module and loaded via the service-provider loading mechanism.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void test00() throws Exception {
        System.out.println("\n$$$ runing Test00()\n");
        Connection con = DriverManager.getConnection(STUBDRIVERURL);
        assertNotNull(con, "Returned Connection should not be NULL");
        System.out.println("con=" + con.getClass().getName());
        assertTrue(con.getClass().getName().equals(CONNECTION_CLASS_NAME));

    }

    /**
     * Utility method to see if a driver is registered
     */
    private static void dumpRegisteredDrivers() {
        System.out.println("\n+++ Loaded Drivers +++");

         DriverManager.drivers().forEach(d -> System.out.println("\t\t### Driver:" + d));

        System.out.println("++++++++++++++++++++++++");
    }

    /**
     * Utility method to load the LuckyDogDriver
     */
    private static void loadDriver() {
        try {
            Class.forName("luckydogtennis.LuckyDogDriver");
        } catch (ClassNotFoundException ex) {
            System.out.println("**** Error: luckydogtennis.LuckyDogDriver not found");
        }
        System.out.println("Driver Loaded");
    }

    /**
     * Utility method to see if a driver is registered
     */
    private static boolean isDriverRegistered(Driver d) {
        return DriverManager.drivers().filter(driver-> driver == d).findFirst().isPresent();

    }
}
