/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static jdk.internal.net.http.hpack.TestHelper.*;

//
// Tests whose names start with "testX" are the ones captured from real HPACK
// use cases
//
public final class DecoderTest {

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.2.1
    //
    @Test
    public void example1() {
        // @formatter:off
        test("400a 6375 7374 6f6d 2d6b 6579 0d63 7573\n" +
             "746f 6d2d 6865 6164 6572",

             "[  1] (s =  55) custom-key: custom-header\n" +
             "      Table size:  55",

             "custom-key: custom-header");
        // @formatter:on
    }

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.2.2
    //
    @Test
    public void example2() {
        // @formatter:off
        test("040c 2f73 616d 706c 652f 7061 7468",
             "empty.",
             ":path: /sample/path");
        // @formatter:on
    }

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.2.3
    //
    @Test
    public void example3() {
        // @formatter:off
        test("1008 7061 7373 776f 7264 0673 6563 7265\n" +
             "74",
             "empty.",
             "password: secret");
        // @formatter:on
    }

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.2.4
    //
    @Test
    public void example4() {
        // @formatter:off
        test("82",
             "empty.",
             ":method: GET");
        // @formatter:on
    }

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.3
    //
    @Test
    public void example5() {
        // @formatter:off
        Decoder d = new Decoder(256);

        test(d, "8286 8441 0f77 7777 2e65 7861 6d70 6c65\n" +
                "2e63 6f6d",

                "[  1] (s =  57) :authority: www.example.com\n" +
                "      Table size:  57",

                ":method: GET\n" +
                ":scheme: http\n" +
                ":path: /\n" +
                ":authority: www.example.com");

        test(d, "8286 84be 5808 6e6f 2d63 6163 6865",

                "[  1] (s =  53) cache-control: no-cache\n" +
                "[  2] (s =  57) :authority: www.example.com\n" +
                "      Table size: 110",

                ":method: GET\n" +
                ":scheme: http\n" +
                ":path: /\n" +
                ":authority: www.example.com\n" +
                "cache-control: no-cache");

        test(d, "8287 85bf 400a 6375 7374 6f6d 2d6b 6579\n" +
                "0c63 7573 746f 6d2d 7661 6c75 65",

                "[  1] (s =  54) custom-key: custom-value\n" +
                "[  2] (s =  53) cache-control: no-cache\n" +
                "[  3] (s =  57) :authority: www.example.com\n" +
                "      Table size: 164",

                ":method: GET\n" +
                ":scheme: https\n" +
                ":path: /index.html\n" +
                ":authority: www.example.com\n" +
                "custom-key: custom-value");

        // @formatter:on
    }

