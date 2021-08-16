/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6505016
 * @summary Socket spec should clarify what getInetAddress/getPort/etc return after the Socket is closed
 */

import java.net.*;
import java.io.*;

public class TestAfterClose
{
    static int failCount;

    public static void main(String[] args) {
        try {
            ServerSocket ss = new ServerSocket(0, 0, null);
            test(ss);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        if (failCount > 0)
            throw new RuntimeException("Failed: failcount = " + failCount);

    }

    static void test(ServerSocket ss) throws IOException {
        //Before Close
        InetAddress ssInetAddress = ss.getInetAddress();
        int ssLocalPort = ss.getLocalPort();
        SocketAddress ssLocalSocketAddress = ss.getLocalSocketAddress();

        //After Close
        ss.close();

        if (ssLocalPort != ss.getLocalPort()) {
            System.out.println("ServerSocket.getLocalPort failed");
            failCount++;
        }

        if (!ss.getInetAddress().equals(ssInetAddress)) {
            System.out.println("ServerSocket.getInetAddress failed");
            failCount++;
        }

        if (!ss.getLocalSocketAddress().equals(ssLocalSocketAddress)) {
            System.out.println("ServerSocket.getLocalSocketAddress failed");
            failCount++;
        }

        if (!ss.isBound()) {
            System.out.println("ServerSocket.isBound failed");
            failCount++;
        }

    }
}
