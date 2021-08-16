/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @build DummyWebSocketServer
 * @run testng/othervm
 *      -Djdk.internal.httpclient.websocket.debug=true
 *       AutomaticPong
 */
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.net.http.HttpClient.newHttpClient;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class AutomaticPong {
    /*
     * The sendClose method has been invoked and a Ping comes from the server.
     * Naturally, the client cannot reply with a Pong (the output has been
     * closed). However, this MUST not be treated as an error.
     * At this stage the server either has received or pretty soon will receive
     * the Close message sent by the sendClose. Thus, the server will know the
     * client cannot send further messages and it's up to the server to decide
     * how to react on the corresponding Pong not being received.
     */
    @Test
    public void sendCloseThenAutomaticPong() throws IOException {
        int[] bytes = {
                0x89, 0x00,                                     // ping
                0x89, 0x06, 0x68, 0x65, 0x6c, 0x6c, 0x6f, 0x3f, // ping hello?
                0x88, 0x00,                                     // close
        };
        try (var server = Support.serverWithCannedData(bytes)) {
            server.open();
            MockListener listener = new MockListener() {
                @Override
                protected void onOpen0(WebSocket webSocket) {
                    /* request nothing */
                }
            };
            var webSocket = newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            try {
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "ok").join();
                // now request all messages available
                webSocket.request(Long.MAX_VALUE);
                List<MockListener.Invocation> actual = listener.invocations();
                ByteBuffer hello = ByteBuffer.wrap("hello?".getBytes(StandardCharsets.UTF_8));
                ByteBuffer empty = ByteBuffer.allocate(0);
                List<MockListener.Invocation> expected = List.of(
                        MockListener.Invocation.onOpen(webSocket),
                        MockListener.Invocation.onPing(webSocket, empty),
                        MockListener.Invocation.onPing(webSocket, hello),
                        MockListener.Invocation.onClose(webSocket, 1005, "")
                );
                assertEquals(actual, expected);
            } finally {
                webSocket.abort();
            }
        }
    }

    /*
     * The server sends a number of contiguous Ping messages. The client replies
     * to these messages automatically. According to RFC 6455 a WebSocket client
     * is free to reply only to the most recent Pings.
     *
     * What is checked here is that:
     *
     *     a) the order of Pong replies corresponds to the Pings received,
     *     b) the last Pong corresponds to the last Ping
     *     c) there are no unrelated Pongs
     */
    @Test(dataProvider = "nPings")
    public void automaticPongs(int nPings) throws Exception {
        // big enough to not bother with resize
        ByteBuffer buffer = ByteBuffer.allocate(65536);
        Frame.HeaderWriter w = new Frame.HeaderWriter();
        for (int i = 0; i < nPings; i++) {
            w.fin(true)
             .opcode(Frame.Opcode.PING)
             .noMask()
             .payloadLen(4)    // the length of the number of the Ping (int)
             .write(buffer);
            buffer.putInt(i);  // the number of the Ping (int)
        }
        w.fin(true)
         .opcode(Frame.Opcode.CLOSE)
         .noMask()
         .payloadLen(2)
        .write(buffer);
        buffer.putChar((char) 1000);
        buffer.flip();
        try (var server = Support.serverWithCannedData(buffer.array())) {
            server.open();
            MockListener listener = new MockListener();
            var webSocket = newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            try {
                List<MockListener.Invocation> inv = listener.invocations();
                assertEquals(inv.size(), nPings + 2); // n * onPing + onOpen + onClose

                ByteBuffer data = server.read();
                Frame.Reader reader = new Frame.Reader();

                Frame.Consumer consumer = new Frame.Consumer() {

                    ByteBuffer number = ByteBuffer.allocate(4);
                    Frame.Masker masker = new Frame.Masker();
                    int i = -1;
                    boolean closed;

                    @Override
                    public void fin(boolean value) {
                        assertTrue(value);
                    }

                    @Override
                    public void rsv1(boolean value) {
                        assertFalse(value);
                    }

                    @Override
                    public void rsv2(boolean value) {
                        assertFalse(value);
                    }

                    @Override
                    public void rsv3(boolean value) {
                        assertFalse(value);
                    }

                    @Override
                    public void opcode(Frame.Opcode value) {
                        if (value == Frame.Opcode.CLOSE) {
                            closed = true;
                            return;
                        }
                        assertEquals(value, Frame.Opcode.PONG);
                    }

                    @Override
                    public void mask(boolean value) {
                        assertTrue(value);
                    }

                    @Override
                    public void payloadLen(long value) {
                        if (!closed)
                            assertEquals(value, 4);
                    }

                    @Override
                    public void maskingKey(int value) {
                        masker.mask(value);
                    }

                    @Override
                    public void payloadData(ByteBuffer src) {
                        masker.transferMasking(src, number);
                        if (closed) {
                            return;
                        }
                        number.flip();
                        int n = number.getInt();
                        System.out.printf("pong number=%s%n", n);
                        number.clear();
                        // a Pong with the number less than the maximum of Pongs already
                        // received MUST never be received
                        if (i >= n) {
                            fail(String.format("i=%s, n=%s", i, n));
                        }
                        i = n;
                    }

                    @Override
                    public void endFrame() {
                    }
                };
                while (data.hasRemaining()) {
                    reader.readFrame(data, consumer);
                }
            } finally {
                webSocket.abort();
            }
        }
    }


    @DataProvider(name = "nPings")
    public Object[][] nPings() {
        return new Object[][]{{1}, {2}, {4}, {8}, {9}, {256}};
    }
}
