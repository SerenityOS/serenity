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
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static jdk.internal.net.http.hpack.BuffersTestingKit.concat;
import static jdk.internal.net.http.hpack.BuffersTestingKit.forEachSplit;
import static jdk.internal.net.http.hpack.SpecHelper.toHexdump;
import static jdk.internal.net.http.hpack.TestHelper.assertVoidThrows;
import static java.util.Arrays.asList;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

// TODO: map textual representation of commands from the spec to actual
// calls to encoder (actually, this is a good idea for decoder as well)
public final class EncoderTest {

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.2.1
    //
    @Test
    public void example1() {

        Encoder e = newCustomEncoder(256);
        drainInitialUpdate(e);

        e.literalWithIndexing("custom-key", false, "custom-header", false);
        // @formatter:off
        test(e,

             "400a 6375 7374 6f6d 2d6b 6579 0d63 7573\n" +
             "746f 6d2d 6865 6164 6572",

             "[  1] (s =  55) custom-key: custom-header\n" +
             "      Table size:  55");
        // @formatter:on
    }

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.2.2
    //
    @Test
    public void example2() {

        Encoder e = newCustomEncoder(256);
        drainInitialUpdate(e);

        e.literal(4, "/sample/path", false);
        // @formatter:off
        test(e,

             "040c 2f73 616d 706c 652f 7061 7468",

             "empty.");
        // @formatter:on
    }

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.2.3
    //
    @Test
    public void example3() {

        Encoder e = newCustomEncoder(256);
        drainInitialUpdate(e);

        e.literalNeverIndexed("password", false, "secret", false);
        // @formatter:off
        test(e,

             "1008 7061 7373 776f 7264 0673 6563 7265\n" +
             "74",

             "empty.");
        // @formatter:on
    }

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.2.4
    //
    @Test
    public void example4() {

        Encoder e = newCustomEncoder(256);
        drainInitialUpdate(e);

        e.indexed(2);
        // @formatter:off
        test(e,

             "82",

             "empty.");
        // @formatter:on
    }

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.3
    //
    @Test
    public void example5() {
        Encoder e = newCustomEncoder(256);
        drainInitialUpdate(e);

        ByteBuffer output = ByteBuffer.allocate(64);
        e.indexed(2);
        e.encode(output);
        e.indexed(6);
        e.encode(output);
        e.indexed(4);
        e.encode(output);
        e.literalWithIndexing(1, "www.example.com", false);
        e.encode(output);

        output.flip();

        // @formatter:off
        test(e, output,

             "8286 8441 0f77 7777 2e65 7861 6d70 6c65\n" +
             "2e63 6f6d",

             "[  1] (s =  57) :authority: www.example.com\n" +
             "      Table size:  57");

        output.clear();

        e.indexed( 2);
        e.encode(output);
        e.indexed( 6);
        e.encode(output);
        e.indexed( 4);
        e.encode(output);
        e.indexed(62);
        e.encode(output);
        e.literalWithIndexing(24, "no-cache", false);
        e.encode(output);

        output.flip();

        test(e, output,

             "8286 84be 5808 6e6f 2d63 6163 6865",

             "[  1] (s =  53) cache-control: no-cache\n" +
             "[  2] (s =  57) :authority: www.example.com\n" +
             "      Table size: 110");

        output.clear();

        e.indexed( 2);
        e.encode(output);
        e.indexed( 7);
        e.encode(output);
        e.indexed( 5);
        e.encode(output);
        e.indexed(63);
        e.encode(output);
        e.literalWithIndexing("custom-key", false, "custom-value", false);
        e.encode(output);

        output.flip();

        test(e, output,

             "8287 85bf 400a 6375 7374 6f6d 2d6b 6579\n" +
             "0c63 7573 746f 6d2d 7661 6c75 65",

             "[  1] (s =  54) custom-key: custom-value\n" +
             "[  2] (s =  53) cache-control: no-cache\n" +
             "[  3] (s =  57) :authority: www.example.com\n" +
             "      Table size: 164");
        // @formatter:on
    }

