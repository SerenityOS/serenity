/*
 * Copyright (c) 2002, 2010, Oracle and/or its affiliates. All rights reserved.
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

/**
 * Utility class for tests. A simple "in-thread" server to accept connections
 * and write bytes.
 * @author kladko
 */

import java.net.Socket;
import java.net.ServerSocket;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.io.IOException;
import java.io.Closeable;

public class ByteServer implements Closeable {

    private final ServerSocket ss;
    private Socket s;

    ByteServer() throws IOException {
        this.ss = new ServerSocket(0);
    }

    SocketAddress address() {
        return new InetSocketAddress(ss.getInetAddress(), ss.getLocalPort());
    }

    void acceptConnection() throws IOException {
        if (s != null)
            throw new IllegalStateException("already connected");
        this.s = ss.accept();
    }

    void closeConnection() throws IOException {
        Socket s = this.s;
        if (s != null) {
            this.s = null;
            s.close();
        }
    }

    void write(int count) throws IOException {
        if (s == null)
            throw new IllegalStateException("no connection");
        s.getOutputStream().write(new byte[count]);
    }

    public void close() throws IOException {
        if (s != null)
            s.close();
        ss.close();
    }
}
