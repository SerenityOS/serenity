/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4468875
 * @summary Simple test of DatagramChannel isBound
 * @library .. /test/lib
 * @build jdk.test.lib.Utils TestServers
 * @run main IsBound
 */

import java.net.*;
import java.nio.*;
import java.nio.channels.*;


public class IsBound {
    public static void main(String argv[]) throws Exception {
        try (TestServers.UdpDayTimeServer daytimeServer
                = TestServers.UdpDayTimeServer.startNewServer(100)) {
            InetSocketAddress isa = new InetSocketAddress(
                daytimeServer.getAddress(),
                daytimeServer.getPort());
            ByteBuffer bb = ByteBuffer.allocateDirect(256);
            bb.put("hello".getBytes());
            bb.flip();

            DatagramChannel dc = DatagramChannel.open();
            dc.send(bb, isa);
            if(!dc.socket().isBound())
                throw new Exception("Test failed");
            dc.close();

            dc = DatagramChannel.open();
            if(dc.socket().isBound())
                throw new Exception("Test failed");
            dc.close();
        }
    }
}
