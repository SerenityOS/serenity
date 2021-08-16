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
import javax.sql.rowset.serial.SQLInputImpl;
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

public class SQLInputImplTests extends BaseTest {

    // Copy of the array of data type values
    private Object[] typeValues;
    private TestSQLDataImpl impl;
    private Map<String, Class<?>> map ;
    private SuperHero hero;
    private final String sqlType = "SUPERHERO";

    @BeforeMethod
    @Override
    public void setUpMethod() throws Exception {
        map = new HashMap<>();
        impl = new TestSQLDataImpl("TestSQLData");
        typeValues = Arrays.copyOf(TestSQLDataImpl.attributes,
                TestSQLDataImpl.attributes.length);
        hero = new SuperHero(sqlType, "Bruce", "Wayne",
                1939, "Batman");
    }

    /*
     * Validate that a SQLException is thrown if the attribute value is
     * null
     */
    @Test(expectedExceptions = SQLException.class)
    public void test() throws Exception {
        SQLInputImpl x = new SQLInputImpl(null, map);
    }

    /*
     * Validate that a SQLException is thrown if the map value is
     * null
     */
    @Test(expectedExceptions = SQLException.class)
    public void test02() throws Exception {
        SQLInputImpl x = new SQLInputImpl(typeValues, null);
    }

    /*
     * Read in the various datatypes via readSQL (which will exercise the
     * various readXXX methods and validate that results are as expected
     */
    @Test()
    public void test03() throws Exception {
        impl.readSQL(new SQLInputImpl(typeValues, map), "misc");
        assertTrue(Arrays.equals(impl.toArray(), typeValues));
        // Null out a field and make sure the arrays do not match
        typeValues[2] = null;
        assertFalse(Arrays.equals(impl.toArray(), typeValues));
    }

    /*
     * Validate that wasNull indicates if a null was read in
     */
    @Test()
    public void test04() throws Exception {
        Object[] values = {"Hello", null, 1};
        SQLInputImpl sqli = new SQLInputImpl(values, map);
        String s = sqli.readString();
        assertFalse(sqli.wasNull());
        s = sqli.readString();
        assertTrue(sqli.wasNull());
        int i = sqli.readInt();
        assertFalse(sqli.wasNull());
    }

    /*
     * Validate that readObject returns the correct value
     */
    @Test()
    public void test05() throws Exception {
        Object[] values = {hero};
        SQLInputImpl sqli = new SQLInputImpl(values, map);
        Object o = sqli.readObject();
        assertTrue(hero.equals(o));

    }

    /*
     * Validate a Array can be read
     */
    @Test(enabled = true)
    public void test06() throws Exception {
        Object[] coffees = new Object[]{"Espresso", "Colombian", "French Roast",
            "Cappuccino"};
        Array a = new StubArray("VARCHAR", coffees);
        Object[] values = {a};
        SQLInputImpl sqli = new SQLInputImpl(values, map);
        Array a2 = sqli.readArray();
        assertTrue(Arrays.equals((Object[]) a2.getArray(), (Object[]) a.getArray()));
        assertTrue(a.getBaseTypeName().equals(a2.getBaseTypeName()));
    }

    /*
     * Validate a Blob can be read
     */
    @Test(enabled = true)
    public void test07() throws Exception {
        Blob b = new StubBlob();
        Object[] values = {b};
        SQLInputImpl sqli = new SQLInputImpl(values, map);
        Blob b2 = sqli.readBlob();
        assertTrue(Arrays.equals(
                b.getBytes(1, (int) b.length()),
                b2.getBytes(1, (int) b2.length())));
    }

    /*
     * Validate a Clob can be read
     */
    @Test(enabled = true)
    public void test08() throws Exception {
        Clob c = new StubClob();
        Object[] values = {c};
        SQLInputImpl sqli = new SQLInputImpl(values, map);
        Clob c2 = sqli.readClob();
        assertTrue(c.getSubString(1,
                (int) c.length()).equals(c2.getSubString(1, (int) c2.length())));
    }

    /*
     * Validate a Ref can be read
     */
    @Test(enabled = true)
    public void test09() throws Exception {
        Ref ref = new StubRef(sqlType, hero);
        Object[] values = {ref};
        SQLInputImpl sqli = new SQLInputImpl(values, map);
        Ref ref2 = sqli.readRef();
        assertTrue(ref.getObject().equals(ref2.getObject()));
        assertTrue(ref.getBaseTypeName().equals(ref2.getBaseTypeName()));
    }

    /*
     * Validate a URL can be read
     */
    @Test(enabled = true)
    public void test10() throws Exception {
        URL u = new URL("http://www.oracle.com/");;
        Object[] values = {u};
        SQLInputImpl sqli = new SQLInputImpl(values, map);
        URL u2 = sqli.readURL();
        assertTrue(u2.equals(u));
        assertTrue(u2.sameFile(u));
    }

    /*
     * Validate that readObject returns the correct value when a Struct is
     * next on the stream
     */
    @Test()
    public void test11() throws Exception {
        Object[] attributes = new Object[]{"Bruce", "Wayne", 1939,
            "Batman"};
        map.put(sqlType, Class.forName("util.SuperHero"));
        Struct struct = new StubStruct(sqlType, attributes);
        Object[] values = {struct};
        SQLInputImpl sqli = new SQLInputImpl(values, map);
        Object o = sqli.readObject();

        assertTrue(hero.equals(o));

    }
}
