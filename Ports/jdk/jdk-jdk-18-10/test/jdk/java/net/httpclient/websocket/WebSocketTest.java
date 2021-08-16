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

/*
 * @test
 * @bug 8217429
 * @build DummyWebSocketServer
 * @run testng/othervm
 *       WebSocketTest
 */

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.net.http.WebSocketHandshakeException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static java.net.http.HttpClient.Builder.NO_PROXY;
import static java.net.http.HttpClient.newBuilder;
import static java.net.http.WebSocket.NORMAL_CLOSURE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.fail;

public class WebSocketTest {

    private static final Class<IllegalArgumentException> IAE = IllegalArgumentException.class;
    private static final Class<IllegalStateException> ISE = IllegalStateException.class;
    private static final Class<IOException> IOE = IOException.class;

    /* shortcut */
    private static void assertFails(Class<? extends Throwable> clazz,
                                    CompletionStage<?> stage) {
        Support.assertCompletesExceptionally(clazz, stage);
    }

    @Test
    public void illegalArgument() throws IOException {
        try (var server = new DummyWebSocketServer()) {
            server.open();
            var webSocket = newBuilder().proxy(NO_PROXY).build()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();
            try {
                assertFails(IAE, webSocket.sendPing(ByteBuffer.allocate(126)));
                assertFails(IAE, webSocket.sendPing(ByteBuffer.allocate(127)));
                assertFails(IAE, webSocket.sendPing(ByteBuffer.allocate(128)));
                assertFails(IAE, webSocket.sendPing(ByteBuffer.allocate(129)));
                assertFails(IAE, webSocket.sendPing(ByteBuffer.allocate(256)));

                assertFails(IAE, webSocket.sendPong(ByteBuffer.allocate(126)));
                assertFails(IAE, webSocket.sendPong(ByteBuffer.allocate(127)));
                assertFails(IAE, webSocket.sendPong(ByteBuffer.allocate(128)));
                assertFails(IAE, webSocket.sendPong(ByteBuffer.allocate(129)));
                assertFails(IAE, webSocket.sendPong(ByteBuffer.allocate(256)));

                assertFails(IOE, webSocket.sendText(Support.incompleteString(), true));
                assertFails(IOE, webSocket.sendText(Support.incompleteString(), false));
                assertFails(IOE, webSocket.sendText(Support.malformedString(), true));
                assertFails(IOE, webSocket.sendText(Support.malformedString(), false));

                assertFails(IAE, webSocket.sendClose(NORMAL_CLOSURE, Support.stringWithNBytes(124)));
                assertFails(IAE, webSocket.sendClose(NORMAL_CLOSURE, Support.stringWithNBytes(125)));
                assertFails(IAE, webSocket.sendClose(NORMAL_CLOSURE, Support.stringWithNBytes(128)));
                assertFails(IAE, webSocket.sendClose(NORMAL_CLOSURE, Support.stringWithNBytes(256)));
                assertFails(IAE, webSocket.sendClose(NORMAL_CLOSURE, Support.stringWithNBytes(257)));
                assertFails(IAE, webSocket.sendClose(NORMAL_CLOSURE, Support.stringWith2NBytes((123 / 2) + 1)));
                assertFails(IAE, webSocket.sendClose(NORMAL_CLOSURE, Support.malformedString()));
                assertFails(IAE, webSocket.sendClose(NORMAL_CLOSURE, Support.incompleteString()));

                assertFails(IAE, webSocket.sendClose(-2, "a reason"));
                assertFails(IAE, webSocket.sendClose(-1, "a reason"));
                assertFails(IAE, webSocket.sendClose(0, "a reason"));
                assertFails(IAE, webSocket.sendClose(1, "a reason"));
                assertFails(IAE, webSocket.sendClose(500, "a reason"));
                assertFails(IAE, webSocket.sendClose(998, "a reason"));
                assertFails(IAE, webSocket.sendClose(999, "a reason"));
                assertFails(IAE, webSocket.sendClose(1002, "a reason"));
                assertFails(IAE, webSocket.sendClose(1003, "a reason"));
                assertFails(IAE, webSocket.sendClose(1006, "a reason"));
                assertFails(IAE, webSocket.sendClose(1007, "a reason"));
                assertFails(IAE, webSocket.sendClose(1009, "a reason"));
                assertFails(IAE, webSocket.sendClose(1010, "a reason"));
                assertFails(IAE, webSocket.sendClose(1012, "a reason"));
                assertFails(IAE, webSocket.sendClose(1013, "a reason"));
                assertFails(IAE, webSocket.sendClose(1015, "a reason"));
                assertFails(IAE, webSocket.sendClose(5000, "a reason"));
                assertFails(IAE, webSocket.sendClose(32768, "a reason"));
                assertFails(IAE, webSocket.sendClose(65535, "a reason"));
                assertFails(IAE, webSocket.sendClose(65536, "a reason"));
                assertFails(IAE, webSocket.sendClose(Integer.MAX_VALUE, "a reason"));
                assertFails(IAE, webSocket.sendClose(Integer.MIN_VALUE, "a reason"));

                assertThrows(IAE, () -> webSocket.request(Integer.MIN_VALUE));
                assertThrows(IAE, () -> webSocket.request(Long.MIN_VALUE));
                assertThrows(IAE, () -> webSocket.request(-1));
                assertThrows(IAE, () -> webSocket.request(0));

            } finally {
                webSocket.abort();
            }
        }
    }

