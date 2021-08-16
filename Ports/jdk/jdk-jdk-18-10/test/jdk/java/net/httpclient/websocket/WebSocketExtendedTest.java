/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8159053
 *
 *
 * @run testng/othervm
 *      -Djdk.internal.httpclient.websocket.debug=true
 *      -Djdk.internal.httpclient.debug=true
 *      -Djdk.httpclient.websocket.writeBufferSize=1024
 *      -Djdk.httpclient.websocket.intermediateBufferSize=2048 WebSocketExtendedTest
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import static java.net.http.HttpClient.Builder.NO_PROXY;
import static java.net.http.HttpClient.newBuilder;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;


/*
 * This battery of tests exercises sending data (Text/Binary) messages with
 * possible fragmentation.
 */
public class WebSocketExtendedTest {
// * run testng/othervm
// *      -Djdk.httpclient.websocket.writeBufferSize=16
// *      -Djdk.httpclient.sendBufferSize=32 WebSocketTextTest

    private final static Random random;
    static {
        long seed = System.currentTimeMillis();
        System.out.println("seed=" + seed);
        random = new Random(seed);
    }

    // FIXME ensure subsequent (sendText/Binary, false) only CONTINUATIONs

    @Test(dataProvider = "binary")
    public void binary(ByteBuffer expected) throws IOException, InterruptedException {
        try (DummyWebSocketServer server = new DummyWebSocketServer()) {
            server.open();
            WebSocket ws = newBuilder().proxy(NO_PROXY).build()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();
            ws.sendBinary(expected.duplicate(), true).join();
            ws.abort();
            ByteBuffer data = server.read();
            List<Frame> frames = readFrames(data);
            assertEquals(frames.size(), 1);
            Frame f = frames.get(0);
            assertTrue(f.last);
            assertEquals(f.opcode, Frame.Opcode.BINARY);
            assertEquals(f.data, expected);
        }
    }

    private static List<Frame> readFrames(ByteBuffer src) {
        List<Frame> frames = new ArrayList<>();
        Frame.Consumer consumer = new Frame.Consumer() {

            ByteBuffer data;
            Frame.Opcode opcode;
            Frame.Masker masker = new Frame.Masker();
            boolean last;

            @Override
            public void fin(boolean value) {
                last = value;
            }

            @Override
            public void rsv1(boolean value) {
                if (value) {
                    throw new AssertionError();
                }
            }

            @Override
            public void rsv2(boolean value) {
                if (value) {
                    throw new AssertionError();
                }
            }

            @Override
            public void rsv3(boolean value) {
                if (value) {
                    throw new AssertionError();
                }
            }

            @Override
            public void opcode(Frame.Opcode value) {
                opcode = value;
            }

            @Override
            public void mask(boolean value) {
                if (!value) { // Frames from the client MUST be masked
                    throw new AssertionError();
                }
            }

            @Override
            public void payloadLen(long value) {
                data = ByteBuffer.allocate((int) value);
            }

            @Override
            public void maskingKey(int value) {
                masker.mask(value);
            }

            @Override
            public void payloadData(ByteBuffer data) {
                masker.transferMasking(data, this.data);
            }

            @Override
            public void endFrame() {
                frames.add(new Frame(opcode, this.data.flip(), last));
            }
        };

        Frame.Reader r = new Frame.Reader();
        while (src.hasRemaining()) {
            r.readFrame(src, consumer);
        }
        return frames;
    }

    @Test(dataProvider = "pingPong")
    public void ping(ByteBuffer expected) throws Exception {
        try (DummyWebSocketServer server = new DummyWebSocketServer()) {
            server.open();
            WebSocket ws = newBuilder().proxy(NO_PROXY).build()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();
            ws.sendPing(expected.duplicate()).join();
            ws.abort();
            ByteBuffer data = server.read();
            List<Frame> frames = readFrames(data);
            assertEquals(frames.size(), 1);
            Frame f = frames.get(0);
            assertEquals(f.opcode, Frame.Opcode.PING);
            ByteBuffer actual = ByteBuffer.allocate(expected.remaining());
            actual.put(f.data);
            actual.flip();
            assertEquals(actual, expected);
        }
    }

    @Test(dataProvider = "pingPong")
    public void pong(ByteBuffer expected) throws Exception {
        try (DummyWebSocketServer server = new DummyWebSocketServer()) {
            server.open();
            WebSocket ws = newBuilder().proxy(NO_PROXY).build()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();
            ws.sendPong(expected.duplicate()).join();
            ws.abort();
            ByteBuffer data = server.read();
            List<Frame> frames = readFrames(data);
            assertEquals(frames.size(), 1);
            Frame f = frames.get(0);
            assertEquals(f.opcode, Frame.Opcode.PONG);
            ByteBuffer actual = ByteBuffer.allocate(expected.remaining());
            actual.put(f.data);
            actual.flip();
            assertEquals(actual, expected);
        }
    }

