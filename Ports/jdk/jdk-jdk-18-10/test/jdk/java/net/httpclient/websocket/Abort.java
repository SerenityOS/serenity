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
 *       Abort
 */

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.ProtocolException;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static java.net.http.HttpClient.newHttpClient;
import static java.net.http.WebSocket.NORMAL_CLOSURE;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

public class Abort {

    private static final Class<NullPointerException> NPE = NullPointerException.class;
    private static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;
    private static final Class<IOException> IOE = IOException.class;


    @Test
    public void onOpenThenAbort() throws Exception {
        int[] bytes = new int[]{
                0x88, 0x00, // opcode=close
        };
        try (var server = Support.serverWithCannedData(bytes)) {
            server.open();
            // messages are available
            MockListener listener = new MockListener() {
                @Override
                protected void onOpen0(WebSocket webSocket) {
                    // unbounded request
                    webSocket.request(Long.MAX_VALUE);
                    webSocket.abort();
                }
            };
            var webSocket = newHttpClient().newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            try {
                TimeUnit.SECONDS.sleep(5);
                List<MockListener.Invocation> inv = listener.invocationsSoFar();
                // no more invocations after onOpen as WebSocket was aborted
                assertEquals(inv, List.of(MockListener.Invocation.onOpen(webSocket)));
            } finally {
                webSocket.abort();
            }
        }
    }

    @Test
    public void onOpenThenOnTextThenAbort() throws Exception {
        int[] bytes = new int[]{
                0x81, 0x00, // opcode=text, fin=true
                0x88, 0x00, // opcode=close
        };
        try (var server = Support.serverWithCannedData(bytes)) {
            server.open();
            MockListener listener = new MockListener() {
                @Override
                protected void onOpen0(WebSocket webSocket) {
                    // unbounded request
                    webSocket.request(Long.MAX_VALUE);
                }

                @Override
                protected CompletionStage<?> onText0(WebSocket webSocket,
                                                     CharSequence message,
                                                     boolean last) {
                    webSocket.abort();
                    return super.onText0(webSocket, message, last);
                }
            };
            var webSocket = newHttpClient().newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            try {
                TimeUnit.SECONDS.sleep(5);
                List<MockListener.Invocation> inv = listener.invocationsSoFar();
                // no more invocations after onOpen, onBinary as WebSocket was aborted
                List<MockListener.Invocation> expected = List.of(
                        MockListener.Invocation.onOpen(webSocket),
                        MockListener.Invocation.onText(webSocket, "", true));
                assertEquals(inv, expected);
            } finally {
                webSocket.abort();
            }
        }
    }

    @Test
    public void onOpenThenOnBinaryThenAbort() throws Exception {
        int[] bytes = new int[]{
                0x82, 0x00, // opcode=binary, fin=true
                0x88, 0x00, // opcode=close
        };
        try (var server = Support.serverWithCannedData(bytes)) {
            server.open();
            MockListener listener = new MockListener() {
                @Override
                protected void onOpen0(WebSocket webSocket) {
                    // unbounded request
                    webSocket.request(Long.MAX_VALUE);
                }

                @Override
                protected CompletionStage<?> onBinary0(WebSocket webSocket,
                                                       ByteBuffer message,
                                                       boolean last) {
                    webSocket.abort();
                    return super.onBinary0(webSocket, message, last);
                }
            };
            var webSocket = newHttpClient().newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            try {
                TimeUnit.SECONDS.sleep(5);
                List<MockListener.Invocation> inv = listener.invocationsSoFar();
                // no more invocations after onOpen, onBinary as WebSocket was aborted
                List<MockListener.Invocation> expected = List.of(
                        MockListener.Invocation.onOpen(webSocket),
                        MockListener.Invocation.onBinary(webSocket, ByteBuffer.allocate(0), true));
                assertEquals(inv, expected);
            } finally {
                webSocket.abort();
            }
        }
    }

