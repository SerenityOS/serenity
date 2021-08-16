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

import java.sql.Array;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.sql.rowset.serial.SerialArray;
import javax.sql.rowset.serial.SerialException;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import util.BaseTest;
import util.StubArray;

public class SerialArrayTests extends BaseTest {

    private Object[] coffees;
    private final String sqlType = "VARCHAR";
    private Array a;
    private Map<String, Class<?>> map;

    @BeforeMethod
    public void setUpMethod() throws Exception {
        coffees = new Object[]{"Espresso", "Colombian", "French Roast",
            "Cappuccino"};
        a = new StubArray(sqlType, coffees);
        map = new HashMap<>();
    }

    /*
     * Validate a SerialArray can be created from an Array
     */
    @Test
    public void test01() throws Exception {
        SerialArray sa = new SerialArray(a);
    }

    /*
     * Validate a SQLException is thrown if the map is null
     */
    @Test(expectedExceptions = SQLException.class)
    public void test02() throws Exception {
        SerialArray sa = new SerialArray(a, null);
    }

    /*
     * Validate a SerialException is thrown when getResultSet() is called
     */
    @Test(expectedExceptions = SerialException.class)
    public void test03() throws Exception {
        SerialArray sa = new SerialArray(a);
        sa.getResultSet();
    }

    /*
     * Validate a SerialException is thrown when getResultSet() is called
     */
    @Test(expectedExceptions = SerialException.class)
    public void test04() throws Exception {
        SerialArray sa = new SerialArray(a);
        sa.getResultSet(null);
    }

    /*
     * Validate a SerialException is thrown when getResultSet() is called
     */
    @Test(expectedExceptions = SerialException.class)
    public void test05() throws Exception {
        SerialArray sa = new SerialArray(a);
        sa.getResultSet(1, 1);
    }

    /*
     * Validate a SerialException is thrown when getResultSet() is called
     */
    @Test(expectedExceptions = SerialException.class)
    public void test06() throws Exception {
        SerialArray sa = new SerialArray(a);
        sa.getResultSet(1, 1, null);
    }

    /*
     * Validate a SerialException is thrown when  getArray() is invoked after
     * free() is called
     */
    @Test(expectedExceptions = SerialException.class)
    public void test07() throws Exception {
        SerialArray sa = new SerialArray(a);
        sa.free();
        sa.getArray();
    }

    /*
     * Validate a SerialException is thrown when  getArray() is invoked after
     * free() is called
     */
    @Test(expectedExceptions = SerialException.class)
    public void test08() throws Exception {
        SerialArray sa = new SerialArray(a);
        sa.free();
        sa.getArray(map);
    }

    /*
     * Validate a SerialException is thrown when  getArray() is invoked after
     * free() is called
     */
    @Test(expectedExceptions = SerialException.class)
    public void test09() throws Exception {
        SerialArray sa = new SerialArray(a);
        sa.free();
        sa.getArray(1, 1, map);
    }

    /*
     * Validate a SerialException is thrown when  getArray() is invoked after
     * free() is called
     */
    @Test(expectedExceptions = SerialException.class)
    public void test10() throws Exception {
        SerialArray sa = new SerialArray(a);
        sa.free();
        sa.getArray(1, 1);
    }

    /*
     * Validate a SerialException is thrown when  getBaseType() is invoked after
     * free() is called
     */
    @Test(expectedExceptions = SerialException.class)
    public void test11() throws Exception {
        SerialArray sa = new SerialArray(a);
        sa.free();
        sa.getBaseType();
    }

    /*
     * Validate a SerialException is thrown when  getBaseTypeName() is invoked after
     * free() is called
     */
    @Test(expectedExceptions = SerialException.class)
    public void test12() throws Exception {
        SerialArray sa = new SerialArray(a);
        sa.free();
        sa.getBaseTypeName();
    }

    /*
     * Validate getArray() returns the same Object[] used to create the
     * SerialArray
     */
    @Test
    public void test13() throws Exception {
        SerialArray sa = new SerialArray(a);
        Object[] o = (Object[]) sa.getArray();
        assertTrue(Arrays.equals(o, coffees));
    }

    /*
     * Validate getArray() returns the same Object[] used to create the
     * SerialArray
     */
    @Test
    public void test14() throws Exception {
        SerialArray sa = new SerialArray(a);
        Object[] o = (Object[]) sa.getArray(map);
        assertTrue(Arrays.equals(o, coffees));
    }

    /*
     * Validate getArray() returns the same Object[] used to create the
     * SerialArray
     */
    @Test
    public void test15() throws Exception {
        SerialArray sa = new SerialArray(a);
        Object[] o = (Object[]) sa.getArray(1, 2);
        assertTrue(Arrays.equals(o, Arrays.copyOfRange(coffees, 1, 3)));
    }

    /*
     * Validate getArray() returns the same Object[] used to create the
     * SerialArray
     */
    @Test
    public void test16() throws Exception {
        SerialArray sa = new SerialArray(a);
        Object[] o = (Object[]) sa.getArray(1, 2, map);
        assertTrue(Arrays.equals(o, Arrays.copyOfRange(coffees, 1, 3)));
    }

    /*
     * clone() a SerialArray and check that it is equal to the
     * object it was cloned from
     */
    @Test
    public void test17() throws Exception {
        SerialArray sa = new SerialArray(a);
        SerialArray sa1 = (SerialArray) sa.clone();
        assertTrue(sa.equals(sa1));
    }

    /*
     * Validate that a SerialArray that is serialized & deserialized is equal to
     * itself
     */
    @Test
    public void test18() throws Exception {
        SerialArray sa = new SerialArray(a);
        SerialArray sa1 = serializeDeserializeObject(sa);;
        assertTrue(sa.equals(sa1));
    }
}
