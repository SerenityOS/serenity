/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.net.http.hpack;

import org.testng.annotations.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import static jdk.internal.net.http.hpack.BuffersTestingKit.*;
import static jdk.internal.net.http.hpack.TestHelper.newRandom;

//
// Some of the tests below overlap in what they test. This allows to diagnose
// bugs quicker and with less pain by simply ruling out common working bits.
//
public final class BinaryPrimitivesTest {

    private final Random random = newRandom();

    @Test
    public void integerRead1() {
        verifyRead(bytes(0b00011111, 0b10011010, 0b00001010), 1337, 5);
    }

    @Test
    public void integerRead2() {
        verifyRead(bytes(0b00001010), 10, 5);
    }

    @Test
    public void integerRead3() {
        verifyRead(bytes(0b00101010), 42, 8);
    }

    @Test
    public void integerWrite1() {
        verifyWrite(bytes(0b00011111, 0b10011010, 0b00001010), 1337, 5);
    }

    @Test
    public void integerWrite2() {
        verifyWrite(bytes(0b00001010), 10, 5);
    }

    @Test
    public void integerWrite3() {
        verifyWrite(bytes(0b00101010), 42, 8);
    }

    //
    // Since readInteger(x) is the inverse of writeInteger(x), thus:
    //
    // for all x: readInteger(writeInteger(x)) == x
    //
    @Test
    public void integerIdentity() throws IOException {
        final int MAX_VALUE = 1 << 22;
        int totalCases = 0;
        int maxFilling = 0;
        IntegerReader r = new IntegerReader();
        IntegerWriter w = new IntegerWriter();
        ByteBuffer buf = ByteBuffer.allocate(8);
        for (int N = 1; N < 9; N++) {
            for (int expected = 0; expected <= MAX_VALUE; expected++) {
                w.reset().configure(expected, N, 1).write(buf);
                buf.flip();
                totalCases++;
                maxFilling = Math.max(maxFilling, buf.remaining());
                r.reset().configure(N).read(buf);
                assertEquals(r.get(), expected);
                buf.clear();
            }
        }
//        System.out.printf("totalCases: %,d, maxFilling: %,d, maxValue: %,d%n",
//                totalCases, maxFilling, MAX_VALUE);
    }

    @Test
    public void integerReadChunked() {
        final int NUM_TESTS = 1024;
        IntegerReader r = new IntegerReader();
        ByteBuffer bb = ByteBuffer.allocate(8);
        IntegerWriter w = new IntegerWriter();
        for (int i = 0; i < NUM_TESTS; i++) {
            final int N = 1 + random.nextInt(8);
            final int expected = random.nextInt(Integer.MAX_VALUE) + 1;
            w.reset().configure(expected, N, random.nextInt()).write(bb);
            bb.flip();

            forEachSplit(bb,
                    (buffers) -> {
                        Iterable<? extends ByteBuffer> buf = relocateBuffers(injectEmptyBuffers(buffers));
                        r.configure(N);
                        for (ByteBuffer b : buf) {
                            try {
                                r.read(b);
                            } catch (IOException e) {
                                throw new UncheckedIOException(e);
                            }
                        }
                        assertEquals(r.get(), expected);
                        r.reset();
                    });
            bb.clear();
        }
    }

    // FIXME: use maxValue in the test

