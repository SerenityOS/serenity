/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4960962 6215050
   @summary Test if the registered SocketChannel can be closed immediately
   @run main/timeout=10 CloseRegisteredChannel
 */

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

public class CloseRegisteredChannel {
    public static void main(String[] args) throws Exception {
        ServerSocketChannel server = ServerSocketChannel.open();
        ServerSocket s = server.socket ();
        s.bind (new InetSocketAddress (0));
        int port = s.getLocalPort ();
        //System.out.println ("listening on port " + port);

        SocketChannel client = SocketChannel.open ();
        client.connect (new InetSocketAddress (InetAddress.getLoopbackAddress(), port));
        SocketChannel peer = server.accept();
        peer.configureBlocking(true);

        Selector selector = Selector.open ();
        client.configureBlocking (false);
        SelectionKey key = client.register (
            selector, SelectionKey.OP_READ, null
        );
        client.close();
        //System.out.println ("client.isOpen = " + client.isOpen());
        System.out.println ("Will hang here...");
        int nb = peer.read(ByteBuffer.allocate (1024));
        //System.out.println("read nb=" + nb);

        selector.close();
        server.close();
    }
}
