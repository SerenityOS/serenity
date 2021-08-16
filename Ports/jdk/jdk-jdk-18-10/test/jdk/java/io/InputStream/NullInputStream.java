/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import static org.testng.Assert.*;

/*
 * @test
 * @bug 4358774 6516099 8139206
 * @run testng NullInputStream
 * @summary Check for expected behavior of InputStream.nullInputStream().
 */
public class NullInputStream {
    private static InputStream openStream;
    private static InputStream closedStream;

    @BeforeClass
    public static void setup() {
        openStream = InputStream.nullInputStream();
        closedStream = InputStream.nullInputStream();
        try {
           closedStream.close();
        } catch (IOException e) {
            fail("Unexpected IOException");
        }
    }

    @AfterClass
    public static void closeStream() {
        try {
            openStream.close();
        } catch (IOException e) {
            fail("Unexpected IOException");
        }
    }

    @Test
    public static void testOpen() {
        assertNotNull(openStream, "InputStream.nullInputStream() returned null");
    }

    @Test
    public static void testAvailable() {
        try {
            assertEquals(0, openStream.available(), "available() != 0");
        } catch (IOException ioe) {
            fail("Unexpected IOException");
        }
    }

    @Test
    public static void testRead() {
        try {
            assertEquals(-1, openStream.read(), "read() != -1");
        } catch (IOException ioe) {
            fail("Unexpected IOException");
        }
    }

    @Test
    public static void testReadBII() {
        try {
            assertEquals(-1, openStream.read(new byte[1], 0, 1),
                "read(byte[],int,int) != -1");
        } catch (IOException ioe) {
            fail("Unexpected IOException");
        }
    }

    @Test
    public static void testReadAllBytes() {
        try {
            assertEquals(0, openStream.readAllBytes().length,
                "readAllBytes().length != 0");
        } catch (IOException ioe) {
            fail("Unexpected IOException");
        }
    }

    @Test
    public static void testReadNBytes() {
        try {
            assertEquals(0, openStream.readNBytes(new byte[1], 0, 1),
                "readNBytes(byte[],int,int) != 0");
        } catch (IOException ioe) {
            fail("Unexpected IOException");
        }
    }

    @Test
    public static void testReadNBytesWithLength() {
        try {
            assertEquals(0, openStream.readNBytes(-1).length,
                "readNBytes(-1) != 0");
            fail("Expected IllegalArgumentException not thrown");
        } catch (IllegalArgumentException iae) {
        } catch (IOException ioe) {
            fail("Unexpected IOException");
        }
        try {
            assertEquals(0, openStream.readNBytes(0).length,
                "readNBytes(0, false) != 0");
            assertEquals(0, openStream.readNBytes(1).length,
                "readNBytes(1, false) != 0");
        } catch (IOException ioe) {
            fail("Unexpected IOException");
        }
    }

    @Test
    public static void testSkip() {
        try {
            assertEquals(0, openStream.skip(1), "skip() != 0");
        } catch (IOException ioe) {
            fail("Unexpected IOException");
        }
    }

    @Test
    public static void testSkipNBytes() {
        try {
            openStream.skipNBytes(-1);
            openStream.skipNBytes(0);
        } catch (IOException ioe) {
            fail("Unexpected IOException");
        }
    }

    @Test(expectedExceptions = EOFException.class)
    public static void testSkipNBytesEOF() throws IOException {
        openStream.skipNBytes(1);
    }

    @Test
    public static void testTransferTo() {
        try {
            assertEquals(0, openStream.transferTo(new ByteArrayOutputStream(7)),
                "transferTo() != 0");
        } catch (IOException ioe) {
            fail("Unexpected IOException");
        }
    }

    @Test
    public static void testAvailableClosed() {
        try {
            closedStream.available();
            fail("Expected IOException not thrown");
        } catch (IOException e) {
        }
    }

    @Test
    public static void testReadClosed() {
        try {
            closedStream.read();
            fail("Expected IOException not thrown");
        } catch (IOException e) {
        }
    }

    @Test
    public static void testReadBIIClosed() {
        try {
            closedStream.read(new byte[1], 0, 1);
            fail("Expected IOException not thrown");
        } catch (IOException e) {
        }
    }

    @Test
    public static void testReadAllBytesClosed() {
        try {
            closedStream.readAllBytes();
            fail("Expected IOException not thrown");
        } catch (IOException e) {
        }
    }

    @Test
    public static void testReadNBytesClosed() {
        try {
            closedStream.readNBytes(new byte[1], 0, 1);
            fail("Expected IOException not thrown");
        } catch (IOException e) {
        }
    }

    @Test
    public static void testReadNBytesWithLengthClosed() {
        try {
            closedStream.readNBytes(1);
            fail("Expected IOException not thrown");
        } catch (IOException e) {
        }
    }

    @Test
    public static void testSkipClosed() {
        try {
            closedStream.skip(1);
            fail("Expected IOException not thrown");
        } catch (IOException e) {
        }
    }

    @Test
    public static void testSkipNBytesClosed() {
        try {
            closedStream.skipNBytes(1);
            fail("Expected IOException not thrown");
        } catch (IOException e) {
        }
    }

    @Test
    public static void testTransferToClosed() {
        try {
            closedStream.transferTo(new ByteArrayOutputStream(7));
            fail("Expected IOException not thrown");
        } catch (IOException e) {
        }
    }
}
