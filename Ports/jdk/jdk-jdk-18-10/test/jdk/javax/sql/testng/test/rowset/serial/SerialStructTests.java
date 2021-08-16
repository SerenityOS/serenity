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

import java.sql.Struct;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.sql.rowset.serial.SerialStruct;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import util.BaseTest;
import util.StubStruct;
import util.SuperHero;

public class SerialStructTests extends BaseTest {

    private static Map<String, Class<?>> map = new HashMap<>();
    private Object[] attributes;
    private Struct struct;
    private final String sqlType = "SUPERHERO";
    private SuperHero hero;

    @BeforeMethod
    public void setUpMethod() throws Exception {
        attributes = new Object[]{"Bruce", "Wayne", 1939,
            "Batman"};
        map.put(sqlType, Class.forName("util.SuperHero"));
        struct = new StubStruct(sqlType, attributes);
        hero = new SuperHero(sqlType, "Bruce", "Wayne", 1939, "Batman");
    }

    /*
     * Validate that getSQLTypeName() returns the same SQLType specified by
     * the Struct used to create the object
     */
    @Test
    public void test01() throws Exception {
        SerialStruct ss = new SerialStruct(struct, map);
        assertEquals(ss.getSQLTypeName(), sqlType);
    }

    /*
     * Validate that getSQLTypeName() returns the same SQLType specified by
     * the Struct used to create the object
     */
    @Test
    public void test02() throws Exception {
        SerialStruct ss = new SerialStruct(hero, map);
        assertEquals(ss.getSQLTypeName(), sqlType);
    }

    /*
     * Validate that getAttributes() returns the same attributes specified by
     * the Struct used to create the object
     */
    @Test
    public void test03() throws Exception {
        SerialStruct ss = new SerialStruct(struct, map);
        assertTrue(Arrays.equals(attributes,
                ss.getAttributes()));
    }

    /*
     * Validate that getAttributes() returns the same attributes specified by
     * the Struct used to create the object
     */
    @Test
    public void test04() throws Exception {
        SerialStruct ss = new SerialStruct(hero, map);
        assertTrue(Arrays.equals(attributes,
                ss.getAttributes()));
    }

    /*
     * Validate that getAttributes() returns the
     same attributes specified by
     * the Struct used to create the object
     */
    @Test
    public void test05() throws Exception {
        SerialStruct ss = new SerialStruct(struct, map);
        assertTrue(Arrays.equals(attributes,
                ss.getAttributes(map)));
    }

    /*
     * Validate that getAttributes() returns the same attributes specified by
     * the Struct used to create the object
     */
    @Test
    public void test06() throws Exception {
        SerialStruct ss = new SerialStruct(hero, map);
        assertTrue(Arrays.equals(attributes,
                ss.getAttributes(map)));
    }

    /*
     * clone() a SerialStruct and check that it is equal to the
     * object it was cloned from
     */
    @Test
    public void test07() throws Exception {
        SerialStruct ss = new SerialStruct(struct, map);
        SerialStruct ss1 = (SerialStruct) ss.clone();
        assertTrue(ss.equals(ss1));
    }

    /**
     * Validate that a SerialStruct that is serialized & deserialized is equal
     * to itself
     */
    @Test
    public void test08() throws Exception {
        SerialStruct ss = new SerialStruct(struct, map);
        SerialStruct ss1 = serializeDeserializeObject(ss);;
        assertTrue(ss.equals(ss1));
    }
}