    @Test
    public void example5AllSplits() {

        List<Consumer<Encoder>> actions = new LinkedList<>();
        actions.add(e -> e.indexed(2));
        actions.add(e -> e.indexed(6));
        actions.add(e -> e.indexed(4));
        actions.add(e -> e.literalWithIndexing(1, "www.example.com", false));

        encodeAllSplits(
                actions,

                "8286 8441 0f77 7777 2e65 7861 6d70 6c65\n" +
                "2e63 6f6d",

                "[  1] (s =  57) :authority: www.example.com\n" +
                "      Table size:  57");
    }

    private static void encodeAllSplits(Iterable<Consumer<Encoder>> consumers,
                                        String expectedHexdump,
                                        String expectedTableState) {
        ByteBuffer buffer = SpecHelper.toBytes(expectedHexdump);
        erase(buffer); // Zeroed buffer of size needed to hold the encoding
        forEachSplit(buffer, iterable -> {
            List<ByteBuffer> copy = new LinkedList<>();
            iterable.forEach(b -> copy.add(ByteBuffer.allocate(b.remaining())));
            Iterator<ByteBuffer> output = copy.iterator();
            if (!output.hasNext()) {
                throw new IllegalStateException("No buffers to encode to");
            }
            Encoder e = newCustomEncoder(256); // FIXME: pull up (as a parameter)
            drainInitialUpdate(e);
            boolean encoded;
            ByteBuffer b = output.next();
            for (Consumer<Encoder> c : consumers) {
                c.accept(e);
                do {
                    encoded = e.encode(b);
                    if (!encoded) {
                        if (output.hasNext()) {
                            b = output.next();
                        } else {
                            throw new IllegalStateException("No room for encoding");
                        }
                    }
                }
                while (!encoded);
            }
            copy.forEach(Buffer::flip);
            ByteBuffer data = concat(copy);
            test(e, data, expectedHexdump, expectedTableState);
        });
    }

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.4
    //
    @Test
    public void example6() {
        Encoder e = newCustomEncoder(256);
        drainInitialUpdate(e);

        ByteBuffer output = ByteBuffer.allocate(64);
        e.indexed(2);
        e.encode(output);
        e.indexed(6);
        e.encode(output);
        e.indexed(4);
        e.encode(output);
        e.literalWithIndexing(1, "www.example.com", true);
        e.encode(output);

        output.flip();

        // @formatter:off
        test(e, output,

             "8286 8441 8cf1 e3c2 e5f2 3a6b a0ab 90f4\n" +
             "ff",

             "[  1] (s =  57) :authority: www.example.com\n" +
             "      Table size:  57");

        output.clear();

        e.indexed( 2);
        e.encode(output);
        e.indexed( 6);
        e.encode(output);
        e.indexed( 4);
        e.encode(output);
        e.indexed(62);
        e.encode(output);
        e.literalWithIndexing(24, "no-cache", true);
        e.encode(output);

        output.flip();

        test(e, output,

             "8286 84be 5886 a8eb 1064 9cbf",

             "[  1] (s =  53) cache-control: no-cache\n" +
             "[  2] (s =  57) :authority: www.example.com\n" +
             "      Table size: 110");

        output.clear();

        e.indexed( 2);
        e.encode(output);
        e.indexed( 7);
        e.encode(output);
        e.indexed( 5);
        e.encode(output);
        e.indexed(63);
        e.encode(output);
        e.literalWithIndexing("custom-key", true, "custom-value", true);
        e.encode(output);

        output.flip();

        test(e, output,

             "8287 85bf 4088 25a8 49e9 5ba9 7d7f 8925\n" +
             "a849 e95b b8e8 b4bf",

             "[  1] (s =  54) custom-key: custom-value\n" +
             "[  2] (s =  53) cache-control: no-cache\n" +
             "[  3] (s =  57) :authority: www.example.com\n" +
             "      Table size: 164");
        // @formatter:on
    }

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.5
    //
    @Test
    public void example7() {
        Encoder e = newCustomEncoder(256);
        drainInitialUpdate(e);

        ByteBuffer output = ByteBuffer.allocate(128);
        // @formatter:off
        e.literalWithIndexing( 8, "302", false);
        e.encode(output);
        e.literalWithIndexing(24, "private", false);
        e.encode(output);
        e.literalWithIndexing(33, "Mon, 21 Oct 2013 20:13:21 GMT", false);
        e.encode(output);
        e.literalWithIndexing(46, "https://www.example.com", false);
        e.encode(output);

        output.flip();

        test(e, output,

             "4803 3330 3258 0770 7269 7661 7465 611d\n" +
             "4d6f 6e2c 2032 3120 4f63 7420 3230 3133\n" +
             "2032 303a 3133 3a32 3120 474d 546e 1768\n" +
             "7474 7073 3a2f 2f77 7777 2e65 7861 6d70\n" +
             "6c65 2e63 6f6d",

             "[  1] (s =  63) location: https://www.example.com\n" +
             "[  2] (s =  65) date: Mon, 21 Oct 2013 20:13:21 GMT\n" +
             "[  3] (s =  52) cache-control: private\n" +
             "[  4] (s =  42) :status: 302\n" +
             "      Table size: 222");

        output.clear();

        e.literalWithIndexing( 8, "307", false);
        e.encode(output);
        e.indexed(65);
        e.encode(output);
        e.indexed(64);
        e.encode(output);
        e.indexed(63);
        e.encode(output);

        output.flip();

        test(e, output,

             "4803 3330 37c1 c0bf",

             "[  1] (s =  42) :status: 307\n" +
             "[  2] (s =  63) location: https://www.example.com\n" +
             "[  3] (s =  65) date: Mon, 21 Oct 2013 20:13:21 GMT\n" +
             "[  4] (s =  52) cache-control: private\n" +
             "      Table size: 222");

        output.clear();

        e.indexed( 8);
        e.encode(output);
        e.indexed(65);
        e.encode(output);
        e.literalWithIndexing(33, "Mon, 21 Oct 2013 20:13:22 GMT", false);
        e.encode(output);
        e.indexed(64);
        e.encode(output);
        e.literalWithIndexing(26, "gzip", false);
        e.encode(output);
        e.literalWithIndexing(55, "foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1", false);
        e.encode(output);

        output.flip();

        test(e, output,

             "88c1 611d 4d6f 6e2c 2032 3120 4f63 7420\n" +
             "3230 3133 2032 303a 3133 3a32 3220 474d\n" +
             "54c0 5a04 677a 6970 7738 666f 6f3d 4153\n" +
             "444a 4b48 514b 425a 584f 5157 454f 5049\n" +
             "5541 5851 5745 4f49 553b 206d 6178 2d61\n" +
             "6765 3d33 3630 303b 2076 6572 7369 6f6e\n" +
             "3d31",

             "[  1] (s =  98) set-cookie: foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1\n" +
             "[  2] (s =  52) content-encoding: gzip\n" +
             "[  3] (s =  65) date: Mon, 21 Oct 2013 20:13:22 GMT\n" +
             "      Table size: 215");
        // @formatter:on
    }

