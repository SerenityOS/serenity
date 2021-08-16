/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6718504
 * @summary IN6_IS_ADDR_ANY tests only 12 bytes of 16-byte address
 */

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;

public class LocalSocketAddress  {
    public static void main(String[] args) throws SocketException {
        InetAddress IPv6LoopbackAddr = null;
        DatagramSocket soc = null;

        try {
            List<NetworkInterface> nics = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nic : nics) {
                if (!nic.isLoopback())
                    continue;

                List<InetAddress> addrs = Collections.list(nic.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (addr instanceof Inet6Address) {
                        IPv6LoopbackAddr = addr;
                        break;
                    }
                }
            }

            if (IPv6LoopbackAddr == null) {
                System.out.println("IPv6 is not available, exiting test.");
                return;
            }

            soc = new DatagramSocket(0, IPv6LoopbackAddr);

            if (!IPv6LoopbackAddr.equals(soc.getLocalAddress())) {
                throw new RuntimeException("Bound address is " + soc.getLocalAddress() +
                                   ", but should be " + IPv6LoopbackAddr);
            }
        }  finally {
            if (soc != null) { soc.close(); }
        }
    }
}
