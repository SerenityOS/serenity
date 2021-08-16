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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.ObjectInputStream;
import java.sql.BatchUpdateException;
import java.sql.SQLException;
import java.util.Arrays;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import util.SerializedBatchUpdateException;
import util.BaseTest;

public class BatchUpdateExceptionTests extends BaseTest {

    private final int[] uc = {1, 2, 3};
    private final long[] luc = {1, 2, 3};

    private final String testSrcDir = System.getProperty("test.src", ".")
            + File.separatorChar;

    /**
     * Create BatchUpdateException and setting all objects to null
     */
    @Test
    public void test() {
        BatchUpdateException be = new BatchUpdateException(null,
                null, errorCode, (int[]) null, null);
        assertTrue(be.getMessage() == null && be.getSQLState() == null
                && be.getUpdateCounts() == null && be.getCause() == null
                && be.getLargeUpdateCounts() == null
                && be.getErrorCode() == errorCode);
    }

    /**
     * Create BatchUpdateException with no-arg constructor
     */
    @Test
    public void test1() {
        BatchUpdateException ex = new BatchUpdateException();
        assertTrue(ex.getMessage() == null
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0
                && ex.getUpdateCounts() == null
                && ex.getLargeUpdateCounts() == null);
    }

    /**
     * Create BatchUpdateException with null Throwable
     */
    @Test
    public void test2() {
        BatchUpdateException ex = new BatchUpdateException((Throwable) null);
        assertTrue(ex.getMessage() == null
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0
                && ex.getUpdateCounts() == null
                && ex.getLargeUpdateCounts() == null);
    }

