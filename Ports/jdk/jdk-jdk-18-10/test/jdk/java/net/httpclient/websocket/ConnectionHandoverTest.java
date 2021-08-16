/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8164625
 * @summary Verifies HttpClient yields the connection to the WebSocket
 * @build DummyWebSocketServer
 * @run main/othervm -Djdk.httpclient.HttpClient.log=trace ConnectionHandoverTest
 */

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;

public class ConnectionHandoverTest {
    /*
     * An I/O channel associated with the connection is closed by WebSocket.abort().
     * If this connection is returned to the connection pool, then the second
     * attempt to use it would fail with a ClosedChannelException.
     *
     * The assumption is that since the WebSocket client is connecting to the
     * same URI, the pooled connection is to be used.
     */
    public static void main(String[] args) throws IOException {
        try (DummyWebSocketServer server = new DummyWebSocketServer()) {
            server.open();
            URI uri = server.getURI();
            WebSocket.Builder webSocketBuilder =
                    HttpClient.newHttpClient().newWebSocketBuilder();

            WebSocket ws1 = webSocketBuilder
                    .buildAsync(uri, new WebSocket.Listener() { }).join();
            ws1.abort();

            WebSocket ws2 = webSocketBuilder
                    .buildAsync(uri, new WebSocket.Listener() { }).join(); // Exception here if the connection was pooled
            ws2.abort();
        }
    }
}