    @Test
    public void example5AllSplits() {
        // @formatter:off
        testAllSplits(
                "8286 8441 0f77 7777 2e65 7861 6d70 6c65\n" +
                "2e63 6f6d",

                "[  1] (s =  57) :authority: www.example.com\n" +
                "      Table size:  57",

                ":method: GET\n" +
                ":scheme: http\n" +
                ":path: /\n" +
                ":authority: www.example.com");
        // @formatter:on
    }

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.4
    //
    @Test
    public void example6() {
        // @formatter:off
        Decoder d = new Decoder(256);

        test(d, "8286 8441 8cf1 e3c2 e5f2 3a6b a0ab 90f4\n" +
                "ff",

                "[  1] (s =  57) :authority: www.example.com\n" +
                "      Table size:  57",

                ":method: GET\n" +
                ":scheme: http\n" +
                ":path: /\n" +
                ":authority: www.example.com");

        test(d, "8286 84be 5886 a8eb 1064 9cbf",

                "[  1] (s =  53) cache-control: no-cache\n" +
                "[  2] (s =  57) :authority: www.example.com\n" +
                "      Table size: 110",

                ":method: GET\n" +
                ":scheme: http\n" +
                ":path: /\n" +
                ":authority: www.example.com\n" +
                "cache-control: no-cache");

        test(d, "8287 85bf 4088 25a8 49e9 5ba9 7d7f 8925\n" +
                "a849 e95b b8e8 b4bf",

                "[  1] (s =  54) custom-key: custom-value\n" +
                "[  2] (s =  53) cache-control: no-cache\n" +
                "[  3] (s =  57) :authority: www.example.com\n" +
                "      Table size: 164",

                ":method: GET\n" +
                ":scheme: https\n" +
                ":path: /index.html\n" +
                ":authority: www.example.com\n" +
                "custom-key: custom-value");
        // @formatter:on
    }

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.5
    //
    @Test
    public void example7() {
        // @formatter:off
        Decoder d = new Decoder(256);

        test(d, "4803 3330 3258 0770 7269 7661 7465 611d\n" +
                "4d6f 6e2c 2032 3120 4f63 7420 3230 3133\n" +
                "2032 303a 3133 3a32 3120 474d 546e 1768\n" +
                "7474 7073 3a2f 2f77 7777 2e65 7861 6d70\n" +
                "6c65 2e63 6f6d",

                "[  1] (s =  63) location: https://www.example.com\n" +
                "[  2] (s =  65) date: Mon, 21 Oct 2013 20:13:21 GMT\n" +
                "[  3] (s =  52) cache-control: private\n" +
                "[  4] (s =  42) :status: 302\n" +
                "      Table size: 222",

                ":status: 302\n" +
                "cache-control: private\n" +
                "date: Mon, 21 Oct 2013 20:13:21 GMT\n" +
                "location: https://www.example.com");

        test(d, "4803 3330 37c1 c0bf",

                "[  1] (s =  42) :status: 307\n" +
                "[  2] (s =  63) location: https://www.example.com\n" +
                "[  3] (s =  65) date: Mon, 21 Oct 2013 20:13:21 GMT\n" +
                "[  4] (s =  52) cache-control: private\n" +
                "      Table size: 222",

                ":status: 307\n" +
                "cache-control: private\n" +
                "date: Mon, 21 Oct 2013 20:13:21 GMT\n" +
                "location: https://www.example.com");

        test(d, "88c1 611d 4d6f 6e2c 2032 3120 4f63 7420\n" +
                "3230 3133 2032 303a 3133 3a32 3220 474d\n" +
                "54c0 5a04 677a 6970 7738 666f 6f3d 4153\n" +
                "444a 4b48 514b 425a 584f 5157 454f 5049\n" +
                "5541 5851 5745 4f49 553b 206d 6178 2d61\n" +
                "6765 3d33 3630 303b 2076 6572 7369 6f6e\n" +
                "3d31",

                "[  1] (s =  98) set-cookie: foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1\n" +
                "[  2] (s =  52) content-encoding: gzip\n" +
                "[  3] (s =  65) date: Mon, 21 Oct 2013 20:13:22 GMT\n" +
                "      Table size: 215",

                ":status: 200\n" +
                "cache-control: private\n" +
                "date: Mon, 21 Oct 2013 20:13:22 GMT\n" +
                "location: https://www.example.com\n" +
                "content-encoding: gzip\n" +
                "set-cookie: foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1");
        // @formatter:on
    }

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.6
    //
    @Test
    public void example8() {
        // @formatter:off
        Decoder d = new Decoder(256);

        test(d, "4882 6402 5885 aec3 771a 4b61 96d0 7abe\n" +
                "9410 54d4 44a8 2005 9504 0b81 66e0 82a6\n" +
                "2d1b ff6e 919d 29ad 1718 63c7 8f0b 97c8\n" +
                "e9ae 82ae 43d3",

                "[  1] (s =  63) location: https://www.example.com\n" +
                "[  2] (s =  65) date: Mon, 21 Oct 2013 20:13:21 GMT\n" +
                "[  3] (s =  52) cache-control: private\n" +
                "[  4] (s =  42) :status: 302\n" +
                "      Table size: 222",

                ":status: 302\n" +
                "cache-control: private\n" +
                "date: Mon, 21 Oct 2013 20:13:21 GMT\n" +
                "location: https://www.example.com");

        test(d, "4883 640e ffc1 c0bf",

                "[  1] (s =  42) :status: 307\n" +
                "[  2] (s =  63) location: https://www.example.com\n" +
                "[  3] (s =  65) date: Mon, 21 Oct 2013 20:13:21 GMT\n" +
                "[  4] (s =  52) cache-control: private\n" +
                "      Table size: 222",

                ":status: 307\n" +
                "cache-control: private\n" +
                "date: Mon, 21 Oct 2013 20:13:21 GMT\n" +
                "location: https://www.example.com");

        test(d, "88c1 6196 d07a be94 1054 d444 a820 0595\n" +
                "040b 8166 e084 a62d 1bff c05a 839b d9ab\n" +
                "77ad 94e7 821d d7f2 e6c7 b335 dfdf cd5b\n" +
                "3960 d5af 2708 7f36 72c1 ab27 0fb5 291f\n" +
                "9587 3160 65c0 03ed 4ee5 b106 3d50 07",

                "[  1] (s =  98) set-cookie: foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1\n" +
                "[  2] (s =  52) content-encoding: gzip\n" +
                "[  3] (s =  65) date: Mon, 21 Oct 2013 20:13:22 GMT\n" +
                "      Table size: 215",

                ":status: 200\n" +
                "cache-control: private\n" +
                "date: Mon, 21 Oct 2013 20:13:22 GMT\n" +
                "location: https://www.example.com\n" +
                "content-encoding: gzip\n" +
                "set-cookie: foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1");
        // @formatter:on
    }

