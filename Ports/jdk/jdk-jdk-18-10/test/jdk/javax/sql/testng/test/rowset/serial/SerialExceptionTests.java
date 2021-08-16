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
package test.rowset.serial;

import java.sql.SQLException;
import javax.sql.rowset.serial.SerialException;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import util.BaseTest;

public class SerialExceptionTests extends BaseTest {

    /*
     * Create SerialException with no-arg constructor
     */
    @Test
    public void test01() {
        SerialException ex = new SerialException();
        assertTrue(ex.getMessage() == null
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0);
    }

    /*
     * Create SerialException with message
     */
    @Test
    public void test02() {
        SerialException ex = new SerialException(reason);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getSQLState() == null
                && ex.getCause() == null
                && ex.getErrorCode() == 0);
    }

    /*
     * Validate that the ordering of the returned Exceptions is correct using
     * for-each loop
     */
    @Test
    public void test03() {
        SerialException ex = new SerialException("Exception 1");
        ex.initCause(t1);
        SerialException ex1 = new SerialException("Exception 2");
        SerialException ex2 = new SerialException("Exception 3");
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
    public void test04() {
        SQLException ex = new SerialException("Exception 1");
        ex.initCause(t1);
        SerialException ex1 = new SerialException("Exception 2");
        SerialException ex2 = new SerialException("Exception 3");
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
     * Serialize a SerialException and make sure you can read it back properly
     */
    @Test
    public void test05() throws Exception {
        SerialException e = new SerialException(reason);
        SerialException ex1 = createSerializedException(e);
        assertTrue(ex1.getMessage().equals(reason)
                && ex1.getSQLState() == null
                && ex1.getCause() == null
                && ex1.getErrorCode() == 0);;
    }
}
