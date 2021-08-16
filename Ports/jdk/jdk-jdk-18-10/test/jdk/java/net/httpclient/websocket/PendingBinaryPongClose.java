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
 *      -Djdk.httpclient.sendBufferSize=8192
 *      -Djdk.internal.httpclient.debug=true
 *      -Djdk.internal.httpclient.websocket.debug=true
 *       PendingBinaryPongClose
 */

import org.testng.annotations.Test;

import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import static java.net.http.HttpClient.Builder.NO_PROXY;
import static java.net.http.HttpClient.newBuilder;

public class PendingBinaryPongClose extends PendingOperations {

    CompletableFuture<WebSocket> cfBinary;
    CompletableFuture<WebSocket> cfPong;
    CompletableFuture<WebSocket> cfClose;

    @Test(dataProvider = "booleans")
    public void pendingBinaryPongClose(boolean last) throws Exception {
        repeatable(() -> {
            server = Support.notReadingServer();
            server.setReceiveBufferSize(1024);
            server.open();
            webSocket = httpClient().newWebSocketBuilder()
                    .buildAsync(server.getURI(), new WebSocket.Listener() { })
                    .join();
            ByteBuffer data = ByteBuffer.allocate(65536);
            for (int i = 0; ; i++) {  // fill up the send buffer
                long start = System.currentTimeMillis();
                System.out.printf("begin cycle #%s at %s%n", i, start);
                cfBinary = webSocket.sendBinary(data, last);
                try {
                    cfBinary.get(MAX_WAIT_SEC, TimeUnit.SECONDS);
                    data.clear();
                } catch (TimeoutException e) {
                    break;
                } finally {
                    long stop = System.currentTimeMillis();
                    System.out.printf("end cycle #%s at %s (%s ms)%n", i, stop, stop - start);
                }
            }
            assertFails(ISE, webSocket.sendText("", true));
            assertFails(ISE, webSocket.sendText("", false));
            assertFails(ISE, webSocket.sendBinary(ByteBuffer.allocate(0), true));
            assertFails(ISE, webSocket.sendBinary(ByteBuffer.allocate(0), false));
            cfPong = webSocket.sendPong(ByteBuffer.allocate(125));
            assertHangs(cfPong);
            assertFails(ISE, webSocket.sendPing(ByteBuffer.allocate(125)));
            assertFails(ISE, webSocket.sendPong(ByteBuffer.allocate(125)));
            cfClose = webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "ok");
            assertHangs(cfClose);
            assertNotDone(cfBinary);
            return null;
        }, () -> cfBinary.isDone());
        webSocket.abort();
        assertFails(IOE, cfBinary);
        assertFails(IOE, cfPong);
        assertFails(IOE, cfClose);
    }
}
