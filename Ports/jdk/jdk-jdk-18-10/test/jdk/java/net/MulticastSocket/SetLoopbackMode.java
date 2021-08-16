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
 * @bug 4686717
 * @summary Test MulticastSocket.setLoopbackMode
 * @library /test/lib
 * @modules java.base/java.net:+open
 * @build jdk.test.lib.NetworkConfiguration
 *        jdk.test.lib.Platform
 * @run main/othervm SetLoopbackMode
 */

import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.*;
import java.io.IOException;
import jdk.test.lib.NetworkConfiguration;

public class SetLoopbackMode {

    static final boolean FAILED = true;
    static final boolean PASSED = false;

    static boolean test(MulticastSocket mc, InetAddress grp) throws IOException {

        boolean disabled = mc.getLoopbackMode();

        if (disabled) {
            System.out.println("Loopback mode is disabled.");
        } else {
            System.out.println("Loopback mode is enabled.");
        }

        System.out.println(mc.getLocalSocketAddress());

        byte b[] = "hello".getBytes();
        DatagramPacket p = new DatagramPacket(b, b.length, grp,
                                mc.getLocalPort());
        mc.send(p);

        boolean gotPacket = false;

        mc.setSoTimeout(1000);
        try {
            b = new byte[16];
            p = new DatagramPacket(b, b.length);
            mc.receive(p);
            gotPacket = true;

            /* purge any additional copies of the packet */
            for (;;) {
                mc.receive(p);
            }

        } catch (SocketTimeoutException x) {
        }

        if (gotPacket && disabled) {
            System.out.println("Packet received (unexpected as loopback is disabled)");
            return FAILED;
        }
        if (!gotPacket && !disabled) {
            System.out.println
                ("Packet not received (packet excepted as loopback is enabled)");
            return FAILED;
        }

        if (gotPacket && !disabled) {
            System.out.println("Packet received - correct.");
        } else {
            System.out.println("Packet not received - correct.");
        }

        return PASSED;
    }

    private static boolean canUseIPv6(NetworkConfiguration nc) {
        return nc.ip6MulticastInterfaces().toArray().length > 0;
    }

    public static void main (String args[]) throws Exception {
        int failures = 0;
        NetworkConfiguration nc = NetworkConfiguration.probe();

        try (MulticastSocket mc = new MulticastSocket()) {
            InetAddress grp = InetAddress.getByName("224.80.80.80");


            /*
             * If IPv6 is available then use IPv6 multicast group - needed
             * to workaround Linux IPv6 bug whereby !IPV6_MULTICAST_LOOP
             * doesn't prevent loopback of IPv4 multicast packets.
             */

            if (canUseIPv6(nc)) {
                System.out.println("IPv6 can be used");
                grp = InetAddress.getByName("ff01::1");
            } else {
                System.out.println("IPv6 cannot be used: using IPv4");
            }
            System.out.println("Default network interface: " + DefaultInterface.getDefaultName());
            System.out.println("\nTest will use multicast group: " + grp);
            try {
                System.out.println("NetworkInterface.getByInetAddress(grp): "
                        + getName(NetworkInterface.getByInetAddress(grp)));
            } catch (Exception x) {
                // OK
            }
            mc.joinGroup(grp);

            System.out.println("\n******************\n");

            mc.setLoopbackMode(true);
            if (test(mc, grp) == FAILED) failures++;

            System.out.println("\n******************\n");

            mc.setLoopbackMode(false);
            if (test(mc, grp) == FAILED) failures++;

            System.out.println("\n******************\n");

            if (failures > 0) {
                throw new RuntimeException("Test failed");
            }
        }
    }

    static String getName(NetworkInterface nif) {
        return nif == null ? null : nif.getName();
    }

    static class DefaultInterface {
        static final Method GET_DEFAULT;
        static {
            try {
                GET_DEFAULT = Class.forName("java.net.DefaultInterface")
                        .getDeclaredMethod("getDefault");
                GET_DEFAULT.setAccessible(true);
            } catch (Exception x) {
                throw new ExceptionInInitializerError(x);
            }
        }
        static NetworkInterface getDefault() {
            try {
                return (NetworkInterface) GET_DEFAULT
                        .invoke(null);
            } catch (Exception x) {
                throw new UndeclaredThrowableException(x);
            }
        }
        static String getDefaultName() {
            return getName(getDefault());
        }
    }
}