    @Test(dataProvider = "close")
    public void close(int statusCode, String reason) throws Exception {
        try (DummyWebSocketServer server = new DummyWebSocketServer()) {
            server.open();
            WebSocket ws = newBuilder().proxy(NO_PROXY).build()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();
            ws.sendClose(statusCode, reason).join();
            ws.abort();
            ByteBuffer data = server.read();
            List<Frame> frames = readFrames(data);
            assertEquals(frames.size(), 1);
            Frame f = frames.get(0);
            assertEquals(f.opcode, Frame.Opcode.CLOSE);
            ByteBuffer actual = ByteBuffer.allocate(Frame.MAX_CONTROL_FRAME_PAYLOAD_SIZE);
            actual.put(f.data);
            actual.flip();
            assertEquals(actual.getChar(), statusCode);
            assertEquals(StandardCharsets.UTF_8.decode(actual).toString(), reason);
        }
    }

    @Test(dataProvider = "text")
    public void text(String expected) throws Exception {
        try (DummyWebSocketServer server = new DummyWebSocketServer()) {
            server.open();
            WebSocket ws = newBuilder().proxy(NO_PROXY).build()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();
            ws.sendText(expected, true).join();
            ws.abort();
            ByteBuffer data = server.read();
            List<Frame> frames = readFrames(data);

            int maxBytes = (int) StandardCharsets.UTF_8.newEncoder().maxBytesPerChar() * expected.length();
            ByteBuffer actual = ByteBuffer.allocate(maxBytes);
            frames.stream().forEachOrdered(f -> actual.put(f.data));
            actual.flip();
            assertEquals(StandardCharsets.UTF_8.decode(actual).toString(), expected);
        }
    }

    @DataProvider(name = "pingPong")
    public Object[][] pingPongSizes() {
        return new Object[][]{
                {bytes(  0)},
                {bytes(  1)},
                {bytes( 63)},
                {bytes(125)},
        };
    }

    @DataProvider(name = "close")
    public Object[][] closeArguments() {
        return new Object[][]{
                {WebSocket.NORMAL_CLOSURE, utf8String( 0)},
                {WebSocket.NORMAL_CLOSURE, utf8String( 1)},
                // 123 / 3 = max reason bytes / max bytes per char
                {WebSocket.NORMAL_CLOSURE, utf8String(41)},
        };
    }

    private static String utf8String(int n) {
        char[] abc = {
                // -- English Alphabet (26 characters, 1 byte per char) --
                0x0041, 0x0042, 0x0043, 0x0044, 0x0045, 0x0046, 0x0047, 0x0048,
                0x0049, 0x004A, 0x004B, 0x004C, 0x004D, 0x004E, 0x004F, 0x0050,
                0x0051, 0x0052, 0x0053, 0x0054, 0x0055, 0x0056, 0x0057, 0x0058,
                0x0059, 0x005A,
                // -- Russian Alphabet (33 characters, 2 bytes per char) --
                0x0410, 0x0411, 0x0412, 0x0413, 0x0414, 0x0415, 0x0401, 0x0416,
                0x0417, 0x0418, 0x0419, 0x041A, 0x041B, 0x041C, 0x041D, 0x041E,
                0x041F, 0x0420, 0x0421, 0x0422, 0x0423, 0x0424, 0x0425, 0x0426,
                0x0427, 0x0428, 0x0429, 0x042A, 0x042B, 0x042C, 0x042D, 0x042E,
                0x042F,
                // -- Hiragana base characters (46 characters, 3 bytes per char) --
                0x3042, 0x3044, 0x3046, 0x3048, 0x304A, 0x304B, 0x304D, 0x304F,
                0x3051, 0x3053, 0x3055, 0x3057, 0x3059, 0x305B, 0x305D, 0x305F,
                0x3061, 0x3064, 0x3066, 0x3068, 0x306A, 0x306B, 0x306C, 0x306D,
                0x306E, 0x306F, 0x3072, 0x3075, 0x3078, 0x307B, 0x307E, 0x307F,
                0x3080, 0x3081, 0x3082, 0x3084, 0x3086, 0x3088, 0x3089, 0x308A,
                0x308B, 0x308C, 0x308D, 0x308F, 0x3092, 0x3093,
        };

        assert new String(abc).getBytes(StandardCharsets.UTF_8).length > abc.length;

        StringBuilder str = new StringBuilder(n);
        random.ints(0, abc.length).limit(n).forEach(i -> str.append(abc[i]));
        return str.toString();
    }

    @DataProvider(name = "text")
    public Object[][] texts() {
        return new Object[][]{
                {utf8String(   0)},
                {utf8String(1024)},
        };
    }

    @DataProvider(name = "binary")
    public Object[][] binary() {
        return new Object[][]{
                {bytes(   0)},
                {bytes(1024)},
        };
    }

    private static ByteBuffer bytes(int n) {
        byte[] array = new byte[n];
        random.nextBytes(array);
        return ByteBuffer.wrap(array);
    }
}