    @Test
    public void onOpenThenOnPingThenAbort() throws Exception {
        int[] bytes = {
                0x89, 0x00, // opcode=ping
                0x88, 0x00, // opcode=close
        };
        try (var server = Support.serverWithCannedData(bytes)) {
            server.open();
            MockListener listener = new MockListener() {
                @Override
                protected void onOpen0(WebSocket webSocket) {
                    // unbounded request
                    webSocket.request(Long.MAX_VALUE);
                }

                @Override
                protected CompletionStage<?> onPing0(WebSocket webSocket,
                                                     ByteBuffer message) {
                    webSocket.abort();
                    return super.onPing0(webSocket, message);
                }
            };
            var webSocket = newHttpClient().newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            try {
                TimeUnit.SECONDS.sleep(5);
                List<MockListener.Invocation> inv = listener.invocationsSoFar();
                // no more invocations after onOpen, onPing as WebSocket was aborted
                List<MockListener.Invocation> expected = List.of(
                        MockListener.Invocation.onOpen(webSocket),
                        MockListener.Invocation.onPing(webSocket, ByteBuffer.allocate(0)));
                assertEquals(inv, expected);
            } finally {
                webSocket.abort();
            }
        }
    }

    @Test
    public void onOpenThenOnPongThenAbort() throws Exception {
        int[] bytes = {
                0x8a, 0x00, // opcode=pong
                0x88, 0x00, // opcode=close
        };
        try (var server = Support.serverWithCannedData(bytes)) {
            server.open();
            MockListener listener = new MockListener() {
                @Override
                protected void onOpen0(WebSocket webSocket) {
                    // unbounded request
                    webSocket.request(Long.MAX_VALUE);
                }

                @Override
                protected CompletionStage<?> onPong0(WebSocket webSocket,
                                                     ByteBuffer message) {
                    webSocket.abort();
                    return super.onPong0(webSocket, message);
                }
            };
            var webSocket = newHttpClient().newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            try {
                TimeUnit.SECONDS.sleep(5);
                List<MockListener.Invocation> inv = listener.invocationsSoFar();
                // no more invocations after onOpen, onPong as WebSocket was aborted
                List<MockListener.Invocation> expected = List.of(
                        MockListener.Invocation.onOpen(webSocket),
                        MockListener.Invocation.onPong(webSocket, ByteBuffer.allocate(0)));
                assertEquals(inv, expected);
            } finally {
                webSocket.abort();
            }
        }
    }

    @Test
    public void onOpenThenOnCloseThenAbort() throws Exception {
        int[] bytes = {
                0x88, 0x00, // opcode=close
                0x8a, 0x00, // opcode=pong
        };
        try (var server = Support.serverWithCannedData(bytes)) {
            server.open();
            MockListener listener = new MockListener() {
                @Override
                protected void onOpen0(WebSocket webSocket) {
                    // unbounded request
                    webSocket.request(Long.MAX_VALUE);
                }

                @Override
                protected CompletionStage<?> onClose0(WebSocket webSocket,
                                                      int statusCode,
                                                      String reason) {
                    webSocket.abort();
                    return super.onClose0(webSocket, statusCode, reason);
                }
            };
            var webSocket = newHttpClient().newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            try {
                TimeUnit.SECONDS.sleep(5);
                List<MockListener.Invocation> inv = listener.invocationsSoFar();
                // no more invocations after onOpen, onClose
                List<MockListener.Invocation> expected = List.of(
                        MockListener.Invocation.onOpen(webSocket),
                        MockListener.Invocation.onClose(webSocket, 1005, ""));
                assertEquals(inv, expected);
            } finally {
                webSocket.abort();
            }
        }
    }

    @Test
    public void onOpenThenOnErrorThenAbort() throws Exception {
        // A header of 128 bytes long Ping (which is a protocol error)
        int[] badPingHeader = new int[]{0x89, 0x7e, 0x00, 0x80};
        int[] closeMessage = new int[]{0x88, 0x00};
        int[] bytes = new int[badPingHeader.length + 128 + closeMessage.length];
        System.arraycopy(badPingHeader, 0, bytes, 0, badPingHeader.length);
        System.arraycopy(closeMessage, 0, bytes, badPingHeader.length + 128, closeMessage.length);
        try (var server = Support.serverWithCannedData(bytes)) {
            server.open();
            MockListener listener = new MockListener() {
                @Override
                protected void onOpen0(WebSocket webSocket) {
                    // unbounded request
                    webSocket.request(Long.MAX_VALUE);
                }

                @Override
                protected void onError0(WebSocket webSocket, Throwable error) {
                    webSocket.abort();
                    super.onError0(webSocket, error);
                }
            };
            var webSocket = newHttpClient().newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            try {
                TimeUnit.SECONDS.sleep(5);
                List<MockListener.Invocation> inv = listener.invocationsSoFar();
                // no more invocations after onOpen, onError
                List<MockListener.Invocation> expected = List.of(
                        MockListener.Invocation.onOpen(webSocket),
                        MockListener.Invocation.onError(webSocket, ProtocolException.class));
                System.out.println("actual invocations:" + Arrays.toString(inv.toArray()));
                assertEquals(inv, expected);
            } finally {
                webSocket.abort();
            }
        }
    }

