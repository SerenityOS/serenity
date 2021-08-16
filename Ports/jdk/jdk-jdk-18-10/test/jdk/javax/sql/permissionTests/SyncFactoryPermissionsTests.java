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
import java.security.AccessControlException;
import java.security.Policy;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.naming.Context;
import javax.sql.rowset.spi.SyncFactory;
import javax.sql.rowset.spi.SyncFactoryException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import util.BaseTest;
import util.StubContext;
import util.TestPolicy;
/*
 * @test
 * @library /java/sql/testng
 * @library /javax/sql/testng
 * @run testng/othervm -Djava.security.manager=allow SyncFactoryPermissionsTests
 * @summary Tests SyncFactory permissions.
 */
public class SyncFactoryPermissionsTests extends BaseTest {

    Context ctx;
    private static Policy policy;
    private static SecurityManager sm;
    private final Logger alogger = Logger.getLogger(this.getClass().getName());

    /*
     * Install a SeeurityManager along with a base Policy to allow testNG to run
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
     * Initialize a Context to be used in our tests.
     * Save off the original Policy and SecurityManager
     */
    public SyncFactoryPermissionsTests() {
        policy = Policy.getPolicy();
        sm = System.getSecurityManager();
        ctx = new StubContext();
    }

    /*
     * Validate that AccessControlException is thrown if
     * SQLPermission("setSyncFactory") has not been granted
     */
    @Test(expectedExceptions = AccessControlException.class)
    public void test() throws Exception {
        setPolicy(new TestPolicy());
        SyncFactory.setJNDIContext(ctx);
    }

    /*
     * Validate that a SyncFactoryException is thrown if the Logger is null
     */
    @Test(expectedExceptions = SyncFactoryException.class)
    public void test00() throws SyncFactoryException {
        Logger l = SyncFactory.getLogger();
    }

    /*
     * Validate that setJNDIContext succeeds if SQLPermission("setSyncFactory")
     * has been granted
     */
    @Test
    public void test01() throws Exception {
        setPolicy(new TestPolicy("setSyncFactory"));
        SyncFactory.setJNDIContext(ctx);
    }

    /*
     * Validate that setJNDIContext succeeds if AllPermissions has been granted
     */
    @Test
    public void test02() throws Exception {
        setPolicy(new TestPolicy("all"));
        SyncFactory.setJNDIContext(ctx);
    }

    /*
     * Validate that AccessControlException is thrown if
     * SQLPermission("setSyncFactory") has not been granted
     */
    @Test(expectedExceptions = AccessControlException.class)
    public void test03() throws Exception {
        setPolicy(new TestPolicy());
        SyncFactory.setLogger(alogger);
    }

    /*
     * Validate that setLogger succeeds if SQLPermission("setSyncFactory")
     * has been granted
     */
    @Test
    public void test04() throws Exception {
        setPolicy(new TestPolicy("setSyncFactory"));
        SyncFactory.setLogger(alogger);
    }

    /*
     * Validate that setLogger succeeds if AllPermissions has been granted
     */
    @Test
    public void test05() throws Exception {
        setPolicy(new TestPolicy("all"));
        SyncFactory.setLogger(alogger);
    }

    /*
     * Validate that AccessControlException is thrown if
     * SQLPermission("setSyncFactory") has not been granted
     */
    @Test(expectedExceptions = AccessControlException.class)
    public void test06() throws Exception {
        setPolicy(new TestPolicy());
        SyncFactory.setLogger(alogger, Level.INFO);
    }

    /*
     * Validate that AccessControlException is thrown if
     * SQLPermission("setSyncFactory")  and LoggingPermission("control", null)
     * have not been granted
     */
    @Test(expectedExceptions = AccessControlException.class)
    public void test07() throws Exception {
        setPolicy(new TestPolicy("setSyncFactory"));
        SyncFactory.setLogger(alogger, Level.INFO);
    }

    /*
     * Validate that setLogger succeeds if SQLPermission("setSyncFactory")
     * has been granted
     */
    @Test
    public void test08() throws Exception {
        setPolicy(new TestPolicy("setSyncFactoryLogger"));
        SyncFactory.setLogger(alogger, Level.INFO);
    }

    /*
     * Validate that setLogger succeeds if AllPermissions has been granted
     */
    @Test
    public void test09() throws Exception {
        setPolicy(new TestPolicy("all"));
        SyncFactory.setLogger(alogger, Level.INFO);
    }
}
