/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4507501
 * @library /test/lib
 * @summary Test various methods that should throw IAE when passed improper
 *          SocketAddress
 * @run main AddressTest
 * @run main/othervm -Djava.net.preferIPv4Stack=true AddressTest
 */

import java.net.*;
import jdk.test.lib.net.IPSupport;

public class AddressTest {
    class MySocketAddress extends SocketAddress {
        public MySocketAddress() {
        }
    }

    public AddressTest() throws Exception {
        SocketAddress addr = new MySocketAddress();
        Socket soc = new Socket();
        ServerSocket serv = new ServerSocket();
        DatagramSocket ds = new DatagramSocket((SocketAddress)null);
        DatagramPacket pac = new DatagramPacket(new byte[20], 20);
        MulticastSocket mul = new MulticastSocket((SocketAddress) null);
        boolean ok = false;
        try {
            soc.bind(addr);
        } catch (IllegalArgumentException e) {
            ok = true;
        } catch (Exception e2) {
        }
        if (!ok)
            throw new RuntimeException("Socket.bind should throw IllegalArgumentException!");

        ok = false;
        soc.close();
        soc = new Socket();
        try {
            soc.connect(addr, 100);
        } catch (IllegalArgumentException e) {
            ok = true;
        } catch (Exception e2) {
        }
        if (!ok)
            throw new RuntimeException("Socket.connect should throw IllegalArgumentException!");

        ok = false;
        try {
            serv.bind(addr);
        } catch (IllegalArgumentException e) {
            ok = true;
        } catch (Exception e2) {
        }
        if (!ok)
            throw new RuntimeException("ServerSocket.bind should throw IllegalArgumentException!");

        ok = false;

        try {
            pac.setSocketAddress(addr);
        } catch (IllegalArgumentException e) {
            ok = true;
        } catch (Exception e2) {
        }

        if (!ok)
            throw new RuntimeException("DatagramPacket.setSocketAddress should throw IllegalArgumentException");

        ok = false;

        try {
            ds.bind(addr);
        } catch (IllegalArgumentException e) {
            ok = true;
        } catch (Exception e2) {
        }

        if (!ok)
            throw new RuntimeException("DatagramSocket.bind should throw IllegalArgumentException");

        ok = false;

        try {
            ds.connect(addr);
        } catch (IllegalArgumentException e) {
            ok = true;
        } catch (Exception e2) {
        }

        if (!ok)
            throw new RuntimeException("DatagramSocket.connect should throw IllegalArgumentException");

        ok = false;

        try {
            mul.bind(addr);
        } catch (IllegalArgumentException e) {
            ok = true;
        } catch (Exception e2) {
        }

        if (!ok)
            throw new RuntimeException("MulticastSocket.bind should throw IllegalArgumentException");

        ok = false;

        mul.close();
        mul = new MulticastSocket(new InetSocketAddress(0));
        try {
            mul.joinGroup(addr, null);
        } catch (IllegalArgumentException e) {
            ok = true;
        } catch (Exception e2) {
        }

        if (!ok)
            throw new RuntimeException("MulticastSocket.joinGroup should throw IllegalArgumentException");

        ok = false;
        try {
            mul.leaveGroup(addr, null);
        } catch (IllegalArgumentException e) {
            ok = true;
        } catch (Exception e2) {
        }

        if (!ok)
            throw new RuntimeException("MulticastSocket.leaveGroup should throw IllegalArgumentException");

    }

    public static void main(String[] args) throws Exception {
        IPSupport.throwSkippedExceptionIfNonOperational();
        new AddressTest();
    }
}