    @Test
    // One of responses from Apache Server that helped to catch a bug
    public void testX() {
        Decoder d = new Decoder(4096);
        // @formatter:off
        test(d, "3fe1 1f88 6196 d07a be94 03ea 693f 7504\n" +
                "00b6 a05c b827 2e32 fa98 b46f 769e 86b1\n" +
                "9272 b025 da5c 2ea9 fd70 a8de 7fb5 3556\n" +
                "5ab7 6ece c057 02e2 2ad2 17bf 6c96 d07a\n" +
                "be94 0854 cb6d 4a08 0075 40bd 71b6 6e05\n" +
                "a531 68df 0f13 8efe 4522 cd32 21b6 5686\n" +
                "eb23 781f cf52 848f d24a 8f0f 0d02 3435\n" +
                "5f87 497c a589 d34d 1f",

                "[  1] (s =  53) content-type: text/html\n" +
                "[  2] (s =  50) accept-ranges: bytes\n" +
                "[  3] (s =  74) last-modified: Mon, 11 Jun 2007 18:53:14 GMT\n" +
                "[  4] (s =  77) server: Apache/2.4.17 (Unix) OpenSSL/1.0.2e-dev\n" +
                "[  5] (s =  65) date: Mon, 09 Nov 2015 16:26:39 GMT\n" +
                "      Table size: 319",

                ":status: 200\n" +
                "date: Mon, 09 Nov 2015 16:26:39 GMT\n" +
                "server: Apache/2.4.17 (Unix) OpenSSL/1.0.2e-dev\n" +
                "last-modified: Mon, 11 Jun 2007 18:53:14 GMT\n" +
                "etag: \"2d-432a5e4a73a80\"\n" +
                "accept-ranges: bytes\n" +
                "content-length: 45\n" +
                "content-type: text/html");
        // @formatter:on
    }

