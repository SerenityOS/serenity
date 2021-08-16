/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
package test.rowset.spi;

import com.sun.rowset.internal.SyncResolverImpl;
import java.sql.SQLException;
import javax.sql.rowset.spi.SyncProviderException;
import javax.sql.rowset.spi.SyncResolver;

import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import util.BaseTest;
import util.StubSyncResolver;

public class SyncProviderExceptionTests extends BaseTest {

    // Used by SyncProviderException::getSyncResolver tests
    private SyncResolver resolver;

    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println(System.getProperty("java.naming.factory.initial"));
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setupTest() {
        resolver = new SyncProviderException().getSyncResolver();
    }

    /*
     * Create SyncProviderException with no-arg constructor
     */
    @Test
    public void test() {
        SyncProviderException ex = new SyncProviderException();
        assertTrue(ex.getMessage() == null
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0
                && ex.getSyncResolver() instanceof SyncResolverImpl);
    }

    /*
     * Create SyncProviderException with no-arg constructor and
     * call setSyncResolver to indicate the SyncResolver to use
     */
    @Test
    public void test01() {
        SyncProviderException ex = new SyncProviderException();
        ex.setSyncResolver(new StubSyncResolver());
        assertTrue(ex.getMessage() == null
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0
                && ex.getSyncResolver() instanceof StubSyncResolver);
    }

    /*
     * Create SyncProviderException with message
     */
    @Test
    public void test02() {
        SyncProviderException ex = new SyncProviderException(reason);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0
                && ex.getSyncResolver() instanceof SyncResolverImpl);
    }

    /*
     * Create SyncProviderException with message and
     * call setSyncResolver to indicate the SyncResolver to use
     */
    @Test
    public void test03() {
        SyncProviderException ex = new SyncProviderException(reason);
        ex.setSyncResolver(new StubSyncResolver());

        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0
                && ex.getSyncResolver() instanceof StubSyncResolver);
    }

    /*
     * Create SyncProviderException with and specify the SyncResolver to use
     */
    @Test
    public void test04() {
        SyncProviderException ex = new SyncProviderException(new StubSyncResolver());
        assertTrue(ex.getMessage() == null
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0
                && ex.getSyncResolver() instanceof StubSyncResolver);
    }

    /*
     * Validate that the ordering of the returned Exceptions is correct using
     * for-each loop
     */
    @Test
    public void test05() {
        SyncProviderException ex = new SyncProviderException("Exception 1");
        ex.initCause(t1);
        SyncProviderException ex1 = new SyncProviderException("Exception 2");
        SyncProviderException ex2 = new SyncProviderException("Exception 3");
        ex2.initCause(t2);
        ex.setNextException(ex1);
        ex.setNextException(ex2);
        int num = 0;
        for (Throwable e : ex) {
            assertTrue(msgs[num++].equals(e.getMessage()));
        }
    }

    /*
     * Validate that the ordering of the returned Exceptions is correct using
     * traditional while loop
     */
    @Test
    public void test06() {
        SQLException ex = new SyncProviderException("Exception 1");
        ex.initCause(t1);
        SyncProviderException ex1 = new SyncProviderException("Exception 2");
        SyncProviderException ex2 = new SyncProviderException("Exception 3");
        ex2.initCause(t2);
        ex.setNextException(ex1);
        ex.setNextException(ex2);
        int num = 0;
        while (ex != null) {
            assertTrue(msgs[num++].equals(ex.getMessage()));
            Throwable c = ex.getCause();
            while (c != null) {
                assertTrue(msgs[num++].equals(c.getMessage()));
                c = c.getCause();
            }
            ex = ex.getNextException();
        }
    }

    /*
     * Serialize a SyncProviderException and make sure you can read it back properly
     */
    @Test
    public void test07() throws Exception {
        SyncProviderException e = new SyncProviderException(reason);
        SyncProviderException ex1 = createSerializedException(e);
        assertTrue(ex1.getMessage().equals(reason)
                && ex1.getSQLState() == null
                && ex1.getCause() == null
                && ex1.getErrorCode() == 0
                && ex1.getSyncResolver() instanceof SyncResolverImpl, ex1.getSyncResolver().getClass().getName());
    }

    /*
     * Serialize a SyncProviderException and make sure you can read it back properly
     */
    @Test
    public void test08() throws Exception {
        SyncProviderException e = new SyncProviderException(reason);
        e.setSyncResolver(new StubSyncResolver());

        SyncProviderException ex1 = createSerializedException(e);
        assertTrue(ex1.getMessage().equals(reason)
                && ex1.getSQLState() == null
                && ex1.getCause() == null
                && ex1.getErrorCode() == 0
                && ex1.getSyncResolver() instanceof StubSyncResolver);
    }

    /*
     * Validate SyncResolver::getStatus() succeeds
     */
    @Test
    public void testgetStatus() {
        int status = resolver.getStatus();
    }

    /*
     * Validate SyncResolver::nextConflict() succeeds
     */
    @Test
    public void testnextConflict() throws SQLException {
         boolean result = resolver.nextConflict();
    }

    /*
     * Validate SyncResolver::previousConflict() succeeds
     */
    @Test
    public void testPreviousConflict() throws SQLException {
        boolean result =  resolver.previousConflict();
    }

    /*
     * Validate SyncResolver::getConflictValue() throws a SQLException
     */
    @Test
    public void testConflictedValueByInt() throws SQLException {
        assertThrows(SQLException.class, () ->resolver.getConflictValue(1));
    }

    /*
     * Validate SyncResolver::getConflictValue() throws a SQLException
     */
    @Test
    public void testConflictedValueByName() throws SQLException {
        assertThrows(SQLException.class, () -> resolver.getConflictValue("foo"));
    }

    /*
     * Validate SyncResolver::setResolvedValue() throws a SQLException
     */
    @Test
    public void testSetResolvedValueByInt() throws SQLException {
        assertThrows(SQLException.class, () -> resolver.setResolvedValue(1, "foo"));
    }

    /*
     * Validate SyncResolver::getConflictValue() throws a SQLException
     */
    @Test
    public void testSetResolvedValueByName() throws SQLException {
        assertThrows(SQLException.class, () -> resolver.setResolvedValue("foo", "bar"));
    }
}
