/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
package test.transaction;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import javax.transaction.xa.XAException;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import util.SerializedTransactionExceptions;

public class XAExceptionTests {

    protected final String reason = "reason";

    /**
     * Create XAException with no-arg constructor
     */
    @Test
    public void test1() {
        XAException ex = new XAException();
        assertTrue(ex.getMessage() == null
                && ex.getCause() == null
                && ex.errorCode == 0);
    }

    /**
     * Create XAException with message
     */
    @Test
    public void test2() {
        XAException ex = new XAException(reason);
        assertTrue(ex.getMessage().equals(reason)
                && ex.getCause() == null
                && ex.errorCode == 0);
    }

    /**
     * De-Serialize a XAException from JDBC 4.0 and make sure you can read it
     * back properly
     */
    @Test
    public void test3() throws Exception {

        ObjectInputStream ois = new ObjectInputStream(
                new ByteArrayInputStream(SerializedTransactionExceptions.XAE_DATA));
        XAException ex = (XAException) ois.readObject();
        assertTrue(reason.equals(ex.getMessage())
                && ex.getCause() == null
                && ex.errorCode == 0);
    }

    /**
     * Create XAException specifying an error code
     */
    @Test
    public void test4() {
        int error = 21;
        XAException ex = new XAException(error);
        assertTrue(ex.getMessage() == null
                && ex.getCause() == null
                && ex.errorCode == error);
    }

    /**
     * Create XAException specifying an error code
     */
    @Test
    public void test5() {
        int error = 21;
        XAException ex = new XAException(error);
        ex.errorCode = error;
        assertTrue(ex.getMessage() == null
                && ex.getCause() == null
                && ex.errorCode == error);
    }

}
