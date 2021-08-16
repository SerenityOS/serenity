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
package util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.Policy;
import java.sql.JDBCType;
import java.sql.SQLException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;

public class BaseTest {

    protected final String reason = "reason";
    protected final String state = "SQLState";
    protected final String cause = "java.lang.Throwable: cause";
    protected final Throwable t = new Throwable("cause");
    protected final Throwable t1 = new Throwable("cause 1");
    protected final Throwable t2 = new Throwable("cause 2");
    protected final int errorCode = 21;
    protected final String[] msgs = {"Exception 1", "cause 1", "Exception 2",
        "Exception 3", "cause 2"};

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /*
     * Take some form of SQLException, serialize and deserialize it
     */
    @SuppressWarnings("unchecked")
    protected <T extends SQLException> T
            createSerializedException(T ex)
            throws IOException, ClassNotFoundException {
        return (T) serializeDeserializeObject(ex);
    }

    /*
     * Utility method to serialize and deserialize an object
     */
    @SuppressWarnings("unchecked")
    protected <T> T serializeDeserializeObject(T o)
            throws IOException, ClassNotFoundException {
        T o1;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(o);
        }
        try (ObjectInputStream ois
                = new ObjectInputStream(new ByteArrayInputStream(baos.toByteArray()))) {
            o1 = (T) ois.readObject();
        }
        return o1;
    }

    /*
     * Utility Method used to set the current Policy
     */
    protected static void setPolicy(Policy p) {
        Policy.setPolicy(p);
    }

    /*
     * DataProvider used to specify the value to set and check for
     * methods using boolean values
     */
    @DataProvider(name = "trueFalse")
    protected Object[][] trueFalse() {
        return new Object[][]{
            {true},
            {false}
        };
    }

    /*
     * DataProvider used to specify the standard JDBC Types
     */
    @DataProvider(name = "jdbcTypes")
    protected Object[][] jdbcTypes() {
        Object[][] o = new Object[JDBCType.values().length][1];
        int pos = 0;
        for (JDBCType c : JDBCType.values()) {
            o[pos++][0] = c.getVendorTypeNumber();
        }
        return o;
    }
}
