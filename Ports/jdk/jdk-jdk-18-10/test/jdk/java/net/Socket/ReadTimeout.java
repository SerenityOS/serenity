/*
 * Copyright (c) 1998, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4169831
 * @library /test/lib
 * @summary test timeout on a socket read
 * @run main/timeout=15 ReadTimeout
 * @run main/othervm/timeout=15 -Djava.net.preferIPv4Stack=true ReadTimeout
 */

import java.net.*;
import java.io.*;
import jdk.test.lib.net.IPSupport;

public class ReadTimeout  {
    public static void main(String args[]) throws Exception {
    IPSupport.throwSkippedExceptionIfNonOperational();

    InetAddress  sin = null;
    Socket       soc = null,soc1 = null;
    InputStream  is = null;
    OutputStream os = null;
    ServerSocket srv = null;
    int          port = 0;
    int          tout = 1000;

    sin = InetAddress.getLocalHost();
    srv = new ServerSocket();
    srv.bind(new InetSocketAddress(sin, 0));
    port = srv.getLocalPort();
    soc = new Socket(sin, port);
    soc1 = srv.accept();
    soc.setSoTimeout(tout);

    try {
      is = soc.getInputStream();
      os = soc1.getOutputStream();
      is.read();
    } catch(InterruptedIOException e) {
    } finally {
        soc.close();
        soc1.close();
        srv.close();
    }
  }
}
