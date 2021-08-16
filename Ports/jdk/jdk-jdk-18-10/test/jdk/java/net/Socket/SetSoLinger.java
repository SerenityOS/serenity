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

/*
 * @test
 * @bug 4151834
 * @library /test/lib
 * @summary Test Socket.setSoLinger
 * @run main SetSoLinger
 * @run main/othervm -Djava.net.preferIPv4Stack=true SetSoLinger
 * @run main/othervm -Djava.net.preferIPv6Addresses=true SetSoLinger
 */

import java.net.*;
import jdk.test.lib.net.IPSupport;

public class SetSoLinger {
    static final int LINGER = 65546;

    public static void main(String args[]) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();

        int value;
        InetAddress addr = InetAddress.getLocalHost();
        ServerSocket ss = new ServerSocket();

        InetSocketAddress socketAddress = new InetSocketAddress(addr, 0);
        ss.bind(socketAddress);
        int port = ss.getLocalPort();

        Socket s = new Socket(addr, port);
        Socket soc = ss.accept();
        soc.setSoLinger(true, LINGER);
        value = soc.getSoLinger();
        soc.close();
        s.close();
        ss.close();

        if(value != 65535)
            throw new RuntimeException("Failed. Value not properly reduced.");
    }
}
