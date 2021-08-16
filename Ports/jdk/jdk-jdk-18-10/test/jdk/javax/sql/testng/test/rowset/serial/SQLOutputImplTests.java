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

import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Ref;
import java.sql.SQLException;
import java.sql.Struct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.sql.rowset.serial.SQLInputImpl;
import javax.sql.rowset.serial.SQLOutputImpl;
import javax.sql.rowset.serial.SerialArray;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialDatalink;
import javax.sql.rowset.serial.SerialRef;
import javax.sql.rowset.serial.SerialStruct;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import util.BaseTest;
import util.StubArray;
import util.StubBlob;
import util.StubClob;
import util.StubRef;
import util.StubStruct;
import util.SuperHero;
import util.TestSQLDataImpl;

public class SQLOutputImplTests extends BaseTest {

    // Copy of the array of data type values
    private Object[] typeValues;
    private TestSQLDataImpl impl;
    private Map<String, Class<?>> map = new HashMap<>();
    private Vector results;
    private final String sqlType = "SUPERHERO";
    private SuperHero hero;
    private SQLOutputImpl outImpl;

    @BeforeMethod
    @Override
    public void setUpMethod() throws Exception {
        results = new Vector();
        impl = new TestSQLDataImpl("TestSQLData");
        typeValues = Arrays.copyOf(TestSQLDataImpl.attributes,
                TestSQLDataImpl.attributes.length);
        hero = new SuperHero(sqlType, "Bruce", "Wayne", 1939, "Batman");
        outImpl = new SQLOutputImpl(results, map);
    }

    /*
     * Validate that a SQLException is thrown if the attribute value is
     * null
     */
    @Test(expectedExceptions = SQLException.class)
    public void test() throws Exception {
        SQLOutputImpl x = new SQLOutputImpl(null, map);
    }

    /*
     * Validate that a SQLException is thrown if the map value is
     * null
     */
    @Test(expectedExceptions = SQLException.class)
    public void test02() throws Exception {
        SQLOutputImpl x = new SQLOutputImpl(results, null);
    }

    /*
     * Read in the various datatypes via readSQL (which will exercise the
     * various readXXX methods and validate that results are as expected
     */
    @Test()
    public void test03() throws Exception {
        impl.readSQL(new SQLInputImpl(typeValues, map), "misc");
        impl.writeSQL(outImpl);
        assertTrue(Arrays.equals(results.toArray(), typeValues));
        // Null out a field and make sure the arrays do not match
        typeValues[2] = null;
        assertFalse(Arrays.equals(results.toArray(), typeValues));
    }

    /*
     * Validate a Array can be written and returned
     */
    @Test(enabled = true)
    public void test04() throws Exception {
        Object[] coffees = new Object[]{"Espresso", "Colombian", "French Roast",
            "Cappuccino"};
        Array a = new StubArray("VARCHAR", coffees);
        outImpl.writeArray(a);
        SerialArray sa = (SerialArray) results.get(0);
        assertTrue(Arrays.equals(coffees, (Object[]) sa.getArray()));
        assertTrue(a.getBaseTypeName().equals(sa.getBaseTypeName()));
    }

    /*
     * Validate a Blob can be written and returned
     */
    @Test(enabled = true)
    public void test05() throws Exception {
        Blob b = new StubBlob();
        outImpl.writeBlob(b);
        SerialBlob sb = (SerialBlob) results.get(0);
        assertTrue(Arrays.equals(
                b.getBytes(1, (int) b.length()),
                sb.getBytes(1, (int) sb.length())));
    }

    /*
     * Validate a Clob can be written and returned
     */
    @Test(enabled = true)
    public void test06() throws Exception {
        Clob c = new StubClob();
        outImpl.writeClob(c);
        SerialClob sc = (SerialClob) results.get(0);
        assertTrue(c.getSubString(1,
                (int) c.length()).equals(sc.getSubString(1, (int) sc.length())));
    }

    /*
     * Validate a Ref can be written and returned
     */
    @Test(enabled = true)
    public void test07() throws Exception {
        Ref ref = new StubRef(sqlType, hero);
        outImpl.writeRef(ref);
        SerialRef sr = (SerialRef) results.get(0);
        assertTrue(hero.equals(sr.getObject()));
    }

    /*
     * Validate a Struct can be written and returned
     */
    @Test(enabled = true)
    public void test08() throws Exception {
        Object[] attributes = new Object[]{"Bruce", "Wayne", 1939,
            "Batman"};
        Struct s = new StubStruct(sqlType, attributes);
        outImpl.writeStruct(s);
        SerialStruct ss = (SerialStruct) results.get(0);
        assertTrue(Arrays.equals(attributes, (Object[]) ss.getAttributes()));
        assertTrue(sqlType.equals(ss.getSQLTypeName()));
    }

    /*
     * Validate a DataLink can be written and returned
     */
    @Test(enabled = true)
    public void test09() throws Exception {
        URL u = new URL("http://www.oracle.com/");
        outImpl.writeURL(u);
        SerialDatalink sdl = (SerialDatalink) results.get(0);
        URL u2 = sdl.getDatalink();
        assertTrue(u2.equals(u));
        assertTrue(u2.sameFile(u));
    }

    /*
     * Validate an Object implementing SQLData can be written and returned
     */
    @Test(enabled = true)
    public void test10() throws Exception {
        Object[] attributes = new Object[]{"Bruce", "Wayne", 1939,
            "Batman"};
        outImpl.writeObject(hero);
        SerialStruct ss = (SerialStruct) results.get(0);
        assertTrue(Arrays.equals(attributes, (Object[]) ss.getAttributes()));
        assertTrue(sqlType.equals(ss.getSQLTypeName()));
    }
}
