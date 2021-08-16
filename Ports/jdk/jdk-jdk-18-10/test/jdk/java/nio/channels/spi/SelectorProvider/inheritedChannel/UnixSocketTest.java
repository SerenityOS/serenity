/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.NetworkInterface;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Enumeration;

import static java.net.StandardProtocolFamily.UNIX;

public class UnixSocketTest {

    public static class Child1 {
        public static void main(String[] args) throws Exception {
            SocketChannel chan = (SocketChannel)System.inheritedChannel();
            ByteBuffer bb = ByteBuffer.allocate(2);
            bb.put((byte)'X');
            bb.put((byte)'Y');
            bb.flip();
            chan.write(bb);
            chan.close();
        }
    }

    public static class Child2 {
        public static void main(String[] args) throws Exception {
            ServerSocketChannel server = (ServerSocketChannel)System.inheritedChannel();
            SocketChannel chan = server.accept();
            UnixDomainSocketAddress sa = (UnixDomainSocketAddress)server.getLocalAddress();
            Files.delete(sa.getPath());
            server.close();
            ByteBuffer bb = ByteBuffer.allocate(2);
            bb.put((byte)'X');
            bb.put((byte)'Y');
            bb.flip();
            chan.write(bb);
            chan.close();
        }
    }

    public static void main(String args[]) throws Exception {
        SocketChannel sc = Launcher.launchWithUnixSocketChannel("UnixSocketTest$Child1");
        ByteBuffer bb = ByteBuffer.allocate(10);
        sc.read(bb);
        if (bb.get(0) != 'X') {
            System.exit(-2);
        }
        if (bb.get(1) != 'Y') {
            System.exit(-2);
        }
        sc.close();

        sc = Launcher.launchWithUnixServerSocketChannel("UnixSocketTest$Child2");
        bb.clear();
        sc.read(bb);
        if (bb.get(0) != 'X') {
            System.exit(-2);
        }
        if (bb.get(1) != 'Y') {
            System.exit(-2);
        }
    }
}