    //
    // http://tools.ietf.org/html/rfc7541#appendix-C.6
    //
    @Test
    public void example8() {
        Encoder e = newCustomEncoder(256);
        drainInitialUpdate(e);

        ByteBuffer output = ByteBuffer.allocate(128);
        // @formatter:off
        e.literalWithIndexing( 8, "302", true);
        e.encode(output);
        e.literalWithIndexing(24, "private", true);
        e.encode(output);
        e.literalWithIndexing(33, "Mon, 21 Oct 2013 20:13:21 GMT", true);
        e.encode(output);
        e.literalWithIndexing(46, "https://www.example.com", true);
        e.encode(output);

        output.flip();

        test(e, output,

             "4882 6402 5885 aec3 771a 4b61 96d0 7abe\n" +
             "9410 54d4 44a8 2005 9504 0b81 66e0 82a6\n" +
             "2d1b ff6e 919d 29ad 1718 63c7 8f0b 97c8\n" +
             "e9ae 82ae 43d3",

             "[  1] (s =  63) location: https://www.example.com\n" +
             "[  2] (s =  65) date: Mon, 21 Oct 2013 20:13:21 GMT\n" +
             "[  3] (s =  52) cache-control: private\n" +
             "[  4] (s =  42) :status: 302\n" +
             "      Table size: 222");

        output.clear();

        e.literalWithIndexing( 8, "307", true);
        e.encode(output);
        e.indexed(65);
        e.encode(output);
        e.indexed(64);
        e.encode(output);
        e.indexed(63);
        e.encode(output);

        output.flip();

        test(e, output,

             "4883 640e ffc1 c0bf",

             "[  1] (s =  42) :status: 307\n" +
             "[  2] (s =  63) location: https://www.example.com\n" +
             "[  3] (s =  65) date: Mon, 21 Oct 2013 20:13:21 GMT\n" +
             "[  4] (s =  52) cache-control: private\n" +
             "      Table size: 222");

        output.clear();

        e.indexed( 8);
        e.encode(output);
        e.indexed(65);
        e.encode(output);
        e.literalWithIndexing(33, "Mon, 21 Oct 2013 20:13:22 GMT", true);
        e.encode(output);
        e.indexed(64);
        e.encode(output);
        e.literalWithIndexing(26, "gzip", true);
        e.encode(output);
        e.literalWithIndexing(55, "foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1", true);
        e.encode(output);

        output.flip();

        test(e, output,

             "88c1 6196 d07a be94 1054 d444 a820 0595\n" +
             "040b 8166 e084 a62d 1bff c05a 839b d9ab\n" +
             "77ad 94e7 821d d7f2 e6c7 b335 dfdf cd5b\n" +
             "3960 d5af 2708 7f36 72c1 ab27 0fb5 291f\n" +
             "9587 3160 65c0 03ed 4ee5 b106 3d50 07",

             "[  1] (s =  98) set-cookie: foo=ASDJKHQKBZXOQWEOPIUAXQWEOIU; max-age=3600; version=1\n" +
             "[  2] (s =  52) content-encoding: gzip\n" +
             "[  3] (s =  65) date: Mon, 21 Oct 2013 20:13:22 GMT\n" +
             "      Table size: 215");
        // @formatter:on
    }

