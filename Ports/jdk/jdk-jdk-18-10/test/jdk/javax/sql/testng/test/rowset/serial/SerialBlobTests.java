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
import java.util.Arrays;
import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialException;
import static org.testng.Assert.*;
import org.testng.annotations.Test;
import util.BaseTest;
import util.StubBlob;

public class SerialBlobTests extends BaseTest {

    // byte[] used to populate SerialBlob
    private byte[] bytes = new byte[]{1, 2, 3, 4, 5};

    /*
     * Validate calling free() does not throw an Exception
     */
    @Test
    public void test() throws Exception {
        SerialBlob sb = new SerialBlob(new StubBlob());
        sb.free();
    }

    /*
     * Validate calling getBinaryStream() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test01() throws Exception {
        SerialBlob sb = new SerialBlob(new StubBlob());
        sb.free();
        sb.getBinaryStream();
    }

    /*
     * Validate calling getBinaryStream() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test02() throws Exception {
        SerialBlob sb = new SerialBlob(new StubBlob());
        sb.free();
        sb.getBinaryStream(1, 5);
    }

    /*
     * Validate calling getBytes() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test03() throws Exception {
        SerialBlob sb = new SerialBlob(new StubBlob());
        sb.free();
        sb.getBytes(1, 1);
    }

    /*
     * Validate calling getLength() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test04() throws Exception {
        SerialBlob sb = new SerialBlob(new StubBlob());
        sb.free();
        sb.length();
    }

    /*
     * Validate calling position() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test05() throws Exception {
        SerialBlob sb = new SerialBlob(new StubBlob());
        sb.free();
        sb.position(new byte[5], 1);
    }

    /*
     * Validate calling position() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test06() throws Exception {
        SerialBlob sb = new SerialBlob(new StubBlob());
        sb.free();
        sb.position(new StubBlob(), 1);
    }

    /*
     * Validate calling free() after calling setBinaryStream() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test07() throws Exception {
        SerialBlob sb = new SerialBlob(new StubBlob());
        sb.free();
        sb.setBinaryStream(5);
    }

    /*
     * Validate calling free() after calling setBytes() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test08() throws Exception {
        SerialBlob sb = new SerialBlob(new StubBlob());
        sb.free();
        sb.setBytes(1, new byte[5]);
    }

    /*
     * Validate calling setBytes() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test09() throws Exception {
        SerialBlob sb = new SerialBlob(new StubBlob());
        sb.free();
        sb.setBytes(1, new byte[10], 0, 5);
    }

    /*
     * Validate calling truncate() after calling free() throws an
     * SerialException
     */
    @Test(expectedExceptions = SerialException.class)
    public void test10() throws Exception {
        SerialBlob sb = new SerialBlob(new StubBlob());
        sb.free();
        sb.truncate(1);
    }

    /*
     * Validate getBinaryStream returns the correct bytes
     */
    @Test
    public void test11() throws Exception {
        byte[] expected = new byte[]{1, 2, 3};
        SerialBlob sb = new SerialBlob(bytes);
        InputStream is = sb.getBinaryStream(1, 3);
        for (byte b : expected) {
            byte val = (byte) is.read();
            assertTrue(b == val, val + " does not match " + b);
        }
    }

    /*
     * Validate a SerialException is thrown if pos < 0 for getBinaryStream
     */
    @Test(expectedExceptions = SerialException.class)
    public void test12() throws Exception {
        SerialBlob sb = new SerialBlob(bytes);
        InputStream is = sb.getBinaryStream(-1, 3);
    }

    /*
     * Validate a SerialException is thrown if pos = 0 for getBinaryStream
     */
    @Test(expectedExceptions = SerialException.class)
    public void test13() throws Exception {
        SerialBlob sb = new SerialBlob(bytes);
        InputStream is = sb.getBinaryStream(0, 3);
    }

    /*
     * Validate a SerialException is thrown if len > the length of the stream
     * for getBinaryStream
     */
    @Test(expectedExceptions = SerialException.class)
    public void test14() throws Exception {
        SerialBlob sb = new SerialBlob(bytes);
        InputStream is = sb.getBinaryStream(0, 3);
    }

    /*
     * Validate a SerialException is thrown if length < 1
     */
    @Test(expectedExceptions = SerialException.class)
    public void test15() throws Exception {
        SerialBlob sb = new SerialBlob(bytes);
        InputStream is = sb.getBinaryStream(1, 0);
    }

    /*
     * Validate a SerialException is thrown if length > byte array length
     */
    @Test(expectedExceptions = SerialException.class)
    public void test16() throws Exception {
        SerialBlob sb = new SerialBlob(bytes);
        InputStream is = sb.getBinaryStream(1, 6);
    }

    /*
     * Validate a SerialException is thrown if pos > byte array length
     */
    @Test(expectedExceptions = SerialException.class)
    public void test17() throws Exception {
        SerialBlob sb = new SerialBlob(bytes);
        InputStream is = sb.getBinaryStream(bytes.length + 2, 6);
    }

    /*
     * Validate that a cloned SerializedBlob bytes match the original
     */
    @Test
    public void test18() throws Exception {
        SerialBlob sb = new SerialBlob(bytes);
        SerialBlob sb2 = (SerialBlob) sb.clone();
        assertTrue(
                Arrays.equals(sb.getBytes(1, (int) sb.length()),
                        sb2.getBytes(1, (int) sb2.length())),
                "arrays do not match ");
    }

