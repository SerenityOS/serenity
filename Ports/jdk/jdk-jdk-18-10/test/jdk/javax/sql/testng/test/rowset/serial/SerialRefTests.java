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

import java.sql.Ref;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import javax.sql.rowset.serial.SerialRef;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import util.BaseTest;
import util.StubRef;
import util.SuperHero;

public class SerialRefTests extends BaseTest {

    private static Map<String, Class<?>> map = new HashMap<>();
    private Ref ref;
    private final String sqlType = "SUPERHERO";
    private SuperHero hero;

    @BeforeMethod
    public void setUpMethod() throws Exception {
        map.put(sqlType, Class.forName("util.SuperHero"));
        hero = new SuperHero(sqlType, "Bruce", "Wayne", 1939, "Batman");
        ref = new StubRef(sqlType, hero);
    }

    /*
     * Validate that a SQLException() is thrown if the Ref is null
     */
    @Test(expectedExceptions = SQLException.class)
    public void test01() throws Exception {
        SerialRef sr = new SerialRef(null);
    }

    /*
     * Validate that a SQLException() is thrown if the typeName is null in the
     * Ref used to create the SerialRef
     */
    @Test(expectedExceptions = SQLException.class)
    public void test02() throws Exception {
        SerialRef sr = new SerialRef(new StubRef(null, hero));
    }

    /*
     * Validate that getBaseTypeName() returns the same SQLType specified
     * to create the Ref
     */
    @Test
    public void test03() throws Exception {
        SerialRef sr = new SerialRef(ref);
        assertEquals(sr.getBaseTypeName(), sqlType);
    }

    /*
     * Validate that getObject() returns the same object used to create the Ref
     */
    @Test
    public void test04() throws Exception {
        SerialRef sr = new SerialRef(ref);
        assertTrue(hero.equals(sr.getObject()));
    }

    /*
     * Validate that getObject() returns the same object used to create the Ref
     */
    @Test(enabled = false)
    public void test05() throws Exception {
        SerialRef sr = new SerialRef(ref);
        assertTrue(hero.equals(sr.getObject(map)));
    }

    /*
     * Validate that setObject() can be used to change the value of the object
     * pointed to by the SerialRef
     */
    @Test
    public void test06() throws Exception {
        SerialRef sr = new SerialRef(ref);
        assertTrue(hero.equals(sr.getObject()));
        SuperHero h = new SuperHero(sqlType, "Dick", "Grayson", 1940, "Robin");
        sr.setObject(h);
        assertFalse(hero.equals(sr.getObject()));
    }

    /*
     * clone() a SerialRef and check that it is equal to the
     * object it was cloned from
     */
    @Test
    public void test09() throws Exception {
        SerialRef sr = new SerialRef(ref);
        SerialRef sr1 = (SerialRef) sr.clone();
        assertTrue(sr.equals(sr1));
    }

    /**
     * Validate that a SerialRef that is serialized & deserialized is equal to
     * itself for the Object & baseTypeName
     */
    @Test
    public void test10() throws Exception {
        SerialRef sr = new SerialRef(ref);
        SerialRef sr1 = serializeDeserializeObject(sr);
        assertTrue(sr1.getObject().equals(sr.getObject())
                && sr1.getBaseTypeName().equals(sr.getBaseTypeName()));
    }
}