    @Test
    public void initialSizeUpdateDefaultEncoder() throws IOException {
        Function<Integer, Encoder> e = Encoder::new;
        testSizeUpdate(e, 1024, asList(), asList(0));
        testSizeUpdate(e, 1024, asList(1024), asList(0));
        testSizeUpdate(e, 1024, asList(1024, 1024), asList(0));
        testSizeUpdate(e, 1024, asList(1024, 512), asList(0));
        testSizeUpdate(e, 1024, asList(512, 1024), asList(0));
        testSizeUpdate(e, 1024, asList(512, 2048), asList(0));
    }

    @Test
    public void initialSizeUpdateCustomEncoder() throws IOException {
        Function<Integer, Encoder> e = EncoderTest::newCustomEncoder;
        testSizeUpdate(e, 1024, asList(), asList(1024));
        testSizeUpdate(e, 1024, asList(1024), asList(1024));
        testSizeUpdate(e, 1024, asList(1024, 1024), asList(1024));
        testSizeUpdate(e, 1024, asList(1024, 512), asList(512));
        testSizeUpdate(e, 1024, asList(512, 1024), asList(1024));
        testSizeUpdate(e, 1024, asList(512, 2048), asList(2048));
    }

    @Test
    public void seriesOfSizeUpdatesDefaultEncoder() throws IOException {
        Function<Integer, Encoder> e = c -> {
            Encoder encoder = new Encoder(c);
            drainInitialUpdate(encoder);
            return encoder;
        };
        testSizeUpdate(e, 0, asList(0), asList());
        testSizeUpdate(e, 1024, asList(1024), asList());
        testSizeUpdate(e, 1024, asList(2048), asList());
        testSizeUpdate(e, 1024, asList(512), asList());
        testSizeUpdate(e, 1024, asList(1024, 1024), asList());
        testSizeUpdate(e, 1024, asList(1024, 2048), asList());
        testSizeUpdate(e, 1024, asList(2048, 1024), asList());
        testSizeUpdate(e, 1024, asList(1024, 512), asList());
        testSizeUpdate(e, 1024, asList(512, 1024), asList());
    }