    @Test
    public void testX1() {
        // Supplier of a decoder with a particular state
        Supplier<Decoder> s = () -> {
            Decoder d = new Decoder(4096);
            // @formatter:off
            test(d, "88 76 92 ca 54 a7 d7 f4 fa ec af ed 6d da 61 d7 bb 1e ad ff" +
                    "df 61 97 c3 61 be 94 13 4a 65 b6 a5 04 00 b8 a0 5a b8 db 77" +
                    "1b 71 4c 5a 37 ff 0f 0d 84 08 00 00 03",

                    "[  1] (s =  65) date: Fri, 24 Jun 2016 14:55:56 GMT\n" +
                    "[  2] (s =  59) server: Jetty(9.3.z-SNAPSHOT)\n" +
                    "      Table size: 124",

                    ":status: 200\n" +
                    "server: Jetty(9.3.z-SNAPSHOT)\n" +
                    "date: Fri, 24 Jun 2016 14:55:56 GMT\n" +
                    "content-length: 100000"
            );
            // @formatter:on
            return d;
        };
        // For all splits of the following data fed to the supplied decoder we
        // must get what's expected
        // @formatter:off
        testAllSplits(s,
                "88 bf be 0f 0d 84 08 00 00 03",

                "[  1] (s =  65) date: Fri, 24 Jun 2016 14:55:56 GMT\n" +
                "[  2] (s =  59) server: Jetty(9.3.z-SNAPSHOT)\n" +
                "      Table size: 124",

                ":status: 200\n" +
                "server: Jetty(9.3.z-SNAPSHOT)\n" +
                "date: Fri, 24 Jun 2016 14:55:56 GMT\n" +
                "content-length: 100000");
        // @formatter:on
    }

    //
    // This test is missing in the spec
    //
    @Test
    public void sizeUpdate() throws IOException {
        Decoder d = new Decoder(4096);
        assertEquals(d.getTable().maxSize(), 4096);
        d.decode(ByteBuffer.wrap(new byte[]{0b00111110}), true, nopCallback()); // newSize = 30
        assertEquals(d.getTable().maxSize(), 30);
    }

    @Test
    public void incorrectSizeUpdate() {
        ByteBuffer b = ByteBuffer.allocate(8);
        Encoder e = new Encoder(8192) {
            @Override
            protected int calculateCapacity(int maxCapacity) {
                return maxCapacity;
            }
        };
        e.header("a", "b");
        e.encode(b);
        b.flip();
        {
            Decoder d = new Decoder(4096);
            assertVoidThrows(IOException.class,
                    () -> d.decode(b, true, (name, value) -> { }));
        }
        b.flip();
        {
            Decoder d = new Decoder(4096);
            assertVoidThrows(IOException.class,
                    () -> d.decode(b, false, (name, value) -> { }));
        }
    }

    @Test
    public void corruptedHeaderBlockInteger() {
        Decoder d = new Decoder(4096);
        ByteBuffer data = ByteBuffer.wrap(new byte[]{
                (byte) 0b11111111, // indexed
                (byte) 0b10011010  // 25 + ...
        });
        IOException e = assertVoidThrows(IOException.class,
                () -> d.decode(data, true, nopCallback()));
        assertExceptionMessageContains(e, "Unexpected end of header block");
    }

    // 5.1.  Integer Representation
    // ...
    // Integer encodings that exceed implementation limits -- in value or octet
    // length -- MUST be treated as decoding errors. Different limits can
    // be set for each of the different uses of integers, based on
    // implementation constraints.
    @Test
    public void headerBlockIntegerNoOverflow() {
        Decoder d = new Decoder(4096);
        ByteBuffer data = ByteBuffer.wrap(new byte[]{
                (byte) 0b11111111, // indexed + 127
                // Integer.MAX_VALUE - 127 (base 128, little-endian):
                (byte) 0b10000000,
                (byte) 0b11111111,
                (byte) 0b11111111,
                (byte) 0b11111111,
                (byte) 0b00000111
        });

        IOException e = assertVoidThrows(IOException.class,
                () -> d.decode(data, true, nopCallback()));

        assertExceptionMessageContains(e.getCause(), "index=2147483647");
    }

    @Test
    public void headerBlockIntegerOverflow() {
        Decoder d = new Decoder(4096);
        ByteBuffer data = ByteBuffer.wrap(new byte[]{
                (byte) 0b11111111, // indexed + 127
                // Integer.MAX_VALUE - 127 + 1 (base 128, little endian):
                (byte) 0b10000001,
                (byte) 0b11111111,
                (byte) 0b11111111,
                (byte) 0b11111111,
                (byte) 0b00000111
        });

        IOException e = assertVoidThrows(IOException.class,
                () -> d.decode(data, true, nopCallback()));

        assertExceptionMessageContains(e, "Integer overflow");
    }

