/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6411513
 * @summary java.net.DatagramSocket.receive: packet isn't received
 */

import java.net.*;
import java.util.*;

public class B6411513 {

    public static void main( String[] args ) throws Exception {
        Enumeration<NetworkInterface> nics = NetworkInterface.getNetworkInterfaces();
        while (nics.hasMoreElements()) {
            NetworkInterface nic = nics.nextElement();
            if (nic.isUp() && !nic.isVirtual()) {
                Enumeration<InetAddress> addrs = nic.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();

                    // Currently, seems there's a bug on Linux that one is
                    // unable to get IPv6 datagrams to be received by an
                    // IPv6 socket bound to any address except ::1. So filter
                    // out IPv6 address here. The test should be revisited
                    // later when aforementioned bug gets fixed.
                    if (addr instanceof Inet4Address) {
                        System.out.printf("%s : %s\n", nic.getName(), addr);
                        testConnectedUDP(addr);
                    }
                }
            }
        }
    }


    /*
     * Connect a UDP socket, disconnect it, then send and recv on it.
     * It will fail on Linux if we don't silently bind(2) again at the
     * end of DatagramSocket.disconnect().
     */
    private static void testConnectedUDP(InetAddress addr) throws Exception {
        try {
            DatagramSocket s = new DatagramSocket(0, addr);
            DatagramSocket ss = new DatagramSocket(0, addr);
            System.out.print("\tconnect...");
            s.connect(ss.getLocalAddress(), ss.getLocalPort());
            System.out.print("disconnect...");
            s.disconnect();

            System.out.println("local addr: " + s.getLocalAddress());
            System.out.println("local port: " + s.getLocalPort());

            byte[] data = { 0, 1, 2 };
            DatagramPacket p = new DatagramPacket(data, data.length,
                    s.getLocalAddress(), s.getLocalPort());
            s.setSoTimeout( 10000 );
            System.out.print("send...");
            s.send( p );
            System.out.print("recv...");
            s.receive( p );
            System.out.println("OK");

            ss.close();
            s.close();
        } catch( Exception e ){
            e.printStackTrace();
            throw e;
        }
    }
}
