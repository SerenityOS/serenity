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

import java.lang.reflect.Field;
import java.util.Arrays;
import javax.sql.rowset.RowSetMetaDataImpl;
import javax.sql.rowset.serial.SerialException;
import javax.sql.rowset.serial.SerialJavaObject;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import util.BaseTest;

public class SerialJavaObjectTests extends BaseTest {

    /*
     * Validate that an NPE is thrown when null is specified to create
     * the SerialJavaObject
     */
    @Test(expectedExceptions = NullPointerException.class)
    public void test() throws Exception {
        SerialJavaObject sjo = new SerialJavaObject(null);
    }

    /*
     * Validate that an SerialExcepion is thrown when the object specified
     * contains public static fields
     */
    @Test(expectedExceptions = SerialException.class, enabled = false)
    public void test01() throws Exception {
        SerialJavaObject sjo = new SerialJavaObject(new RowSetMetaDataImpl());
    }

    /*
     * Validate that an getFields()s returns the same Field[] for the object
     * used to create the SerialJavaObject
     */
    @Test
    public void test02() throws Exception {
        SerialException e = new SerialException();
        SerialJavaObject sjo = new SerialJavaObject(e);
        Field[] f = e.getClass().getFields();
        assertTrue(Arrays.equals(f, sjo.getFields()));
        assertFalse(Arrays.equals("hello".getClass().getFields(),
                sjo.getFields()));
    }

    /*
     * clone() a SerialJavaObject and check that it is equal to the
     * object it was cloned from
     */
    @Test
    public void test03() throws Exception {
        SerialJavaObject sjo = new SerialJavaObject("Hello");
        SerialJavaObject sjo2 = (SerialJavaObject) sjo.clone();
        assertTrue(sjo.equals(sjo2));
    }

    /**
     * Validate that a SerialJavaObject that is serialized & deserialized is
     * equal to itself
     */
    @Test
    public void test04() throws Exception {
        SerialJavaObject sjo = new SerialJavaObject("Hello");
        SerialJavaObject sjo2 = serializeDeserializeObject(sjo);
        assertTrue(sjo.equals(sjo2));
    }

    /*
     * Validate that a getObject() returns an object used to create the
     * SerialJavaObject
     */
    @Test
    public void test05() throws Exception {
        String s = "Hello world";
        SerialJavaObject sjo = new SerialJavaObject(s);
        assertTrue(s.equals(sjo.getObject()));
    }
}