    //
    // https://tools.ietf.org/html/rfc7541#section-4.2
    //
    @Test
    public void seriesOfSizeUpdatesCustomEncoder() throws IOException {
        Function<Integer, Encoder> e = c -> {
            Encoder encoder = newCustomEncoder(c);
            drainInitialUpdate(encoder);
            return encoder;
        };
        testSizeUpdate(e, 0, asList(0), asList());
        testSizeUpdate(e, 1024, asList(1024), asList());
        testSizeUpdate(e, 1024, asList(2048), asList(2048));
        testSizeUpdate(e, 1024, asList(512), asList(512));
        testSizeUpdate(e, 1024, asList(1024, 1024), asList());
        testSizeUpdate(e, 1024, asList(1024, 2048), asList(2048));
        testSizeUpdate(e, 1024, asList(2048, 1024), asList());
        testSizeUpdate(e, 1024, asList(1024, 512), asList(512));
        testSizeUpdate(e, 1024, asList(512, 1024), asList(512, 1024));
    }

    @Test
    public void callSequenceViolations() {
        {   // Hasn't set up a header
            Encoder e = new Encoder(0);
            assertVoidThrows(IllegalStateException.class, () -> e.encode(ByteBuffer.allocate(16)));
        }
        {   // Can't set up header while there's an unfinished encoding
            Encoder e = new Encoder(0);
            e.indexed(32);
            assertVoidThrows(IllegalStateException.class, () -> e.indexed(32));
        }
        {   // Can't setMaxCapacity while there's an unfinished encoding
            Encoder e = new Encoder(0);
            e.indexed(32);
            assertVoidThrows(IllegalStateException.class, () -> e.setMaxCapacity(512));
        }
        {   // Hasn't set up a header
            Encoder e = new Encoder(0);
            e.setMaxCapacity(256);
            assertVoidThrows(IllegalStateException.class, () -> e.encode(ByteBuffer.allocate(16)));
        }
        {   // Hasn't set up a header after the previous encoding
            Encoder e = new Encoder(0);
            e.indexed(0);
            boolean encoded = e.encode(ByteBuffer.allocate(16));
            assertTrue(encoded); // assumption
            assertVoidThrows(IllegalStateException.class, () -> e.encode(ByteBuffer.allocate(16)));
        }
    }

    private static void test(Encoder encoder,
                             String expectedTableState,
                             String expectedHexdump) {

        ByteBuffer b = ByteBuffer.allocate(128);
        encoder.encode(b);
        b.flip();
        test(encoder, b, expectedTableState, expectedHexdump);
    }

    private static void test(Encoder encoder,
                             ByteBuffer output,
                             String expectedHexdump,
                             String expectedTableState) {

        String actualTableState = encoder.getHeaderTable().getStateString();
        assertEquals(actualTableState, expectedTableState);

        String actualHexdump = toHexdump(output);
        assertEquals(actualHexdump, expectedHexdump.replaceAll("\\n", " "));
    }

    // initial size - the size encoder is constructed with
    // updates      - a sequence of values for consecutive calls to encoder.setMaxCapacity
    // expected     - a sequence of values expected to be decoded by a decoder
    private void testSizeUpdate(Function<Integer, Encoder> encoder,
                                int initialSize,
                                List<Integer> updates,
                                List<Integer> expected) throws IOException {
        Encoder e = encoder.apply(initialSize);
        updates.forEach(e::setMaxCapacity);
        ByteBuffer b = ByteBuffer.allocate(64);
        e.header("a", "b");
        e.encode(b);
        b.flip();
        Decoder d = new Decoder(updates.isEmpty() ? initialSize : Collections.max(updates));
        List<Integer> actual = new ArrayList<>();
        d.decode(b, true, new DecodingCallback() {
            @Override
            public void onDecoded(CharSequence name, CharSequence value) { }

            @Override
            public void onSizeUpdate(int capacity) {
                actual.add(capacity);
            }
        });
        assertEquals(actual, expected);
    }

    //
    // Default encoder does not need any table, therefore a subclass that
    // behaves differently is needed
    //
    private static Encoder newCustomEncoder(int maxCapacity) {
        return new Encoder(maxCapacity) {
            @Override
            protected int calculateCapacity(int maxCapacity) {
                return maxCapacity;
            }
        };
    }

    private static void drainInitialUpdate(Encoder e) {
        ByteBuffer b = ByteBuffer.allocate(4);
        e.header("a", "b");
        boolean done;
        do {
            done = e.encode(b);
            b.flip();
        } while (!done);
    }

    private static void erase(ByteBuffer buffer) {
        buffer.clear();
        while (buffer.hasRemaining()) {
            buffer.put((byte) 0);
        }
        buffer.clear();
    }
}
