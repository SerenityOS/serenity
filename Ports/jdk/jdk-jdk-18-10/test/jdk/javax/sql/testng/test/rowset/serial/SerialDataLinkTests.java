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
import javax.sql.rowset.serial.SerialDatalink;
import javax.sql.rowset.serial.SerialException;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import util.BaseTest;

public class SerialDataLinkTests extends BaseTest {

    private URL u;
    private URL u1;
    private SerialDatalink dl;

    @BeforeMethod
    public void setUpMethod() throws Exception {
        u = new URL("http://www.oracle.com/");
        u1 = new URL("http://www.usatoday.com/");
        dl = new SerialDatalink(u);
    }

    /*
     * Validate that a SerialException is thrown if the URL is null
     */
    @Test(expectedExceptions = SerialException.class)
    public void test() throws Exception {
        SerialDatalink dl1 = new SerialDatalink(null);
    }

    /*
     * Validate that getDatalink() returns the same URL used to create the
     * SerialDatalink object
     */
    @Test
    public void test01() throws Exception {
        URL u2 = dl.getDatalink();
        assertTrue(u2.equals(u));
        assertTrue(u2.sameFile(u));
    }

    /*
     * Validate that URL returned from getDatalink() differs from a URL that was
     * not used to create the SerialDatalink
     */
    @Test
    public void test02() throws Exception {
        URL u2 = dl.getDatalink();
        assertFalse(u2.equals(u1));
        assertFalse(u2.sameFile(u1));
    }

    /*
     * Create a clone of a SerialDatalink and validate that it is equal to the
     * SerialDatalink it was cloned from
     */
    @Test
    public void test03() throws Exception {
        SerialDatalink dl2 = (SerialDatalink) dl.clone();
        assertTrue(dl.equals(dl2));
        SerialDatalink dl3 = new SerialDatalink(u1);
        assertFalse(dl2.equals(dl3));
    }

    /*
     * Validate that a SerialDatalink that is serialized & deserialized is
     * equal to itself
     */
    @Test
    public void test04() throws Exception {
        SerialDatalink dl2 = serializeDeserializeObject(dl);
        SerialDatalink dl3 = new SerialDatalink(u);
        assertTrue(dl.equals(dl2));
        assertTrue(dl3.equals(dl2));
    }

    /**
     * Validate that a SerialDatalink that is serialized & deserialized is not equal
     * to to a SerialDatalink created using a different URL
     */
    @Test
    public void test05() throws Exception {
        SerialDatalink dl2 = serializeDeserializeObject(dl);
        SerialDatalink d3 = new SerialDatalink(u1);
        assertFalse(d3.equals(dl2));
    }
}