    @Test
    public void corruptedHeaderBlockString1() {
        Decoder d = new Decoder(4096);
        ByteBuffer data = ByteBuffer.wrap(new byte[]{
                0b00001111, // literal, index=15
                0b00000000,
                0b00001000, // huffman=false, length=8
                0b00000000, // \
                0b00000000, //  but only 3 octets available...
                0b00000000  // /
        });
        IOException e = assertVoidThrows(IOException.class,
                () -> d.decode(data, true, nopCallback()));
        assertExceptionMessageContains(e, "Unexpected end of header block");
    }

    @Test
    public void corruptedHeaderBlockString2() {
        Decoder d = new Decoder(4096);
        ByteBuffer data = ByteBuffer.wrap(new byte[]{
                0b00001111, // literal, index=15
                0b00000000,
                (byte) 0b10001000, // huffman=true, length=8
                0b00000000, // \
                0b00000000, //  \
                0b00000000, //   but only 5 octets available...
                0b00000000, //  /
                0b00000000  // /
        });
        IOException e = assertVoidThrows(IOException.class,
                () -> d.decode(data, true, nopCallback()));
        assertExceptionMessageContains(e, "Unexpected end of header block");
    }

    // 5.2.  String Literal Representation
    // ...A Huffman-encoded string literal containing the EOS symbol MUST be
    // treated as a decoding error...
    @Test
    public void corruptedHeaderBlockHuffmanStringEOS() {
        Decoder d = new Decoder(4096);
        ByteBuffer data = ByteBuffer.wrap(new byte[]{
                0b00001111, // literal, index=15
                0b00000000,
                (byte) 0b10000110, // huffman=true, length=6
                0b00011001, 0b01001101, (byte) 0b11111111,
                (byte) 0b11111111, (byte) 0b11111111, (byte) 0b11111100
        });
        IOException e = assertVoidThrows(IOException.class,
                () -> d.decode(data, true, nopCallback()));

        assertExceptionMessageContains(e, "Encountered EOS");
    }

    // 5.2.  String Literal Representation
    // ...A padding strictly longer than 7 bits MUST be treated as a decoding
    // error...
    @Test
    public void corruptedHeaderBlockHuffmanStringLongPadding1() {
        Decoder d = new Decoder(4096);
        ByteBuffer data = ByteBuffer.wrap(new byte[]{
                0b00001111, // literal, index=15
                0b00000000,
                (byte) 0b10000011, // huffman=true, length=3
                0b00011001, 0b01001101, (byte) 0b11111111
                // len("aei") + len(padding) = (5 + 5 + 5) + (9)
        });
        IOException e = assertVoidThrows(IOException.class,
                () -> d.decode(data, true, nopCallback()));

        assertExceptionMessageContains(e, "Padding is too long", "len=9");
    }

    @Test
    public void corruptedHeaderBlockHuffmanStringLongPadding2() {
        Decoder d = new Decoder(4096);
        ByteBuffer data = ByteBuffer.wrap(new byte[]{
                0b00001111, // literal, index=15
                0b00000000,
                (byte) 0b10000011, // huffman=true, length=3
                0b00011001, 0b01111010, (byte) 0b11111111
                // len("aek") + len(padding) = (5 + 5 + 7) + (7)
        });
        assertVoidDoesNotThrow(() -> d.decode(data, true, nopCallback()));
    }

    // 5.2.  String Literal Representation
    // ...A padding not corresponding to the most significant bits of the code
    // for the EOS symbol MUST be treated as a decoding error...
    @Test
    public void corruptedHeaderBlockHuffmanStringNotEOSPadding() {
        Decoder d = new Decoder(4096);
        ByteBuffer data = ByteBuffer.wrap(new byte[]{
                0b00001111, // literal, index=15
                0b00000000,
                (byte) 0b10000011, // huffman=true, length=3
                0b00011001, 0b01111010, (byte) 0b11111110
        });
        IOException e = assertVoidThrows(IOException.class,
                () -> d.decode(data, true, nopCallback()));

        assertExceptionMessageContains(e, "Not a EOS prefix");
    }

