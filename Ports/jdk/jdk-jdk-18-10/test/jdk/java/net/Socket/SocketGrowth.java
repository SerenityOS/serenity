/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7181793
 * @summary getOutputStream create streams that cannot be GC'ed until Socket is closed
 * @run main/othervm -Xmx32m SocketGrowth
 */

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketGrowth {

    public static void main(String[] args) throws IOException {
        InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
        try (ServerSocket ss = new ServerSocket(0, 0, loopbackAddress)) {
            try (Socket s = new Socket(loopbackAddress, ss.getLocalPort());
                    Socket peer = ss.accept()) {
                for (int i=0; i<1000000; i++) {
                    // buggy JDK will run out of memory in this loop
                    s.getOutputStream();
                    // test InputStream also while we're here
                    s.getInputStream();
                    if (i % 100000 == 0) System.out.println(i);
                }
            }
        }
    }
}