    @Test
    public void partialBinaryThenText() throws IOException {
        try (var server = new DummyWebSocketServer()) {
            server.open();
            var webSocket = newBuilder().proxy(NO_PROXY).build().newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();
            try {
                webSocket.sendBinary(ByteBuffer.allocate(16), false).join();
                assertFails(ISE, webSocket.sendText("text", false));
                assertFails(ISE, webSocket.sendText("text", true));
                // Pings & Pongs are fine
                webSocket.sendPing(ByteBuffer.allocate(125)).join();
                webSocket.sendPong(ByteBuffer.allocate(125)).join();
            } finally {
                webSocket.abort();
            }
        }
    }

    @Test
    public void partialTextThenBinary() throws IOException {
        try (var server = new DummyWebSocketServer()) {
            server.open();
            var webSocket = newBuilder().proxy(NO_PROXY).build().newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();
            try {
                webSocket.sendText("text", false).join();
                assertFails(ISE, webSocket.sendBinary(ByteBuffer.allocate(16), false));
                assertFails(ISE, webSocket.sendBinary(ByteBuffer.allocate(16), true));
                // Pings & Pongs are fine
                webSocket.sendPing(ByteBuffer.allocate(125)).join();
                webSocket.sendPong(ByteBuffer.allocate(125)).join();
            } finally {
                webSocket.abort();
            }
        }
    }

