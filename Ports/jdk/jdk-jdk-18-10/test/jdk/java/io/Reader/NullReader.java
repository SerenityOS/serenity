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

import java.io.Reader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.CharBuffer;
import java.nio.ReadOnlyBufferException;

import org.testng.annotations.*;

import static org.testng.Assert.*;

/*
 * @test
 * @bug 8196298 8204930
 * @run testng NullReader
 * @summary Check for expected behavior of Reader.nullReader().
 */
public class NullReader {
    private static Reader openReader;
    private static Reader closedReader;

    @BeforeClass
    public static void setup() throws IOException {
        openReader = Reader.nullReader();
        closedReader = Reader.nullReader();
        closedReader.close();
    }

    @AfterClass
    public static void closeStream() throws IOException {
        openReader.close();
    }

    @Test
    public static void testOpen() {
        assertNotNull(openReader, "Reader.nullReader() returned null");
    }

    @Test
    public static void testRead() throws IOException {
        assertEquals(-1, openReader.read(), "read() != -1");
    }

    @Test
    public static void testReadBII() throws IOException {
        assertEquals(-1, openReader.read(new char[1], 0, 1),
                "read(char[],int,int) != -1");
    }

    @Test
    public static void testReadBIILenZero() throws IOException {
        assertEquals(0, openReader.read(new char[1], 0, 0),
                "read(char[],int,int) != 0");
    }

    @Test
    public static void testReadCharBuffer() throws IOException {
        CharBuffer charBuffer = CharBuffer.allocate(1);
        assertEquals(-1, openReader.read(charBuffer),
                "read(CharBuffer) != -1");
    }

    @Test
    public static void testReadCharBufferZeroRemaining() throws IOException {
        CharBuffer charBuffer = CharBuffer.allocate(0);
        assertEquals(0, openReader.read(charBuffer),
                "read(CharBuffer) != 0");
    }

    @Test
    public static void testReady() throws IOException {
        assertFalse(openReader.ready());
    }

    @Test
    public static void testSkip() throws IOException {
        assertEquals(0, openReader.skip(1), "skip() != 0");
    }

    @Test
    public static void testTransferTo() throws IOException {
        assertEquals(0, openReader.transferTo(new StringWriter(7)),
                "transferTo() != 0");
    }

    @Test(expectedExceptions = IOException.class)
    public static void testReadClosed() throws IOException {
        closedReader.read();
    }

    @Test(expectedExceptions = IOException.class)
    public static void testReadBIIClosed() throws IOException {
        closedReader.read(new char[1], 0, 1);
    }

    @Test(expectedExceptions = IOException.class)
    public static void testReadCharBufferClosed() throws IOException {
        CharBuffer charBuffer = CharBuffer.allocate(0);
        closedReader.read(charBuffer);
    }

    @Test(expectedExceptions = IOException.class)
    public static void testReadCharBufferZeroRemainingClosed() throws IOException {
        CharBuffer charBuffer = CharBuffer.allocate(0);
        closedReader.read(charBuffer);
    }

    @Test(expectedExceptions = IOException.class)
    public static void testReadyClosed() throws IOException {
        closedReader.ready();
    }

    @Test(expectedExceptions = IOException.class)
    public static void testSkipClosed() throws IOException {
        closedReader.skip(1);
    }

    @Test(expectedExceptions = IOException.class)
    public static void testTransferToClosed() throws IOException {
        closedReader.transferTo(new StringWriter(7));
    }
}
