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
package test.rowset;

import java.sql.SQLException;
import javax.sql.rowset.RowSetWarning;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import util.BaseTest;

public class RowSetWarningTests extends BaseTest {

    private final String[] warnings = {"Warning 1", "cause 1", "Warning 2",
        "Warning 3", "cause 2"};

    /*
     * Create RowSetWarning and setting all objects to null
     */
    @Test
    public void test() {
        RowSetWarning e = new RowSetWarning(null, null, errorCode);
        assertTrue(e.getMessage() == null && e.getSQLState() == null
                && e.getCause() == null && e.getErrorCode() == errorCode);
    }

    /*
     * Create RowSetWarning with no-arg constructor
     */
    @Test
    public void test01() {
        RowSetWarning ex = new RowSetWarning();
        assertTrue(ex.getMessage() == null
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0);
    }

    /*
     * Create RowSetWarning with message
     */
    @Test
    public void test02() {
        RowSetWarning ex = new RowSetWarning(reason);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0);
    }

    /*
     * Create RowSetWarning with message, and SQLState
     */
    @Test
    public void test03() {

        RowSetWarning ex = new RowSetWarning(reason, state);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && ex.getCause() == null
                && ex.getErrorCode() == 0);
    }

    /*
     * Create RowSetWarning with message, SQLState, and error code
     */
    @Test
    public void test04() {
        RowSetWarning ex = new RowSetWarning(reason, state, errorCode);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && ex.getCause() == null
                && ex.getErrorCode() == errorCode);
    }

    /*
     * Serialize a RowSetWarning and make sure you can read it back properly
     */
    @Test
    public void test05() throws Exception {
        RowSetWarning e = new RowSetWarning(reason, state, errorCode);
        e.initCause(t);
        RowSetWarning ex1 = createSerializedException(e);
        assertTrue(reason.equals(ex1.getMessage())
                && ex1.getSQLState().equals(state)
                && cause.equals(ex1.getCause().toString())
                && ex1.getErrorCode() == errorCode);
    }

    /*
     * Validate that the ordering of the returned Exceptions is correct using
     * for-each loop
     */
    @Test
    public void test06() {
        RowSetWarning ex = new RowSetWarning("Exception 1");
        ex.initCause(t1);
        RowSetWarning ex1 = new RowSetWarning("Exception 2");
        RowSetWarning ex2 = new RowSetWarning("Exception 3");
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
    public void test07() {
        RowSetWarning ex = new RowSetWarning("Exception 1");
        ex.initCause(t1);
        RowSetWarning ex1 = new RowSetWarning("Exception 2");
        RowSetWarning ex2 = new RowSetWarning("Exception 3");
        ex2.initCause(t2);
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

    /*
     * Validate that the ordering of the returned RowSetWarning is correct using
     * for-each loop
     */
    @Test
    public void test08() {
        RowSetWarning ex = new RowSetWarning("Warning 1");
        ex.initCause(t1);
        RowSetWarning ex1 = new RowSetWarning("Warning 2");
        RowSetWarning ex2 = new RowSetWarning("Warning 3");
        ex2.initCause(t2);
        ex.setNextWarning(ex1);
        ex.setNextWarning(ex2);
        int num = 0;
        for (Throwable e : ex) {
            assertTrue(warnings[num++].equals(e.getMessage()));
        }
    }

    /**
     * Validate that the ordering of the returned RowSetWarning is correct using
     * traditional while loop
     */
    @Test
    public void test09() {
        RowSetWarning ex = new RowSetWarning("Warning 1");
        ex.initCause(t1);
        RowSetWarning ex1 = new RowSetWarning("Warning 2");
        RowSetWarning ex2 = new RowSetWarning("Warning 3");
        ex2.initCause(t2);
        ex.setNextWarning(ex1);
        ex.setNextWarning(ex2);
        int num = 0;
        RowSetWarning sqe = ex;
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

    /*
     * Serialize a RowSetWarning and make sure you can read it back properly
     */
    @Test
    public void test10() throws Exception {
        RowSetWarning e = new RowSetWarning(reason, state, errorCode);
        RowSetWarning ex1 = createSerializedException(e);
        assertTrue(reason.equals(ex1.getMessage())
                && ex1.getSQLState().equals(state)
                && ex1.getCause() == null
                && ex1.getErrorCode() == errorCode);
    }

    /*
     *  Serialize a RowSetWarning and make sure you can read it back properly.
     * Validate that the ordering of the returned RowSetWarning is correct using
     * traditional while loop
     */
    @Test
    public void test11() throws Exception {
        RowSetWarning ex = new RowSetWarning("Warning 1");
        ex.initCause(t1);
        RowSetWarning ex1 = new RowSetWarning("Warning 2");
        RowSetWarning ex2 = new RowSetWarning("Warning 3");
        ex2.initCause(t2);
        ex.setNextWarning(ex1);
        ex.setNextWarning(ex2);
        int num = 0;
        RowSetWarning sqe = createSerializedException(ex);
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
