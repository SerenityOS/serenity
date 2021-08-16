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
import java.sql.SQLWarning;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import util.BaseTest;

public class SQLWarningTests extends BaseTest {

    private final String[] warnings = {"Warning 1", "cause 1", "Warning 2",
            "Warning 3", "cause 2"};

    /**
     * Create SQLWarning and setting all objects to null
     */
    @Test
    public void test() {
        SQLWarning e = new SQLWarning(null, null, errorCode, null);
        assertTrue(e.getMessage() == null && e.getSQLState() == null
                && e.getCause() == null && e.getErrorCode() == errorCode);
    }

    /**
     * Create SQLWarning with no-arg constructor
     */
    @Test
    public void test1() {
        SQLWarning ex = new SQLWarning();
        assertTrue(ex.getMessage() == null
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0);
    }

    /**
     * Create SQLWarning with message
     */
    @Test
    public void test2() {
        SQLWarning ex = new SQLWarning(reason);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0);
    }

    /**
     * Create SQLWarning with message, and SQLState
     */
    @Test
    public void test3() {

        SQLWarning ex = new SQLWarning(reason, state);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && ex.getCause() == null
                && ex.getErrorCode() == 0);
    }

    /**
     * Create SQLWarning with message, SQLState, and error code
     */
    @Test
    public void test4() {
        SQLWarning ex = new SQLWarning(reason, state, errorCode);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && ex.getCause() == null
                && ex.getErrorCode() == errorCode);
    }

    /**
     * Create SQLWarning with message, SQLState, errorCode, and Throwable
     */
    @Test
    public void test5() {
        SQLWarning ex = new SQLWarning(reason, state, errorCode, t);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && cause.equals(ex.getCause().toString())
                && ex.getErrorCode() == errorCode);
    }

    /**
     * Create SQLWarning with message, SQLState, and Throwable
     */
    @Test
    public void test6() {
        SQLWarning ex = new SQLWarning(reason, state, t);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && cause.equals(ex.getCause().toString())
                && ex.getErrorCode() == 0);
    }

    /**
     * Create SQLWarning with message, and Throwable
     */
    @Test
    public void test7() {
        SQLWarning ex = new SQLWarning(reason, t);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState() == null
                && cause.equals(ex.getCause().toString())
                && ex.getErrorCode() == 0);
    }

    /**
     * Create SQLWarning with null Throwable
     */
    @Test
    public void test8() {
        SQLWarning ex = new SQLWarning((Throwable) null);
        assertTrue(ex.getMessage() == null
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0);
    }

    /**
     * Create SQLWarning with Throwable
     */
    @Test
    public void test9() {
        SQLWarning ex = new SQLWarning(t);
        assertTrue(ex.getMessage().equals(cause)
                && ex.getSQLState() == null
                && cause.equals(ex.getCause().toString())
                && ex.getErrorCode() == 0);
    }

    /**
     * Serialize a SQLWarning and make sure you can read it back properly
     */
    @Test
    public void test10() throws Exception {
        SQLWarning e = new SQLWarning(reason, state, errorCode, t);
        SQLWarning ex1 = createSerializedException(e);
        assertTrue(reason.equals(ex1.getMessage())
                && ex1.getSQLState().equals(state)
                && cause.equals(ex1.getCause().toString())
                && ex1.getErrorCode() == errorCode);
    }

    /**
     * Validate that the ordering of the returned Exceptions is correct using
     * for-each loop
     */
    @Test
    public void test11() {
        SQLWarning ex = new SQLWarning("Exception 1", t1);
        SQLWarning ex1 = new SQLWarning("Exception 2");
        SQLWarning ex2 = new SQLWarning("Exception 3", t2);
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
        SQLWarning ex = new SQLWarning("Exception 1", t1);
        SQLWarning ex1 = new SQLWarning("Exception 2");
        SQLWarning ex2 = new SQLWarning("Exception 3", t2);
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
     * Validate that the ordering of the returned SQLWarning is correct using
     * for-each loop
     */
    @Test
    public void test13() {
        SQLWarning ex = new SQLWarning("Warning 1", t1);
        SQLWarning ex1 = new SQLWarning("Warning 2");
        SQLWarning ex2 = new SQLWarning("Warning 3", t2);
        ex.setNextWarning(ex1);
        ex.setNextWarning(ex2);
        int num = 0;
        for (Throwable e : ex) {
            assertTrue(warnings[num++].equals(e.getMessage()));
        }
    }

    /**
     * Validate that the ordering of the returned SQLWarning is correct using
     * traditional while loop
     */
    @Test
    public void test14() {
        SQLWarning ex = new SQLWarning("Warning 1", t1);
        SQLWarning ex1 = new SQLWarning("Warning 2");
        SQLWarning ex2 = new SQLWarning("Warning 3", t2);
        ex.setNextWarning(ex1);
        ex.setNextWarning(ex2);
        int num = 0;
        SQLWarning sqe = ex;
        while (sqe != null) {
            assertTrue(warnings[num++].equals(sqe.getMessage()));
            Throwable c = sqe.getCause();
            while (c != null) {
                assertTrue(msgs[num++].equals(c.getMessage()));
                c = c.getCause();
            }
            sqe = sqe.getNextWarning();
        }
    }
}
