/*
 * Copyright (c) 2000, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 4158021
 * @library /test/lib
 * @summary cannot distinguish Thread.interrupt and Socket.setSoTimeout exceptions
 * @run main SocketTimeout
 * @run main/othervm -Djava.net.preferIPv4Stack=true SocketTimeout
 */

import java.net.*;
import java.io.*;
import jdk.test.lib.net.IPSupport;

public class SocketTimeout  {
    static final int TIMEOUT = 1000;

    public static void main(String args[]) throws Exception {
    IPSupport.throwSkippedExceptionIfNonOperational();
    InetAddress  sin = InetAddress.getLocalHost();
    Socket       soc = null,soc1 = null;
    InputStream  is = null;
    ServerSocket srv = null;
    int          port = 0;

    srv = new ServerSocket();
    srv.bind(new InetSocketAddress(sin, 0));
    port = srv.getLocalPort();
    soc = new Socket(sin, port);
    soc1 = srv.accept();
    soc.setSoTimeout(TIMEOUT);
    srv.setSoTimeout(TIMEOUT);

    try {
      is = soc.getInputStream();
      is.read();
    } catch(InterruptedIOException e) {
        try {
            if (! (e instanceof java.net.SocketTimeoutException))
                throw new Exception ("Wrong exception class thrown");
        } catch(NoClassDefFoundError e1) {
            throw new Exception ("SocketTimeoutException: not found");
        }
    } finally {
        soc.close();
        soc1.close();
    }

    // now check accept

    try {
      srv.accept ();
    } catch(InterruptedIOException e) {
        try {
            if (! (e instanceof java.net.SocketTimeoutException))
                throw new Exception ("Wrong exception class thrown");
        } catch(NoClassDefFoundError e1) {
            throw new Exception ("SocketTimeoutException: not found");
        }
    } finally {
        srv.close();
    }

    // Now check DatagramSocket.receive()

    DatagramSocket dg = new DatagramSocket ();
    dg.setSoTimeout (TIMEOUT);

    try {
      dg.receive (new DatagramPacket (new byte [64], 64));
    } catch(InterruptedIOException e) {
        try {
            if (! (e instanceof java.net.SocketTimeoutException))
                throw new Exception ("Wrong exception class thrown");
        } catch(NoClassDefFoundError e1) {
            throw new Exception ("SocketTimeoutException: not found");
        }
    } finally {
        dg.close();
    }
  }
}
