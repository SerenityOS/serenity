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

import java.sql.ClientInfoStatus;
import java.sql.SQLClientInfoException;
import java.sql.SQLException;
import java.util.HashMap;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import util.BaseTest;

public class SQLClientInfoExceptionTests extends BaseTest {

    private final HashMap<String, ClientInfoStatus> map = new HashMap<>();

    public SQLClientInfoExceptionTests() {
        map.put("1", ClientInfoStatus.REASON_UNKNOWN_PROPERTY);
        map.put("21", ClientInfoStatus.REASON_UNKNOWN_PROPERTY);
    }

    /**
     * Create SQLClientInfoException and setting all objects to null
     */
    @Test
    public void test() {
        SQLClientInfoException e = new SQLClientInfoException(null);
        assertTrue(e.getMessage() == null && e.getSQLState() == null
                && e.getCause() == null && e.getErrorCode() == 0
                && e.getFailedProperties() == null);
    }

    /**
     * Create SQLClientInfoException with no-arg constructor
     */
    @Test
    public void test1() {
        SQLClientInfoException ex = new SQLClientInfoException();
        assertTrue(ex.getMessage() == null
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0
                && ex.getFailedProperties() == null);
    }

    /**
     * Create SQLClientInfoException with null Throwable
     */
    @Test
    public void test2() {

        SQLClientInfoException ex = new SQLClientInfoException(map, null);
        assertTrue(ex.getMessage() == null
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0
                && ex.getFailedProperties().equals(map));
    }

    /**
     * Create SQLClientInfoException with message
     */
    @Test
    public void test3() {
        SQLClientInfoException ex = new SQLClientInfoException(reason, map);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0
                && ex.getFailedProperties().equals(map));
    }

    /**
     * Create SQLClientInfoException with null Throwable
     */
    @Test
    public void test4() {
        SQLClientInfoException ex = new SQLClientInfoException(reason, map, null);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0
                && ex.getFailedProperties().equals(map));
    }

    /**
     * Create SQLClientInfoException with message, and SQLState
     */
    @Test
    public void test5() {
        SQLClientInfoException ex = new SQLClientInfoException(reason, state,
                map);

        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && ex.getCause() == null
                && ex.getErrorCode() == 0
                && ex.getFailedProperties().equals(map));
    }

    /**
     * Create SQLClientInfoException with message, and SQLState
     */
    @Test
    public void test6() {
        SQLClientInfoException ex = new SQLClientInfoException(reason, state,
                map, t);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && cause.equals(ex.getCause().toString())
                && ex.getErrorCode() == 0
                && ex.getFailedProperties().equals(map));
    }

    /**
     * Create SQLClientInfoException with message, SQLState, errorCode, and
     * Throwable
     */
    @Test
    public void test7() {
        SQLClientInfoException ex = new SQLClientInfoException(reason, state,
                errorCode, map);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && ex.getCause() == null
                && ex.getErrorCode() == errorCode
                && ex.getFailedProperties().equals(map));
    }

    /**
     * Create SQLClientInfoException with message, SQLState, and error code
     */
    @Test
    public void test8() {
        SQLClientInfoException ex = new SQLClientInfoException(reason, state,
                errorCode, map, t);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && cause.equals(ex.getCause().toString())
                && ex.getErrorCode() == errorCode
                && ex.getFailedProperties().equals(map));
    }

    /**
     * Serialize a SQLClientInfoException and make sure you can read it back
     * properly
     */
    @Test
    public void test10() throws Exception {
        SQLClientInfoException e = new SQLClientInfoException(reason, state,
                errorCode, map, t);
        SQLClientInfoException ex1 =
                createSerializedException(e);
        assertTrue(reason.equals(ex1.getMessage())
                && ex1.getSQLState().equals(state)
                && cause.equals(ex1.getCause().toString())
                && ex1.getErrorCode() == errorCode
                && ex1.getFailedProperties().equals(map));
    }

    /**
     * Validate that the ordering of the returned Exceptions is correct using
     * for-each loop
     */
    @Test
    public void test11() {
        SQLClientInfoException ex = new SQLClientInfoException("Exception 1",
                map, t1);
        SQLClientInfoException ex1 = new SQLClientInfoException("Exception 2",
                map);
        SQLClientInfoException ex2 = new SQLClientInfoException("Exception 3",
                map, t2);
        ex.setNextException(ex1);
        ex.setNextException(ex2);
        int num = 0;
        for (Throwable e : ex) {
            assertTrue(msgs[num++].equals(e.getMessage()));
        }
    }

    /**
     * Validate that the ordering of the returned Exceptions is correct using
     * traditional while loop
     */
    @Test
    public void test12() {
        SQLClientInfoException ex = new SQLClientInfoException("Exception 1",
                map, t1);
        SQLClientInfoException ex1 = new SQLClientInfoException("Exception 2",
                map);
        SQLClientInfoException ex2 = new SQLClientInfoException("Exception 3",
                map, t2);
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
}
