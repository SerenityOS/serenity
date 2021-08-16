/*
 * Copyright (c) 2014, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import util.BaseTest;
import util.StubDriver;
import util.TestPolicy;

import java.security.AccessControlException;
import java.security.Policy;
import java.sql.DriverManager;
import java.sql.SQLException;

/*
 * @test
 * @library /java/sql/testng
 * @run testng/othervm -Djava.security.manager=allow DriverManagerPermissionsTests
 * @summary Tests that a JDBC Driver that is a module can be loaded
 * via the service-provider loading mechanism.
 */
public class DriverManagerPermissionsTests extends BaseTest {

    private  static Policy policy;
    private static SecurityManager sm;

    /*
     * Install a SecurityManager along with a base Policy to allow testNG to run
     */
    @BeforeClass
    public static void setUpClass() throws Exception {
        setPolicy(new TestPolicy());
        System.setSecurityManager(new SecurityManager());
    }

    /*
     * Install the original Policy and SecurityManager
     */
    @AfterClass
    public static void tearDownClass() throws Exception {
        System.setSecurityManager(sm);
        setPolicy(policy);
    }

    /*
     * Save off the original Policy and SecurityManager
     */
    public DriverManagerPermissionsTests() {
        policy = Policy.getPolicy();
        sm = System.getSecurityManager();
    }

    /*
     * Validate that AccessControlException is thrown if SQLPermission("setLog")
     * has not been granted
     */
    @Test(expectedExceptions = AccessControlException.class)
    public void test() {
        setPolicy(new TestPolicy());
        DriverManager.setLogStream(null);
    }

    /*
     * Validate that setLogStream succeeds if SQLPermission("setLog") has been
     * granted
     */
    @Test
    public void test1() {
        Policy.setPolicy(new TestPolicy("setLog"));
        DriverManager.setLogStream(null);
    }

    /*
     * Validate that setLogStream succeeds if AllPermissions has been granted
     */
    @Test
    public void test2() {
        setPolicy(new TestPolicy("all"));
        DriverManager.setLogStream(null);
    }

    /*
     * Validate that AccessControlException is thrown if SQLPermission("setLog")
     * has not been granted
     */
    @Test(expectedExceptions = AccessControlException.class)
    public void test4() {
        setPolicy(new TestPolicy());
        DriverManager.setLogWriter(null);
    }

    /*
     * Validate that setLogWriter succeeds if SQLPermission("setLog") has been
     * granted
     */
    @Test
    public void test5() {
        setPolicy(new TestPolicy("setLog"));
        DriverManager.setLogWriter(null);
    }

    /*
     * Validate that setLogWriter succeeds if AllPermissions has been granted
     */
    @Test
    public void test6() {
        setPolicy(new TestPolicy("all"));
        DriverManager.setLogWriter(null);
    }

    /*
     * Validate that AccessControlException is thrown if
     * SQLPermission("deregisterDriver") has not been granted
     */
    @Test(expectedExceptions = AccessControlException.class)
    public void test7() throws SQLException {
        setPolicy(new TestPolicy());
        DriverManager.deregisterDriver(new StubDriver());
    }

    /*
     * Validate that deregisterDriver succeeds if
     * SQLPermission("deregisterDriver") has been granted
     */
    @Test
    public void test8() throws SQLException {
        setPolicy(new TestPolicy("deregisterDriver"));
        DriverManager.deregisterDriver(new StubDriver());
    }

    /*
     * Validate that deregisterDriver succeeds if AllPermissions has been
     * granted
     */
    @Test
    public void test9() throws SQLException {
        setPolicy(new TestPolicy("all"));
        DriverManager.deregisterDriver(new StubDriver());
    }
}