    @Test
    public void argsTestBiConsumerIsNull() {
        Decoder decoder = new Decoder(4096);
        assertVoidThrows(NullPointerException.class,
                () -> decoder.decode(ByteBuffer.allocate(16), true, null));
    }

    @Test
    public void argsTestByteBufferIsNull() {
        Decoder decoder = new Decoder(4096);
        assertVoidThrows(NullPointerException.class,
                () -> decoder.decode(null, true, nopCallback()));
    }

    @Test
    public void argsTestBothAreNull() {
        Decoder decoder = new Decoder(4096);
        assertVoidThrows(NullPointerException.class,
                () -> decoder.decode(null, true, null));
    }

    private static void test(String hexdump,
                             String headerTable, String headerList) {
        test(new Decoder(4096), hexdump, headerTable, headerList);
    }

    private static void testAllSplits(String hexdump,
                                      String expectedHeaderTable,
                                      String expectedHeaderList) {
        testAllSplits(() -> new Decoder(256), hexdump, expectedHeaderTable, expectedHeaderList);
    }

    private static void testAllSplits(Supplier<Decoder> supplier,
                                      String hexdump,
                                      String expectedHeaderTable,
                                      String expectedHeaderList) {
        ByteBuffer source = SpecHelper.toBytes(hexdump);

        BuffersTestingKit.forEachSplit(source, iterable -> {
            List<String> actual = new LinkedList<>();
            Iterator<? extends ByteBuffer> i = iterable.iterator();
            if (!i.hasNext()) {
                return;
            }
            Decoder d = supplier.get();
            do {
                ByteBuffer n = i.next();
                try {
                    d.decode(n, !i.hasNext(), (name, value) -> {
                        if (value == null) {
                            actual.add(name.toString());
                        } else {
                            actual.add(name + ": " + value);
                        }
                    });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } while (i.hasNext());
            assertEquals(d.getTable().getStateString(), expectedHeaderTable);
            assertEquals(actual.stream().collect(Collectors.joining("\n")), expectedHeaderList);
        });

        // Now introduce last ByteBuffer which is empty and EOF (mimics idiom
        // I've found in HttpClient code)
        BuffersTestingKit.forEachSplit(source, iterable -> {
            List<String> actual = new LinkedList<>();
            Iterator<? extends ByteBuffer> i = iterable.iterator();
            if (!i.hasNext()) {
                return;
            }
            Decoder d = supplier.get();
            do {
                ByteBuffer n = i.next();
                try {
                    d.decode(n, false, (name, value) -> {
                        if (value == null) {
                            actual.add(name.toString());
                        } else {
                            actual.add(name + ": " + value);
                        }
                    });
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            } while (i.hasNext());

            try {
                d.decode(ByteBuffer.allocate(0), false, (name, value) -> {
                    if (value == null) {
                        actual.add(name.toString());
                    } else {
                        actual.add(name + ": " + value);
                    }
                });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }

            assertEquals(d.getTable().getStateString(), expectedHeaderTable);
            assertEquals(actual.stream().collect(Collectors.joining("\n")), expectedHeaderList);
        });
    }

    //
    // Sometimes we need to keep the same decoder along several runs,
    // as it models the same connection
    //
    private static void test(Decoder d, String hexdump,
                             String expectedHeaderTable, String expectedHeaderList) {

        ByteBuffer source = SpecHelper.toBytes(hexdump);

        List<String> actual = new LinkedList<>();
        try {
            d.decode(source, true, (name, value) -> {
                if (value == null) {
                    actual.add(name.toString());
                } else {
                    actual.add(name + ": " + value);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        assertEquals(d.getTable().getStateString(), expectedHeaderTable);
        assertEquals(actual.stream().collect(Collectors.joining("\n")), expectedHeaderList);
    }

    private static DecodingCallback nopCallback() {
        return (t, u) -> { };
    }
}
