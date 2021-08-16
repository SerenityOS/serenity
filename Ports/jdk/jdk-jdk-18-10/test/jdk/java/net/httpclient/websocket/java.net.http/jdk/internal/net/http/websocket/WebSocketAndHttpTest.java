/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http.websocket;

import java.net.*;
import java.nio.CharBuffer;
import java.io.*;

import com.sun.net.httpserver.*;

public class WebSocketAndHttpTest {
    static class WHandler extends DefaultMessageStreamHandler {
        volatile MessageStreamResponder responder;

        public void onText(CharSequence data, boolean last) {
            System.out.println("onText: " + data);
            System.out.println("onText: " + Thread.currentThread());
            try {
                responder.sendText(CharBuffer.wrap(data), true);
                System.out.println("onText: send ok");
            } catch (IOException e) {
                System.out.println("onText: " + e);
                throw new UncheckedIOException(e);
            }
        }

        public void onInit(MessageStreamResponder responder) {
            System.out.println("onInit");
            this.responder = responder;
        }
    }

    static HttpHandler httpHandler = (ex) -> ex.sendResponseHeaders(200, -1);

    public static void main(String[] args) throws Exception {
        HttpServer hserver = null;
        try {
            WebSocketServer server = new WebSocketServer(new WHandler());
            server.open();
            URI uri = server.getURI();

            hserver = HttpServer.create(new InetSocketAddress(0), 4);
            hserver.createContext("/", httpHandler);
            hserver.start();

            int port = hserver.getAddress().getPort();
            URI huri = new URI("http://127.0.0.1:" + port + "/foo");

            WebSocketAndHttpClient.main(new String[]{uri.toString(), huri.toString()});
        } finally {
            hserver.stop(0);
        }
    }
}
