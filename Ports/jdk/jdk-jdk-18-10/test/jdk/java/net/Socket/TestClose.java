/*
 * Copyright (c) 2001, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4408755
 * @library /test/lib
 * @summary This tests whether it's possible to get some informations
 *          out of a closed socket. This is for backward compatibility
 *          purposes.
 * @run main TestClose
 * @run main/othervm -Djava.net.preferIPv4Stack=true TestClose
 */

import java.net.*;
import jdk.test.lib.net.IPSupport;

public class TestClose {

    public static void main(String[] args) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        ServerSocket ss;
        Socket s;
        InetAddress ad1, ad2;
        int port1, port2, serverport;

        InetAddress loopback = InetAddress.getLoopbackAddress();
        ss = new ServerSocket();
        ss.bind(new InetSocketAddress(loopback, 0));
        serverport = ss.getLocalPort();
        s = new Socket(loopback, serverport);
        s.close();
        ss.close();
        ad1 = ss.getInetAddress();
        if (ad1 == null)
            throw new RuntimeException("ServerSocket.getInetAddress() returned null");
        port1 = ss.getLocalPort();
        if (port1 != serverport)
            throw new RuntimeException("ServerSocket.getLocalPort() returned the wrong value");
        ad2 = s.getInetAddress();
        if (ad2 == null)
            throw new RuntimeException("Socket.getInetAddress() returned null");
        port2 = s.getPort();
        if (port2 != serverport)
            throw new RuntimeException("Socket.getPort() returned wrong value");
        ad2 = s.getLocalAddress();
        if (ad2 == null)
            throw new RuntimeException("Socket.getLocalAddress() returned null");
        port2 = s.getLocalPort();
        if (port2 == -1)
            throw new RuntimeException("Socket.getLocalPort returned -1");
    }
}