    @Test
    public void immediateAbort() throws Exception {
        CompletableFuture<Void> messageReceived = new CompletableFuture<>();
        WebSocket.Listener listener = new WebSocket.Listener() {

            @Override
            public void onOpen(WebSocket webSocket) {
                /* no initial request */
            }

            @Override
            public CompletionStage<?> onText(WebSocket webSocket,
                                             CharSequence message,
                                             boolean last) {
                messageReceived.complete(null);
                return null;
            }

            @Override
            public CompletionStage<?> onBinary(WebSocket webSocket,
                                               ByteBuffer message,
                                               boolean last) {
                messageReceived.complete(null);
                return null;
            }

            @Override
            public CompletionStage<?> onPing(WebSocket webSocket,
                                             ByteBuffer message) {
                messageReceived.complete(null);
                return null;
            }

            @Override
            public CompletionStage<?> onPong(WebSocket webSocket,
                                             ByteBuffer message) {
                messageReceived.complete(null);
                return null;
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket,
                                              int statusCode,
                                              String reason) {
                messageReceived.complete(null);
                return null;
            }
        };

        int[] bytes = new int[]{
                0x82, 0x00, // opcode=binary, fin=true
                0x88, 0x00, // opcode=close
        };
        try (var server = Support.serverWithCannedData(bytes)) {
            server.open();

            WebSocket ws = newHttpClient()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            try {
                for (int i = 0; i < 3; i++) {
                    System.out.printf("iteration #%s%n", i);
                    // after the first abort() each consecutive one must be a no-op,
                    // moreover, query methods should continue to return consistent
                    // values
                    for (int j = 0; j < 3; j++) {
                        System.out.printf("abort #%s%n", j);
                        ws.abort();
                        assertTrue(ws.isInputClosed());
                        assertTrue(ws.isOutputClosed());
                        assertEquals(ws.getSubprotocol(), "");
                    }
                    // at this point valid requests MUST be a no-op:
                    for (int j = 0; j < 3; j++) {
                        System.out.printf("request #%s%n", j);
                        ws.request(1);
                        ws.request(2);
                        ws.request(8);
                        ws.request(Integer.MAX_VALUE);
                        ws.request(Long.MAX_VALUE);
                        // invalid requests MUST throw IAE:
                        assertThrows(IAE, () -> ws.request(Integer.MIN_VALUE));
                        assertThrows(IAE, () -> ws.request(Long.MIN_VALUE));
                        assertThrows(IAE, () -> ws.request(-1));
                        assertThrows(IAE, () -> ws.request(0));
                    }
                }
                // even though there is a bunch of messages readily available on the
                // wire we shouldn't have received any of them as we aborted before
                // the first request
                try {
                    messageReceived.get(5, TimeUnit.SECONDS);
                    fail();
                } catch (TimeoutException expected) {
                    System.out.println("Finished waiting");
                }
                for (int i = 0; i < 3; i++) {
                    System.out.printf("send #%s%n", i);
                    Support.assertFails(IOE, ws.sendText("text!", false));
                    Support.assertFails(IOE, ws.sendText("text!", true));
                    Support.assertFails(IOE, ws.sendBinary(ByteBuffer.allocate(16), false));
                    Support.assertFails(IOE, ws.sendBinary(ByteBuffer.allocate(16), true));
                    Support.assertFails(IOE, ws.sendPing(ByteBuffer.allocate(16)));
                    Support.assertFails(IOE, ws.sendPong(ByteBuffer.allocate(16)));
                    Support.assertFails(IOE, ws.sendClose(NORMAL_CLOSURE, "a reason"));
                    assertThrows(NPE, () -> ws.sendText(null, false));
                    assertThrows(NPE, () -> ws.sendText(null, true));
                    assertThrows(NPE, () -> ws.sendBinary(null, false));
                    assertThrows(NPE, () -> ws.sendBinary(null, true));
                    assertThrows(NPE, () -> ws.sendPing(null));
                    assertThrows(NPE, () -> ws.sendPong(null));
                    assertThrows(NPE, () -> ws.sendClose(NORMAL_CLOSURE, null));
                }
            } finally {
                ws.abort();
            }
        }
    }
}