    /*
     * Test clone after free has been called that the clone is not accessible
     */
    @Test(expectedExceptions = SerialException.class)
    public void test19() throws Exception {
        SerialBlob sb = new SerialBlob(bytes);
        sb.free();
        SerialBlob sb2 = (SerialBlob) sb.clone();
        InputStream is = sb2.getBinaryStream(1, 3);
    }

    /*
     * Validate that a SerialBlob that is serialized & deserialized is equal to
     * itself
     */
    @Test
    public void test20() throws Exception {
        SerialBlob sb = new SerialBlob(bytes);
        SerialBlob sb2 = serializeDeserializeObject(sb);
        assertTrue(sb.equals(sb2), "SerialBlob not equal");
    }

    /*
     * Validate a SerialException is thrown if byte[] is used to
     * create the SeriablBlob and setBinaryStream is called
     */
    @Test(expectedExceptions = SerialException.class)
    public void test21() throws Exception {
        SerialBlob sb = new SerialBlob(bytes);
        sb.setBinaryStream(3);
    }

    /*
     * Validate that setBytes will properly write a set of bytes to the
     * specified location in the SerialBlob and the correct count is returned
     * for bytes written
     */
    @Test
    public void test22() throws Exception {
        byte[] diff = new byte[]{7, 8, 9};
        byte[] expected = new byte[]{1, 7, 8, 9, 5};
        SerialBlob sb = new SerialBlob(bytes);
        int written = sb.setBytes(2, diff);
        assertEquals(written, diff.length);
        assertTrue(
                Arrays.equals(sb.getBytes(1, (int) sb.length()),
                        expected),
                "arrays do not match ");
    }

    /*
     * Validate that setBytes will properly write a set of bytes to the
     * specified location in the SerialBlob and the correct count is returned
     * for bytes written
     */
    @Test
    public void test23() throws Exception {
        int bytesToWrite = 3;
        byte[] diff = new byte[]{7, 8, 9, 0};
        byte[] expected = new byte[]{1, 8, 9, 0, 5};
        SerialBlob sb = new SerialBlob(bytes);
        int written = sb.setBytes(2, diff, 1, bytesToWrite);
        assertEquals(written, bytesToWrite);
        assertTrue(
                Arrays.equals(sb.getBytes(1, (int) sb.length()),
                        expected),
                "arrays do not match ");
    }

    /*
     * Validate that truncate reduces the length of the SerlizedBlob to the
     * specified value
     */
    @Test
    public void test24() throws Exception {
        SerialBlob sb = new SerialBlob(bytes);
        sb.truncate(0);
        assertTrue(sb.length() == 0);
        sb = new SerialBlob(bytes);
        sb.truncate(3);
        assertTrue(sb.length() == 3);
    }

    /*
     * Validate getBinaryStream returns the correct bytes
     */
    @Test
    public void test25() throws Exception {
        byte[] expected = bytes;
        SerialBlob sb = new SerialBlob(bytes);
        InputStream is = sb.getBinaryStream();
        for (byte b : expected) {
            byte val = (byte) is.read();
            assertTrue(b == val, val + " does not match " + b);
        }
    }

    /*
     * Validate setBinaryStream returns an OutputStream when passed a Blob
     */
    @Test
    public void test26() throws Exception {
        SerialBlob sb = new SerialBlob(new StubBlob());
        OutputStream os = sb.setBinaryStream(0);
        assertTrue(os != null);
    }

    /*
     * Validate that position returns the correct starting location for a
     * pattern in the SerialBlob
     */
    @Test
    public void test27() throws Exception {
        long expectedPos = 3; // starting offset is 1 vs 0
        byte[] pattern = new byte[]{3, 4};
        SerialBlob sb = new SerialBlob(bytes);
        long pos = sb.position(pattern, 1);
        assertEquals(pos, expectedPos);
    }

    /*
     * Validate that position returns the correct starting location for a
     * pattern in the SerialBlob
     */
    @Test
    public void test28() throws Exception {
        long expectedPos = 3; // starting offset is 1 vs 0
        byte[] pattern = new byte[]{3, 4, 5};
        SerialBlob sb = new SerialBlob(bytes);
        long pos = sb.position(pattern, 2);
        assertEquals(pos, expectedPos);
    }

    /*
     * Validate that position returns the correct starting location for a
     * pattern in the SerialBlob
     */
    @Test
    public void test29() throws Exception {
        long expectedPos = 2; // starting offset is 1 vs 0
        byte[] pattern = new byte[]{4, 6};
        SerialBlob sb = new SerialBlob(new StubBlob());
        long pos = sb.position(pattern, 1);
        assertEquals(pos, expectedPos);
    }

    /*
     * Validate that position returns the correct starting location for a
     * pattern in the SerialBlob
     */
    @Test
    public void test30() throws Exception {
        long expectedPos = 3; // starting offset is 1 vs 0
        byte[] pattern = new byte[]{6, 8};
        SerialBlob sb = new SerialBlob(new StubBlob());
        long pos = sb.position(pattern, 2);
        assertEquals(pos, expectedPos);
    }
}
