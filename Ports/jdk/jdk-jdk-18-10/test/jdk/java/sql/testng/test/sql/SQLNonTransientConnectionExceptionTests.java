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

import java.sql.SQLException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLNonTransientException;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import util.BaseTest;

public class SQLNonTransientConnectionExceptionTests extends BaseTest {

    /**
     * Create SQLNonTransientConnectionException and setting all objects to null
     */
    @Test
    public void test() {
        SQLNonTransientConnectionException e =
                new SQLNonTransientConnectionException(null,
                null, errorCode, null);
        assertTrue(e.getMessage() == null && e.getSQLState() == null
                && e.getCause() == null && e.getErrorCode() == errorCode);
    }

    /**
     * Create SQLNonTransientConnectionException with no-arg constructor
     */
    @Test
    public void test1() {
        SQLNonTransientConnectionException ex =
                new SQLNonTransientConnectionException();
        assertTrue(ex.getMessage() == null
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0);
    }

    /**
     * Create SQLNonTransientConnectionException with message
     */
    @Test
    public void test2() {
        SQLNonTransientConnectionException ex =
                new SQLNonTransientConnectionException(reason);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0);
    }

    /**
     * Create SQLNonTransientConnectionException with message, and SQLState
     */
    @Test
    public void test3() {
        SQLNonTransientConnectionException ex =
                new SQLNonTransientConnectionException(reason, state);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && ex.getCause() == null
                && ex.getErrorCode() == 0);
    }

    /**
     * Create SQLNonTransientConnectionException with message, SQLState, and error code
     */
    @Test
    public void test4() {
        SQLNonTransientConnectionException ex =
                new SQLNonTransientConnectionException(reason, state, errorCode);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && ex.getCause() == null
                && ex.getErrorCode() == errorCode);
    }

    /**
     * Create SQLNonTransientConnectionException with message, SQLState, errorCode, and Throwable
     */
    @Test
    public void test5() {
        SQLNonTransientConnectionException ex =
                new SQLNonTransientConnectionException(reason, state, errorCode, t);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && cause.equals(ex.getCause().toString())
                && ex.getErrorCode() == errorCode);
    }

    /**
     * Create SQLNonTransientConnectionException with message, SQLState, and Throwable
     */
    @Test
    public void test6() {
        SQLNonTransientConnectionException ex =
                new SQLNonTransientConnectionException(reason, state, t);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && cause.equals(ex.getCause().toString())
                && ex.getErrorCode() == 0);
    }

    /**
     * Create SQLNonTransientConnectionException with message, and Throwable
     */
    @Test
    public void test7() {
        SQLNonTransientConnectionException ex =
                new SQLNonTransientConnectionException(reason, t);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState() == null
                && cause.equals(ex.getCause().toString())
                && ex.getErrorCode() == 0);
    }

    /**
     * Create SQLNonTransientConnectionException with null Throwable
     */
    @Test
    public void test8() {
        SQLNonTransientConnectionException ex =
                new SQLNonTransientConnectionException((Throwable)null);
        assertTrue(ex.getMessage() == null
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0);
    }

    /**
     * Create SQLNonTransientConnectionException with Throwable
     */
    @Test
    public void test9() {
        SQLNonTransientConnectionException ex =
                new SQLNonTransientConnectionException(t);
        assertTrue(ex.getMessage().equals(cause)
                && ex.getSQLState() == null
                && cause.equals(ex.getCause().toString())
                && ex.getErrorCode() == 0);
    }

    /**
     * Serialize a SQLNonTransientConnectionException and make sure you can
     * read it back properly
     */
    @Test
    public void test10() throws Exception {
        SQLNonTransientConnectionException e =
                new SQLNonTransientConnectionException(reason, state, errorCode, t);
        SQLNonTransientConnectionException ex1 =
                createSerializedException(e);
        assertTrue(reason.equals(ex1.getMessage())
                && ex1.getSQLState().equals(state)
                && cause.equals(ex1.getCause().toString())
                && ex1.getErrorCode() == errorCode);
    }

    /**
     * Validate that the ordering of the returned Exceptions is correct
     * using for-each loop
     */
    @Test
    public void test11() {
        SQLNonTransientConnectionException ex =
                new SQLNonTransientConnectionException("Exception 1", t1);
        SQLNonTransientConnectionException ex1 =
                new SQLNonTransientConnectionException("Exception 2");
        SQLNonTransientConnectionException ex2 =
                new SQLNonTransientConnectionException("Exception 3", t2);
        ex.setNextException(ex1);
        ex.setNextException(ex2);
        int num = 0;
        for (Throwable e : ex) {
            assertTrue(msgs[num++].equals(e.getMessage()));
        }
    }

    /**
     * Validate that the ordering of the returned Exceptions is correct
     * using traditional while loop
     */
    @Test
    public void test12() {
        SQLNonTransientConnectionException ex =
                new SQLNonTransientConnectionException("Exception 1", t1);
        SQLNonTransientConnectionException ex1 =
                new SQLNonTransientConnectionException("Exception 2");
        SQLNonTransientConnectionException ex2 =
                new SQLNonTransientConnectionException("Exception 3", t2);
        ex.setNextException(ex1);
        ex.setNextException(ex2);
        int num = 0;
        SQLException sqe = ex;
        while (sqe != null) {
            assertTrue(msgs[num++].equals(sqe.getMessage()));
            Throwable c = sqe.getCause();
            while (c != null) {
                assertTrue(msgs[num++].equals(c.getMessage()));
                c = c.getCause();
            }
            sqe = sqe.getNextException();
        }
    }

    /**
     * Create SQLNonTransientConnectionException and validate it is an instance of
     * SQLNonTransientException
     */
    @Test
    public void test13() {
        Exception ex = new SQLNonTransientConnectionException();
        assertTrue(ex instanceof SQLNonTransientException);
    }
}