    @Test
    public void sendMethodsThrowIOE1() throws IOException {
        try (var server = new DummyWebSocketServer()) {
            server.open();
            var webSocket = newBuilder().proxy(NO_PROXY).build()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();
            try {
                webSocket.sendClose(NORMAL_CLOSURE, "ok").join();

                assertFails(IOE, webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "ok"));

                assertFails(IOE, webSocket.sendText("", true));
                assertFails(IOE, webSocket.sendText("", false));
                assertFails(IOE, webSocket.sendText("abc", true));
                assertFails(IOE, webSocket.sendText("abc", false));
                assertFails(IOE, webSocket.sendBinary(ByteBuffer.allocate(0), true));
                assertFails(IOE, webSocket.sendBinary(ByteBuffer.allocate(0), false));
                assertFails(IOE, webSocket.sendBinary(ByteBuffer.allocate(1), true));
                assertFails(IOE, webSocket.sendBinary(ByteBuffer.allocate(1), false));

                assertFails(IOE, webSocket.sendPing(ByteBuffer.allocate(125)));
                assertFails(IOE, webSocket.sendPing(ByteBuffer.allocate(124)));
                assertFails(IOE, webSocket.sendPing(ByteBuffer.allocate(1)));
                assertFails(IOE, webSocket.sendPing(ByteBuffer.allocate(0)));

                assertFails(IOE, webSocket.sendPong(ByteBuffer.allocate(125)));
                assertFails(IOE, webSocket.sendPong(ByteBuffer.allocate(124)));
                assertFails(IOE, webSocket.sendPong(ByteBuffer.allocate(1)));
                assertFails(IOE, webSocket.sendPong(ByteBuffer.allocate(0)));
            } finally {
                webSocket.abort();
            }
        }
    }

    @DataProvider(name = "sequence")
    public Object[][] data1() {
        int[] CLOSE = {
                0x81, 0x00, // ""
                0x82, 0x00, // []
                0x89, 0x00, // <PING>
                0x8a, 0x00, // <PONG>
                0x88, 0x00, // <CLOSE>
        };
        int[] ERROR = {
                0x81, 0x00, // ""
                0x82, 0x00, // []
                0x89, 0x00, // <PING>
                0x8a, 0x00, // <PONG>
                0x8b, 0x00, // 0xB control frame (causes an error)
        };
        return new Object[][]{
                {CLOSE, 1},
                {CLOSE, 3},
                {CLOSE, 4},
                {CLOSE, Long.MAX_VALUE},
                {ERROR, 1},
                {ERROR, 3},
                {ERROR, 4},
                {ERROR, Long.MAX_VALUE},
        };
    }

    @Test(dataProvider = "sequence")
    public void listenerSequentialOrder(int[] binary, long requestSize)
            throws IOException
    {
        try (var server = Support.serverWithCannedData(binary)) {
            server.open();

            CompletableFuture<Void> violation = new CompletableFuture<>();

            MockListener listener = new MockListener(requestSize) {

                final AtomicBoolean guard = new AtomicBoolean();

                private <T> T checkRunExclusively(Supplier<T> action) {
                    if (guard.getAndSet(true)) {
                        violation.completeExceptionally(new RuntimeException());
                    }
                    try {
                        return action.get();
                    } finally {
                        if (!guard.getAndSet(false)) {
                            violation.completeExceptionally(new RuntimeException());
                        }
                    }
                }

                @Override
                public void onOpen(WebSocket webSocket) {
                    checkRunExclusively(() -> {
                        super.onOpen(webSocket);
                        return null;
                    });
                }

                @Override
                public CompletionStage<?> onText(WebSocket webSocket,
                                                 CharSequence data,
                                                 boolean last) {
                    return checkRunExclusively(
                            () -> super.onText(webSocket, data, last));
                }

                @Override
                public CompletionStage<?> onBinary(WebSocket webSocket,
                                                   ByteBuffer data,
                                                   boolean last) {
                    return checkRunExclusively(
                            () -> super.onBinary(webSocket, data, last));
                }

                @Override
                public CompletionStage<?> onPing(WebSocket webSocket,
                                                 ByteBuffer message) {
                    return checkRunExclusively(
                            () -> super.onPing(webSocket, message));
                }

                @Override
                public CompletionStage<?> onPong(WebSocket webSocket,
                                                 ByteBuffer message) {
                    return checkRunExclusively(
                            () -> super.onPong(webSocket, message));
                }

                @Override
                public CompletionStage<?> onClose(WebSocket webSocket,
                                                  int statusCode,
                                                  String reason) {
                    return checkRunExclusively(
                            () -> super.onClose(webSocket, statusCode, reason));
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    checkRunExclusively(() -> {
                        super.onError(webSocket, error);
                        return null;
                    });
                }
            };

            var webSocket = newBuilder().proxy(NO_PROXY).build().newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            try {
                listener.invocations();
                violation.complete(null); // won't affect if completed exceptionally
                violation.join();
            } finally {
                webSocket.abort();
            }
        }
    }

    @Test
    public void sendMethodsThrowIOE2() throws Exception {
        try (var server = Support.serverWithCannedData(0x88, 0x00)) {
            server.open();

            CompletableFuture<Void> onCloseCalled = new CompletableFuture<>();
            CompletableFuture<Void> canClose = new CompletableFuture<>();

            WebSocket.Listener listener = new WebSocket.Listener() {
                @Override
                public CompletionStage<?> onClose(WebSocket webSocket,
                                                  int statusCode,
                                                  String reason) {
                    System.out.printf("onClose(%s, '%s')%n", statusCode, reason);
                    onCloseCalled.complete(null);
                    return canClose;
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    System.out.println("onError(" + error + ")");
                    onCloseCalled.completeExceptionally(error);
                }
            };

            var webSocket = newBuilder().proxy(NO_PROXY).build().newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            try {
                onCloseCalled.join();      // Wait for onClose to be called
                canClose.complete(null);   // Signal to the WebSocket it can close the output
                TimeUnit.SECONDS.sleep(5); // Give canClose some time to reach the WebSocket

                assertFails(IOE, webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "ok"));

                assertFails(IOE, webSocket.sendText("", true));
                assertFails(IOE, webSocket.sendText("", false));
                assertFails(IOE, webSocket.sendText("abc", true));
                assertFails(IOE, webSocket.sendText("abc", false));
                assertFails(IOE, webSocket.sendBinary(ByteBuffer.allocate(0), true));
                assertFails(IOE, webSocket.sendBinary(ByteBuffer.allocate(0), false));
                assertFails(IOE, webSocket.sendBinary(ByteBuffer.allocate(1), true));
                assertFails(IOE, webSocket.sendBinary(ByteBuffer.allocate(1), false));

                assertFails(IOE, webSocket.sendPing(ByteBuffer.allocate(125)));
                assertFails(IOE, webSocket.sendPing(ByteBuffer.allocate(124)));
                assertFails(IOE, webSocket.sendPing(ByteBuffer.allocate(1)));
                assertFails(IOE, webSocket.sendPing(ByteBuffer.allocate(0)));

                assertFails(IOE, webSocket.sendPong(ByteBuffer.allocate(125)));
                assertFails(IOE, webSocket.sendPong(ByteBuffer.allocate(124)));
                assertFails(IOE, webSocket.sendPong(ByteBuffer.allocate(1)));
                assertFails(IOE, webSocket.sendPong(ByteBuffer.allocate(0)));
            } finally {
                webSocket.abort();
            }
        }
    }

    // Used to verify a server requiring Authentication
    private static final String USERNAME = "chegar";
    private static final String PASSWORD = "a1b2c3";

    static class WSAuthenticator extends Authenticator {
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(USERNAME, PASSWORD.toCharArray());
        }
    }

    static final Function<int[],DummyWebSocketServer> SERVER_WITH_CANNED_DATA =
        new Function<>() {
            @Override public DummyWebSocketServer apply(int[] data) {
                return Support.serverWithCannedData(data); }
            @Override public String toString() { return "SERVER_WITH_CANNED_DATA"; }
        };

    static final Function<int[],DummyWebSocketServer> AUTH_SERVER_WITH_CANNED_DATA =
        new Function<>() {
            @Override public DummyWebSocketServer apply(int[] data) {
                return Support.serverWithCannedDataAndAuthentication(USERNAME, PASSWORD, data); }
            @Override public String toString() { return "AUTH_SERVER_WITH_CANNED_DATA"; }
        };

    @DataProvider(name = "servers")
    public Object[][] servers() {
        return new Object[][] {
            { SERVER_WITH_CANNED_DATA },
            { AUTH_SERVER_WITH_CANNED_DATA },
        };
    }

    record bytes(byte[] bytes) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o instanceof bytes other) {
                return Arrays.equals(bytes(), other.bytes());
            }
            return false;
        }
        @Override
        public int hashCode() { return Arrays.hashCode(bytes()); }
        public String toString() {
            return "0x" + HexFormat.of()
                    .withUpperCase()
                    .formatHex(bytes());
        }
    }

    static List<bytes> ofBytes(List<byte[]> bytes) {
        return bytes.stream().map(bytes::new).toList();
    }

    static String diagnose(List<byte[]> a, List<byte[]> b) {
        var actual = ofBytes(a);
        var expected = ofBytes(b);
        var message = actual.equals(expected) ? "match" : "differ";
        return "%s and %s %s".formatted(actual, expected, message);
    }

    @Test(dataProvider = "servers")
    public void simpleAggregatingBinaryMessages
            (Function<int[],DummyWebSocketServer> serverSupplier)
        throws IOException
    {
        List<byte[]> expected = List.of("alpha", "beta", "gamma", "delta")
                .stream()
                .map(s -> s.getBytes(StandardCharsets.US_ASCII))
                .collect(Collectors.toList());
        int[] binary = new int[]{
                0x82, 0x05, 0x61, 0x6c, 0x70, 0x68, 0x61, // [alpha]
                0x02, 0x02, 0x62, 0x65,                   // [be
                0x80, 0x02, 0x74, 0x61,                   // ta]
                0x02, 0x01, 0x67,                         // [g
                0x00, 0x01, 0x61,                         // a
                0x00, 0x00,                               //
                0x00, 0x00,                               //
                0x00, 0x01, 0x6d,                         // m
                0x00, 0x01, 0x6d,                         // m
                0x80, 0x01, 0x61,                         // a]
                0x8a, 0x00,                               // <PONG>
                0x02, 0x04, 0x64, 0x65, 0x6c, 0x74,       // [delt
                0x00, 0x01, 0x61,                         // a
                0x80, 0x00,                               // ]
                0x88, 0x00                                // <CLOSE>
        };
        CompletableFuture<List<byte[]>> actual = new CompletableFuture<>();

        try (var server = serverSupplier.apply(binary)) {
            server.open();

            WebSocket.Listener listener = new WebSocket.Listener() {

                List<byte[]> collectedBytes = new ArrayList<>();
                ByteBuffer buffer = ByteBuffer.allocate(1024);

                @Override
                public CompletionStage<?> onBinary(WebSocket webSocket,
                                                   ByteBuffer message,
                                                   boolean last) {
                    System.out.printf("onBinary(%s, %s)%n", message, last);
                    webSocket.request(1);

                    append(message);
                    if (last) {
                        buffer.flip();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        buffer.clear();
                        processWholeBinary(bytes);
                    }
                    return null;
                }

                private void append(ByteBuffer message) {
                    if (buffer.remaining() < message.remaining()) {
                        assert message.remaining() > 0;
                        int cap = (buffer.capacity() + message.remaining()) * 2;
                        ByteBuffer b = ByteBuffer.allocate(cap);
                        b.put(buffer.flip());
                        buffer = b;
                    }
                    buffer.put(message);
                }

                private void processWholeBinary(byte[] bytes) {
                    String stringBytes = new String(bytes, UTF_8);
                    System.out.println("processWholeBinary: " + stringBytes);
                    collectedBytes.add(bytes);
                }

                @Override
                public CompletionStage<?> onClose(WebSocket webSocket,
                                                  int statusCode,
                                                  String reason) {
                    actual.complete(collectedBytes);
                    return null;
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    actual.completeExceptionally(error);
                }
            };

            var webSocket = newBuilder()
                    .proxy(NO_PROXY)
                    .authenticator(new WSAuthenticator())
                    .build().newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            try {
                List<byte[]> a = actual.join();
                assertEquals(ofBytes(a), ofBytes(expected), diagnose(a, expected));
            } finally {
                webSocket.abort();
            }
        }
    }

    @Test(dataProvider = "servers")
    public void simpleAggregatingTextMessages
            (Function<int[],DummyWebSocketServer> serverSupplier)
        throws IOException
    {
        List<String> expected = List.of("alpha", "beta", "gamma", "delta");

        int[] binary = new int[]{
                0x81, 0x05, 0x61, 0x6c, 0x70, 0x68, 0x61, // "alpha"
                0x01, 0x02, 0x62, 0x65,                   // "be
                0x80, 0x02, 0x74, 0x61,                   // ta"
                0x01, 0x01, 0x67,                         // "g
                0x00, 0x01, 0x61,                         // a
                0x00, 0x00,                               //
                0x00, 0x00,                               //
                0x00, 0x01, 0x6d,                         // m
                0x00, 0x01, 0x6d,                         // m
                0x80, 0x01, 0x61,                         // a"
                0x8a, 0x00,                               // <PONG>
                0x01, 0x04, 0x64, 0x65, 0x6c, 0x74,       // "delt
                0x00, 0x01, 0x61,                         // a
                0x80, 0x00,                               // "
                0x88, 0x00                                // <CLOSE>
        };
        CompletableFuture<List<String>> actual = new CompletableFuture<>();

        try (var server = serverSupplier.apply(binary)) {
            server.open();

            WebSocket.Listener listener = new WebSocket.Listener() {

                List<String> collectedStrings = new ArrayList<>();
                StringBuilder text = new StringBuilder();

                @Override
                public CompletionStage<?> onText(WebSocket webSocket,
                                                 CharSequence message,
                                                 boolean last) {
                    System.out.printf("onText(%s, %s)%n", message, last);
                    webSocket.request(1);
                    text.append(message);
                    if (last) {
                        String str = text.toString();
                        text.setLength(0);
                        processWholeText(str);
                    }
                    return null;
                }

                private void processWholeText(String string) {
                    System.out.println(string);
                    collectedStrings.add(string);
                }

                @Override
                public CompletionStage<?> onClose(WebSocket webSocket,
                                                  int statusCode,
                                                  String reason) {
                    actual.complete(collectedStrings);
                    return null;
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    actual.completeExceptionally(error);
                }
            };

            var webSocket = newBuilder()
                    .proxy(NO_PROXY)
                    .authenticator(new WSAuthenticator())
                    .build().newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            try {
                List<String> a = actual.join();
                assertEquals(a, expected);
            } finally {
                webSocket.abort();
            }
        }
    }

    /*
     * Exercises the scenario where requests for more messages are made prior to
     * completing the returned CompletionStage instances.
     */
    @Test(dataProvider = "servers")
    public void aggregatingTextMessages
        (Function<int[],DummyWebSocketServer> serverSupplier)
        throws IOException
    {
        List<String> expected = List.of("alpha", "beta", "gamma", "delta");

        int[] binary = new int[]{
                0x81, 0x05, 0x61, 0x6c, 0x70, 0x68, 0x61, // "alpha"
                0x01, 0x02, 0x62, 0x65,                   // "be
                0x80, 0x02, 0x74, 0x61,                   // ta"
                0x01, 0x01, 0x67,                         // "g
                0x00, 0x01, 0x61,                         // a
                0x00, 0x00,                               //
                0x00, 0x00,                               //
                0x00, 0x01, 0x6d,                         // m
                0x00, 0x01, 0x6d,                         // m
                0x80, 0x01, 0x61,                         // a"
                0x8a, 0x00,                               // <PONG>
                0x01, 0x04, 0x64, 0x65, 0x6c, 0x74,       // "delt
                0x00, 0x01, 0x61,                         // a
                0x80, 0x00,                               // "
                0x88, 0x00                                // <CLOSE>
        };
        CompletableFuture<List<String>> actual = new CompletableFuture<>();

        try (var server = serverSupplier.apply(binary)) {
            server.open();

            WebSocket.Listener listener = new WebSocket.Listener() {

                List<CharSequence> parts = new ArrayList<>();
                /*
                 * A CompletableFuture which will complete once the current
                 * message has been fully assembled. Until then the listener
                 * returns this instance for every call.
                 */
                CompletableFuture<?> currentCf = new CompletableFuture<>();
                List<String> collected = new ArrayList<>();

                @Override
                public CompletionStage<?> onText(WebSocket webSocket,
                                                 CharSequence message,
                                                 boolean last) {
                    parts.add(message);
                    if (!last) {
                        webSocket.request(1);
                    } else {
                        this.currentCf.thenRun(() -> webSocket.request(1));
                        CompletableFuture<?> refCf = this.currentCf;
                        processWholeMessage(new ArrayList<>(parts), refCf);
                        currentCf = new CompletableFuture<>();
                        parts.clear();
                        return refCf;
                    }
                    return currentCf;
                }

                @Override
                public CompletionStage<?> onClose(WebSocket webSocket,
                                                  int statusCode,
                                                  String reason) {
                    actual.complete(collected);
                    return null;
                }

                @Override
                public void onError(WebSocket webSocket, Throwable error) {
                    actual.completeExceptionally(error);
                }

                public void processWholeMessage(List<CharSequence> data,
                                                CompletableFuture<?> cf) {
                    StringBuilder b = new StringBuilder();
                    data.forEach(b::append);
                    String s = b.toString();
                    System.out.println(s);
                    cf.complete(null);
                    collected.add(s);
                }
            };

            var webSocket = newBuilder()
                    .proxy(NO_PROXY)
                    .authenticator(new WSAuthenticator())
                    .build().newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            try {
                List<String> a = actual.join();
                assertEquals(a, expected);
            } finally {
                webSocket.abort();
            }
        }
    }

    // -- authentication specific tests

    /*
     * Ensures authentication succeeds when an Authenticator set on client builder.
     */
    @Test
    public void clientAuthenticate() throws IOException  {
        try (var server = new DummyWebSocketServer(USERNAME, PASSWORD)){
            server.open();

            var webSocket = newBuilder()
                    .proxy(NO_PROXY)
                    .authenticator(new WSAuthenticator())
                    .build()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();
            webSocket.abort();
        }
    }

    /*
     * Ensures authentication succeeds when an `Authorization` header is explicitly set.
     */
    @Test
    public void explicitAuthenticate() throws IOException  {
        try (var server = new DummyWebSocketServer(USERNAME, PASSWORD)) {
            server.open();

            String hv = "Basic " + Base64.getEncoder().encodeToString(
                    (USERNAME + ":" + PASSWORD).getBytes(UTF_8));

            var webSocket = newBuilder()
                    .proxy(NO_PROXY).build()
                    .newWebSocketBuilder()
                    .header("Authorization", hv)
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();
            webSocket.abort();
        }
    }

    /*
     * Ensures authentication does not succeed when no authenticator is present.
     */
    @Test
    public void failNoAuthenticator() throws IOException  {
        try (var server = new DummyWebSocketServer(USERNAME, PASSWORD)) {
            server.open();

            CompletableFuture<WebSocket> cf = newBuilder()
                    .proxy(NO_PROXY).build()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { });

            try {
                var webSocket = cf.join();
                silentAbort(webSocket);
                fail("Expected exception not thrown");
            } catch (CompletionException expected) {
                WebSocketHandshakeException e = (WebSocketHandshakeException)expected.getCause();
                HttpResponse<?> response = e.getResponse();
                assertEquals(response.statusCode(), 401);
            }
        }
    }

    /*
     * Ensures authentication does not succeed when the authenticator presents
     * unauthorized credentials.
     */
    @Test
    public void failBadCredentials() throws IOException  {
        try (var server = new DummyWebSocketServer(USERNAME, PASSWORD)) {
            server.open();

            Authenticator authenticator = new Authenticator() {
                @Override protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication("BAD" + USERNAME, "".toCharArray());
                }
            };

            CompletableFuture<WebSocket> cf = newBuilder()
                    .proxy(NO_PROXY)
                    .authenticator(authenticator)
                    .build()
                    .newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { });

            try {
                var webSocket = cf.join();
                silentAbort(webSocket);
                fail("Expected exception not thrown");
            } catch (CompletionException expected) {
                System.out.println("caught expected exception:" + expected);
            }
        }
    }
    private static void silentAbort(WebSocket ws) {
        try {
            ws.abort();
        } catch (Throwable t) { }
    }
}
