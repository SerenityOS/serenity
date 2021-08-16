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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import javax.sql.rowset.serial.SerialClob;
import javax.sql.rowset.serial.SerialException;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import util.BaseTest;
import util.StubClob;

public class SerialClobTests extends BaseTest {

    // char[] used to populate SerialClob
    private final char[] chars;

    public SerialClobTests() {
        this.chars = new char[]{'h', 'e', 'l', 'l', 'o', ' ', 'w',
            'o', 'r', 'l', 'd'};
    }

    /*
     * Validate calling free() does not throw an Exception
     */
    @Test
    public void test() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        sc.free();
    }

    /*
     * Validate calling getCharacterStream() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test01() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        sc.free();
        sc.getCharacterStream();
    }

    /*
     * Validate calling getCharacterStream() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test02() throws Exception {
        SerialClob sc = new SerialClob(chars);
        sc.free();
        sc.getCharacterStream();
    }

    /*
     * Validate calling getCharacterStream() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test03() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        sc.free();
        sc.getCharacterStream(1, 5);
    }

    /*
     * Validate calling getSubString() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test04() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        sc.free();
        sc.getSubString(1, 1);
    }

    /*
     * Validate calling truncate() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test05() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        sc.free();
        sc.truncate(1);
    }

    /*
     * Validate calling getAsciiStream() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test06() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        sc.free();
        sc.getAsciiStream();
    }

    /*
     * Validate calling length() after calling free() throws an SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test07() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        sc.free();
        sc.length();
    }

    /*
     * Validate calling position() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test08() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        sc.free();
        sc.position("hello", 1);
    }

    /*
     * Validate calling position() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test09() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        sc.free();
        sc.position(new StubClob(), 1);
    }

    /*
     * Validate calling setAsciiStream() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test10() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        sc.free();
        sc.setAsciiStream(5);
    }

    /*
     * Validate calling setCharacterStream() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test11() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        sc.free();
        sc.setCharacterStream(5);
    }

    /*
     * Validate calling setString() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test12() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        sc.free();
        sc.setString(1, "hello");
    }

    /*
     * Validate calling setString() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test13() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        sc.free();
        sc.setString(1, "hello", 0, 5);
    }

    /*
     * Test that SerialException is thrown if pos < 0 on a call to
     * getCharacterStream
     */
    @Test(expectedExceptions = SerialException.class)
    public void test14() throws Exception {
        SerialClob sc = new SerialClob(chars);
        sc.getCharacterStream(-1, 5);
    }

    /*
     * Test that SerialException is thrown if pos = 0 on a call to
     * getCharacterStream
     */
    @Test(expectedExceptions = SerialException.class)
    public void test15() throws Exception {
        SerialClob sc = new SerialClob(chars);
        sc.getCharacterStream(0, 5);
    }

    /*
     * Test that SerialException is thrown if pos = 0 on a call to
     * getCharacterStream
     */
    @Test(expectedExceptions = SerialException.class)
    public void test16() throws Exception {
        SerialClob sc = new SerialClob(chars);
        sc.getCharacterStream(1, 100);
    }

    /*
     * Test that SerialException is thrown if length = 0 on a call to
     * getCharacterStream
     */
    @Test(expectedExceptions = SerialException.class)
    public void test17() throws Exception {
        SerialClob sc = new SerialClob(chars);
        sc.getCharacterStream(1, 0);
    }

    /*
     * Test that SerialException is thrown if pos > length on a call to
     * getCharacterStream
     */
    @Test(expectedExceptions = SerialException.class)
    public void test18() throws Exception {
        SerialClob sc = new SerialClob(chars);
        sc.getCharacterStream(100, 5);
    }

    /*
     * Clone a SerialClob and check that it is equal to itself
     */
    @Test
    public void test19() throws Exception {
        SerialClob sc = new SerialClob(chars);
        SerialClob sc1 = (SerialClob) sc.clone();
        assertTrue(sc.equals(sc1), "SerialClobs not equal");
    }

    /*
     * Validate that a getAsciiStream() returns an InputStream when a Clob is
     * used to create the SerialClob
     */
    @Test
    public void test20() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        InputStream is = sc.getAsciiStream();
        assertTrue(is != null);
    }

    /*
     * Validate that a getCharacterStream() returns an Reader when a Clob is
     * used to create the SerialClob
     */
    @Test
    public void test21() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        Reader is = sc.getCharacterStream();
        assertTrue(is != null);
    }

    /*
     * Validate that a getCharacterStream() returns an Reader when a char[] is
     * used to create the SerialClob
     */
    @Test
    public void test22() throws Exception {
        SerialClob sc = new SerialClob(chars);
        Reader is = sc.getCharacterStream();
        assertTrue(is != null);
    }

    /*
     * Validate that a getSubString() returns the correct value when a char[] is
     * used to create the SerialClob
     */
    @Test
    public void test23() throws Exception {
        SerialClob sc = new SerialClob(chars);
        String expected = "world";
        assertEquals(expected, sc.getSubString(7, 5));
    }

    /*
     * Validate that a getSubString() returns the correct value when a Clob is
     * used to create the SerialClob
     */
    @Test
    public void test24() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        String expected = "test string";
        assertEquals(expected, sc.getSubString(5, 11));
    }

    /*
     * Validate that position() returns the correct value when a Clob is used to
     * create the SerialClob
     */
    @Test
    public void test25() throws Exception {
        long expectedPos = 5;
        SerialClob sc = new SerialClob(new StubClob());
        String expected = "test string";
        long pos = sc.position(expected, 1);
        assertEquals(expectedPos, pos);
    }

    /*
     * Validate that position returned is -1 when an the search string is not
     * part of the SerialClob
     */
    @Test
    public void test26() throws Exception {
        long expectedPos = -1;
        SerialClob sc = new SerialClob(chars);
        String expected = "test string";
        long pos = sc.position(expected, 1);
        assertEquals(expectedPos, pos);
    }

    /*
     * Validate that position() returned is -1 when an the search string is not
     * part of the SerialClob
     */
    @Test
    public void test27() throws Exception {
        long expectedPos = -1;
        SerialClob sc = new SerialClob(new StubClob());
        String expected = "I am Batman";
        long pos = sc.position(expected, 2);
        assertEquals(expectedPos, pos);
    }

    /*
     * Validate that position() returns the correct value when a char[] is used
     * to create the SerialClob
     */
    @Test
    public void test28() throws Exception {
        long expectedPos = 2;
        SerialClob sc = new SerialClob(chars);
        String expected = "ello";
        long pos = sc.position(expected, 1);
        assertEquals(expectedPos, pos);
    }

    /*
     * Validate that position() returns the correct value when a SerialClob is
     * used for the search argument
     */
    @Test
    public void test29() throws Exception {
        long expectedPos = 21;
        String expected = "Batman";
        String buf = "I am Joker, not the Batman, hahaha";
        SerialClob sc = new SerialClob(expected.toCharArray());
        SerialClob sc1 = new SerialClob(buf.toCharArray());
        long pos = sc1.position(sc, 1);
        assertEquals(expectedPos, pos);
    }

    /*
     * Validate that position() returns the correct value when a SerialClob is
     * used for the search argument
     */
    @Test
    public void test30() throws Exception {
        long expectedPos = 17;
        String expected = "012";
        SerialClob sc = new SerialClob(expected.toCharArray());
        SerialClob sc1 = new SerialClob(new StubClob());
        long pos = sc1.position(sc, 1);
        assertEquals(expectedPos, pos);
    }

    /*
     * Check that setString() updates the appropriate characters in the
     * SerialClob
     */
    @Test
    public void test31() throws Exception {
        String val = "Hello, I am Bruce Wayne";
        String val1 = "the Batman!";
        String expected = "Hello, I am the Batman!";
        SerialClob sc = new SerialClob(val.toCharArray());
        int written = sc.setString(13, val1);
        assertEquals(val1.length(), written);
        assertTrue(expected.equals(sc.getSubString(1, (int) sc.length())));
    }

    /*
     * Check that setString() updates the appropriate characters in the
     * SerialClob
     */
    @Test(enabled = false)
    public void test32() throws Exception {
        int expectedWritten = 9;
        String val = "Hi, I am Catwoman!!!!!!";
        String val1 = "Hahaha the Joker, who are you?!";
        String expected = "Hi, I am the Joker!";
        SerialClob sc = new SerialClob(val.toCharArray());
        int written = sc.setString(10, val1, 8, expectedWritten+1);
        assertEquals(written, expectedWritten);

    }

    /*
     * Check that setCharacterStream() returns a non-null Writer for an
     * SerialClob created from a Clob
     */
    @Test
    public void test33() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        Writer w = sc.setCharacterStream(1);
        assertTrue(w != null);
    }

    /*
     * Check that setAsciiStream() returns a non-null OutputStream for an SerialClob
     * created from a Clob
     */
    @Test
    public void test34() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        OutputStream os = sc.setAsciiStream(1);
        assertTrue(os != null);
    }

    /*
     * Check that truncate() truncates the length of the SerialClob to the
     * specified size
     */
    @Test
    public void test35() throws Exception {
        SerialClob sc = new SerialClob(new StubClob());
        sc.truncate(0);
        assertTrue(sc.length() == 0);
        sc = new SerialClob(chars);
        sc.truncate(5);
        assertTrue(sc.length() == 5);
    }

    /*
     * Check that getCharacterStream() returns a Reader and that the char[] that
     * was specified to create the SerialClob can be returned via the Reader
     */
    @Test
    public void test36() throws Exception {
        SerialClob sc = new SerialClob(chars);
        Reader r = sc.getCharacterStream();
        for (char c : chars) {
            char val = (char) r.read();
            assertTrue(c == val, val + " does not match " + c);
        }
    }

    /*
     * Check that getCharacterStream() returns a Reader and that the char[] that
     * was specified to create the SerialClob can be returned via the Reader
     */
    @Test(enabled = false)
    public void test37() throws Exception {
        SerialClob sc = new SerialClob(chars);
        String expected = "ello w";
        Reader r = sc.getCharacterStream(2, 6);
        for (char c : expected.toCharArray()) {
            char val = (char) r.read();
            assertTrue(c == val, val + " does not match " + c);
        }
    }

    /*
     * Validate that a SerialClob that is serialized & deserialized is equal to
     * itself
     */
    @Test
    public void test38() throws Exception {
        SerialClob sc = new SerialClob(chars);
        SerialClob sc2 = serializeDeserializeObject(sc);
        assertTrue(sc.equals(sc2), "SerialClobs not equal");
    }
}
