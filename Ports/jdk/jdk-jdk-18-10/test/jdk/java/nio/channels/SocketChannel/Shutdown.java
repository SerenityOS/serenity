/*
 * Copyright (c) 2002, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4618960 4516760
 * @summary Test shutdownXXX and isInputShutdown
 */

import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

public class Shutdown {

    /**
     * Accept a connection, and close it immediately causing a hard reset.
     */
    static void acceptAndReset(ServerSocketChannel ssc) throws IOException {
        SocketChannel peer = ssc.accept();
        try {
            peer.setOption(StandardSocketOptions.SO_LINGER, 0);
            peer.configureBlocking(false);
            peer.write(ByteBuffer.wrap(new byte[128*1024]));
        } finally {
            peer.close();
        }
    }

    public static void main(String[] args) throws Exception {
        ServerSocketChannel ssc = ServerSocketChannel.open()
            .bind(new InetSocketAddress(0));
        try {
            InetAddress lh = InetAddress.getLocalHost();
            int port = ((InetSocketAddress)(ssc.getLocalAddress())).getPort();
            SocketAddress remote = new InetSocketAddress(lh, port);

            // Test SocketChannel shutdownXXX
            SocketChannel sc;
            sc = SocketChannel.open(remote);
            try {
                acceptAndReset(ssc);
                sc.shutdownInput();
                sc.shutdownOutput();
            } finally {
                sc.close();
            }

            // Test Socket adapter shutdownXXX and isShutdownInput
            sc = SocketChannel.open(remote);
            try {
                acceptAndReset(ssc);
                boolean before = sc.socket().isInputShutdown();
                sc.socket().shutdownInput();
                boolean after = sc.socket().isInputShutdown();
                if (before || !after)
                    throw new RuntimeException("Before and after test failed");
                sc.socket().shutdownOutput();
            } finally {
                sc.close();
            }
        } finally {
            ssc.close();
        }
    }
}
