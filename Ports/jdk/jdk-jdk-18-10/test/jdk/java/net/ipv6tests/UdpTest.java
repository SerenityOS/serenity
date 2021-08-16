/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4868820
 * @key intermittent
 * @summary IPv6 support for Windows XP and 2003 server.
 *          This test requires binding to the wildcard address and as such
 *          may fail intermittently on some platforms.
 * @library /test/lib
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 * @run main UdpTest -d
 */

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;

public class UdpTest extends Tests {
    static DatagramSocket c3, s1, s2, s3;
    static InetAddress s1peer, s2peer;

    static InetAddress ia4any;
    static InetAddress ia6any;
    static Inet6Address ia6addr;
    static InetAddress ia6bad; /* a global 6to4 IPv6 address, which cant be connected to */
    static InetAddress ia6rem1;
    static Inet4Address ia4addr;

    static {
        try {
            ia4any = InetAddress.getByName ("0.0.0.0");
            ia6any = InetAddress.getByName ("::0");
            try {
                ia6bad = InetAddress.getByName ("2002:819c:dc29:1:1322:33ff:fe44:5566%net0");
            } catch (Exception e) {
                ia6bad = InetAddress.getByName ("2002:819c:dc29:1:1322:33ff:fe44:5566");
            }
            //ia6rem1 = InetAddress.getByName ("fe80::a00:20ff:feed:b08d%eth0");
            //ia6rem1 = InetAddress.getByName ("129.156.220.63");
        } catch (Exception e) {
            e.printStackTrace();
        }
        ia6addr = getFirstLocalIPv6Address ();
        ia4addr = getFirstLocalIPv4Address ();
    }

    public static void main (String[] args) throws Exception {
        checkDebug(args);
        if (ia4addr == null) {
            System.out.println ("No local IPv4 addresses: exiting now");
            return;
        }
        if (ia6addr == null) {
            System.out.println ("No local IPv6 addresses: exiting now");
            return;
        }
        dprintln ("Local Addresses");
        dprintln (ia4addr.toString());
        dprintln (ia6addr.toString());
        test1 ();
        test2 ();
        if (!isLinux()) {
            test3 ();
        }
        test4 ();
    }

    /* basic UDP connectivity test using IPv6 only and IPv4/IPv6 together */

    static void test1 () throws Exception {
        System.out.println("Test1 starting");
        s1 = new DatagramSocket ();
        s2 = new DatagramSocket ();
        simpleDataExchange (s1, ia4addr, s2, ia4addr);
        s1.close (); s2.close ();

        /* IPv6 */
        s1 = new DatagramSocket ();
        s2 = new DatagramSocket ();
        simpleDataExchange (s1, ia6addr, s2, ia6addr);
        s1.close (); s2.close ();

        /* IPv6 only */
        s1 = new DatagramSocket (0, ia6addr);
        s2 = new DatagramSocket (0, ia6addr);
        simpleDataExchange (s1, ia6addr, s2, ia6addr);
        s1.close (); s2.close ();

        /* IPv6 and IPv4 */
        s1 = new DatagramSocket ();
        s2 = new DatagramSocket ();
        simpleDataExchange (s1, ia6addr, s2, ia4addr);
        s1.close (); s2.close ();

        /* listen on anyaddr and check receive from IPv4 and IPv6 */

        s1 = new DatagramSocket ();
        s2 = new DatagramSocket (0, ia6addr);
        s3 = new DatagramSocket (0, ia4addr);
        datagramEcho (s2, s1, ia6addr);
        datagramEcho (s3, s1, ia4addr);
        s1.close (); s2.close (); s3.close();

        System.out.println ("Test1: OK");
    }

    /* check timeouts on receive */

    static void test2 () throws Exception {
        System.out.println("Test2 starting");
        s1 = new DatagramSocket ();
        s2 = new DatagramSocket ();
        s1.setSoTimeout (4000);
        long t1 = System.currentTimeMillis();
        try {
            s1.receive (new DatagramPacket (new byte [128], 128));
            throw new Exception ("expected receive timeout ");
        } catch (SocketTimeoutException e) {
        }
        checkTime (System.currentTimeMillis() - t1, 4000);

        /* check data can be exchanged now */

        simpleDataExchange (s1, ia6addr, s2, ia4addr);

        /* double check timeout still works */
        t1 = System.currentTimeMillis();
        try {
            s1.receive (new DatagramPacket (new byte [128], 128));
            throw new Exception ("expected receive timeout ");
        } catch (SocketTimeoutException e) {
        }
        checkTime (System.currentTimeMillis() - t1, 4000);

        /* check receive works after a delay < timeout */

        final DatagramSocket s = s2;
        final InetAddress ia6 = ia6addr;
        final int port = s1.getLocalPort();

        s1.setSoTimeout(10000);
        runAfter (2000, new Runnable () {
            public void run () {
                try {
                    DatagramPacket p = new DatagramPacket ("Hello 123".getBytes(), 0, 8, ia6, port);
                    s.send (p);
                } catch (Exception e) {}
            }
        });
        t1 = System.currentTimeMillis();
        s1.receive (new DatagramPacket (new byte [128], 128));
        checkTime (System.currentTimeMillis() - t1, 2000, 10000);
        s1.close ();
        s2.close ();
        System.out.println ("Test2: OK");
    }

    /* check connected sockets */

    static void test3 () throws Exception {
        System.out.println("Test3 starting");
        s1 = new DatagramSocket ();
        s2 = new DatagramSocket ();
        s1.connect (ia6addr, s2.getLocalPort());
        datagramEcho (s1, s2, null);
        s1.close (); s2.close();
        System.out.println ("Test3: OK");
    }

    /* check PortUnreachable */

    static void test4 () throws Exception {
        System.out.println("Test4 starting");
        s1 = new DatagramSocket ();
        s1.connect (ia6addr, 5000);
        s1.setSoTimeout (3000);
        try {
            DatagramPacket p = new DatagramPacket ("HelloWorld".getBytes(), "HelloWorld".length());
            s1.send (p);
            p = new DatagramPacket (new byte[128], 128);
            s1.receive (p);
        } catch (PortUnreachableException e) {
            System.out.println ("Test4: OK");
            return;
        } catch (SocketTimeoutException e) {
            System.out.println ("Test4: failed. Never mind, it's an OS bug");
        }
    }

}
