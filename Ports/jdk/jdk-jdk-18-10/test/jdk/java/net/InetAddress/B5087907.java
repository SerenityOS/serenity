/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5087907
 * @summary  InetAddress.getAllByName does not obey setting of java.net.preferIPv6Addresses
 * @run main/othervm -Djava.net.preferIPv6Addresses=false B5087907
 * @run main/othervm -Djava.net.preferIPv6Addresses=true B5087907
 * @run main/othervm B5087907
 */

import java.net.*;

public class B5087907 {

    public static void main(String args[]) {
        InetAddress lh = null;
        InetAddress addrs[] = null;
        try {
            lh = InetAddress.getByName("localhost");
            addrs = InetAddress.getAllByName("localhost");
        } catch (UnknownHostException e) {
            System.out.println ("Cant lookup localhost. cant run test");
            return;
        }

        boolean hasIPv4Address = false;
        boolean hasIPv6Address = false;
        for (InetAddress addr: addrs) {
            if (addr instanceof Inet4Address) {
                hasIPv4Address = true;
            }
            if (addr instanceof Inet6Address) {
                hasIPv6Address = true;
            }
            if (hasIPv4Address && hasIPv6Address) {
                break;
            }
        }

        String prop = System.getProperty("java.net.preferIPv6Addresses");
        boolean preferIPv6Addresses = (prop == null) ? false : prop.equals("true");

        System.out.println("java.net.preferIPv6Addresses: " + preferIPv6Addresses);
        System.out.println("localhost resolves to:");
        for (InetAddress addr: addrs) {
            System.out.println("  " + addr);
        }
        System.out.println("InetAddres.getByName returned: " + lh);

        boolean failed = false;
        if (preferIPv6Addresses && hasIPv6Address) {
            if (!(lh instanceof Inet6Address)) failed = true;
        }
        if (!preferIPv6Addresses && hasIPv4Address) {
            if (!(lh instanceof Inet4Address)) failed = true;
        }
        if (failed) {
            throw new RuntimeException("Test failed!");
        }
    }

}