    /**
     * Create BatchUpdateException with message and update counts
     */
    @Test
    public void test3() {

        BatchUpdateException ex = new BatchUpdateException(reason, uc);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0
                && Arrays.equals(ex.getUpdateCounts(), uc)
                && Arrays.equals(ex.getLargeUpdateCounts(), luc)
        );
    }

    /**
     * Create BatchUpdateException with update counts
     */
    @Test
    public void test4() {
        BatchUpdateException ex = new BatchUpdateException(uc);
        assertTrue(ex.getMessage() == null
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0
                && Arrays.equals(ex.getUpdateCounts(), uc)
                && Arrays.equals(ex.getLargeUpdateCounts(), luc)
        );
    }

    /**
     * Create BatchUpdateException with Throwable and update counts
     */
    @Test
    public void test5() {
        BatchUpdateException ex = new BatchUpdateException(uc, t);
        assertTrue(ex.getMessage().equals(cause)
                && ex.getSQLState() == null
                && cause.equals(ex.getCause().toString())
                && ex.getErrorCode() == 0
                && Arrays.equals(ex.getUpdateCounts(), uc)
                && Arrays.equals(ex.getLargeUpdateCounts(), luc)
        );
    }

    /**
     * Create BatchUpdateException with message, Throwable, and update counts
     */
    @Test
    public void test6() {
        BatchUpdateException ex = new BatchUpdateException(reason, uc, t);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState() == null
                && cause.equals(ex.getCause().toString())
                && ex.getErrorCode() == 0
                && Arrays.equals(ex.getUpdateCounts(), uc)
                && Arrays.equals(ex.getLargeUpdateCounts(), luc)
        );
    }

    /**
     * Create BatchUpdateException with message, SQLState, Throwable, and update
     * counts
     */
    @Test
    public void test7() {
        BatchUpdateException ex = new BatchUpdateException(reason, state, uc, t);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && cause.equals(ex.getCause().toString())
                && ex.getErrorCode() == 0
                && Arrays.equals(ex.getUpdateCounts(), uc)
                && Arrays.equals(ex.getLargeUpdateCounts(), luc)
        );
    }

    /**
     * Create BatchUpdateException with message, SQLState, errorCode code
     * Throwable, and update counts
     */
    @Test
    public void test8() {
        BatchUpdateException ex = new BatchUpdateException(reason, state, errorCode,
                uc, t);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && cause.equals(ex.getCause().toString())
                && ex.getErrorCode() == errorCode
                && Arrays.equals(ex.getUpdateCounts(), uc)
                && Arrays.equals(ex.getLargeUpdateCounts(), luc)
        );
    }

    /**
     * Create BatchUpdateException with message, SQLState, errorCode code
     * Throwable, and long [] update counts
     */
    @Test
    public void test9() {
        BatchUpdateException ex = new BatchUpdateException(reason, state, errorCode,
                luc, t);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState().equals(state)
                && cause.equals(ex.getCause().toString())
                && ex.getErrorCode() == errorCode
                && Arrays.equals(ex.getUpdateCounts(), uc)
                && Arrays.equals(ex.getLargeUpdateCounts(), luc)
        );
    }

    /**
     * Validate that a copy of the update counts array is made
     */
    @Test
    public void test10() {
        int[] uc1 = {1, 2};
        BatchUpdateException ex = new BatchUpdateException(uc1);
        assertTrue(Arrays.equals(ex.getUpdateCounts(), uc1));
        uc1[0] = 6689;
        assertFalse(Arrays.equals(ex.getUpdateCounts(), uc1));
    }

    /**
     * Validate that if null is specified for the update count, it is returned
     * as null
     */
    @Test
    public void test11() {
        BatchUpdateException ex = new BatchUpdateException((int[]) null);
        assertTrue(ex.getMessage() == null && ex.getSQLState() == null
                && ex.getErrorCode() == 0 && ex.getUpdateCounts() == null
                && ex.getLargeUpdateCounts() == null);
    }

    /**
     * Serialize a BatchUpdateException and make sure you can read it back
     * properly
     */
    @Test
    public void test12() throws Exception {
        BatchUpdateException be = new BatchUpdateException(reason, state, errorCode,
                uc, t);
        BatchUpdateException bue
                = createSerializedException(be);
        assertTrue(reason.equals(bue.getMessage())
                && bue.getSQLState().equals(state)
                && cause.equals(bue.getCause().toString())
                && bue.getErrorCode() == errorCode
                && Arrays.equals(bue.getLargeUpdateCounts(), luc)
                && Arrays.equals(bue.getUpdateCounts(), uc));
    }



    /**
     * De-Serialize a BatchUpdateException from JDBC 4.0 and make sure you can
     * read it back properly
     */
    @Test
    public void test13() throws Exception {
        String reason1 = "This was the error msg";
        String state1 = "user defined sqlState";
        String cause1 = "java.lang.Throwable: throw 1";
        int errorCode1 = 99999;
        Throwable t = new Throwable("throw 1");
        int[] uc1 = {1, 2, 21};
        long[] luc1 = {1, 2, 21};

        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(SerializedBatchUpdateException.DATA));
        BatchUpdateException bue = (BatchUpdateException) ois.readObject();
        assertTrue(reason1.equals(bue.getMessage())
                && bue.getSQLState().equals(state1)
                && bue.getErrorCode() == errorCode1
                && cause1.equals(bue.getCause().toString())
                && Arrays.equals(bue.getLargeUpdateCounts(), luc1)
                && Arrays.equals(bue.getUpdateCounts(), uc1));
    }

    /**
     * Serialize a BatchUpdateException with an Integer.MAX_VALUE + 1 and
     * validate you can read it back properly
     */
    @Test
    public void test14() throws Exception {
        int[] uc1 = {Integer.MAX_VALUE, Integer.MAX_VALUE + 1};
        long[] luc1 = {Integer.MAX_VALUE, Integer.MAX_VALUE + 1};
        BatchUpdateException be = new BatchUpdateException(reason, state, errorCode,
                luc1, t);
                BatchUpdateException bue
                = createSerializedException(be);
        assertTrue(reason.equals(bue.getMessage())
                && bue.getSQLState().equals(state)
                && cause.equals(bue.getCause().toString())
                && bue.getErrorCode() == errorCode
                && Arrays.equals(bue.getLargeUpdateCounts(), luc1)
                && Arrays.equals(bue.getUpdateCounts(), uc1));
    }

    /**
     * Validate that the ordering of the returned Exceptions is correct
     * using for-each loop
     */
    @Test
    public void test15() {
        BatchUpdateException ex = new BatchUpdateException("Exception 1", uc, t1);
        BatchUpdateException ex1 = new BatchUpdateException("Exception 2", uc);
        BatchUpdateException ex2 = new BatchUpdateException("Exception 3", uc, t2);
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
    public void test16() {
        BatchUpdateException ex = new BatchUpdateException("Exception 1", uc,  t1);
        BatchUpdateException ex1 = new BatchUpdateException("Exception 2", uc);
        BatchUpdateException ex2 = new BatchUpdateException("Exception 3", uc, t2);
        ex.setNextException(ex1);
        ex.setNextException(ex2);
        SQLException sqe = ex;
        int num = 0;
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