    @Test
    // FIXME: tune values for better coverage
    public void integerWriteChunked() {
        ByteBuffer bb = ByteBuffer.allocate(6);
        IntegerWriter w = new IntegerWriter();
        IntegerReader r = new IntegerReader();
        for (int i = 0; i < 1024; i++) { // number of tests
            final int N = 1 + random.nextInt(8);
            final int payload = random.nextInt(255);
            final int expected = random.nextInt(Integer.MAX_VALUE) + 1;

            forEachSplit(bb,
                    (buffers) -> {
                        List<ByteBuffer> buf = new ArrayList<>();
                        relocateBuffers(injectEmptyBuffers(buffers)).forEach(buf::add);
                        boolean written = false;
                        w.configure(expected, N, payload); // TODO: test for payload it can be read after written
                        for (ByteBuffer b : buf) {
                            int pos = b.position();
                            written = w.write(b);
                            b.position(pos);
                        }
                        if (!written) {
                            fail("please increase bb size");
                        }
                        try {
                            r.configure(N).read(concat(buf));
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                        // TODO: check payload here
                        assertEquals(r.get(), expected);
                        w.reset();
                        r.reset();
                        bb.clear();
                    });
        }
    }


    //
    // Since readString(x) is the inverse of writeString(x), thus:
    //
    // for all x: readString(writeString(x)) == x
    //
    @Test
    public void stringIdentity() throws IOException {
        final int MAX_STRING_LENGTH = 4096;
        ByteBuffer bytes = ByteBuffer.allocate(MAX_STRING_LENGTH + 6); // it takes 6 bytes to encode string length of Integer.MAX_VALUE
        CharBuffer chars = CharBuffer.allocate(MAX_STRING_LENGTH);
        StringReader reader = new StringReader();
        StringWriter writer = new StringWriter();
        for (int len = 0; len <= MAX_STRING_LENGTH; len++) {
            for (int i = 0; i < 64; i++) {
                // not so much "test in isolation", I know... we're testing .reset() as well
                bytes.clear();
                chars.clear();

                byte[] b = new byte[len];
                random.nextBytes(b);

                String expected = new String(b, StandardCharsets.ISO_8859_1); // reference string

                boolean written = writer
                        .configure(CharBuffer.wrap(expected), 0, expected.length(), false)
                        .write(bytes);

                if (!written) {
                    fail("please increase 'bytes' size");
                }
                bytes.flip();
                reader.read(bytes, chars);
                chars.flip();
                assertEquals(chars.toString(), expected);
                reader.reset();
                writer.reset();
            }
        }
    }

//    @Test
//    public void huffmanStringWriteChunked() {
//        fail();
//    }
//
//    @Test
//    public void huffmanStringReadChunked() {
//        fail();
//    }

    @Test
    public void stringWriteChunked() {
        final int MAX_STRING_LENGTH = 8;
        final ByteBuffer bytes = ByteBuffer.allocate(MAX_STRING_LENGTH + 6);
        final CharBuffer chars = CharBuffer.allocate(MAX_STRING_LENGTH);
        final StringReader reader = new StringReader();
        final StringWriter writer = new StringWriter();
        for (int len = 0; len <= MAX_STRING_LENGTH; len++) {

            byte[] b = new byte[len];
            random.nextBytes(b);

            String expected = new String(b, StandardCharsets.ISO_8859_1); // reference string

            forEachSplit(bytes, (buffers) -> {
                writer.configure(expected, 0, expected.length(), false);
                boolean written = false;
                for (ByteBuffer buf : buffers) {
                    int p0 = buf.position();
                    written = writer.write(buf);
                    buf.position(p0);
                }
                if (!written) {
                    fail("please increase 'bytes' size");
                }
                try {
                    reader.read(concat(buffers), chars);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                chars.flip();
                assertEquals(chars.toString(), expected);
                reader.reset();
                writer.reset();
                chars.clear();
                bytes.clear();
            });
        }
    }

    @Test
    public void stringReadChunked() {
        final int MAX_STRING_LENGTH = 16;
        final ByteBuffer bytes = ByteBuffer.allocate(MAX_STRING_LENGTH + 6);
        final CharBuffer chars = CharBuffer.allocate(MAX_STRING_LENGTH);
        final StringReader reader = new StringReader();
        final StringWriter writer = new StringWriter();
        for (int len = 0; len <= MAX_STRING_LENGTH; len++) {

            byte[] b = new byte[len];
            random.nextBytes(b);

            String expected = new String(b, StandardCharsets.ISO_8859_1); // reference string

            boolean written = writer
                    .configure(CharBuffer.wrap(expected), 0, expected.length(), false)
                    .write(bytes);
            writer.reset();

            if (!written) {
                fail("please increase 'bytes' size");
            }
            bytes.flip();

            forEachSplit(bytes, (buffers) -> {
                for (ByteBuffer buf : buffers) {
                    int p0 = buf.position();
                    try {
                        reader.read(buf, chars);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    buf.position(p0);
                }
                chars.flip();
                assertEquals(chars.toString(), expected);
                reader.reset();
                chars.clear();
            });

            bytes.clear();
        }
    }

//    @Test
//    public void test_Huffman_String_Identity() {
//        StringWriter writer = new StringWriter();
//        StringReader reader = new StringReader();
//        // 256 * 8 gives 2048 bits in case of plain 8 bit coding
//        // 256 * 30 gives you 7680 bits or 960 bytes in case of almost
//        //          improbable event of 256 30 bits symbols in a row
//        ByteBuffer binary = ByteBuffer.allocate(960);
//        CharBuffer text = CharBuffer.allocate(960 / 5); // 5 = minimum code length
//        for (int len = 0; len < 128; len++) {
//            for (int i = 0; i < 256; i++) {
//                // not so much "test in isolation", I know...
//                binary.clear();
//
//                byte[] bytes = new byte[len];
//                random.nextBytes(bytes);
//
//                String s = new String(bytes, StandardCharsets.ISO_8859_1);
//
//                writer.write(CharBuffer.wrap(s), binary, true);
//                binary.flip();
//                reader.read(binary, text);
//                text.flip();
//                assertEquals(text.toString(), s);
//            }
//        }
//    }

    // TODO: atomic failures: e.g. readonly/overflow

    private static byte[] bytes(int... data) {
        byte[] bytes = new byte[data.length];
        for (int i = 0; i < data.length; i++) {
            bytes[i] = (byte) data[i];
        }
        return bytes;
    }

    private static void verifyRead(byte[] data, int expected, int N) {
        ByteBuffer buf = ByteBuffer.wrap(data, 0, data.length);
        IntegerReader reader = new IntegerReader();
        try {
            reader.configure(N).read(buf);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        assertEquals(reader.get(), expected);
    }

    private void verifyWrite(byte[] expected, int data, int N) {
        IntegerWriter w = new IntegerWriter();
        ByteBuffer buf = ByteBuffer.allocate(2 * expected.length);
        w.configure(data, N, 1).write(buf);
        buf.flip();
        assertEquals(buf, ByteBuffer.wrap(expected));
    }
}
