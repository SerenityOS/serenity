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
 * @build DummyWebSocketServer
 * @run testng/othervm
 *      -Djdk.internal.httpclient.debug=true
 *      -Djdk.internal.httpclient.websocket.debug=true
 *       BlowupOutputQueue
 */

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.testng.Assert.assertFalse;

public class BlowupOutputQueue extends PendingOperations {

    CompletableFuture<WebSocket> cfText;
    CompletableFuture<WebSocket> cfPing;
    CompletableFuture<WebSocket> cfClose;
    MockListener listener;

    /*
     * The idea is to arrange things such that the internal queue will be fully
     * utilized and then make sure there won't be any errors caused by that.
     *
     * First, fill the queue with Text messages. Once done, send a Ping message.
     * At this stage, there are at least 2 messages are in queue. Now, start
     * receiving. Received Ping messages will cause automatic Pong replies. When
     * all done, there will be at least 3 messages in the queue. (As at least
     * the a single Ping has to be replied). Then send a Close message. Now
     * there are at least 4 messages in the queue. Finally, receive the last
     * message which is a Close message. This will cause an automatic reply with
     * a Close message from the client side. All in all there should be at least
     * 5 messages in the queue.
     */
    @Test
    public void full() throws Exception {
        int N = 32;
        int[] incoming = new int[2 * (N + 1)]; // 2 bytes per message
        for (int i = 0; i < incoming.length - 2; i += 2) {
            // <PING>
            incoming[i + 0] = 0x89;
            incoming[i + 1] = 0x00;
        }
        // <CLOSE>
        incoming[incoming.length - 2] = 0x88;
        incoming[incoming.length - 1] = 0x00;

        repeatable(() -> {
            CountDownLatch allButCloseReceived = new CountDownLatch(N);
            server = Support.writingServer(incoming);
            server.open();
            listener = new MockListener() {

                @Override
                protected void onOpen0(WebSocket webSocket) {
                    /* do nothing */
                }

                @Override
                protected void replenish(WebSocket webSocket) {
                    /* do nothing */
                }

                @Override
                protected CompletionStage<?> onPing0(WebSocket webSocket,
                                                     ByteBuffer message) {
                    allButCloseReceived.countDown();
                    return null;
                }
            };
            webSocket = httpClient().newWebSocketBuilder()
                    .buildAsync(server.getURI(), listener)
                    .join();
            CharBuffer data = CharBuffer.allocate(65536);
            for (int i = 0; ; i++) {  // fill up the send buffer
                long start = System.currentTimeMillis();
                System.out.printf("begin cycle #%s at %s%n", i, start);
                cfText = webSocket.sendText(data, true);
                try {
                    cfText.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
                    data.clear();
                } catch (TimeoutException e) {
                    break;
                } finally {
                    long stop = System.currentTimeMillis();
                    System.out.printf("end cycle #%s at %s (%s ms)%n", i, stop, stop - start);
                }
            }
            cfPing = webSocket.sendPing(ByteBuffer.allocate(125));
            webSocket.request(N);
            allButCloseReceived.await();
            webSocket.request(1); // Receive the last message: Close
            return null;
        }, () -> cfText.isDone());
        List<MockListener.Invocation> invocations = listener.invocations();
        cfClose = webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "ok");

        assertFalse(invocations.contains(new MockListener.OnError(webSocket, IOException.class)));
        assertFalse(cfText.isDone());
        assertFalse(cfPing.isDone());
        assertFalse(cfClose.isDone());
    }
}
